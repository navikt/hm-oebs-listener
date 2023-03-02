package no.nav.hjelpemidler.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.Configuration
import no.nav.hjelpemidler.model.OrdrelinjeMessage
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.RåOrdrelinje
import no.nav.hjelpemidler.model.UvalidertOrdrelinjeMessage
import no.nav.hjelpemidler.model.erOpprettetFraHOTSAK
import no.nav.hjelpemidler.model.fiksTommeSerienumre
import no.nav.hjelpemidler.model.toRåOrdrelinje
import opprettHotsakOrdrelinje
import opprettInfotrygdOrdrelinje
import parseHotsakOrdrelinje
import parseInfotrygdOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
private val mapperXml = XmlMapper().registerModule(JavaTimeModule()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

internal fun Route.ordrelinjeAPI(context: Context) {
    post("/push") {
        logg.info("incoming push")
        try {
            // Parse innkommende json/xml
            val ordrelinje = parseOrdrelinje(context, call) ?:
                return@post call.respond(HttpStatusCode.BadRequest, "request body was not in a valid format")

            // Vi deler alle typer ordrelinjer med kommune-apiet
            sendUvalidertOrdrelinjeTilRapid(context, ordrelinje.toRåOrdrelinje())

            // Avslutt tidlig hvis ordrelinjen ikke er relevant for oss
            if (!erOrdrelinjeRelevantForOss(context, ordrelinje)) {
                logg.info("Urelevant ordrelinje mottatt og ignorert")
                call.respond(HttpStatusCode.OK)
                return@post
            }

            // Anti-corruption lag
            val melding = if (ordrelinje.erOpprettetFraHOTSAK()) {
                parseHotsakOrdrelinje(context, ordrelinje)
                context.metrics.hotsakSF()
                opprettHotsakOrdrelinje(ordrelinje)
            } else {
                parseInfotrygdOrdrelinje(context, ordrelinje)
                context.metrics.infotrygdSF()
                opprettInfotrygdOrdrelinje(ordrelinje)
            }

            // Publiser resultat
            publiserMelding(context, ordrelinje, melding)
            call.respond(HttpStatusCode.OK)

        } catch (e: Exception) {
            logg.error(e) { "Uventet feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }
    }
}

private suspend fun parseOrdrelinje(context: Context, call: ApplicationCall): OrdrelinjeOebs? {
    var incomingFormatType = "JSON"
    if (call.request.header("Content-Type").toString().contains("application/xml")) {
        incomingFormatType = "XML"
    }

    val requestBody: String = call.receiveText()
    context.metrics.meldingFraOebs()
    if (Configuration.profile != Configuration.Profile.PROD) {
        sikkerlogg.info("Received $incomingFormatType push request from OEBS: $requestBody")
    }

    // Check for valid json request
    val ordrelinje: OrdrelinjeOebs
    try {
        ordrelinje = if (incomingFormatType == "XML") {
            mapperXml.readValue<OrdrelinjeOebs>(requestBody)
        } else {
            mapperJson.readValue<OrdrelinjeOebs>(requestBody)
        }.fiksTommeSerienumre()

        if (Configuration.profile != Configuration.Profile.PROD) {
            sikkerlogg.info(
                "Parsing incoming $incomingFormatType request successful: ${
                    mapperJson.writeValueAsString(
                        ordrelinje
                    )
                }"
            )
        }
        context.metrics.oebsParsingOk()
        return ordrelinje
    } catch (e: Exception) {
        // Deal with invalid json/xml in request
        withLoggingContext(
            mapOf(
                "rawRequestBody" to requestBody,
            )
        ) {
            sikkerlogg.info("Parsing incoming $incomingFormatType request failed with exception (responding 4xx): $e")
            if (Configuration.profile != Configuration.Profile.PROD) {
                sikkerlogg.info(
                    "$incomingFormatType in failed parsing: ${mapperJson.writeValueAsString(requestBody)}"
                )
            }
        }
        context.metrics.oebsParsingFeilet()
        return null
    }
}

private fun sendUvalidertOrdrelinjeTilRapid(context: Context, ordrelinje: RåOrdrelinje) {
    try {
        logg.info("Publiserer uvalidert ordrelinje med OebsId ${ordrelinje.oebsId} til rapid i miljø ${Configuration.profile}")
        context.publish(
            ordrelinje.fnrBruker, mapperJson.writeValueAsString(
                UvalidertOrdrelinjeMessage(
                    eventId = UUID.randomUUID(),
                    eventName = "hm-uvalidert-ordrelinje",
                    eventCreated = LocalDateTime.now(),
                    orderLine = ordrelinje,
                )
            )
        )
        context.metrics.meldingTilRapidSuksess()
    } catch (e: Exception) {
        context.metrics.meldingTilRapidFeilet()
        sikkerlogg.error("Sending av uvalidert ordrelinje til rapid feilet, exception: $e\n\n${e.printStackTrace()}")
        throw RapidsAndRiverException("Noe gikk feil ved publisering av melding")
    }
}

private fun erOrdrelinjeRelevantForOss(context: Context, ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.serviceforespørseltype != "Vedtak Infotrygd") {
        if (ordrelinje.serviceforespørseltype == "") {
            logg.info(
                "Mottok melding fra oebs som ikke er en SF. Avbryter prosesseringen og returnerer"
            )
            context.metrics.sfTypeBlank()
        } else {
            logg.info(
                "Mottok melding fra oebs med sf-type ${ordrelinje.serviceforespørseltype} og sf-status ${ordrelinje.serviceforespørselstatus}. " +
                        "Avbryter prosesseringen og returnerer"
            )
            context.metrics.sfTypeUlikVedtakInfotrygd()
        }
        return false
    } else {
        context.metrics.sfTypeVedtakInfotrygd()
    }

    if (ordrelinje.hjelpemiddeltype != "Hjelpemiddel" &&
        ordrelinje.hjelpemiddeltype != "Individstyrt hjelpemiddel" &&
        ordrelinje.hjelpemiddeltype != "Del"
    ) {
        logg.info("Mottok melding fra oebs med irrelevant hjelpemiddeltype ${ordrelinje.hjelpemiddeltype}. Avbryter prosessering")
        context.metrics.irrelevantHjelpemiddeltype()
        return false
    } else {
        context.metrics.rettHjelpemiddeltype()
    }

    return true
}

private fun publiserMelding(
    context: Context,
    ordrelinje: OrdrelinjeOebs,
    melding: OrdrelinjeMessage,
) {
    try {
        logg.info("Publiserer ordrelinje med OebsId ${ordrelinje.oebsId} til rapid i miljø ${Configuration.profile}")
        context.publish(ordrelinje.fnrBruker, mapperJson.writeValueAsString(melding))
        context.metrics.meldingTilRapidSuksess()

        // TODO: Remove logging when interface stabilizes
        ordrelinje.fnrBruker = "MASKERT"
        if (ordrelinje.sendtTilAdresse.take(4).toIntOrNull() == null) {
            // If address is not municipality-intake we mask it in logging.
            ordrelinje.sendtTilAdresse = "MASKERT"
        }
        sikkerlogg.info(
            "Ordrelinje med OebsId ${ordrelinje.oebsId} mottatt og sendt til rapid: ${
                mapperJson.writeValueAsString(
                    ordrelinje
                )
            }"
        )
    } catch (e: Exception) {
        context.metrics.meldingTilRapidFeilet()
        sikkerlogg.error("Sending til rapid feilet", e)
        throw RapidsAndRiverException("Noe gikk feil ved publisering av melding")
    }
}

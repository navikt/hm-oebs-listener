package no.nav.hjelpemidler.api

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
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.model.OrdrelinjeMessage
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.RåOrdrelinje
import no.nav.hjelpemidler.model.UvalidertOrdrelinjeMessage
import no.nav.hjelpemidler.model.erOpprettetFraHOTSAK
import no.nav.hjelpemidler.model.toRåOrdrelinje
import opprettHotsakOrdrelinje
import opprettInfotrygdOrdrelinje
import parseHotsakOrdrelinje
import parseInfotrygdOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())
private val mapperXml = XmlMapper().registerModule(JavaTimeModule())

internal fun Route.ordrelinjeAPI(context: Context) {
    post("/push") {
        logg.info("incoming push")
        try {
            val ordrelinje = parseOrdrelinje(context, call) ?: return@post
            sendUvalidertOrdrelinjeTilRapid(context, ordrelinje.toRåOrdrelinje())
            validerOrdrelinje(context, ordrelinje)
            val melding = if (ordrelinje.erOpprettetFraHOTSAK()) {
                parseHotsakOrdrelinje(context, ordrelinje)
                context.metrics.hotsakSF()
                opprettHotsakOrdrelinje(ordrelinje)
            } else {
                parseInfotrygdOrdrelinje(context, ordrelinje)
                context.metrics.infotrygdSF()
                opprettInfotrygdOrdrelinje(ordrelinje)
            }

            publiserMelding(context, ordrelinje, melding)
            call.respond(HttpStatusCode.OK)
        } catch (e: RapidsAndRiverException) {
            logg.error(e) { "Feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
            return@post
        } catch (e: RuntimeException) {
            logg.error(e) { "Feil under prosessering" }
            call.respond(HttpStatusCode.OK)
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
    if (Configuration.application["APP_PROFILE"] != "prod") {
        sikkerlogg.info("Received $incomingFormatType push request from OEBS: $requestBody")
    }

    // Check for valid json request
    val ordrelinje: OrdrelinjeOebs
    try {
        ordrelinje = if (incomingFormatType == "XML") {
            mapperXml.readValue(requestBody)
        } else {
            mapperJson.readValue(requestBody)
        }
        if (Configuration.application["APP_PROFILE"] != "prod") {
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
        sikkerlogg.info("Parsing incoming $incomingFormatType request failed with exception (responding 4xx): $e")
        if (Configuration.application["APP_PROFILE"] != "prod") {
            sikkerlogg.info(
                "$incomingFormatType in failed parsing: ${mapperJson.writeValueAsString(requestBody)}"
            )
        }
        context.metrics.oebsParsingFeilet()
        call.respond(HttpStatusCode.BadRequest, "bad request: $incomingFormatType not valid")
        return null
    }
}

private fun sendUvalidertOrdrelinjeTilRapid(context: Context, ordrelinje: RåOrdrelinje) {
    try {
        logg.info("Publiserer uvalidert ordrelinje med OebsId ${ordrelinje.oebsId} til rapid i miljø ${Configuration.application["APP_PROFILE"]}")
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

private fun validerOrdrelinje(context: Context, ordrelinje: OrdrelinjeOebs) {
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
        throw RuntimeException("Ugyldig ordrelinje")
    } else {
        context.metrics.sfTypeVedtakInfotrygd()
    }

    if (ordrelinje.hjelpemiddeltype != "Hjelpemiddel" &&
        ordrelinje.hjelpemiddeltype != "Individstyrt hjelpemiddel" &&
        ordrelinje.hjelpemiddeltype != "Del"
    ) {
        logg.info("Mottok melding fra oebs med irrelevant hjelpemiddeltype ${ordrelinje.hjelpemiddeltype}. Avbryter prosessering")
        context.metrics.irrelevantHjelpemiddeltype()
        throw RuntimeException("Ugyldig ordrelinje")
    } else {
        context.metrics.rettHjelpemiddeltype()
    }
}

private fun publiserMelding(
    context: Context,
    ordrelinje: OrdrelinjeOebs,
    melding: OrdrelinjeMessage,
) {
    try {
        logg.info("Publiserer ordrelinje med OebsId ${ordrelinje.oebsId} til rapid i miljø ${Configuration.application["APP_PROFILE"]}")
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
        sikkerlogg.error("Sending til rapid feilet, exception: $e")
        throw RapidsAndRiverException("Noe gikk feil ved publisering av melding")
    }
}

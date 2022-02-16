package no.nav.hjelpemidler.api

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import mu.KotlinLogging
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.model.OrdrelinjeMessage
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.erOpprettetFraHOTSAK
import opprettHotsakOrdrelinje
import opprettInfotrygdOrdrelinje
import parseHotsakOrdrelinje
import parseInfotrygdOrdrelinje

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())
private val mapperXml = XmlMapper().registerModule(JavaTimeModule())

internal fun Route.ordrelinjeAPI(context: Context) {
    post("/push") {
        logg.info("incoming push")
        val authHeader = call.request.header("Authorization").toString()
        if (!authHeader.startsWith("Bearer ") || authHeader.substring(7) != Configuration.application["OEBSTOKEN"]!!) {
            call.respond(HttpStatusCode.Unauthorized, "unauthorized")
            return@post
        }

        try {
            val ordrelinje = parseOrdrelinje(context, call) ?: return@post
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
            call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
            return@post
        } catch (e: RuntimeException) {
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
        ordrelinje.hjelpemiddeltype != "Individstyrt hjelpemiddel"
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

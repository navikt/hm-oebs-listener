package no.nav.hjelpemidler.oebs.listener.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.oebs.listener.Context
import no.nav.hjelpemidler.oebs.listener.jsonMapper
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.model.RåOrdrelinje
import no.nav.hjelpemidler.oebs.listener.model.UvalidertOrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.erOpprettetFraHotsak
import no.nav.hjelpemidler.oebs.listener.model.fiksTommeSerienumre
import no.nav.hjelpemidler.oebs.listener.model.toRåOrdrelinje
import no.nav.hjelpemidler.oebs.listener.xmlMapper
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger {}
private val secureLog = KotlinLogging.logger("tjenestekall")

fun Route.ordrelinjeAPI(context: Context) {
    post("/push") {
        log.info { "incoming push" }
        try {
            // Parse innkommende json/xml
            val ordrelinje =
                parseOrdrelinje(call)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "request body was not in a valid format")

            if (ordrelinje.skipningsinstrukser?.contains("Tekniker") == true) {
                secureLog.info { "Delbestilling ordrelinje: <$ordrelinje>" }
            }

            // Vi deler alle typer ordrelinjer med delbestilling (som sjekker på ordrenummer) og kommune-apiet
            sendUvalidertOrdrelinjeTilRapid(context, ordrelinje.toRåOrdrelinje())

            // Avslutt tidlig hvis ordrelinjen ikke er relevant for oss
            if (!erOrdrelinjeRelevantForHotsak(ordrelinje)) {
                log.info { "Urelevant ordrelinje mottatt og ignorert" }
                call.respond(HttpStatusCode.OK)
                return@post
            }

            // Anti-corruption lag
            val melding =
                if (ordrelinje.erOpprettetFraHotsak()) {
                    if (!hotsakOrdrelinjeOK(ordrelinje)) {
                        log.info { "Hotsak ordrelinje mottatt som ikke passerer validering. Logger til slack og ignorerer.." }
                        call.respond(HttpStatusCode.OK)
                        return@post
                    }
                    if (ordrelinje.hotSakSaksnummer?.startsWith("hmdel_") == true) {
                        log.info { "Ordrelinje fra delebestilling mottatt. Ignorer." }
                        secureLog.info { "Ignorert ordrelinje for delebestilling: $ordrelinje" }
                        return@post call.respond(HttpStatusCode.OK)
                    }
                    opprettHotsakOrdrelinje(ordrelinje)
                } else {
                    if (!infotrygdOrdrelinjeOK(ordrelinje)) {
                        log.warn { "Infotrygd ordrelinje mottatt som ikke passerer validering. Logger til slack og ignorerer.." }
                        call.respond(HttpStatusCode.OK)
                        return@post
                    }
                    opprettInfotrygdOrdrelinje(ordrelinje)
                }

            // Publiser resultat
            publiserMelding(context, ordrelinje, melding)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error(e) { "Uventet feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }
    }
}

private suspend fun parseOrdrelinje(call: ApplicationCall): OrdrelinjeOebs? {
    var incomingFormatType = "JSON"
    if (call.request.header("Content-Type").toString().contains("application/xml")) {
        incomingFormatType = "XML"
    }

    val requestBody: String = call.receiveText()
    if (!Environment.current.tier.isProd) {
        withLoggingContextAsync(
            mapOf(
                "requestBody" to requestBody,
            ),
        ) {
            secureLog.info { "Received $incomingFormatType push request from OEBS" }
        }
    }

    // Check for valid json request
    val ordrelinje: OrdrelinjeOebs
    try {
        ordrelinje =
            if (incomingFormatType == "XML") {
                xmlMapper.readValue<OrdrelinjeOebs>(requestBody)
            } else {
                jsonMapper.readValue<OrdrelinjeOebs>(requestBody)
            }.fiksTommeSerienumre()

        if (!Environment.current.tier.isProd) {
            withLoggingContextAsync(
                mapOf(
                    "ordrelinje" to jsonMapper.writeValueAsString(ordrelinje),
                ),
            ) {
                secureLog.info { "Parsing incoming $incomingFormatType request successful" }
            }
        }
        return ordrelinje
    } catch (e: Exception) {
        // Deal with invalid json/xml in request
        withLoggingContextAsync(
            mapOf(
                "requestBody" to requestBody,
            ),
        ) {
            secureLog.error(e) { "Parsing incoming $incomingFormatType request failed with exception (responding 4xx)" }
        }
        return null
    }
}

private fun sendUvalidertOrdrelinjeTilRapid(
    context: Context,
    ordrelinje: RåOrdrelinje,
) {
    try {
        log.info {
            buildString {
                append("Publiserer uvalidert ordrelinje med oebsId: ")
                append(ordrelinje.oebsId)
                append(" og ordrenr: ")
                append(ordrelinje.ordrenr)
                append(" til rapid i miljø: ")
                append(Environment.current)
            }
        }
        context.publish(
            ordrelinje.fnrBruker,
            UvalidertOrdrelinjeMessage(
                eventId = UUID.randomUUID(),
                eventName = "hm-uvalidert-ordrelinje",
                eventCreated = LocalDateTime.now(),
                orderLine = ordrelinje,
            ),
        )
    } catch (e: Exception) {
        secureLog.error(e) { "Sending av uvalidert ordrelinje til rapid feilet" }
        error("Noe gikk feil ved publisering av melding")
    }
}

private fun erOrdrelinjeRelevantForHotsak(ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.serviceforespørseltype != "Vedtak Infotrygd") {
        if (ordrelinje.serviceforespørseltype == "") {
            log.info { "Mottok melding fra OEBS som ikke er en SF. Avbryter prosesseringen og returnerer" }
        } else {
            log.info {
                buildString {
                    append("Mottok melding fra oebs med serviceforespørseltype: ")
                    append(ordrelinje.serviceforespørseltype)
                    append(" og serviceforespørselstatus: ")
                    append(ordrelinje.serviceforespørselstatus)
                    append(". Avbryter prosesseringen og returnerer")
                }
            }
        }
        return false
    }

    if (ordrelinje.hjelpemiddeltype != "Hjelpemiddel" &&
        ordrelinje.hjelpemiddeltype != "Individstyrt hjelpemiddel" &&
        ordrelinje.hjelpemiddeltype != "Del"
    ) {
        log.info { "Mottok melding fra OEBS med irrelevant hjelpemiddeltype: ${ordrelinje.hjelpemiddeltype}. Avbryter prosessering" }
        return false
    }

    return true
}

private fun publiserMelding(
    context: Context,
    ordrelinje: OrdrelinjeOebs,
    melding: OrdrelinjeMessage,
) {
    try {
        log.info { "Publiserer ordrelinje med oebsId: ${ordrelinje.oebsId} til rapid i miljø: ${Environment.current}" }
        context.publish(ordrelinje.fnrBruker, melding)

        ordrelinje.fnrBruker = "MASKERT"
        if (ordrelinje.sendtTilAdresse.take(4).toIntOrNull() == null) {
            // If address is not municipality-intake we mask it in logging.
            ordrelinje.sendtTilAdresse = "MASKERT"
        }
        withLoggingContext(
            mapOf(
                "ordrelinje" to jsonMapper.writeValueAsString(ordrelinje),
            ),
        ) {
            secureLog.info { "Ordrelinje med oebsId: ${ordrelinje.oebsId} mottatt og sendt til rapid" }
        }
    } catch (e: Exception) {
        secureLog.error(e) { "Sending til rapid feilet" }
        error("Noe gikk feil ved publisering av melding")
    }
}

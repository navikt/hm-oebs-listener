package no.nav.hjelpemidler.oebs.listener.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.logging.teamError
import no.nav.hjelpemidler.logging.teamInfo
import no.nav.hjelpemidler.logging.teamWarn
import no.nav.hjelpemidler.oebs.listener.Context
import no.nav.hjelpemidler.oebs.listener.Slack
import no.nav.hjelpemidler.oebs.listener.model.HotsakOrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.InfotrygdOrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.model.RåOrdrelinje
import no.nav.hjelpemidler.oebs.listener.model.UvalidertOrdrelinjeMessage
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.serialization.jackson.jsonToValue

private val log = KotlinLogging.logger {}

fun Route.ordrelinjeAPI(context: Context) {
    post("/push") {
        log.info { "Innkommende ordrelinje" }
        try {
            val innkommendeOrdrelinje = call.receiveText()
            val ordrelinje = jsonToValue<OrdrelinjeOebs>(innkommendeOrdrelinje).fiksTommeSerienumre()

            if (ordrelinje.mottakendeSystem.trim() != "DIGIHOT") {
                log.warn { "System \"${ordrelinje.mottakendeSystem}\" ikke støttet enda, stopper prosessering her!" }
                call.respond(HttpStatusCode.OK)
                return@post
            }

            // Logg innkommende ordrelinjer fra OEBS i dev i et rått-format slik at vi kan feilsøke når OEBS gjør
            // endringer som vi etterhvert skal oppdatere OrdrelinjeOebs-typen med.
            if (Environment.current.isDev) {
                withLoggingContext(
                    mapOf(
                        "ordrelinje" to innkommendeOrdrelinje,
                        "utlånsstatistikk" to jsonMapper.writeValueAsString(ordrelinje.utlånsstatistikk()),
                    ),
                ) {
                    log.info { "Innkommende ordrelinje fra OEBS" }
                }
            }

            if (ordrelinje.skipningsinstrukser?.contains("Tekniker") == true) {
                log.teamInfo { "Delbestilling ordrelinje: '$ordrelinje'" }
            }

            // Vi deler alle typer ordrelinjer med delbestilling (som sjekker på ordrenummer) og kommune-API-et
            sendUvalidertOrdrelinjeTilKafka(context, RåOrdrelinje(ordrelinje))

            // Avslutt tidlig hvis ordrelinjen ikke er relevant for oss
            if (!erOrdrelinjeRelevant(ordrelinje)) {
                log.info { "Irrelevant ordrelinje mottatt og ignorert" }
                call.respond(HttpStatusCode.OK)
                return@post
            }

            // Anti-corruption lag
            val melding =
                if (ordrelinje.kildeHotsak) {
                    if (!hotsakOrdrelinjeOK(ordrelinje)) {
                        log.info { "Hotsak-ordrelinje mottatt som ikke passerer validering. Logger til Slack og ignorerer." }
                        return@post call.respond(HttpStatusCode.OK)
                    }
                    if (ordrelinje.delbestilling) {
                        log.info { "Ordrelinje fra delbestilling mottatt. Ignorerer." }
                        log.teamInfo { "Ignorert ordrelinje for delbestilling: '$ordrelinje'" }
                        return@post call.respond(HttpStatusCode.OK)
                    }
                    HotsakOrdrelinjeMessage(ordrelinje)
                } else {
                    if (!infotrygdOrdrelinjeOK(ordrelinje)) {
                        log.warn { "Infotrygd-ordrelinje mottatt som ikke passerer validering. Ignorerer." }
                        return@post call.respond(HttpStatusCode.OK)
                    }
                    InfotrygdOrdrelinjeMessage(ordrelinje)
                }

            // Publiser resultat
            publiserMelding(context, ordrelinje, melding)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error(e) { "Uventet feil under prosessering" }
            return@post call.respond(HttpStatusCode.InternalServerError)
        }
    }
}

private suspend fun sendUvalidertOrdrelinjeTilKafka(
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
                append(" på Kafka i miljø: ")
                append(Environment.current)
            }
        }
        context.publish(
            ordrelinje.fnrBruker,
            UvalidertOrdrelinjeMessage(ordrelinje),
        )
    } catch (e: Exception) {
        log.teamError(e) { "Sending av uvalidert ordrelinje på Kafka feilet" }
        error("Noe gikk feil ved publisering av melding")
    }
}

private fun erOrdrelinjeRelevant(ordrelinje: OrdrelinjeOebs): Boolean {
    if (!ordrelinje.serviceforespørseltypeVedtak) {
        if (ordrelinje.serviceforespørseltype == "") {
            log.info { "Mottok melding fra OeBS som ikke er en SF. Avbryter prosesseringen og returnerer" }
        } else {
            log.info {
                buildString {
                    append("Mottok melding fra OeBS med serviceforespørseltype: '")
                    append(ordrelinje.serviceforespørseltype)
                    append("' og serviceforespørselstatus: '")
                    append(ordrelinje.serviceforespørselstatus)
                    append("'. Avbryter prosesseringen og returnerer.")
                }
            }
        }
        return false
    }

    if (!ordrelinje.relevantHjelpemiddeltype) {
        log.info { "Mottok melding fra OeBS med irrelevant hjelpemiddeltype: '${ordrelinje.hjelpemiddeltype}'. Avbryter prosessering" }
        return false
    }

    return true
}

private suspend fun hotsakOrdrelinjeOK(ordrelinje: OrdrelinjeOebs): Boolean {
    if (!ordrelinje.gyldigHotsak) {
        log.warn { "Melding fra OeBS mangler saksnummer fra Hotsak" }
        ordrelinje.fnrBruker = "MASKERT"
        val message = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        log.teamWarn { "Vedtak Hotsak-melding med manglende informasjon: '$message'" }
        Slack.post(
            text = "*${Environment.current}* - Manglende felt i Hotsak OeBS ordrelinje: ```$message```",
            channel = "#digihot-hotsak-varslinger-dev",
        )
        return false
    }
    return true
}

private fun infotrygdOrdrelinjeOK(ordrelinje: OrdrelinjeOebs): Boolean {
    if (!ordrelinje.gyldigInfotrygd) {
        log.warn { "Melding fra OeBS mangler saksblokkOgSaksnr, vedtaksdato eller fnrBruker!" }
        ordrelinje.fnrBruker = "MASKERT"
        log.teamWarn {
            val message = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
            "Vedtak Infotrygd-melding med manglende informasjon: '$message'"
        }
        return false
    }
    return true
}

private suspend fun publiserMelding(
    context: Context,
    ordrelinje: OrdrelinjeOebs,
    melding: OrdrelinjeMessage<*>,
) {
    try {
        log.info { "Publiserer ordrelinje med oebsId: ${ordrelinje.oebsId} på Kafka i miljø: ${Environment.current}" }
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
            log.teamInfo { "Ordrelinje med oebsId: ${ordrelinje.oebsId} mottatt og sendt på Kafka" }
        }
    } catch (e: Exception) {
        log.teamError(e) { "Sending på Kafka feilet" }
        error("Noe gikk feil ved publisering av melding")
    }
}

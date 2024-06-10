package no.nav.hjelpemidler.oebs.listener.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.oebs.listener.Context
import no.nav.hjelpemidler.oebs.listener.Slack
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.model.toHotsakOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())

fun hotsakOrdrelinjeOK(
    context: Context,
    ordrelinje: OrdrelinjeOebs,
): Boolean {
    if (ordrelinje.hotSakSaksnummer.isNullOrBlank()) {
        logg.warn { "Melding frå OEBS manglar HOTSAK saksnummer" }
        ordrelinje.fnrBruker = "MASKERT"
        val message = mapperJson.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        sikkerlogg.warn { "Vedtak HOTSAK-melding med manglende informasjon: $message" }
        context.metrics.manglendeFeltForVedtakHOTSAK()
        Slack.post(
            text = "*${Environment.current}* - Manglende felt i Hotsak Oebs ordrelinje: ```$message```",
            channel = "#digihot-hotsak-varslinger-dev",
        )
        return false
    }
    return true
}

fun opprettHotsakOrdrelinje(ordrelinje: OrdrelinjeOebs) =
    OrdrelinjeMessage(
        eventId = UUID.randomUUID(),
        eventName = "hm-NyOrdrelinje-hotsak",
        opprettet = LocalDateTime.now(),
        fnrBruker = ordrelinje.fnrBruker,
        data = ordrelinje.toHotsakOrdrelinje(),
    )

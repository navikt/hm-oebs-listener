package no.nav.hjelpemidler.oebs.listener.api

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.oebs.listener.Slack
import no.nav.hjelpemidler.oebs.listener.jsonMapper
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.model.toHotsakOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger {}
private val secureLog = KotlinLogging.logger("tjenestekall")

fun hotsakOrdrelinjeOK(ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.hotSakSaksnummer.isNullOrBlank()) {
        log.warn { "Melding frå OEBS manglar HOTSAK saksnummer" }
        ordrelinje.fnrBruker = "MASKERT"
        val message = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        secureLog.warn { "Vedtak HOTSAK-melding med manglende informasjon: $message" }
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

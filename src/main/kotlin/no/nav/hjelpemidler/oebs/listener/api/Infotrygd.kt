package no.nav.hjelpemidler.oebs.listener.api

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.oebs.listener.Slack
import no.nav.hjelpemidler.oebs.listener.jsonMapper
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.model.toOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger {}
private val secureLog = KotlinLogging.logger("tjenestekall")

fun infotrygdOrdrelinjeOK(ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.saksblokkOgSaksnr?.isBlank() == true || ordrelinje.vedtaksdato == null || ordrelinje.fnrBruker.isBlank()) {
        log.warn { "Melding frå OEBS manglar saksblokk, vedtaksdato eller fnr!" }
        ordrelinje.fnrBruker = "MASKERT"
        val message = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        secureLog.warn { "Vedtak Infotrygd-melding med manglande informasjon: $message" }
        Slack.post(
            text = "*${Environment.current}* - Manglande felt i Vedtak Infotrygd-melding: ```$message```",
            channel = "#digihot-brukers-hjelpemiddelside-dev",
        )
        return false
    }
    return true
}

fun opprettInfotrygdOrdrelinje(ordrelinje: OrdrelinjeOebs): OrdrelinjeMessage =
    OrdrelinjeMessage(
        eventId = UUID.randomUUID(),
        eventName = "hm-NyOrdrelinje",
        opprettet = LocalDateTime.now(),
        fnrBruker = ordrelinje.fnrBruker,
        data = ordrelinje.toOrdrelinje(),
    )

package no.nav.hjelpemidler.oebs.listener.api

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.logging.secureLog
import no.nav.hjelpemidler.oebs.listener.jsonMapper
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeMessage
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs

private val log = KotlinLogging.logger {}

fun infotrygdOrdrelinjeOK(ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.saksblokkOgSaksnr?.isBlank() == true || ordrelinje.vedtaksdato == null || ordrelinje.fnrBruker.isBlank()) {
        log.warn { "Melding fra OeBS mangler saksblokk, vedtaksdato eller fnr!" }
        ordrelinje.fnrBruker = "MASKERT"
        val message = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        secureLog.warn { "Vedtak Infotrygd-melding med manglende informasjon: $message" }
        return false
    }
    return true
}

fun opprettInfotrygdOrdrelinje(ordrelinje: OrdrelinjeOebs): OrdrelinjeMessage =
    OrdrelinjeMessage(
        eventName = "hm-NyOrdrelinje",
        fnrBruker = ordrelinje.fnrBruker,
        data = ordrelinje.toOrdrelinje(),
    )

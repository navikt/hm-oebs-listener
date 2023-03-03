import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.Slack
import no.nav.hjelpemidler.Configuration
import no.nav.hjelpemidler.model.OrdrelinjeMessage
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.toOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())

fun infotrygdOrdrelinjeOk(context: Context, ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.saksblokkOgSaksnr?.isBlank() == true || ordrelinje.vedtaksdato == null || ordrelinje.fnrBruker.isBlank()) {
        logg.warn("Melding frå OEBS manglar saksblokk, vedtaksdato eller fnr!")
        ordrelinje.fnrBruker = "MASKERT"
        val message = mapperJson.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        sikkerlogg.warn("Vedtak Infotrygd-melding med manglande informasjon: $message")
        context.metrics.manglendeFeltForVedtakInfotrygd()
        Slack.post(
            text = "*${Configuration.profile}* - Manglande felt i Vedtak Infotrygd-melding: ```$message```",
            channel = "#digihot-brukers-hjelpemiddelside-dev"
        )
        return false
    }
    return true
}

fun opprettInfotrygdOrdrelinje(ordrelinje: OrdrelinjeOebs) = OrdrelinjeMessage(
    eventId = UUID.randomUUID(),
    eventName = "hm-NyOrdrelinje",
    opprettet = LocalDateTime.now(),
    fnrBruker = ordrelinje.fnrBruker,
    data = ordrelinje.toOrdrelinje()
)

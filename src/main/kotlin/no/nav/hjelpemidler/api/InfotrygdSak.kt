import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.metrics.SensuMetrics
import no.nav.hjelpemidler.model.Message
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.toOrdrelinje
import no.nav.hjelpemidler.slack.PostToSlack
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())

fun parseInfotrygdOrdrelinje(ordrelinje: OrdrelinjeOebs) {

    if (ordrelinje.saksblokkOgSaksnr?.isBlank() == true || ordrelinje.vedtaksdato == null || ordrelinje.fnrBruker.isBlank()) {
        logg.warn("Melding frå OEBS manglar saksblokk, vedtaksdato eller fnr!")
        ordrelinje.fnrBruker = "MASKERT"
        val message = mapperJson.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        sikkerlogg.warn("Vedtak Infotrygd-melding med manglande informasjon: $message")
        SensuMetrics().manglendeFeltForVedtakInfotrygd()

        PostToSlack().post(
            Configuration.application["SLACK_HOOK"]!!,
            "*${Configuration.application["APP_PROFILE"]!!.toUpperCase()}* - Manglande felt i Vedtak Infotrygd-melding: ```$message```",
            "#digihot-brukers-hjelpemiddelside-dev"
        )
       throw RuntimeException("Ugyldig Infotrygd ordelinje")
    }
}

fun opprettInfotrygdOrdrelinje(ordrelinje: OrdrelinjeOebs) = Message(
    eventId = UUID.randomUUID(),
    eventName = "hm-NyOrdrelinje",
    opprettet = LocalDateTime.now(),
    fnrBruker = ordrelinje.fnrBruker,
    data = ordrelinje.toOrdrelinje()
)

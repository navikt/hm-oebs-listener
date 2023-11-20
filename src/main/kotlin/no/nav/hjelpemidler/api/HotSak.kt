import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.hjelpemidler.Configuration
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.Slack
import no.nav.hjelpemidler.model.OrdrelinjeMessage
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.toHotsakOrdrelinje
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())

fun hotsakOrdrelinjeOk(context: Context, ordrelinje: OrdrelinjeOebs): Boolean {
    if (ordrelinje.hotSakSaksnummer.isNullOrBlank()) {
        logg.warn("Melding frå OEBS manglar HOTSAK saksnummer")
        ordrelinje.fnrBruker = "MASKERT"
        val message = mapperJson.writerWithDefaultPrettyPrinter().writeValueAsString(ordrelinje)
        sikkerlogg.warn("Vedtak HOTSAK-melding med manglende informasjon: $message")
        context.metrics.manglendeFeltForVedtakHOTSAK()
        Slack.post(
            text = "*${Configuration.profile}* - Manglende felt i Hotsak Oebs ordrelinje: ```$message```",
            channel = "#digihot-hotsak-varslinger-dev"
        )
        return false
    }
    return true
}

fun opprettHotsakOrdrelinje(ordrelinje: OrdrelinjeOebs) = OrdrelinjeMessage(
    eventId = UUID.randomUUID(),
    eventName = "hm-NyOrdrelinje-hotsak",
    opprettet = LocalDateTime.now(),
    fnrBruker = ordrelinje.fnrBruker,
    data = ordrelinje.toHotsakOrdrelinje()
)

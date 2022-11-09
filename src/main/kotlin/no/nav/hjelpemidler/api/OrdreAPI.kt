package no.nav.hjelpemidler.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.Ntfy
import no.nav.hjelpemidler.Slack
import no.nav.hjelpemidler.configuration.Configuration
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

fun Route.ordreAPI(context: Context) {
    post("/ordrekvittering") {
        val kvittering = call.receive<Ordrekvittering>()
        log.info {
            "Mottok ordrekvittering, $kvittering"
        }
        context.publish(
            kvittering.saksnummer,
            OrdrekvitteringMottatt(kvittering = kvittering)
        )
        call.response.status(HttpStatusCode.OK)
    }
    post("/ordrefeilmelding") {
        val feilmelding = call.receive<Ordrefeilmelding>()
        log.warn {
            "Mottok ordrefeilmelding, $feilmelding"
        }
        context.publish(
            feilmelding.saksnummer,
            OrdrefeilmeldingMottatt(feilmelding = feilmelding)
        )
        Slack.post(
            text = "*${Configuration.profile}* - $feilmelding",
            channel = "#papaya-alerts"
        )
        Ntfy.publish(
            Ntfy.Notification(
                title = "Mottok ordrefeilmelding",
                message = "Status: ${feilmelding.status}",
                priority = Ntfy.Priority.HIGH,
                actions = setOf(
                    Ntfy.Action(
                        action = Ntfy.ActionType.VIEW,
                        label = "Se detaljer i Slack",
                        clear = true,
                        url = "https://nav-it.slack.com/archives/C02LS2W05E1"
                    )
                )
            )
        )
        call.response.status(HttpStatusCode.OK)
    }
}

data class Ordrekvittering(
    val id: String,
    val saksnummer: String,
    val ordrenummer: String,
    val system: String,
    val status: String,
) {
    override fun toString(): String =
        "id: $id, saksnummer: $saksnummer, ordrenummer: $ordrenummer, system: $system, status: $status"
}

data class Ordrefeilmelding(
    val id: String,
    val saksnummer: String,
    val feilmelding: String,
    val system: String,
    val status: String,
) {
    override fun toString(): String =
        "id: $id, saksnummer: $saksnummer, feilmelding: $feilmelding, system: $system, status: $status"
}

data class OrdrekvitteringMottatt(
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String = "hm-ordrekvittering-mottatt",
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val kvittering: Ordrekvittering,
)

data class OrdrefeilmeldingMottatt(
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String = "hm-ordrefeilmelding-mottatt",
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val feilmelding: Ordrefeilmelding,
)

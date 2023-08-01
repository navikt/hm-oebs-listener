package no.nav.hjelpemidler.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.hjelpemidler.Configuration
import no.nav.hjelpemidler.Configuration.Profile
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.Ntfy
import no.nav.hjelpemidler.Slack
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

fun Route.ordreAPI(context: Context) {
    post("/ordrekvittering") {
        try {
            val kvittering = call.receive<Ordrekvittering>()
            log.info {
                "Mottok ordrekvittering, $kvittering"
            }

            if (kvittering.saksnummer.startsWith("hmdel_")) {
                log.info { "Publiserer ordrekvittering for delebestilling: $kvittering" }
                val sakId = kvittering.saksnummer.removePrefix("hmdel_") // trenger ikke denne videre nedover
                val status = kvittering.status.uppercase() // for enums
                context.publish(
                    kvittering.saksnummer,
                    OrdrekvitteringDelbestillingMottatt(kvittering = kvittering.copy(saksnummer = sakId, status = status))
                )
            } else {
                context.publish(
                    kvittering.saksnummer,
                    OrdrekvitteringMottatt(kvittering = kvittering)
                )
            }

            call.response.status(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error(e) { "Uventet feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }
    }
    post("/ordrefeilmelding") {
        try {
            val feilmelding = call.receive<Ordrefeilmelding>()
            log.warn {
                "Mottok ordrefeilmelding, $feilmelding"
            }

            if (feilmelding.saksnummer.startsWith("hmdel_")) {
                log.error { "Ignorerer ordrefeilmelding for delebestilling: $feilmelding" }
                return@post call.response.status(HttpStatusCode.OK)
            }

            context.publish(
                feilmelding.saksnummer,
                OrdrefeilmeldingMottatt(feilmelding = feilmelding)
            )
            if (Configuration.profile == Profile.PROD) {
                Slack.post(
                    text = "*${Configuration.profile}* - $feilmelding - <@${Configuration.slackRecipient}>",
                    channel = "#digihot-oebs"
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
            }
            call.response.status(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error(e) { "Uventet feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError)
            return@post
        }
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

data class OrdrekvitteringDelbestillingMottatt(
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String = "hm-ordrekvittering-delbestilling-mottatt",
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val kvittering: Ordrekvittering,
)

data class OrdrefeilmeldingMottatt(
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String = "hm-ordrefeilmelding-mottatt",
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val feilmelding: Ordrefeilmelding,
)

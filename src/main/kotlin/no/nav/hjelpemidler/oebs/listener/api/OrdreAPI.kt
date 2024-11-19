package no.nav.hjelpemidler.oebs.listener.api

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.oebs.listener.Configuration
import no.nav.hjelpemidler.oebs.listener.Context
import no.nav.hjelpemidler.oebs.listener.Metrics
import no.nav.hjelpemidler.oebs.listener.Ntfy
import no.nav.hjelpemidler.oebs.listener.Slack
import no.nav.hjelpemidler.oebs.listener.model.Message

private val log = KotlinLogging.logger { }

fun Route.ordreAPI(context: Context) {
    post("/ordrekvittering") {
        try {
            val kvittering = call.receive<Ordrekvittering>()
            log.info {
                "Mottok ordrekvittering, $kvittering"
            }

            Metrics.ordrekvitteringCounter.increment()

            if (kvittering.delbestilling) {
                log.info { "Publiserer ordrekvittering for delbestilling: $kvittering" }
                val sakId = kvittering.saksnummer.removePrefix("hmdel_") // trenger ikke denne videre nedover
                val status = kvittering.status.uppercase() // for enums
                context.publish(
                    kvittering.saksnummer,
                    OrdrekvitteringDelbestillingMottatt(
                        kvittering =
                            kvittering.copy(
                                saksnummer = sakId,
                                status = status,
                            ),
                    ),
                )
            } else {
                context.publish(
                    kvittering.saksnummer,
                    OrdrekvitteringMottatt(kvittering = kvittering),
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

            if (feilmelding.delbestilling) {
                log.error { "Ignorerer ordrefeilmelding for delbestilling: $feilmelding" }
                return@post call.response.status(HttpStatusCode.OK)
            }

            context.publish(
                feilmelding.saksnummer,
                OrdrefeilmeldingMottatt(feilmelding = feilmelding),
            )
            if (Environment.current.tier.isProd) {
                Slack.post(
                    text = "*${Environment.current}* - $feilmelding - <@${Configuration.SLACK_RECIPIENT}>",
                    channel = "#digihot-oebs",
                )
                Ntfy.publish(
                    Ntfy.Notification(
                        title = "Mottok ordrefeilmelding",
                        message = "Status: ${feilmelding.status}",
                        priority = Ntfy.Priority.HIGH,
                        actions =
                            setOf(
                                Ntfy.Action(
                                    action = Ntfy.ActionType.VIEW,
                                    label = "Se detaljer i Slack",
                                    clear = true,
                                    url = "https://nav-it.slack.com/archives/C02LS2W05E1",
                                ),
                            ),
                    ),
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
    val delbestilling: Boolean @JsonIgnore get() = saksnummer.startsWith("hmdel_")

    override fun toString(): String = "id: $id, saksnummer: $saksnummer, ordrenummer: $ordrenummer, system: $system, status: $status"
}

data class Ordrefeilmelding(
    val id: String,
    val saksnummer: String,
    val feilmelding: String,
    val system: String,
    val status: String,
) {
    val delbestilling: Boolean @JsonIgnore get() = saksnummer.startsWith("hmdel_")

    override fun toString(): String = "id: $id, saksnummer: $saksnummer, feilmelding: $feilmelding, system: $system, status: $status"
}

class OrdrekvitteringMottatt(val kvittering: Ordrekvittering) :
    Message(eventName = "hm-ordrekvittering-mottatt")

class OrdrekvitteringDelbestillingMottatt(val kvittering: Ordrekvittering) :
    Message(eventName = "hm-ordrekvittering-delbestilling-mottatt")

class OrdrefeilmeldingMottatt(val feilmelding: Ordrefeilmelding) :
    Message(eventName = "hm-ordrefeilmelding-mottatt")

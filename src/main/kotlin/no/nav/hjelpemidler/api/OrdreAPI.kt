package no.nav.hjelpemidler.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.hjelpemidler.Context
import java.time.LocalDateTime
import java.util.UUID

private val log = KotlinLogging.logger { }

fun Route.ordreAPI(context: Context) {
    post("/ordrekvittering") {
        val kvittering = call.receive<Ordrekvittering>()
        log.info {
            "Mottok ordrekvittering, id=${kvittering.id}, saksnummer=${kvittering.saksnummer}, ordrenummer: ${kvittering.ordrenummer}"
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
            "Mottok ordrefeilmelding, id=${feilmelding.id}, saksnummer=${feilmelding.saksnummer}, feilmelding: ${feilmelding.feilmelding}"
        }
        context.publish(
            feilmelding.saksnummer,
            OrdrefeilmeldingMottatt(feilmelding = feilmelding)
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
)

data class Ordrefeilmelding(
    val id: String,
    val saksnummer: String,
    val feilmelding: String,
    val system: String,
    val status: String,
)

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

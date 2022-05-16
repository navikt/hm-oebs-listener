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

private val logg = KotlinLogging.logger { }

fun Route.ordreAPI(context: Context) {
    post("/ordrekvittering") {
        val kvittering = call.receive<Ordrekvittering>()
        logg.info { "Mottok ordrekvittering, id=${kvittering.id}" }
        context.publish(
            kvittering.saksnummer,
            OrdrekvitteringMottatt(kvittering = kvittering)
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

data class OrdrekvitteringMottatt(
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String = "hm-ordrekvittering-mottatt",
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val kvittering: Ordrekvittering,
)

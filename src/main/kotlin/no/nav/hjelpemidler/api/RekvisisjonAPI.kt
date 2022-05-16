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

fun Route.rekvisisjonAPI(context: Context) {
    post("/rekvisisjon-kvittering") {
        val kvittering = call.receive<RekvisisjonKvittering>()
        logg.info { "Mottok kvittering for rekvisisjon, id=${kvittering.id}" }
        context.publish(
            kvittering.saksnummer,
            RekvisisjonKvitteringMottatt(kvittering = kvittering)
        )
        call.response.status(HttpStatusCode.OK)
    }
}

data class RekvisisjonKvittering(
    val id: String,
    val saksnummer: String,
    val ordrenummer: String,
    val system: String,
    val status: String,
)

data class RekvisisjonKvitteringMottatt(
    val eventId: UUID = UUID.randomUUID(),
    val eventName: String = "hm-rekvisisjon-kvittering-mottatt",
    val opprettet: LocalDateTime = LocalDateTime.now(),
    val kvittering: RekvisisjonKvittering,
)

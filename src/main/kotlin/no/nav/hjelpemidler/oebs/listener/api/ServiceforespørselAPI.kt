package no.nav.hjelpemidler.oebs.listener.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.hjelpemidler.logging.secureLog
import no.nav.hjelpemidler.oebs.listener.Context
import no.nav.hjelpemidler.oebs.listener.model.ServiceforespørselEndringMessage

private val log = KotlinLogging.logger {}

fun Route.serviceforespørselAPI(context: Context) {
    post("/sf") {
        log.info { "Innkommende SF-oppdatering" }
        try {
            val endring = call.receive<ServiceforespørselEndring>()
            val message = ServiceforespørselEndringMessage(endring)
            publiserMelding(context, endring, message)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error(e) { "Feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
            return@post
        }
    }
}

data class ServiceforespørselEndring(
    val system: String,
    val id: String,
    val sfnummer: String,
    @JsonProperty("saknummer")
    val saksnummer: String,
    val antallKostnadslinjer: String?,
    val ordre: List<ServiceForespørselOrdre>? = null,
    val status: SFEndringType,
)

data class ServiceForespørselOrdre(
    val ordrenummer: String,
    val status: String,
)

@Suppress("unused")
enum class SFEndringType {
    OPPRETTET,
    LUKKET,
    TILORDNET,
    FEIL_KOSTNADSLINJER,
}

private suspend fun publiserMelding(
    context: Context,
    endring: ServiceforespørselEndring,
    message: ServiceforespørselEndringMessage,
) {
    try {
        log.info {
            buildString {
                append("Publiserer oppdatering for SF fra OeBS med id: ")
                append(endring.id)
                append(", SF-nummer: ")
                append(endring.sfnummer)
                append(", saksnummer: ")
                append(endring.saksnummer)
                append(", status: ")
                append(endring.status)
                append(", ordre: ")
                append(endring.ordre)
                append(", antall kostnadslinjer opprettet: ")
                append(endring.antallKostnadslinjer ?: '-')
            }
        }
        context.publish(
            endring.saksnummer,
            message,
        )
    } catch (e: Exception) {
        secureLog.error(e) { "Sending på Kafka feilet" }
        error("Noe gikk feil ved publisering av melding")
    }
}

package no.nav.hjelpemidler.oebs.listener.api

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.hjelpemidler.oebs.listener.Context
import no.nav.hjelpemidler.oebs.listener.model.SfMessage
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

fun Route.serviceforespørselAPI(context: Context) {
    post("/sf") {
        logg.info { "incoming sf-oppdatering" }
        try {
            val serviceForespørselEndring = call.receive<ServiceForespørselEndring>()
            val sfMessage =
                SfMessage(
                    eventId = UUID.randomUUID(),
                    eventName = "hm-EndretSF-oebs-v2",
                    opprettet = LocalDateTime.now(),
                    data = serviceForespørselEndring,
                )
            publiserMelding(context, serviceForespørselEndring, sfMessage)
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            logg.error(e) { "Feil under prosessering" }
            call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
            return@post
        }
    }
}

data class ServiceForespørselEndring(
    val system: String,
    val id: String,
    val sfnummer: String,
    val saknummer: String,
    val antallKostnadslinjer: String?,
    val ordre: List<ServiceForespørselOrdre>? = null,
    @JsonProperty("status")
    val status: SFEndringType,
)

data class ServiceForespørselOrdre(
    val ordrenummer: String,
    val status: String,
)

enum class SFEndringType(value: String) {
    OPPRETTET("Opprettet"),
    LUKKET("Lukket"),
    TILORDNET("Tilordnet"),
    ;

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String) = valueOf(value.uppercase())
    }
}

private fun publiserMelding(
    context: Context,
    serviceForespørselEndring: ServiceForespørselEndring,
    sfMessage: SfMessage,
) {
    try {
        logg.info {
            buildString {
                append("Publiserer oppdatering for SF fra OEBS med id: ")
                append(serviceForespørselEndring.id)
                append(", sfnummer: ")
                append(serviceForespørselEndring.sfnummer)
                append(", saknummer: ")
                append(serviceForespørselEndring.saknummer)
                append(", status: ")
                append(serviceForespørselEndring.status)
                append(", ordre: ")
                append(serviceForespørselEndring.ordre)
                append(", antall kostnadslinjer opprettet: ")
                append(serviceForespørselEndring.antallKostnadslinjer ?: '-')
            }
        }
        context.publish(
            serviceForespørselEndring.saknummer,
            mapperJson.writeValueAsString(sfMessage),
        )
    } catch (e: Exception) {
        sikkerlogg.error(e) { "Sending til rapid feilet" }
        error("Noe gikk feil ved publisering av melding")
    }
}

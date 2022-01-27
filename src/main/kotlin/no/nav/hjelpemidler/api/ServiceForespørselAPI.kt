package no.nav.hjelpemidler.api

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.metrics.SensuMetrics
import no.nav.hjelpemidler.model.SfMessage
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())

internal fun Route.ServiceforespørselApi(rapidApp: RapidsConnection?) {
    post("/sf") {
        logg.info("incoming sf-oppdatering")
        val authHeader = call.request.header("Authorization").toString()
        if (!authHeader.startsWith("Bearer ") || authHeader.substring(7) != Configuration.application["OEBSTOKEN"]!!) {
            call.respond(HttpStatusCode.Unauthorized, "unauthorized")
            return@post
        }

        try {
            val requestBody: String = call.receiveText()
            val serviceForespørselEndring = mapperJson.readValue<ServiceForespørselEndring>(requestBody)
            val sfMessage = SfMessage(
                eventId = UUID.randomUUID(),
                eventName = "hm-EndretSF-oebs",
                opprettet = LocalDateTime.now(),
                data = serviceForespørselEndring
            )
            publiserMelding(serviceForespørselEndring, rapidApp, sfMessage)
            call.respond(HttpStatusCode.OK)

        } catch (e: RapidsAndRiverException) {
            call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
            return@post
        } catch (e: RuntimeException) {
            call.respond(HttpStatusCode.OK)
            return@post
        }
    }
}

data class ServiceForespørselEndring(
    val system: String,
    val id: String,
    val sfnummer: String,
    val saknummer: String,
    val ordre: List<ServiceForespørselOrdre>? = null,
    val status: String?
)

data class ServiceForespørselOrdre(
    val ordrenummer: String,
    val status: String
)

private fun publiserMelding(
    serviceForespørselEndring: ServiceForespørselEndring,
    rapidApp: RapidsConnection?,
    sfMessage: SfMessage
) {
    try {
        logg.info(
            "Publiserer oppdatering for SF fra OEBS med id ${serviceForespørselEndring.id}, " +
                "sfNummer: ${serviceForespørselEndring.sfnummer}, saknr: ${serviceForespørselEndring.saknummer}"
        )
        rapidApp!!.publish(
            serviceForespørselEndring.saknummer,
            mapperJson.writeValueAsString(sfMessage)
        )
        SensuMetrics().meldingTilRapidSuksess()
    } catch (e: Exception) {
        SensuMetrics().meldingTilRapidFeilet()
        sikkerlogg.error("Sending til rapid feilet, exception: $e")
        throw RapidsAndRiverException("Noe gikk feil ved publisering av melding")
    }
}

package no.nav.hjelpemidler

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.metrics.SensuMetrics
import java.net.InetAddress
import java.time.LocalDateTime
import java.util.*

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

fun main() {
    var rapidApp: RapidsConnection? = null
    rapidApp = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig(
            Configuration.rapidConfig["RAPID_APP_NAME"],
            InetAddress.getLocalHost().hostName,
            Configuration.rapidConfig["KAFKA_RAPID_TOPIC"]!!,
            emptyList(),
            KafkaConfig(
                Configuration.rapidConfig["KAFKA_BOOTSTRAP_SERVERS"]!!,
                Configuration.rapidConfig["KAFKA_CONSUMER_GROUP_ID"]!!,
                Configuration.rapidConfig["KAFKA_CLIENT_ID"]!!,
                null,
                null,
                Configuration.rapidConfig["KAFKA_TRUSTSTORE_PATH"]!!,
                Configuration.rapidConfig["KAFKA_TRUSTSTORE_PASSWORD"]!!,
                "jks",
                "PKCS12",
                Configuration.rapidConfig["KAFKA_KEYSTORE_PATH"]!!,
                Configuration.rapidConfig["KAFKA_KEYSTORE_PASSWORD"]!!,
                Configuration.rapidConfig["KAFKA_RESET_POLICY"]!!,
                false,
                null,
                null,
            ),
            8080,
        )
    ).withKtorModule {
        routing {
            post("/push") {
                logg.info("incoming push")
                val authHeader = call.request.header("Authorization").toString()
                if (!authHeader.startsWith("Bearer ") || authHeader.substring(7) != Configuration.application["OEBSTOKEN"]!!) {
                    call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    return@post
                }

                val rawJson: String = call.receiveText()
                sikkerlogg.info("Received JSON push request from OEBS: $rawJson")

                // Check for valid json request
                val ordrelinje: Statusinfo?
                try {
                    ordrelinje = Klaxon().parse<Statusinfo>(rawJson)
                    sikkerlogg.info("Parsing incoming json request successful: ${Klaxon().toJsonString(ordrelinje)}")
                    SensuMetrics().meldingFraOebs()
                } catch (e: Exception) {
                    // Deal with invalid json in request
                    sikkerlogg.info("Parsing incoming json request failed with exception (responding 4xx): $e")
                    SensuMetrics().feilVedMeldingFraOebs()
                    call.respond(HttpStatusCode.BadRequest, "bad request: json not valid")
                    return@post
                }

                if (ordrelinje!!.serviceforespoerseltype != "Vedtak Infotrygd") {
                    log.info("Mottok melding fra oebs med sf-type ${ordrelinje.serviceforespoerseltype} og sf-status ${ordrelinje.serviceforespoerselstatus}. " +
                            "Avbryter prosesseringen og returnerer")
                    call.respond(HttpStatusCode.OK)
                    return@post
                }

                val melding = Message(
                    eventId = UUID.randomUUID(),
                    eventName = "hm-nyOrdrelinje",
                    opprettet = LocalDateTime.now(),
                    fnrBruker = ordrelinje.fnrBruker,
                    data = ordrelinje
                )

                // Publish the received json to our rapid
                try {
                    rapidApp!!.publish(ordrelinje.fnrBruker, Klaxon().toJsonString(melding))
                    SensuMetrics().meldingTilRapidSuksess()
                } catch (e: Exception) {
                    SensuMetrics().meldingTilRapidFeilet()
                    sikkerlogg.error("Sending til rapid feilet, exception: $e")
                    call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
                    return@post
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }.build()

    // Run our rapid and rivers implementation facing hm-rapid
    logg.info("Starting Rapid & Rivers app towards hm-rapid")
    rapidApp.start()
    logg.info("Application ending.")
}

data class Message(
    val eventId: UUID,
    val eventName: String,
    val opprettet: LocalDateTime,
    val fnrBruker: String,
    val data: Statusinfo,
)

data class Statusinfo(
    @Json(name = "System")
    val mottakendeSystem: String,
    @Json(name = "IncidentNummer")
    val serviceforespoersel: Int,
    @Json(name = "IncidentStatus")
    val serviceforespoerselstatus: String,
    @Json(name = "IncidentType")
    val serviceforespoerseltype: String,
    @Json(name = "IncidentSoknadType")
    val soeknadstype: String,
    @Json(name = "IncidentVedtakDato")
    val vedtaksdato: String,
    @Json(name = "IncidentSoknad")
    val soeknad: String,
    @Json(name = "IncidentResultat")
    val resultat: String,
    @Json(name = "IncidentRef")
    val saksblokkOgSaksnummer: String,
    @Json(name = "OrdreNumber")
    val ordrenummer: Int,
    @Json(name = "LineNumber")
    val ordrelinjenummer: Int,
    @Json(name = "Description")
    val artikkelbeskrivelse: String,
    @Json(name = "CategoryDescription")
    val kategorinavn: String,
    @Json(name = "OrderedItem")
    val artikkel: Int,
    @Json(name = "User_ItemType")
    val hjelpemiddeltype: String,
    @Json(name = "Quantity")
    val antall: Int,
    @Json(name = "AccountNumber")
    val fnrBruker: String,
    @Json(name = "LastUpdateDate")
    val sistOppdatert: String,
)

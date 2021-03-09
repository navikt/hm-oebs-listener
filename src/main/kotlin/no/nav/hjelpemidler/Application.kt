package no.nav.hjelpemidler

import com.beust.klaxon.Klaxon
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import mu.KotlinLogging
import mu.toKLogger
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.rivers.LoggRiver
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
                val authHeader = call.request.header("Authorization").toString()
                if (!authHeader.startsWith("Bearer ") || authHeader.substring(7) != Configuration.application["OEBSTOKEN"]!!) {
                    call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    return@post
                }

                val rawJson: String = call.receiveText()
                sikkerlogg.info("Received JSON push request from OEBS: $rawJson")

                // Check for valid json request
                try {
                    Klaxon().parse<Map<String, Any?>>(rawJson)
                } catch (e: Exception) {
                    // Deal with invalid json in request
                    sikkerlogg.info("Parsing incoming json request failed with exception (responding 4xx): $e")
                    e.printStackTrace()

                    call.respond(HttpStatusCode.BadRequest, "bad request: json not valid")
                    return@post
                }

                val uid = UUID.randomUUID()
                val opprettet = LocalDateTime.now()

                // Publish the received json to our rapid
                rapidApp!!.publish(
                    UUID.randomUUID().toString(),
                    "{\"@id\": \"$uid\", \"@event_name\": \"oebs-listener-testevent\", \"@opprettet\": \"$opprettet\", \"data\": $rawJson}"
                )

                call.respond(HttpStatusCode.OK, "ok")
            }
        }
    }.build().apply {
        LoggRiver(this)
    }

    // Run our rapid and rivers implementation facing hm-rapid
    logg.info("Starting Rapid & Rivers app towards hm-rapid")
    rapidApp.start()
    logg.info("Application ending.")
}

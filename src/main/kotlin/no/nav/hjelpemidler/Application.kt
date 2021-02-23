package no.nav.hjelpemidler

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.rivers.LoggRiver
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

private val log = LoggerFactory.getLogger("main")

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

                val uid = UUID.randomUUID()
                val opprettet = LocalDateTime.now()

                val rawJson: String = call.receiveText()
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
    log.info("Starting Rapid & Rivers app towwards hm-rapid")
    rapidApp.start()
    log.info("Application ending.")
}

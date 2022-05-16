package no.nav.hjelpemidler

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.api.ordrelinjeAPI
import no.nav.hjelpemidler.api.serviceforespørselAPI
import no.nav.hjelpemidler.configuration.Configuration
import java.net.InetAddress

private val logg = KotlinLogging.logger {}

fun main() {
    val instanceId = InetAddress.getLocalHost().hostName

    lateinit var rapidApp: RapidsConnection
    rapidApp = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig(
            Configuration.rapidConfig["RAPID_APP_NAME"],
            instanceId,
            Configuration.rapidConfig["KAFKA_RAPID_TOPIC"]!!,
            emptyList(),
            KafkaConfig(
                Configuration.rapidConfig["KAFKA_BROKERS"]!!,
                Configuration.rapidConfig["KAFKA_CONSUMER_GROUP_ID"]!!,
                instanceId,
                null,
                null,
                Configuration.rapidConfig["KAFKA_TRUSTSTORE_PATH"]!!,
                Configuration.rapidConfig["KAFKA_CREDSTORE_PASSWORD"]!!,
                "jks",
                "PKCS12",
                Configuration.rapidConfig["KAFKA_KEYSTORE_PATH"]!!,
                Configuration.rapidConfig["KAFKA_CREDSTORE_PASSWORD"]!!,
                Configuration.rapidConfig["KAFKA_RESET_POLICY"]!!,
                false,
                null,
                null,
            ),
            Configuration.rapidConfig["HTTP_PORT"]!!.toInt(),
        )
    ).withKtorModule {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        val context = Context(rapidApp)
        routing {
            ordrelinjeAPI(context)
            serviceforespørselAPI(context)
        }
    }.build()

    // Run our rapid and rivers implementation facing hm-rapid
    logg.info("Starting Rapid & Rivers app towards hm-rapid")
    rapidApp.start()
    logg.info("Application ending.")
}

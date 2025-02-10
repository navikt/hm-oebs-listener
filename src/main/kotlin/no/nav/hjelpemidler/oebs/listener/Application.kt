package no.nav.hjelpemidler.oebs.listener

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.hjelpemidler.kafka.createKafkaProducer
import no.nav.hjelpemidler.oebs.listener.api.ordreAPI
import no.nav.hjelpemidler.oebs.listener.api.ordrelinjeAPI
import no.nav.hjelpemidler.oebs.listener.api.serviceforespørselAPI
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import org.apache.kafka.clients.producer.Producer
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module(producer: Producer<String, String> = createKafkaProducer()) {
    val authenticationName = "oebsToken"
    install(Authentication) {
        bearer(authenticationName) {
            realm = "Tilgang til OeBS-API-er"
            authenticate { tokenCredential ->
                if (tokenCredential.token == Configuration.OEBS_TOKEN) {
                    UserIdPrincipal("OeBS")
                } else {
                    null
                }
            }
        }
    }

    install(CallLogging) {
        disableDefaultColors()
        level = Level.INFO
        filter {
            it.request.path() in setOf("/sf")
            it.request.path() !in setOf("/isalive", "/isready", "/metrics")
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(jsonMapper))
    }

    install(MicrometerMetrics) {
        registry = Metrics.registry
    }

    val context = Context(producer)
    monitor.subscribe(ApplicationStopping) { application ->
        context.close()
        application.environment.log.info("Applikasjonen har stoppet")
        application.monitor.unsubscribe(ApplicationStopping) {}
    }

    routing {
        get("/isalive") { call.respond(HttpStatusCode.OK, "ALIVE") }
        get("/isready") { call.respond(HttpStatusCode.OK, "READY") }
        get("/metrics") { call.respond(Metrics.scrape()) }

        authenticate(authenticationName) {
            ordreAPI(context)
            ordrelinjeAPI(context)
            serviceforespørselAPI(context)
        }
    }
}

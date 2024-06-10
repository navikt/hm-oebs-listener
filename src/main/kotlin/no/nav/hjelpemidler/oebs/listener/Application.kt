package no.nav.hjelpemidler.oebs.listener

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidApplication.RapidApplicationConfig
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.oebs.listener.api.ordreAPI
import no.nav.hjelpemidler.oebs.listener.api.ordrelinjeAPI
import no.nav.hjelpemidler.oebs.listener.api.serviceforespørselAPI
import org.slf4j.event.Level

private val log = KotlinLogging.logger {}

fun main() {
    lateinit var rapidApp: RapidsConnection
    rapidApp =
        RapidApplication.Builder(RapidApplicationConfig.fromEnv(no.nav.hjelpemidler.configuration.Configuration.current))
            .withKtorModule {
                install(CallLogging) {
                    disableDefaultColors()
                    level = Level.DEBUG
                    filter {
                        when (it.request.path()) {
                            "/isalive" -> false
                            "/isready" -> false
                            else -> true
                        }
                    }
                }
                install(ContentNegotiation) {
                    jackson { registerModule(JavaTimeModule()) }
                }
                install(Authentication) {
                    token("oebsToken") { validate(OEBSTOKEN) }
                }
                val context = Context(rapidApp)
                routing {
                    authenticate("oebsToken") {
                        ordrelinjeAPI(context)
                        serviceforespørselAPI(context)
                        ordreAPI(context)
                    }
                }
            }.build()

    log.info { "hm-oebs-listener with environment: ${Environment.current} starting..." }
    rapidApp.start()
    log.info { "Application stopping..." }
}

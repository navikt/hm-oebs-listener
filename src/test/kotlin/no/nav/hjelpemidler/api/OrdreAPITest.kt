package no.nav.hjelpemidler.api

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import io.mockk.verify
import no.nav.hjelpemidler.Context
import no.nav.hjelpemidler.shouldBe
import no.nav.hjelpemidler.token
import kotlin.test.Test

internal class OrdreAPITest {

    private val context = Context(mockk(relaxed = true))

    @Test
    internal fun `sender ut ordrekvittering på rapid`() = testApplication {
        configure()
        val body = context.jsonMapper.writeValueAsString(
            Ordrekvittering(
                id = "1",
                saksnummer = "2",
                ordrenummer = "3",
                system = "HOTSAK",
                status = "ENTERED"
            )
        )
        client.post("/ordrekvittering") {
            bearerAuth("qwer1234")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.apply {
            status shouldBe HttpStatusCode.OK
            verify {
                context.publish("2", match { it.contains(body) })
            }
        }
    }

    @Test
    internal fun `sender ut ordrefeilmelding på rapid`() = testApplication {
        configure()
        val body = context.jsonMapper.writeValueAsString(
            Ordrefeilmelding(
                id = "1",
                saksnummer = "2",
                feilmelding = "Feilmelding",
                system = "HOTSAK",
                status = "ERROR"
            )
        )
        client.post("/ordrefeilmelding") {
            bearerAuth("qwer1234")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.apply {
            status shouldBe HttpStatusCode.OK
            verify {
                context.publish("2", match { it.contains(body) })
            }
        }
    }

    private fun ApplicationTestBuilder.configure() {
        install(ContentNegotiation) {
            jackson()
        }
        install(Authentication) {
            token("oebsToken") {
                validate("qwer1234")
            }
        }
        routing {
            authenticate("oebsToken") {
                ordreAPI(context)
            }
        }
    }
}

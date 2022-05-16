package no.nav.hjelpemidler

import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test

internal class TokenAuthenticationProviderTest {

    @Test
    internal fun `authentication fails, missing token`() = testApplication {
        configure()
        client.get("/secured") {
        }.apply {
            status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    internal fun `authentication fails, wrong token`() = testApplication {
        configure()
        client.get("/secured") {
            bearerAuth("1234qwer")
        }.apply {
            status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    internal fun `authentication fails, wrong scheme`() = testApplication {
        configure()
        client.get("/secured") {
            basicAuth("foo", "bar")
        }.apply {
            status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    internal fun `authentication succeeds`() = testApplication {
        configure()
        client.get("/secured") {
            bearerAuth("qwer1234")
        }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "secret"
        }
    }

    private fun ApplicationTestBuilder.configure() {
        install(Authentication) {
            token("oebsToken") {
                validate("qwer1234")
            }
        }
        routing {
            authenticate("oebsToken") {
                get("/secured") {
                    call.respondText("secret")
                }
            }
        }
    }
}

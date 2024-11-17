package no.nav.hjelpemidler.oebs.listener

import io.kotest.matchers.shouldBe
import io.ktor.client.request.basicAuth
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.oebs.listener.api.SFEndringType
import no.nav.hjelpemidler.oebs.listener.api.ServiceforespørselEndring
import no.nav.hjelpemidler.oebs.listener.test.runTest
import no.nav.hjelpemidler.oebs.listener.test.validToken
import kotlin.test.Test

class TokenAuthenticationTest {
    @Test
    fun `Autentisering feiler, mangler token`() =
        runTest {
            val response = client.post("/sf")
            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `Autentisering feiler, ugyldig token`() =
        runTest {
            val response =
                client.post("/sf") {
                    bearerAuth("foobar")
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `Autentisering feiler, feil type`() =
        runTest {
            val response =
                client.post("/sf") {
                    basicAuth("foo", "bar")
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `Autentisering lykkes`() =
        runTest {
            val response =
                client.post("/sf") {
                    validToken()
                    contentType(ContentType.Application.Json)
                    setBody(
                        ServiceforespørselEndring(
                            system = "HOTSAK",
                            id = "1",
                            sfnummer = "1",
                            saksnummer = "1",
                            antallKostnadslinjer = "1",
                            ordre = emptyList(),
                            status = SFEndringType.OPPRETTET,
                        ),
                    )
                }
            response.status shouldBe HttpStatusCode.OK
        }
}

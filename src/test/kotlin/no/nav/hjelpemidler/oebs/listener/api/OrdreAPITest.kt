package no.nav.hjelpemidler.oebs.listener.api

import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.oebs.listener.test.runTest
import no.nav.hjelpemidler.oebs.listener.test.shouldHaveKey
import no.nav.hjelpemidler.oebs.listener.test.shouldHaveValue
import no.nav.hjelpemidler.oebs.listener.test.validToken
import kotlin.test.Test

/**
 * @see [no.nav.hjelpemidler.oebs.listener.api.ordreAPI]
 */
class OrdreAPITest {
    @Test
    fun `Sender ut ordrekvittering på Kafka`() =
        runTest {
            val body =
                Ordrekvittering(
                    id = "1",
                    saksnummer = "2",
                    ordrenummer = "3",
                    system = "HOTSAK",
                    status = "ENTERED",
                )

            val response =
                client.post("/ordrekvittering") {
                    validToken()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

            response.status shouldBe HttpStatusCode.OK

            kafkaHistory.shouldForOne {
                it shouldHaveKey body.saksnummer
                it.shouldHaveValue<OrdrekvitteringMottatt> { value ->
                    value.kvittering shouldBe body
                }
            }
        }

    @Test
    fun `Sender ut ordrefeilmelding på Kafka`() =
        runTest {
            val body =
                Ordrefeilmelding(
                    id = "1",
                    saksnummer = "2",
                    feilmelding = "Feilmelding",
                    system = "HOTSAK",
                    status = "ERROR",
                )

            val response =
                client.post("/ordrefeilmelding") {
                    validToken()
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

            response.status shouldBe HttpStatusCode.OK

            kafkaHistory.shouldForOne {
                it shouldHaveKey body.saksnummer
                it.shouldHaveValue<OrdrefeilmeldingMottatt> { value ->
                    value.feilmelding shouldBe body
                }
            }
        }
}

package no.nav.hjelpemidler.oebs.listener.api

import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.oebs.listener.test.runTest
import no.nav.hjelpemidler.oebs.listener.test.shouldContainRecord
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

            kafkaHistory.shouldContainRecord(
                expectedKey = body.saksnummer,
                expectedEventName = "hm-ordrekvittering-mottatt",
            ) {
                it.shouldContainJsonKey("kvittering")
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

            kafkaHistory.shouldContainRecord(
                expectedKey = body.saksnummer,
                expectedEventName = "hm-ordrefeilmelding-mottatt",
            ) {
                it.shouldContainJsonKey("feilmelding")
            }
        }
}

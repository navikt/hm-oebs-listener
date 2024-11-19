package no.nav.hjelpemidler.oebs.listener.api

import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
import no.nav.hjelpemidler.oebs.listener.test.runTest
import no.nav.hjelpemidler.oebs.listener.test.shouldContainRecord
import no.nav.hjelpemidler.oebs.listener.test.validToken
import kotlin.test.Test

/**
 * @see [no.nav.hjelpemidler.oebs.listener.api.serviceforespørselAPI]
 */
class ServiceforespørselAPITest {
    @Test
    fun `Sender ut SF-oppdatering på Kafka`() =
        runTest {
            val endring = Fixtures.lagServiceforespørselEndring()

            val response =
                client.post("/sf") {
                    validToken()
                    contentType(ContentType.Application.Json)
                    setBody(endring)
                }

            response.status shouldBe HttpStatusCode.OK

            kafkaHistory.shouldContainRecord(
                expectedKey = endring.saksnummer,
                expectedEventName = "hm-EndretSF-oebs-v2",
            ) {
                it.shouldContainJsonKey("data")
            }
        }
}

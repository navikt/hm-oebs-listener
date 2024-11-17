package no.nav.hjelpemidler.oebs.listener.api

import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.oebs.listener.model.ServiceforespørselEndringMessage
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
import no.nav.hjelpemidler.oebs.listener.test.runTest
import no.nav.hjelpemidler.oebs.listener.test.shouldHaveKey
import no.nav.hjelpemidler.oebs.listener.test.shouldHaveValue
import no.nav.hjelpemidler.oebs.listener.test.validToken
import kotlin.test.Test

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

            kafkaHistory.shouldForOne {
                it shouldHaveKey endring.saksnummer
                it.shouldHaveValue<ServiceforespørselEndringMessage> { value ->
                    value.data shouldBe endring
                }
            }
        }
}

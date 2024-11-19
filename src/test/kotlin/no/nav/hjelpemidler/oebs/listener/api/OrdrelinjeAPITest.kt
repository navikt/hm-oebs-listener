package no.nav.hjelpemidler.oebs.listener.api

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.oebs.listener.jsonToValue
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
import no.nav.hjelpemidler.oebs.listener.test.OrdrelinjeOebsJsonBuilder
import no.nav.hjelpemidler.oebs.listener.test.TestContext
import no.nav.hjelpemidler.oebs.listener.test.runTest
import no.nav.hjelpemidler.oebs.listener.test.shouldContainRecord
import no.nav.hjelpemidler.oebs.listener.test.shouldNotContainRecord
import no.nav.hjelpemidler.oebs.listener.test.validToken
import java.time.LocalDate
import kotlin.test.Test

/**
 * @see [no.nav.hjelpemidler.oebs.listener.api.ordrelinjeAPI]
 */
class OrdrelinjeAPITest {
    @Test
    fun `Ikke relevant ordrelinje`() =
        runTest {
            val ordrelinje =
                push {
                    serviceforespørseltype = "Behov"
                }

            ordrelinje.serviceforespørseltypeVedtak shouldBe false

            kafkaHistory.shouldNotContainRecord(excludedEventName = "hm-NyOrdrelinje")
            kafkaHistory.shouldNotContainRecord(excludedEventName = "hm-NyOrdrelinje-hotsak")
        }

    @Test
    fun `Ny ordrelinje for Hotsak mottatt`() =
        runTest {
            val ordrelinje =
                push {
                    saksnummer = "1000"
                    kilde = "HOTSAK"
                }

            kafkaHistory.shouldContainRecord(
                expectedKey = ordrelinje.fnrBruker,
                expectedEventName = "hm-NyOrdrelinje-hotsak",
            ) {
                it.shouldContainJsonKeyValue("$.data.saksnummer", ordrelinje.hotSakSaksnummer)
            }
        }

    @Test
    fun `Ny ordrelinje for del mottatt`() =
        runTest {
            val ordrelinje =
                push {
                    saksnummer = "hmdel_1000"
                    kilde = "HOTSAK"
                }

            ordrelinje.delbestilling shouldBe true

            kafkaHistory.shouldNotContainRecord(excludedEventName = "hm-NyOrdrelinje")
            kafkaHistory.shouldNotContainRecord(excludedEventName = "hm-NyOrdrelinje-hotsak")
        }

    @Test
    fun `Ny ordrelinje for Infotrygd mottatt`() =
        runTest {
            val ordrelinje =
                push {
                    vedtaksdato = LocalDate.now().toString()
                    saksblokkOgSaksnr = "X99"
                    kilde = ""
                }

            kafkaHistory.shouldContainRecord(
                expectedKey = ordrelinje.fnrBruker,
                expectedEventName = "hm-NyOrdrelinje",
            ) {
                it.shouldContainJsonKeyValue("$.data.saksblokkOgSaksnr", ordrelinje.saksblokkOgSaksnr)
            }
        }
}

private suspend fun TestContext.push(block: OrdrelinjeOebsJsonBuilder.() -> Unit = {}): OrdrelinjeOebs {
    val ordrelinjeJson = Fixtures.lagOrdrelinjeOebsJson(block)
    val response =
        client.post("/push") {
            validToken()
            contentType(ContentType.Application.Json)
            setBody(ordrelinjeJson)
        }

    response.status shouldBe HttpStatusCode.OK

    val ordrelinje = jsonToValue<OrdrelinjeOebs>(ordrelinjeJson)

    kafkaHistory.shouldContainRecord(
        expectedKey = ordrelinje.fnrBruker,
        expectedEventName = "hm-uvalidert-ordrelinje",
    ) {
        it.shouldContainJsonKeyValue("$.orderLine.fnrBruker", ordrelinje.fnrBruker)
    }

    return ordrelinje
}

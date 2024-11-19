package no.nav.hjelpemidler.oebs.listener.api

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.domain.person.Fødselsnummer
import no.nav.hjelpemidler.domain.person.år
import no.nav.hjelpemidler.oebs.listener.jsonToValue
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
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
    private val fnrBruker = Fødselsnummer(80.år).toString()

    @Test
    fun `Ny ordrelinje for Hotsak mottatt`() =
        runTest {
            val ordrelinje =
                push(
                    Fixtures.lagOrdrelinjeOebsJson(
                        saksnummer = "1000",
                        kilde = "HOTSAK",
                        fnrBruker = fnrBruker,
                    ),
                )

            kafkaHistory.shouldContainRecord(
                expectedKey = fnrBruker,
                expectedEventName = "hm-NyOrdrelinje-hotsak",
            ) {
                it.shouldContainJsonKeyValue("$.data.saksnummer", ordrelinje.hotSakSaksnummer)
            }
        }

    @Test
    fun `Ny ordrelinje for del mottatt`() =
        runTest {
            val ordrelinje =
                push(
                    Fixtures.lagOrdrelinjeOebsJson(
                        saksnummer = "hmdel_1000",
                        kilde = "HOTSAK",
                        fnrBruker = fnrBruker,
                    ),
                )

            ordrelinje.delbestilling shouldBe true

            kafkaHistory.shouldNotContainRecord(excludedEventName = "hm-NyOrdrelinje-hotsak")
        }

    @Test
    fun `Ny ordrelinje for Infotrygd mottatt`() =
        runTest {
            val ordrelinje =
                push(
                    Fixtures.lagOrdrelinjeOebsJson(
                        vedtaksdato = LocalDate.now().toString(),
                        saksblokkOgSaksnr = "X99",
                        kilde = "",
                        fnrBruker = fnrBruker,
                    ),
                )

            kafkaHistory.shouldContainRecord(
                expectedKey = fnrBruker,
                expectedEventName = "hm-NyOrdrelinje",
            ) {
                it.shouldContainJsonKeyValue("$.data.saksblokkOgSaksnr", ordrelinje.saksblokkOgSaksnr)
            }
        }
}

private suspend fun TestContext.push(body: String): OrdrelinjeOebs {
    val response =
        client.post("/push") {
            validToken()
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    response.status shouldBe HttpStatusCode.OK

    val ordrelinje = jsonToValue<OrdrelinjeOebs>(body)

    kafkaHistory.shouldContainRecord(
        expectedKey = ordrelinje.fnrBruker,
        expectedEventName = "hm-uvalidert-ordrelinje",
    ) {
        it.shouldContainJsonKeyValue("$.orderLine.fnrBruker", ordrelinje.fnrBruker)
    }

    return ordrelinje
}

package no.nav.hjelpemidler.oebs.listener.model

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.oebs.listener.jsonToValue
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
import java.time.LocalDate
import kotlin.test.Test

class OrdrelinjeOebsTest {
    @Test
    fun `Parse vedtaksdato to LocalDate`() {
        val vedtaksdato = LocalDate.of(2021, 4, 4)
        val sistOppdatert = LocalDate.of(2021, 4, 5)
        val result =
            jsonToValue<OrdrelinjeOebs>(
                Fixtures.lagOrdrelinjeOebsJson(
                    vedtaksdato = vedtaksdato.toString(),
                    sistOppdatert = sistOppdatert.toString(),
                ),
            )

        result.vedtaksdato shouldBe vedtaksdato
        result.sistOppdatert shouldBe sistOppdatert
    }

    @Test
    fun `Parse serienumre`() {
        val result =
            jsonToValue<OrdrelinjeOebs>(
                Fixtures.lagOrdrelinjeOebsJson(
                    serienumre = """["1", "2", "3"]""",
                ),
            )

        result.serienumre shouldBe listOf("1", "2", "3")
    }

    @Test
    fun `Parse tom dato-streng til LocalDate`() {
        val result =
            jsonToValue<OrdrelinjeOebs>(
                Fixtures.lagOrdrelinjeOebsJson(
                    vedtaksdato = "",
                ),
            )
        result.vedtaksdato shouldBe null
    }

    @Test
    fun `Parse artikkelnr med leading zero`() {
        val artikkelnr = "654321"
        val result =
            jsonToValue<OrdrelinjeOebs>(
                Fixtures.lagOrdrelinjeOebsJson(
                    artikkelnr = artikkelnr,
                ),
            )

        result.artikkelnr shouldBe artikkelnr
    }

    @Test
    fun `Parse int til double`() {
        val antall = "2"
        val result =
            jsonToValue<OrdrelinjeOebs>(
                Fixtures.lagOrdrelinjeOebsJson(
                    antall = antall,
                ),
            )

        result.antall shouldBe 2.0
    }

    @Test
    fun `Parse flyttall til double`() {
        val antall = "2.99"
        val result =
            jsonToValue<OrdrelinjeOebs>(
                Fixtures.lagOrdrelinjeOebsJson(
                    antall = antall,
                ),
            )

        result.antall shouldBe 2.99
    }
}

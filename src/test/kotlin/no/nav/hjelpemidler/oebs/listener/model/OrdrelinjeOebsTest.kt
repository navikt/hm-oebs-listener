package no.nav.hjelpemidler.oebs.listener.model

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
import no.nav.hjelpemidler.oebs.listener.test.OrdrelinjeOebsJsonBuilder
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test

class OrdrelinjeOebsTest {
    @Test
    fun `Parse vedtaksdato til LocalDate`() {
        val vedtaksdato = LocalDate.of(2021, 4, 4)
        val sistOppdatert = LocalDate.of(2021, 4, 5)
        val result =
            lagOrdrelinje {
                this.vedtaksdato = vedtaksdato.toString()
                this.sistOppdatert = sistOppdatert.toString()
            }

        result.vedtaksdato shouldBe vedtaksdato
        result.sistOppdatert shouldBe sistOppdatert
    }

    @Test
    fun `Parse serienumre`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1", "2", "3"]"""
            }

        result.serienumre shouldBe listOf("1", "2", "3")
    }

    @Test
    fun `Parse tom dato-streng til LocalDate`() {
        val result =
            lagOrdrelinje {
                vedtaksdato = ""
            }

        result.vedtaksdato shouldBe null
    }

    @Test
    fun `Parse artikkelnr med leading zero`() {
        val artikkelnr = "012345"
        val result =
            lagOrdrelinje {
                this.artikkelnr = artikkelnr
            }

        result.artikkelnr shouldBe artikkelnr
    }

    @Test
    fun `Parse heltall til Double`() {
        val result =
            lagOrdrelinje {
                antall = "2"
            }

        result.antall shouldBe 2.0
    }

    @Test
    fun `Parse flyttall til double`() {
        val result =
            lagOrdrelinje {
                antall = "2.99"
            }

        result.antall shouldBe 2.99
    }

    @Test
    fun `Parse FørsteGangsUtlån, FørsteTransaksjonsDato and AntallUtlån`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1", "2", "3"]"""
                førsteGangsUtlån = """N, Y, """
                førsteTransaksjonsDato = """01-JAN-25, , 04-OCT-25"""
                antallUtlån = """2, 1, """
            }.utlånsstatistikk()

        result[0].førstegangsUtlån shouldBe false
        result[1].førstegangsUtlån shouldBe true
        result[2].førstegangsUtlån shouldBe null

        result[0].førsteTransaksjonsDato shouldBe LocalDate.of(2025, Month.JANUARY, 1)
        result[1].førsteTransaksjonsDato shouldBe null
        result[2].førsteTransaksjonsDato shouldBe LocalDate.of(2025, Month.OCTOBER, 4)

        result[0].antallUtlån shouldBe 2
        result[1].antallUtlån shouldBe 1
        result[2].antallUtlån shouldBe null
    }

    @Test
    fun `Parse FørsteGangsUtlån mangler`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1"]"""
                førsteGangsUtlån = ""
                førsteTransaksjonsDato = ""
            }.utlånsstatistikk()

        result[0].førstegangsUtlån shouldBe null
        result[0].førsteTransaksjonsDato shouldBe null
        result[0].antallUtlån shouldBe null
    }

    @Test
    fun `Parse FørsteGangsUtlån første mangler`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1", "2"]"""
                førsteGangsUtlån = ", Y"
                førsteTransaksjonsDato = ", 20-MAR-25"
                antallUtlån = ", 1"
            }.utlånsstatistikk()

        result[0].førstegangsUtlån shouldBe null
        result[1].førstegangsUtlån shouldBe true

        result[0].førsteTransaksjonsDato shouldBe null
        result[1].førsteTransaksjonsDato shouldBe LocalDate.of(2025, Month.MARCH, 20)

        result[0].antallUtlån shouldBe null
        result[1].antallUtlån shouldBe 1
    }

    private fun lagOrdrelinje(
        block: OrdrelinjeOebsJsonBuilder.() -> Unit = {
        },
    ): OrdrelinjeOebs = jsonToValue<OrdrelinjeOebs>(Fixtures.lagOrdrelinjeOebsJson(block))
}

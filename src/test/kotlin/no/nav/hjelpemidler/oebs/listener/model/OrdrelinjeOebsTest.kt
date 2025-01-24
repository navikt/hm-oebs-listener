package no.nav.hjelpemidler.oebs.listener.model

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.oebs.listener.test.Fixtures
import no.nav.hjelpemidler.oebs.listener.test.OrdrelinjeOebsJsonBuilder
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import java.time.LocalDate
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
    fun `Parse FørsteGangsUtlån and AntallUtlån`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1", "2", "3"]"""
                førsteGangsUtlån = """N, Y, """
                antallUtlån = """2, 1, """
            }

        result.serienumreStatistikk()[0].førsteGangsUtlån shouldBe false
        result.serienumreStatistikk()[1].førsteGangsUtlån shouldBe true
        result.serienumreStatistikk()[2].førsteGangsUtlån shouldBe null

        result.serienumreStatistikk()[0].antallUtlån shouldBe 2
        result.serienumreStatistikk()[1].antallUtlån shouldBe 1
        result.serienumreStatistikk()[2].antallUtlån shouldBe null
    }

    @Test
    fun `Parse FørsteGangsUtlån mangler`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1"]"""
                førsteGangsUtlån = ""
            }

        result.serienumreStatistikk()[0].førsteGangsUtlån shouldBe null
        result.serienumreStatistikk()[0].antallUtlån shouldBe null
    }

    @Test
    fun `Parse FørsteGangsUtlån første mangler`() {
        val result =
            lagOrdrelinje {
                serienumre = """["1", "2"]"""
                førsteGangsUtlån = ", Y"
                antallUtlån = ", 1"
            }

        result.serienumreStatistikk()[0].førsteGangsUtlån shouldBe null
        result.serienumreStatistikk()[1].førsteGangsUtlån shouldBe true

        result.serienumreStatistikk()[0].antallUtlån shouldBe null
        result.serienumreStatistikk()[1].antallUtlån shouldBe 1
    }

    private fun lagOrdrelinje(
        block: OrdrelinjeOebsJsonBuilder.() -> Unit = {
        },
    ): OrdrelinjeOebs = jsonToValue<OrdrelinjeOebs>(Fixtures.lagOrdrelinjeOebsJson(block))
}

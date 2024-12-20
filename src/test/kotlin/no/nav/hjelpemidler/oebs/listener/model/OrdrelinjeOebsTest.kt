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

    private fun lagOrdrelinje(block: OrdrelinjeOebsJsonBuilder.() -> Unit = {}): OrdrelinjeOebs {
        return jsonToValue<OrdrelinjeOebs>(Fixtures.lagOrdrelinjeOebsJson(block))
    }
}

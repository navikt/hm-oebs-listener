package no.nav.hjelpemidler

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.model.Statusinfo
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

internal class DateTimeTest {

    @ExperimentalTime
    @Test
    fun `Parse vedtaksdato to LocalDate`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())

        val result: Statusinfo = mapper.readValue(
            """
            {
                "System": "DIGIHOT",
                "IncidentNummer":21072339,
                "IncidentStatus": "Open",
                "IncidentType": "Vedtak Infotrygd",
                "IncidentSoknadType": "HJDAAN",
                "IncidentVedtakDato": "2021-04-04",
                "IncidentSoknad": "S",
                "IncidentResultat": "I",
                "IncidentRef": "A01",
                "OrdreNumber":7068818,
                "LineNumber":1,
                "ShipmentNumber":1,
                "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                "CategoryDescription": "",
                "OrderedItem":149305,
                "User_ItemType": "Hjelpemiddel",
                "Quantity":1,
                "AccountNumber": "XXXXXXXXXXX",
                "LastUpdateDate": "2021-04-05"
            }
            """.trimIndent()
        )

        println(result.toString())
        assertEquals(LocalDate.of(2021, 4, 4), result.vedtaksdato)
        assertEquals(LocalDate.of(2021, 4, 5), result.sistOppdatert)
        println(mapper.writeValueAsString(result))
    }
}

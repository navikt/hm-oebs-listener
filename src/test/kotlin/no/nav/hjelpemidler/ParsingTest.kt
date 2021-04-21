package no.nav.hjelpemidler

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

internal class ParsingTest {

    @ExperimentalTime
    @Test
    fun `Parse vedtaksdato to LocalDate`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())

        val result: OrdrelinjeOebs = mapper.readValue(
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

    @ExperimentalTime
    @Test
    fun `Parse tom dato-streng til LocalDate`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())

        val result: OrdrelinjeOebs = mapper.readValue(
            """
            {
                "System": "DIGIHOT",
                "IncidentNummer":21072339,
                "IncidentStatus": "Open",
                "IncidentType": "Vedtak Infotrygd",
                "IncidentSoknadType": "HJDAAN",
                "IncidentVedtakDato": "",
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
        assertEquals(null, result.vedtaksdato)
        assertEquals(LocalDate.of(2021, 4, 5), result.sistOppdatert)
        println(mapper.writeValueAsString(result))
    }

    @ExperimentalTime
    @Test
    fun `Parse artikkelnr med leading zero`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())

        val result: OrdrelinjeOebs = mapper.readValue(
            """
            {
                "System": "DIGIHOT",
                "IncidentNummer":21072339,
                "IncidentStatus": "Open",
                "IncidentType": "Vedtak Infotrygd",
                "IncidentSoknadType": "HJDAAN",
                "IncidentVedtakDato": "",
                "IncidentSoknad": "S",
                "IncidentResultat": "I",
                "IncidentRef": "A01",
                "OrdreNumber":7068818,
                "LineNumber":1,
                "ShipmentNumber":1,
                "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                "CategoryDescription": "",
                "OrderedItem": "012345",
                "User_ItemType": "Hjelpemiddel",
                "Quantity":1,
                "AccountNumber": "XXXXXXXXXXX",
                "LastUpdateDate": "2021-04-05"
            }
            """.trimIndent()
        )

        println(result.toString())
        assertEquals("012345", result.artikkelnr)
        println(mapper.writeValueAsString(result))
    }

    @Test
    fun `Parse XML`() {
        val mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())

        val result: OrdrelinjeOebs = mapper.readValue(
            """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:IncidentNummer/>
                    <ki:IncidentStatus/>
                    <ki:IncidentType/>
                    <ki:IncidentSoknadType/>
                    <ki:IncidentVedtakDato/>
                    <ki:IncidentSoknad/>
                    <ki:IncidentResultat/>
                    <ki:IncidentRef/>
                    <ki:OrdreNumber>0178581</ki:OrdreNumber>
                    <ki:LineNumber>6</ki:LineNumber>
                    <ki:ShipmentNumber>2</ki:ShipmentNumber>
                    <ki:Description>Putevibrator FlexiBlink Life med quote: &quot; æøå </ki:Description>
                    <ki:CategoryDescription/>
                    <ki:OrderedItem>012345</ki:OrderedItem>
                    <ki:User_ItemType>Hjelpemiddel</ki:User_ItemType>
                    <ki:Quantity>3</ki:Quantity>
                    <ki:AccountNumber>01127622634</ki:AccountNumber>
                    <ki:OeBSInternFnr>01127622634</ki:OeBSInternFnr>
                    <ki:LastUpdateDate>2021-04-15</ki:LastUpdateDate>
                </ki:StatusInfo>
            """.trimIndent()
        )

        println(result.toString())
        assertEquals("012345", result.artikkelnr)
        println(mapper.writeValueAsString(result))
    }
}

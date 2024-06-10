package no.nav.hjelpemidler.oebs

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

class ParsingTest {
    @ExperimentalTime
    @Test
    fun `Parse vedtaksdato to LocalDate`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                {
                    "System": "DIGIHOT",
                    "Id":585,
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
                    "CategoryNum": "122291",
                    "User_ItemType": "Hjelpemiddel",
                    "Quantity":1,
                    "ShippingQuantityUom": "STK",
                    "AccountNumber": "XXXXXXXXXXX",
                    "EgenAnsatt": "Y",
                    "LastUpdateDate": "2021-04-05",
                    "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        println(result.toString())
        assertEquals(LocalDate.of(2021, 4, 4), result.vedtaksdato)
        assertEquals(LocalDate.of(2021, 4, 5), result.sistOppdatert)
        println(mapper.writeValueAsString(result))
    }

    @ExperimentalTime
    @Test
    fun `Parse serienumre`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                {
                    "System": "DIGIHOT",
                    "Id":585,
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
                    "CategoryNum": "122291",
                    "User_ItemType": "Hjelpemiddel",
                    "Quantity":1,
                    "ShippingQuantityUom": "STK",
                    "AccountNumber": "XXXXXXXXXXX",
                    "EgenAnsatt": "Y",
                    "LastUpdateDate": "2021-04-05",
                    "SendTilAddresse1": "1234 Oslo, bla bla bla",
                    "SerieNummerListe":["660383", "693065", "726136", "733046"]
                }
                """.trimIndent(),
            )

        println(result.toString())
        val expected = listOf("660383", "693065", "726136", "733046")
        for (serienr in result.serienumre ?: listOf()) { //  RåOrdrelinje.serienumreListeFraRå(result.serienumreRå!!)) {
            assert(expected.contains(serienr)) { "Expected to find only the serial numbers in the raw example" }
        }
    }

    @ExperimentalTime
    @Test
    fun `Parse tom dato-streng til LocalDate`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                {
                    "System": "DIGIHOT",
                    "Id":585,
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
                    "CategoryNum": "122291",
                    "User_ItemType": "Hjelpemiddel",
                    "Quantity":1,
                    "ShippingQuantityUom": "STK",
                    "AccountNumber": "XXXXXXXXXXX",
                    "EgenAnsatt": "N",
                    "LastUpdateDate": "2021-04-05",
                    "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
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
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                {
                    "System": "DIGIHOT",
                    "Id":585,
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
                    "CategoryNum": "122291",
                    "User_ItemType": "Hjelpemiddel",
                    "Quantity":1,
                    "ShippingQuantityUom": "STK",
                    "AccountNumber": "XXXXXXXXXXX",
                    "EgenAnsatt": "Y",
                    "LastUpdateDate": "2021-04-05",
                    "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        println(result.toString())
        assertEquals("012345", result.artikkelnr)
        println(mapper.writeValueAsString(result))
    }

    @Test
    fun `Parse XML`() {
        val mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:Id>645</ki:Id>
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
                    <ki:CategoryNum>012345</ki:CategoryNum>
                    <ki:User_ItemType>Hjelpemiddel</ki:User_ItemType>
                    <ki:Quantity>3</ki:Quantity>
                    <ki:ShippingQuantityUom>STK</ki:ShippingQuantityUom>
                    <ki:AccountNumber>01127622634</ki:AccountNumber>
                    <ki:EgenAnsatt>N</ki:EgenAnsatt>
                    <ki:LastUpdateDate>2021-04-15</ki:LastUpdateDate>
                    <ki:SendTilAddresse1>1234 Oslo, bla bla bla</ki:SendTilAddresse1>
                </ki:StatusInfo>
                """.trimIndent(),
            )

        println(result.toString())
        assertEquals("012345", result.artikkelnr)
        println(mapper.writeValueAsString(result))
    }

    @ExperimentalTime
    @Test
    fun `Parse int til double`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                {
                    "System": "DIGIHOT",
                    "Id":585,
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
                    "CategoryNum": "122291",
                    "User_ItemType": "Hjelpemiddel",
                    "Quantity":2,
                    "ShippingQuantityUom": "STK",
                    "AccountNumber": "XXXXXXXXXXX",
                    "EgenAnsatt": "N",
                    "LastUpdateDate": "2021-04-05",
                    "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        assertEquals(2.0, result.antall)
    }

    @Test
    fun `Parse int til double for XML`() {
        val mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:Id>123</ki:Id>
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
                    <ki:CategoryNum>012345</ki:CategoryNum>
                    <ki:User_ItemType>Hjelpemiddel</ki:User_ItemType>
                    <ki:Quantity>3</ki:Quantity>
                    <ki:ShippingQuantityUom>STK</ki:ShippingQuantityUom>
                    <ki:AccountNumber>01127622634</ki:AccountNumber>
                    <ki:EgenAnsatt>N</ki:EgenAnsatt>
                    <ki:LastUpdateDate>2021-04-15</ki:LastUpdateDate>
                    <ki:SendTilAddresse1>1234 Oslo, bla bla bla</ki:SendTilAddresse1>
                </ki:StatusInfo>
                """.trimIndent(),
            )

        assertEquals(3.0, result.antall)
    }

    @ExperimentalTime
    @Test
    fun `Parse desimaltal til double`() {
        val mapper = jacksonObjectMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                {
                    "System": "DIGIHOT",
                    "Id":234,
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
                    "CategoryNum": "122291",
                    "User_ItemType": "Hjelpemiddel",
                    "Quantity":2.999,
                    "ShippingQuantityUom": "STK",
                    "AccountNumber": "XXXXXXXXXXX",
                    "EgenAnsatt": "Y",
                    "LastUpdateDate": "2021-04-05",
                    "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        assertEquals(2.999, result.antall)
    }

    @Test
    fun `Parse desimaltal til double for XML`() {
        val mapper = XmlMapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        val result: OrdrelinjeOebs =
            mapper.readValue(
                """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:Id>565</ki:Id>
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
                    <ki:CategoryNum>012345</ki:CategoryNum>
                    <ki:User_ItemType>Hjelpemiddel</ki:User_ItemType>
                    <ki:Quantity>3.999</ki:Quantity>
                    <ki:ShippingQuantityUom>STK</ki:ShippingQuantityUom>
                    <ki:AccountNumber>01127622634</ki:AccountNumber>
                    <ki:EgenAnsatt>N</ki:EgenAnsatt>
                    <ki:LastUpdateDate>2021-04-15</ki:LastUpdateDate>
                    <ki:SendTilAddresse1>1234 Oslo, bla bla bla</ki:SendTilAddresse1>
                </ki:StatusInfo>
                """.trimIndent(),
            )

        assertEquals(3.999, result.antall)
    }
}

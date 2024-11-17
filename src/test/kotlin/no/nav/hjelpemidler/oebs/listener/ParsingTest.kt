package no.nav.hjelpemidler.oebs.listener

import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.oebs.listener.model.OrdrelinjeOebs
import java.time.LocalDate
import kotlin.test.Test

class ParsingTest {
    @Test
    fun `Parse vedtaksdato to LocalDate`() {
        val result: OrdrelinjeOebs =
            jsonToValue(
                """
                {
                  "System": "DIGIHOT",
                  "Id": 585,
                  "IncidentNummer": 21072339,
                  "IncidentStatus": "Open",
                  "IncidentType": "Vedtak Infotrygd",
                  "IncidentSoknadType": "HJDAAN",
                  "IncidentVedtakDato": "2021-04-04",
                  "IncidentSoknad": "S",
                  "IncidentResultat": "I",
                  "IncidentRef": "A01",
                  "OrdreNumber": 7068818,
                  "LineNumber": 1,
                  "ShipmentNumber": 1,
                  "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                  "CategoryDescription": "",
                  "OrderedItem": 149305,
                  "CategoryNum": "122291",
                  "User_ItemType": "Hjelpemiddel",
                  "Quantity": 1,
                  "ShippingQuantityUom": "STK",
                  "AccountNumber": "XXXXXXXXXXX",
                  "EgenAnsatt": "Y",
                  "LastUpdateDate": "2021-04-05",
                  "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        result.vedtaksdato shouldBe LocalDate.of(2021, 4, 4)
        result.sistOppdatert shouldBe LocalDate.of(2021, 4, 5)
    }

    @Test
    fun `Parse serienumre`() {
        val result: OrdrelinjeOebs =
            jsonToValue(
                """
                {
                  "System": "DIGIHOT",
                  "Id": 585,
                  "IncidentNummer": 21072339,
                  "IncidentStatus": "Open",
                  "IncidentType": "Vedtak Infotrygd",
                  "IncidentSoknadType": "HJDAAN",
                  "IncidentVedtakDato": "2021-04-04",
                  "IncidentSoknad": "S",
                  "IncidentResultat": "I",
                  "IncidentRef": "A01",
                  "OrdreNumber": 7068818,
                  "LineNumber": 1,
                  "ShipmentNumber": 1,
                  "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                  "CategoryDescription": "",
                  "OrderedItem": 149305,
                  "CategoryNum": "122291",
                  "User_ItemType": "Hjelpemiddel",
                  "Quantity": 1,
                  "ShippingQuantityUom": "STK",
                  "AccountNumber": "XXXXXXXXXXX",
                  "EgenAnsatt": "Y",
                  "LastUpdateDate": "2021-04-05",
                  "SendTilAddresse1": "1234 Oslo, bla bla bla",
                  "SerieNummerListe": [
                    "660383",
                    "693065",
                    "726136",
                    "733046"
                  ]
                }
                """.trimIndent(),
            )

        result.serienumre shouldBe listOf("660383", "693065", "726136", "733046")
    }

    @Test
    fun `Parse tom dato-streng til LocalDate`() {
        val result: OrdrelinjeOebs =
            jsonToValue(
                """
                {
                  "System": "DIGIHOT",
                  "Id": 585,
                  "IncidentNummer": 21072339,
                  "IncidentStatus": "Open",
                  "IncidentType": "Vedtak Infotrygd",
                  "IncidentSoknadType": "HJDAAN",
                  "IncidentVedtakDato": "",
                  "IncidentSoknad": "S",
                  "IncidentResultat": "I",
                  "IncidentRef": "A01",
                  "OrdreNumber": 7068818,
                  "LineNumber": 1,
                  "ShipmentNumber": 1,
                  "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                  "CategoryDescription": "",
                  "OrderedItem": 149305,
                  "CategoryNum": "122291",
                  "User_ItemType": "Hjelpemiddel",
                  "Quantity": 1,
                  "ShippingQuantityUom": "STK",
                  "AccountNumber": "XXXXXXXXXXX",
                  "EgenAnsatt": "N",
                  "LastUpdateDate": "2021-04-05",
                  "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        result.vedtaksdato shouldBe null
        result.sistOppdatert shouldBe LocalDate.of(2021, 4, 5)
    }

    @Test
    fun `Parse artikkelnr med leading zero`() {
        val result: OrdrelinjeOebs =
            jsonToValue(
                """
                {
                  "System": "DIGIHOT",
                  "Id": 585,
                  "IncidentNummer": 21072339,
                  "IncidentStatus": "Open",
                  "IncidentType": "Vedtak Infotrygd",
                  "IncidentSoknadType": "HJDAAN",
                  "IncidentVedtakDato": "",
                  "IncidentSoknad": "S",
                  "IncidentResultat": "I",
                  "IncidentRef": "A01",
                  "OrdreNumber": 7068818,
                  "LineNumber": 1,
                  "ShipmentNumber": 1,
                  "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                  "CategoryDescription": "",
                  "OrderedItem": "012345",
                  "CategoryNum": "122291",
                  "User_ItemType": "Hjelpemiddel",
                  "Quantity": 1,
                  "ShippingQuantityUom": "STK",
                  "AccountNumber": "XXXXXXXXXXX",
                  "EgenAnsatt": "Y",
                  "LastUpdateDate": "2021-04-05",
                  "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        result.artikkelnr shouldBe "012345"
    }

    @Test
    fun `Parse XML`() {
        val result: OrdrelinjeOebs =
            xmlToValue(
                """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:Id>645</ki:Id>
                    <ki:IncidentNummer />
                    <ki:IncidentStatus />
                    <ki:IncidentType />
                    <ki:IncidentSoknadType />
                    <ki:IncidentVedtakDato />
                    <ki:IncidentSoknad />
                    <ki:IncidentResultat />
                    <ki:IncidentRef />
                    <ki:OrdreNumber>0178581</ki:OrdreNumber>
                    <ki:LineNumber>6</ki:LineNumber>
                    <ki:ShipmentNumber>2</ki:ShipmentNumber>
                    <ki:Description>Putevibrator FlexiBlink Life med quote: &quot; æøå</ki:Description>
                    <ki:CategoryDescription />
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

        result.artikkelnr shouldBe "012345"
    }

    @Test
    fun `Parse int til double`() {
        val result: OrdrelinjeOebs =
            jsonToValue(
                """
                {
                  "System": "DIGIHOT",
                  "Id": 585,
                  "IncidentNummer": 21072339,
                  "IncidentStatus": "Open",
                  "IncidentType": "Vedtak Infotrygd",
                  "IncidentSoknadType": "HJDAAN",
                  "IncidentVedtakDato": "",
                  "IncidentSoknad": "S",
                  "IncidentResultat": "I",
                  "IncidentRef": "A01",
                  "OrdreNumber": 7068818,
                  "LineNumber": 1,
                  "ShipmentNumber": 1,
                  "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                  "CategoryDescription": "",
                  "OrderedItem": "012345",
                  "CategoryNum": "122291",
                  "User_ItemType": "Hjelpemiddel",
                  "Quantity": 2,
                  "ShippingQuantityUom": "STK",
                  "AccountNumber": "XXXXXXXXXXX",
                  "EgenAnsatt": "N",
                  "LastUpdateDate": "2021-04-05",
                  "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        result.antall shouldBe 2.0
    }

    @Test
    fun `Parse int til double for XML`() {
        val result: OrdrelinjeOebs =
            xmlToValue(
                """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:Id>123</ki:Id>
                    <ki:IncidentNummer />
                    <ki:IncidentStatus />
                    <ki:IncidentType />
                    <ki:IncidentSoknadType />
                    <ki:IncidentVedtakDato />
                    <ki:IncidentSoknad />
                    <ki:IncidentResultat />
                    <ki:IncidentRef />
                    <ki:OrdreNumber>0178581</ki:OrdreNumber>
                    <ki:LineNumber>6</ki:LineNumber>
                    <ki:ShipmentNumber>2</ki:ShipmentNumber>
                    <ki:Description>Putevibrator FlexiBlink Life med quote: &quot; æøå</ki:Description>
                    <ki:CategoryDescription />
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

        result.antall shouldBe 3.0
    }

    @Test
    fun `Parse flyttall til double`() {
        val result: OrdrelinjeOebs =
            jsonToValue(
                """
                {
                  "System": "DIGIHOT",
                  "Id": 234,
                  "IncidentNummer": 21072339,
                  "IncidentStatus": "Open",
                  "IncidentType": "Vedtak Infotrygd",
                  "IncidentSoknadType": "HJDAAN",
                  "IncidentVedtakDato": "",
                  "IncidentSoknad": "S",
                  "IncidentResultat": "I",
                  "IncidentRef": "A01",
                  "OrdreNumber": 7068818,
                  "LineNumber": 1,
                  "ShipmentNumber": 1,
                  "Description": "Rullator 4hjul Topro Olympos M b71 h79-95 sh60 sml",
                  "CategoryDescription": "",
                  "OrderedItem": "012345",
                  "CategoryNum": "122291",
                  "User_ItemType": "Hjelpemiddel",
                  "Quantity": 2.999,
                  "ShippingQuantityUom": "STK",
                  "AccountNumber": "XXXXXXXXXXX",
                  "EgenAnsatt": "Y",
                  "LastUpdateDate": "2021-04-05",
                  "SendTilAddresse1": "1234 Oslo, bla bla bla"
                }
                """.trimIndent(),
            )

        result.antall shouldBe 2.999
    }

    @Test
    fun `Parse flyttall til double for XML`() {
        val result: OrdrelinjeOebs =
            xmlToValue(
                """
                <ki:StatusInfo xmlns:ki="urn:nav.no/ordre/statusinfo">
                    <ki:System>DIGIHOT</ki:System>
                    <ki:Id>565</ki:Id>
                    <ki:IncidentNummer />
                    <ki:IncidentStatus />
                    <ki:IncidentType />
                    <ki:IncidentSoknadType />
                    <ki:IncidentVedtakDato />
                    <ki:IncidentSoknad />
                    <ki:IncidentResultat />
                    <ki:IncidentRef />
                    <ki:OrdreNumber>0178581</ki:OrdreNumber>
                    <ki:LineNumber>6</ki:LineNumber>
                    <ki:ShipmentNumber>2</ki:ShipmentNumber>
                    <ki:Description>Putevibrator FlexiBlink Life med quote: &quot; æøå</ki:Description>
                    <ki:CategoryDescription />
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

        result.antall shouldBe 3.999
    }
}

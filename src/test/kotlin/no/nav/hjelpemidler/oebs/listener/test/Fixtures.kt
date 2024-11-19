package no.nav.hjelpemidler.oebs.listener.test

import no.nav.hjelpemidler.oebs.listener.api.SFEndringType
import no.nav.hjelpemidler.oebs.listener.api.ServiceforespørselEndring
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

object Fixtures {
    fun lagServiceforespørselEndring() =
        ServiceforespørselEndring(
            system = "HOTSAK",
            id = "1",
            sfnummer = "2",
            saksnummer = "3",
            antallKostnadslinjer = "1",
            ordre = emptyList(),
            status = SFEndringType.OPPRETTET,
        )

    @Language("JSON")
    fun lagOrdrelinjeOebsJson(
        serviceforespørseltype: String = "Vedtak Infotrygd",
        vedtaksdato: String = "",
        saksblokkOgSaksnr: String = "",
        saksnummer: String = "",
        kilde: String = "",
        artikkelnr: String = "123456",
        serienumre: String = "[]",
        hjelpemiddeltype: String = "Hjelpemiddel",
        antall: String = "1",
        fnrBruker: String = "XXXXXXXXXXX",
        sistOppdatert: String = LocalDateTime.now().toString(),
    ) = """
        {
          "System": "DIGIHOT",
          "Id": "1000",
          "IncidentNummer": "1100",
          "IncidentStatus": "Open",
          "IncidentType": "$serviceforespørseltype",
          "IncidentSoknadType": "HJDAAN",
          "IncidentVedtakDato": "$vedtaksdato",
          "IncidentSoknad": "S",
          "IncidentResultat": "IM",
          "IncidentRef": "$saksblokkOgSaksnr",
          "ReferanseNummer": "$saksnummer",
          "Kilde": "$kilde",
          "OrdreNumber": "1200",
          "LineNumber": "1",
          "ShipmentNumber": "1",
          "CategoryDescription": "",
          "SendTilAddresse1": "",
          "OrderedItem": "$artikkelnr",
          "SerieNummerListe": $serienumre,
          "CategoryNum": "",
          "User_ItemType": "$hjelpemiddeltype",
          "Quantity": "$antall",
          "ShippingQuantityUom": "STK",
          "ShippingInstructions": "",
          "AccountNumber": "$fnrBruker",
          "EgenAnsatt": "N",
          "LastUpdateDate": "$sistOppdatert",
          "Description": ""
        }
        """.trimIndent()
}

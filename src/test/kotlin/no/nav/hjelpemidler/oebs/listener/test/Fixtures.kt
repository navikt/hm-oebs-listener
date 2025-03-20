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

    fun lagOrdrelinjeOebsJson(block: OrdrelinjeOebsJsonBuilder.() -> Unit = {}) = OrdrelinjeOebsJsonBuilder().apply(block).invoke()
}

class OrdrelinjeOebsJsonBuilder {
    var serviceforespørseltype: String = "Vedtak Infotrygd"
    var vedtaksdato: String = ""
    var saksblokkOgSaksnr: String = ""
    var saksnummer: String = ""
    var kilde: String = ""
    var artikkelnr: String = "123456"
    var serienumre: String = "[]"
    var hjelpemiddeltype: String = "Hjelpemiddel"
    var antall: String = "1"
    var fnrBruker: String = "XXXXXXXXXXX"
    var sistOppdatert: String = LocalDateTime.now().toString()
    var førsteGangsUtlån: String? = null
    var førsteTransaksjonsDato: String? = null
    var antallUtlån: String? = null

    @Language("JSON")
    operator fun invoke() =
        """
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
          "Description": "",
          "ForsteGangsUtlan": "$førsteGangsUtlån",
          "ForsteTransDato": "$førsteTransaksjonsDato",
          "AntUtlan": "$antallUtlån"
        }
        """.trimIndent()
}

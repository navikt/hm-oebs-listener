package no.nav.hjelpemidler.oebs.listener.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrdrelinjeOebs(
    @JsonProperty("System")
    val mottakendeSystem: String,
    @JsonProperty("Id")
    val oebsId: Int,
    @JsonProperty("IncidentNummer")
    val serviceforespørsel: Int,
    @JsonProperty("IncidentStatus")
    val serviceforespørselstatus: String,
    @JsonProperty("IncidentType")
    val serviceforespørseltype: String,
    @JsonProperty("IncidentSoknadType")
    val søknadstype: String,
    // N.B.: Hvis dato er "" i meldingen blir den til null under deserialisering og forblir null under serialisering (utgående JSON)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty("IncidentVedtakDato")
    val vedtaksdato: LocalDate?,
    @JsonProperty("IncidentSoknad")
    val søknad: String,
    @JsonProperty("ReferanseNummer")
    val hotSakSaksnummer: String?,
    @JsonProperty("Kilde")
    val kilde: String?,
    @JsonProperty("IncidentResultat")
    val resultat: String,
    @JsonProperty("IncidentRef")
    val saksblokkOgSaksnr: String?,
    @JsonProperty("OrdreNumber")
    val ordrenr: Int,
    @JsonProperty("LineNumber")
    val ordrelinje: Int,
    @JsonProperty("ShipmentNumber")
    val delordrelinje: Int,
    @JsonProperty("Description")
    val artikkelbeskrivelse: String,
    @JsonProperty("CategoryDescription")
    val produktgruppe: String,
    @JsonProperty("CategoryNum")
    val produktgruppeNr: String,
    @JsonProperty("OrderedItem")
    val artikkelnr: String,
    @JsonProperty("User_ItemType")
    val hjelpemiddeltype: String,
    @JsonProperty("Quantity")
    val antall: Double,
    @JsonProperty("ShippingQuantityUom")
    val enhet: String,
    @JsonProperty("ShippingInstructions")
    val skipningsinstrukser: String?,
    @JsonProperty("AccountNumber")
    var fnrBruker: String,
    // Siden dette feltet har informasjon som kan bli utdatert blir ikke dette brukt.
    // Uansett skal en NAV-ansatt også få se hjelpemidlene som er på vei til dem.
    @JsonProperty("EgenAnsatt")
    val egenAnsatt: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty("LastUpdateDate")
    val sistOppdatert: LocalDate,
    @JsonProperty("SendTilAddresse1")
    var sendtTilAdresse: String,
    @JsonProperty("SerieNummerListe")
    val serienumre: List<String>? = emptyList(),
) {
    /**
     * I OeBS får alt fra DigiHoT kilde = HOTSAK
     */
    val opprettetFraHotsak: Boolean @JsonIgnore get() = kilde == "HOTSAK"

    fun fiksTommeSerienumre(): OrdrelinjeOebs = copy(serienumre = serienumre?.map { it.trim() }?.filter { it != "" })

    fun toRåOrdrelinje(): RåOrdrelinje =
        RåOrdrelinje(
            mottakendeSystem = mottakendeSystem,
            oebsId = oebsId,
            serviceforespørsel = serviceforespørsel,
            serviceforespørselstatus = serviceforespørselstatus,
            serviceforespørseltype = serviceforespørseltype,
            søknadstype = søknadstype,
            vedtaksdato = vedtaksdato,
            søknad = søknad,
            hotSakSaksnummer = hotSakSaksnummer,
            kilde = kilde,
            resultat = resultat,
            saksblokkOgSaksnr = saksblokkOgSaksnr,
            ordrenr = ordrenr,
            ordrelinje = ordrelinje,
            delordrelinje = delordrelinje,
            artikkelbeskrivelse = artikkelbeskrivelse,
            produktgruppe = produktgruppe,
            produktgruppeNr = produktgruppeNr,
            artikkelnr = artikkelnr,
            hjelpemiddeltype = hjelpemiddeltype,
            antall = antall,
            enhet = enhet,
            fnrBruker = fnrBruker,
            egenAnsatt = egenAnsatt,
            sistOppdatert = sistOppdatert,
            sendtTilAdresse = sendtTilAdresse,
            serienumre = serienumre ?: listOf(),
        )

    fun toHotsakOrdrelinje(): HotsakOrdrelinje =
        HotsakOrdrelinje(
            mottakendeSystem = mottakendeSystem,
            oebsId = oebsId,
            serviceforespørsel = serviceforespørsel,
            serviceforespørselstatus = serviceforespørselstatus,
            serviceforespørseltype = serviceforespørseltype,
            søknadstype = søknadstype,
            vedtaksdato = vedtaksdato,
            søknad = søknad,
            resultat = resultat,
            saksnummer = hotSakSaksnummer ?: "",
            ordrenr = ordrenr,
            ordrelinje = ordrelinje,
            delordrelinje = delordrelinje,
            artikkelbeskrivelse = artikkelbeskrivelse,
            produktgruppe = produktgruppe,
            produktgruppeNr = produktgruppeNr,
            artikkelnr = artikkelnr,
            hjelpemiddeltype = hjelpemiddeltype,
            antall = antall,
            enhet = enhet,
            fnrBruker = fnrBruker,
            egenAnsatt = egenAnsatt,
            sistOppdatert = sistOppdatert,
            sendtTilAdresse = sendtTilAdresse,
        )

    fun toOrdrelinje(): InfotrygdOrdrelinje =
        InfotrygdOrdrelinje(
            mottakendeSystem = mottakendeSystem,
            oebsId = oebsId,
            serviceforespørsel = serviceforespørsel,
            serviceforespørselstatus = serviceforespørselstatus,
            serviceforespørseltype = serviceforespørseltype,
            søknadstype = søknadstype,
            vedtaksdato = vedtaksdato,
            søknad = søknad,
            resultat = resultat,
            saksblokkOgSaksnr = saksblokkOgSaksnr ?: "",
            ordrenr = ordrenr,
            ordrelinje = ordrelinje,
            delordrelinje = delordrelinje,
            artikkelbeskrivelse = artikkelbeskrivelse,
            produktgruppe = produktgruppe,
            produktgruppeNr = produktgruppeNr,
            artikkelnr = artikkelnr,
            hjelpemiddeltype = hjelpemiddeltype,
            antall = antall,
            enhet = enhet,
            fnrBruker = fnrBruker,
            egenAnsatt = egenAnsatt,
            sistOppdatert = sistOppdatert,
            sendtTilAdresse = sendtTilAdresse,
        )
}

package no.nav.hjelpemidler.oebs.listener.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

const val HOTSAK = "HOTSAK"
const val INFOTRYGD = "INFOTRYGD"

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
    // N.B.: Viss dato er "" i meldinga blir den til null under deserialisering og forblir null under serialisering (utgåande JSON)
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
    // Sidan dette feltet har informasjon som kan bli utdatert blir ikkje dette brukt.
    // Uansett skal ein NAV-ansatt også få sjå hjelpemidla som er på veg til dei.
    @JsonProperty("EgenAnsatt")
    val egenAnsatt: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonProperty("LastUpdateDate")
    val sistOppdatert: LocalDate,
    @JsonProperty("SendTilAddresse1")
    var sendtTilAdresse: String,
    @JsonProperty("SerieNummerListe")
    val serienumre: List<String>? = emptyList(),
)

fun OrdrelinjeOebs.fiksTommeSerienumre(): OrdrelinjeOebs = this.copy(serienumre = this.serienumre?.map { it.trim() }?.filter { it != "" })

fun OrdrelinjeOebs.erOpprettetFraHotsak(): Boolean = kilde != null && kilde == HOTSAK // I OEBS så får alt fra Digihot kilde==HOTSAK

fun OrdrelinjeOebs.toRåOrdrelinje(): RåOrdrelinje =
    RåOrdrelinje(
        mottakendeSystem = this.mottakendeSystem,
        oebsId = this.oebsId,
        serviceforespørsel = this.serviceforespørsel,
        serviceforespørselstatus = this.serviceforespørselstatus,
        serviceforespørseltype = this.serviceforespørseltype,
        søknadstype = this.søknadstype,
        vedtaksdato = this.vedtaksdato,
        søknad = this.søknad,
        hotSakSaksnummer = this.hotSakSaksnummer,
        kilde = this.kilde,
        resultat = this.resultat,
        saksblokkOgSaksnr = this.saksblokkOgSaksnr,
        ordrenr = this.ordrenr,
        ordrelinje = this.ordrelinje,
        delordrelinje = this.delordrelinje,
        artikkelbeskrivelse = this.artikkelbeskrivelse,
        produktgruppe = this.produktgruppe,
        produktgruppeNr = this.produktgruppeNr,
        artikkelnr = this.artikkelnr,
        hjelpemiddeltype = this.hjelpemiddeltype,
        antall = this.antall,
        enhet = this.enhet,
        fnrBruker = this.fnrBruker,
        egenAnsatt = this.egenAnsatt,
        sistOppdatert = this.sistOppdatert,
        sendtTilAdresse = this.sendtTilAdresse,
        serienumre = this.serienumre ?: listOf(),
    )

fun OrdrelinjeOebs.toHotsakOrdrelinje(): HotsakOrdrelinje =
    HotsakOrdrelinje(
        mottakendeSystem = this.mottakendeSystem,
        oebsId = this.oebsId,
        serviceforespørsel = this.serviceforespørsel,
        serviceforespørselstatus = this.serviceforespørselstatus,
        serviceforespørseltype = this.serviceforespørseltype,
        søknadstype = this.søknadstype,
        vedtaksdato = this.vedtaksdato,
        søknad = this.søknad,
        resultat = this.resultat,
        saksnummer = this.hotSakSaksnummer ?: "",
        ordrenr = this.ordrenr,
        ordrelinje = this.ordrelinje,
        delordrelinje = this.delordrelinje,
        artikkelbeskrivelse = this.artikkelbeskrivelse,
        produktgruppe = this.produktgruppe,
        produktgruppeNr = this.produktgruppeNr,
        artikkelnr = this.artikkelnr,
        hjelpemiddeltype = this.hjelpemiddeltype,
        antall = this.antall,
        enhet = this.enhet,
        fnrBruker = this.fnrBruker,
        egenAnsatt = this.egenAnsatt,
        sistOppdatert = this.sistOppdatert,
        sendtTilAdresse = this.sendtTilAdresse,
    )

fun OrdrelinjeOebs.toOrdrelinje(): InfotrygdOrdrelinje =
    InfotrygdOrdrelinje(
        this.mottakendeSystem,
        this.oebsId,
        this.serviceforespørsel,
        this.serviceforespørselstatus,
        this.serviceforespørseltype,
        this.søknadstype,
        this.vedtaksdato,
        this.søknad,
        this.resultat,
        this.saksblokkOgSaksnr ?: "",
        this.ordrenr,
        this.ordrelinje,
        this.delordrelinje,
        this.artikkelbeskrivelse,
        this.produktgruppe,
        this.produktgruppeNr,
        this.artikkelnr,
        this.hjelpemiddeltype,
        this.antall,
        this.enhet,
        this.fnrBruker,
        this.egenAnsatt,
        this.sistOppdatert,
        this.sendtTilAdresse,
    )

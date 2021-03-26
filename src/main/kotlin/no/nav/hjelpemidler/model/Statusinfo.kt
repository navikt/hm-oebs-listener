package no.nav.hjelpemidler.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Statusinfo(
    @JsonProperty("System")
    val mottakendeSystem: String,

    @JsonProperty("IncidentNummer")
    val serviceforespørsel: Int,

    @JsonProperty("IncidentStatus")
    val serviceforespørselstatus: String,

    @JsonProperty("IncidentType")
    val serviceforespørseltype: String,

    @JsonProperty("IncidentSoknadType")
    val søknadstype: String,

    // N.B.: Viss dato er "" i meldinga blir den til null under deserialisering og forblir null under serialisering (utgåande JSON)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("IncidentVedtakDato")
    val vedtaksdato: LocalDate?,

    @JsonProperty("IncidentSoknad")
    val søknad: String,

    @JsonProperty("IncidentResultat")
    val resultat: String,

    @JsonProperty("IncidentRef")
    val saksblokkOgSaksnummer: String,

    @JsonProperty("OrdreNumber")
    val ordrenummer: Int,

    @JsonProperty("LineNumber")
    val ordrelinjenummer: Int,

    @JsonProperty("ShipmentNumber")
    val delordrelinjenummer: Int,

    @JsonProperty("Description")
    val artikkelbeskrivelse: String,

    @JsonProperty("CategoryDescription")
    val produktgruppe: String,

    @JsonProperty("OrderedItem")
    val artikkel: String,

    @JsonProperty("User_ItemType")
    val hjelpemiddeltype: String,

    @JsonProperty("Quantity")
    val antall: Int,

    @JsonProperty("AccountNumber")
    val fnrBruker: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("LastUpdateDate")
    val sistOppdatert: LocalDate
)

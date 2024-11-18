package no.nav.hjelpemidler.oebs.listener.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class HotsakOrdrelinje(
    val mottakendeSystem: String,
    val oebsId: Int,
    val serviceforespørsel: Int,
    val serviceforespørselstatus: String,
    val serviceforespørseltype: String,
    val søknadstype: String,
    // N.B.: Hvis dato er "" i meldingen blir den til null under deserialisering og forblir null under serialisering (utgående JSON)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val vedtaksdato: LocalDate?,
    val søknad: String,
    val resultat: String,
    val saksnummer: String,
    val ordrenr: Int,
    val ordrelinje: Int,
    val delordrelinje: Int,
    val artikkelbeskrivelse: String,
    val produktgruppe: String,
    val produktgruppeNr: String,
    val artikkelnr: String,
    val hjelpemiddeltype: String,
    val antall: Double,
    val enhet: String,
    val fnrBruker: String,
    val egenAnsatt: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val sistOppdatert: LocalDate,
    val sendtTilAdresse: String,
) : Ordrelinje {
    constructor(oebs: OrdrelinjeOebs) : this(
        mottakendeSystem = oebs.mottakendeSystem,
        oebsId = oebs.oebsId,
        serviceforespørsel = oebs.serviceforespørsel,
        serviceforespørselstatus = oebs.serviceforespørselstatus,
        serviceforespørseltype = oebs.serviceforespørseltype,
        søknadstype = oebs.søknadstype,
        vedtaksdato = oebs.vedtaksdato,
        søknad = oebs.søknad,
        resultat = oebs.resultat,
        saksnummer = oebs.hotSakSaksnummer ?: "",
        ordrenr = oebs.ordrenr,
        ordrelinje = oebs.ordrelinje,
        delordrelinje = oebs.delordrelinje,
        artikkelbeskrivelse = oebs.artikkelbeskrivelse,
        produktgruppe = oebs.produktgruppe,
        produktgruppeNr = oebs.produktgruppeNr,
        artikkelnr = oebs.artikkelnr,
        hjelpemiddeltype = oebs.hjelpemiddeltype,
        antall = oebs.antall,
        enhet = oebs.enhet,
        fnrBruker = oebs.fnrBruker,
        egenAnsatt = oebs.egenAnsatt,
        sistOppdatert = oebs.sistOppdatert,
        sendtTilAdresse = oebs.sendtTilAdresse,
    )
}

package no.nav.hjelpemidler.oebs.listener.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class RåOrdrelinje(
    val mottakendeSystem: String,
    val oebsId: Int,
    val serviceforespørsel: Int,
    val serviceforespørselstatus: String,
    val serviceforespørseltype: String,
    val søknadstype: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val vedtaksdato: LocalDate?,
    val søknad: String,
    val hotSakSaksnummer: String?,
    val kilde: String?,
    val resultat: String,
    val saksblokkOgSaksnr: String?,
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
    var serienumre: List<String> = listOf(),
)

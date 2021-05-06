package no.nav.hjelpemidler.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class Ordrelinje(
    val mottakendeSystem: String,
    val serviceforespørsel: Int,
    val serviceforespørselstatus: String,
    val serviceforespørseltype: String,
    val søknadstype: String,

    // N.B.: Viss dato er "" i meldinga blir den til null under deserialisering og forblir null under serialisering (utgåande JSON)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val vedtaksdato: LocalDate?,

    val søknad: String,
    val resultat: String,
    val saksblokkOgSaksnr: String,
    val ordrenr: Int,
    val ordrelinje: Int,
    val delordrelinje: Int,
    val artikkelbeskrivelse: String,
    val produktgruppe: String,
    val artikkelnr: String,
    val hjelpemiddeltype: String,
    val antall: Double,
    val fnrBruker: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val sistOppdatert: LocalDate
)

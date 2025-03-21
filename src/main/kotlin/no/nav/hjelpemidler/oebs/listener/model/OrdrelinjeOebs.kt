package no.nav.hjelpemidler.oebs.listener.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val log = KotlinLogging.logger { }

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
    // For statistikk formål
    @JsonProperty("ForsteGangsUtlan")
    val førstegangsUtlån: String?, // Format: "Y, N, , N, N"
    @JsonProperty("ForsteTransDato")
    val førsteTransaksjonsDato: String?, // Format: "04-MAR-25, , 04-MAR-25"
    @JsonProperty("AntUtlan")
    val antallUtlån: String?, // Format: "1, 2, , 3, 4"
) {
    val serviceforespørseltypeVedtak: Boolean
        @JsonIgnore get() = serviceforespørseltype == "Vedtak Infotrygd"

    /**
     * I OeBS får alt fra DigiHoT kilde = HOTSAK
     */
    val kildeHotsak: Boolean
        @JsonIgnore get() = kilde == "HOTSAK"

    val delbestilling: Boolean
        @JsonIgnore get() = hotSakSaksnummer?.startsWith("hmdel_") == true

    val relevantHjelpemiddeltype: Boolean
        @JsonIgnore get() =
            hjelpemiddeltype in
                setOf(
                    "Hjelpemiddel",
                    "Individstyrt hjelpemiddel",
                    "Del",
                )

    val gyldigHotsak: Boolean
        @JsonIgnore get() {
            return !hotSakSaksnummer.isNullOrBlank()
        }

    val gyldigInfotrygd: Boolean
        @JsonIgnore get() {
            return !saksblokkOgSaksnr.isNullOrBlank() && vedtaksdato != null && fnrBruker.isNotBlank()
        }

    fun fiksTommeSerienumre(): OrdrelinjeOebs = copy(serienumre = serienumre?.map { it.trim() }?.filter { it != "" })

    fun utlånsstatistikk(): List<Utlånsstatistikk> {
        if (serienumre.isNullOrEmpty()) return listOf()

        val førstegangsUtlån =
            førstegangsUtlån?.split(",")?.map {
                when (it.trim()) {
                    "Y" -> true
                    "N" -> false
                    else -> null
                }
            }

        val førsteTransaksjonsDatoFormat =
            DateTimeFormatter.ofPattern(
                "dd-LLL-uu",
                Locale.of("nb", "NO"),
            ) // Format: "04-MAR-25, , 04-MAR-25"
        val førsteTransaksjonsDato =
            førsteTransaksjonsDato?.split(",")?.map { part ->
                val date = part.trim()
                if (date == "") return@map null
                runCatching {
                    LocalDate.parse(
                        // Fiks format fra "JAN" til "Jan", "MAR" til "Mar"
                        date.lowercase(), // .let { it.replaceRange(3, 4, it.substring(3, 4).uppercase()) }.replace("-", " "),
                        førsteTransaksjonsDatoFormat,
                    )
                }.onFailure { e ->
                    log.error(e) { "Feilet i å tolke datoen: $date" }
                }.getOrNull()
            }

        val antallUtlån = antallUtlån?.split(",")?.map { it.trim().toIntOrNull() }

        if ((førstegangsUtlån != null && serienumre.count() != førstegangsUtlån.count()) ||
            (førsteTransaksjonsDato != null && førsteTransaksjonsDato.count() != serienumre.count()) ||
            (antallUtlån != null && antallUtlån.count() != serienumre.count())
        ) {
            log.warn {
                "Uventet antall førstegangsUtlån, førsteTransaksjonsDato eller antallUtlån, må være lik antall serienumre (eller null)"
            }
            return listOf()
        }

        return serienumre.mapIndexed { idx, serieNr ->
            Utlånsstatistikk(
                serieNr = serieNr,
                førstegangsUtlån = førstegangsUtlån?.getOrNull(idx),
                førsteTransaksjonsDato = førsteTransaksjonsDato?.getOrNull(idx),
                antallUtlån = antallUtlån?.getOrNull(idx),
            )
        }
    }
}

data class Utlånsstatistikk(
    val serieNr: String,
    val førstegangsUtlån: Boolean?,
    val førsteTransaksjonsDato: LocalDate?,
    val antallUtlån: Int?,
)

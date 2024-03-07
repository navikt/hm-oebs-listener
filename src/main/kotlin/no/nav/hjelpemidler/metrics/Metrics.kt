package no.nav.hjelpemidler.metrics

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.MessageContext

private val log = KotlinLogging.logger {}

class Metrics(
    private val influxClient: InfluxClient,
    messageContext: MessageContext,
) {
    private val kafkaMetrics = KafkaMetrics(messageContext)

    fun meldingTilRapidSuksess() {
        registerPoint(MELDING_TIL_RAPID_OK, mapOf("counter" to 1L), emptyMap())
    }

    fun meldingTilRapidFeilet() {
        registerPoint(MELDING_TIL_RAPID_FEILET, mapOf("counter" to 1L), emptyMap())
    }

    fun meldingFraOebs() {
        registerPoint(OEBS_MELDING, mapOf("counter" to 1L), emptyMap())
    }

    fun oebsParsingOk() {
        registerPoint(OEBS_PARSING_OK, mapOf("counter" to 1L), emptyMap())
    }

    fun oebsParsingFeilet() {
        registerPoint(OEBS_PARSING_FEILET, mapOf("counter" to 1L), emptyMap())
    }

    fun sfTypeVedtakInfotrygd() {
        registerPoint(OEBS_MELDING_SF_TYPE_VEDTAK_INFOTRYGD, mapOf("counter" to 1L), emptyMap())
    }

    fun sfTypeBlank() {
        registerPoint(OEBS_MELDING_SF_TYPE_BLANK, mapOf("counter" to 1L), emptyMap())
    }

    fun sfTypeUlikVedtakInfotrygd() {
        registerPoint(OEBS_MELDING_SF_TYPE_ULIK_VEDTAK_INFOTRYGD, mapOf("counter" to 1L), emptyMap())
    }

    fun rettHjelpemiddeltype() {
        registerPoint(OEBS_MELDING_RETT_HJELPEMIDDELTYPE, mapOf("counter" to 1L), emptyMap())
    }

    fun irrelevantHjelpemiddeltype() {
        registerPoint(OEBS_MELDING_IRRELEVANT_HJELPEMIDDELTYPE, mapOf("counter" to 1L), emptyMap())
    }

    fun manglendeFeltForVedtakInfotrygd() {
        registerPoint(OEBS_MELDING_MANGLENDE_FELT_INFOTRYGD, mapOf("counter" to 1L), emptyMap())
    }

    fun manglendeFeltForVedtakHOTSAK() {
        registerPoint(OEBS_MELDING_MANGLENDE_FELT_HOTSAK, mapOf("counter" to 1L), emptyMap())
    }

    fun hotsakSF() {
        registerPoint(HOTSAK_SF, mapOf("counter" to 1L), emptyMap())
    }

    fun infotrygdSF() {
        registerPoint(INFOTRYGD_SF, mapOf("counter" to 1L), emptyMap())
    }

    private fun registerPoint(
        measurement: String,
        fields: Map<String, Any>,
        tags: Map<String, String>,
    ) {
        log.info("Posting point to Influx: measurement {} fields {} tags {} ", measurement, fields, tags)
        try {
            influxClient.writeEvent(measurement, fields, tags)
        } catch (e: Exception) {
            log.error("Feil ved sending til Influx, measurement: $measurement", e)
        }
        kafkaMetrics.registerPoint(measurement, fields, tags)
    }

    companion object {
        private const val SOKNADER = "hm-oebs-listener"
        const val MELDING_TIL_RAPID_OK = "$SOKNADER.rapidOk"
        const val MELDING_TIL_RAPID_FEILET = "$SOKNADER.rapidFeilet"
        const val OEBS_MELDING = "$SOKNADER.oebs.melding"
        const val OEBS_PARSING_OK = "$SOKNADER.oebs.parsingOk"
        const val HOTSAK_SF = "$SOKNADER.hotsak.sf"
        const val INFOTRYGD_SF = "$SOKNADER.infotrygd.sf"
        const val OEBS_PARSING_FEILET = "$SOKNADER.oebs.parsingFeilet"
        const val OEBS_MELDING_SF_TYPE_VEDTAK_INFOTRYGD = "$SOKNADER.oebs.sfTypeVedtakInfotrygd"
        const val OEBS_MELDING_SF_TYPE_BLANK = "$SOKNADER.oebs.sfTypeBlank"
        const val OEBS_MELDING_SF_TYPE_ULIK_VEDTAK_INFOTRYGD = "$SOKNADER.oebs.sfTypeUlikVedtakInfotrygd"
        const val OEBS_MELDING_RETT_HJELPEMIDDELTYPE = "$SOKNADER.oebs.rettHjelpemiddeltype"
        const val OEBS_MELDING_IRRELEVANT_HJELPEMIDDELTYPE = "$SOKNADER.oebs.irrelevantHjelpemiddeltype"
        const val OEBS_MELDING_MANGLENDE_FELT_INFOTRYGD = "$SOKNADER.oebs.manglendeFeltForVedtakInfotrygd"
        const val OEBS_MELDING_MANGLENDE_FELT_HOTSAK = "$SOKNADER.oebs.manglendeFeltForVedtakHotsak"
    }
}

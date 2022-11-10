package no.nav.hjelpemidler.metrics

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.hjelpemidler.Configuration
import org.influxdb.dto.Point
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class SensuMetrics(messageContext: MessageContext) {
    private val kafkaMetrics = KafkaMetrics(messageContext)

    private val log = LoggerFactory.getLogger(SensuMetrics::class.java)
    private val sensuURL = Configuration.application["SENSU_URL"] ?: "http://localhost/unconfigured"
    private val sensuName = "hm-oebs-listener-events"

    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

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

    private fun registerPoint(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        log.info("Posting point to Influx: measurment {} fields {} tags {} ", measurement, fields, tags)
        counter = ((counter + 1) % 1000000)
        val point = Point.measurement(measurement)
            .time(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()) + counter, TimeUnit.NANOSECONDS)
            .tag(tags)
            .tag(DEFAULT_TAGS)
            .fields(fields)
            .build()
        try {
            sendEvent(SensuEvent(sensuName, point.lineProtocol()))
        } catch (e: Exception) {
            log.error("Feil ved sending til Sensu: eventname: $measurement")
        }
        kafkaMetrics.registerPoint(measurement, fields, tags)
    }

    private fun sendEvent(sensuEvent: SensuEvent) {
        val body = HttpRequest.BodyPublishers.ofString(sensuEvent.json)
        val request = HttpRequest.newBuilder()
            .POST(body)
            .uri(URI.create(sensuURL))
            .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
            .header("Content-Type", "application/json")
            .header("X-Correlation-ID", UUID.randomUUID().toString())
            .header("Accepts", "application/json")
            .build()
        val response: HttpResponse<String> = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            log.error("sensu metrics unexpected response code from proxy: {}", response.statusCode())
            log.error("sensu metrics response: {}", response.body().toString())
        }
    }

    private class SensuEvent(sensuName: String, output: String) {
        val json: String = "{" +
                "\"name\":\"" + sensuName + "\"," +
                "\"type\":\"metric\"," +
                "\"handlers\":[\"events_nano\"]," +
                "\"output\":\"" + output.replace("\\", "\\\\", true) + "\"," +
                "\"status\":0" +
                "}"
    }

    companion object {
        private val DEFAULT_TAGS: Map<String, String> = mapOf(
            "application" to (Configuration.application["NAIS_APP_NAME"] ?: "hm-oebs-listener"),
            "cluster" to (Configuration.application["NAIS_CLUSTER_NAME"] ?: "dev-fss"),
            "namespace" to (Configuration.application["NAIS_NAMESPACE"] ?: "teamdigihot")
        )

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

        // For å unngå problem med at to eventar blir logga på samme millisekund til InfluxDb, legg vi til ein aukande
        // counter som "fakar" auka oppløysing i nanosekund. Det blir lagt til eit tal modulo 1000000 for at det skal
        // bli eit tal mellom 0 og 999999
        var counter: Long = 0
    }
}

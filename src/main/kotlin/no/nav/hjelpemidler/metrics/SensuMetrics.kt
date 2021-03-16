package no.nav.hjelpemidler.metrics

import no.nav.hjelpemidler.configuration.Configuration
import org.influxdb.dto.Point
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class SensuMetrics {
    private val log = LoggerFactory.getLogger(SensuMetrics::class.java)
    private val sensuURL = Configuration.application["SENSU_URL"] ?: "http://localhost/unconfigured"
    private val sensuName = "hm-oebs-listener-events"

    private val httpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun meldingTilRapidSuksess() {
        registerPoint(MELDING_TIL_RAPID_SUKSESS, mapOf("counter" to 1L), emptyMap())
    }

    fun meldingTilRapidFeilet() {
        registerPoint(MELDING_TIL_RAPID_FEILET, mapOf("counter" to 1L), emptyMap())
    }

    fun meldingFraOebs() {
        registerPoint(MELDING_FRA_OEBS, mapOf("counter" to 1L), emptyMap())
    }

    fun feilVedMeldingFraOebs() {
        registerPoint(OEBS_MELDING_FEIL, mapOf("counter" to 1L), emptyMap())
    }

    private fun registerPoint(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        log.info("Posting point to Influx: measurment {} fields {} tags {} ", measurement, fields, tags)
        val point = Point.measurement(measurement)
            .time(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()), TimeUnit.NANOSECONDS)
            .tag(tags)
            .tag(DEFAULT_TAGS)
            .fields(fields)
            .build()

        try {
            sendEvent(SensuEvent(sensuName, point.lineProtocol()))
        } catch (e: Exception) {
            log.error("Feil ved sending til Sensu: eventname: $measurement")
        }
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
        const val MELDING_TIL_RAPID_SUKSESS = "$SOKNADER.rapid.suksess"
        const val MELDING_TIL_RAPID_FEILET = "$SOKNADER.rapid.feilet"
        const val MELDING_FRA_OEBS = "$SOKNADER.oebs.melding"
        const val OEBS_MELDING_FEIL = "$SOKNADER.oebs.feil"
    }
}

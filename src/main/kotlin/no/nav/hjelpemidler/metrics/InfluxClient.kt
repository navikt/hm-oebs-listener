package no.nav.hjelpemidler.metrics

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import no.nav.hjelpemidler.Configuration
import java.time.Instant

class InfluxClient(
    host: String = Configuration.influxHost,
    port: String = Configuration.influxPort,
    user: String = Configuration.influxUser,
    password: String = Configuration.influxPassword,
    dbName: String = Configuration.influxDbName,
) {

    private val writeApi = InfluxDBClientFactory.createV1(
        "$host:$port",
        user,
        password.toCharArray(),
        dbName,
        null
    ).makeWriteApi()

    fun writeEvent(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        val point = Point(measurement)
            .addTags(DEFAULT_TAGS)
            .addTags(tags)
            .addFields(fields)
            .time(Instant.now().toEpochMilli(), WritePrecision.MS)
        writeApi.writePoint(point)
    }

    private val DEFAULT_TAGS: Map<String, String> = mapOf(
        "application" to Configuration.application,
        "cluster" to Configuration.cluster,
        "namespace" to Configuration.namespace,
    )
}
package no.nav.hjelpemidler.metrics

import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import no.nav.hjelpemidler.INFLUX_DATABASE_NAME
import no.nav.hjelpemidler.INFLUX_HOST
import no.nav.hjelpemidler.INFLUX_PASSWORD
import no.nav.hjelpemidler.INFLUX_PORT
import no.nav.hjelpemidler.INFLUX_USER
import no.nav.hjelpemidler.configuration.NaisEnvironmentVariable
import java.time.Instant

class InfluxClient(
    host: String = INFLUX_HOST,
    port: String = INFLUX_PORT,
    user: String = INFLUX_USER,
    password: String = INFLUX_PASSWORD,
    database: String = INFLUX_DATABASE_NAME,
) {
    private val writeApi: WriteApi =
        InfluxDBClientFactory.createV1(
            "$host:$port",
            user,
            password.toCharArray(),
            database,
            null,
        ).makeWriteApi()

    fun writeEvent(
        measurement: String,
        fields: Map<String, Any>,
        tags: Map<String, String>,
    ) {
        val point =
            Point(measurement)
                .addTags(defaultTags)
                .addTags(tags)
                .addFields(fields)
                .time(Instant.now().toEpochMilli(), WritePrecision.MS)
        writeApi.writePoint(point)
    }

    private val defaultTags: Map<String, String> =
        mapOf(
            "application" to NaisEnvironmentVariable.NAIS_APP_NAME,
            "cluster" to NaisEnvironmentVariable.NAIS_CLUSTER_NAME,
            "namespace" to NaisEnvironmentVariable.NAIS_NAMESPACE,
        )
}

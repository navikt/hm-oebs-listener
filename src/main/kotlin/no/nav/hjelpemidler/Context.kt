package no.nav.hjelpemidler

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.hjelpemidler.metrics.InfluxClient
import no.nav.hjelpemidler.metrics.Metrics

class Context(
    private val messageContext: MessageContext,
    influxClient: InfluxClient = InfluxClient(),
) : MessageContext by messageContext {
    val jsonMapper: JsonMapper =
        jacksonMapperBuilder()
            .addModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
    val metrics: Metrics = Metrics(influxClient, messageContext)

    fun <T> publish(
        key: String,
        message: T,
    ) = publish(key, jsonMapper.writeValueAsString(message))
}

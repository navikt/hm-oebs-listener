package no.nav.hjelpemidler.metrics

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import java.time.LocalDateTime
import java.util.UUID

class KafkaMetrics(private val context: MessageContext) {

    fun registerPoint(measurement: String, fields: Map<String, Any>, tags: Map<String, String>) {
        val opprettet = LocalDateTime.now()
        val message = JsonMessage.newMessage(mapOf(
            "eventId" to UUID.randomUUID(),
            "eventName" to "hm-bigquery-sink-hendelse",
            "schemaId" to "hendelse_v2",
            "payload" to mapOf(
                "opprettet" to opprettet,
                "navn" to measurement,
                "kilde" to "hm-oebs-listener",
                "data" to fields.mapValues { it.value.toString() }
                    .plus(tags)
                    .filterKeys { it != "counter" }
            )
        ))
        context.publish(measurement, message.toJson())
    }
}

package no.nav.hjelpemidler.oebs.listener

import no.nav.hjelpemidler.kafka.sendAsync
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.Closeable

class Context(
    private val producer: Producer<String, String>,
) : Closeable by producer {
    private val topic = "teamdigihot.hm-soknadsbehandling-v1"

    suspend fun <T> publish(
        key: String,
        message: T,
    ) {
        producer.sendAsync(
            ProducerRecord(topic, key, jsonMapper.writeValueAsString(message)),
        )
    }
}

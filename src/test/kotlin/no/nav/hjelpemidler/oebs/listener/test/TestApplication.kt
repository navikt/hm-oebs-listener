package no.nav.hjelpemidler.oebs.listener.test

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.http.HttpMessageBuilder
import io.ktor.server.testing.testApplication
import no.nav.hjelpemidler.http.jackson
import no.nav.hjelpemidler.kafka.createMockProducer
import no.nav.hjelpemidler.oebs.listener.Configuration
import no.nav.hjelpemidler.oebs.listener.module
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.ProducerRecord

fun runTest(test: suspend TestContext.() -> Unit) =
    testApplication {
        val producer = createMockProducer()
        val client = createClient { jackson(jsonMapper) }
        val context = TestContext(client, producer)
        application { module(producer) }
        context.test()
    }

class TestContext(
    val client: HttpClient,
    val producer: MockProducer<String, String>,
) {
    val kafkaHistory: List<ProducerRecord<String, String>> get() = producer.history()
}

fun HttpMessageBuilder.validToken() {
    bearerAuth(Configuration.OEBS_TOKEN)
}

package no.nav.hjelpemidler.oebs.listener.test

import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.serialization.jackson.jsonToValue
import org.apache.kafka.clients.producer.ProducerRecord

fun List<ProducerRecord<String, String>>.shouldContainRecord(
    expectedKey: String,
    expectedEventName: String,
    valueMatcher: (String) -> Unit = {},
) {
    shouldForOne {
        it.key().shouldBe(expectedKey)
        it.value().should { value ->
            value.shouldContainJsonKeyValue("$.eventName", expectedEventName)
            valueMatcher(value)
        }
    }
}

fun List<ProducerRecord<String, String>>.shouldNotContainRecord(excludedEventName: String) {
    this
        .map { jsonToValue<Map<String, Any?>>(it.value()) }
        .mapNotNull { it["eventName"] }
        .shouldNotContain(excludedEventName)
}

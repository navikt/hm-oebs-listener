package no.nav.hjelpemidler.oebs.listener.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.hjelpemidler.oebs.listener.jsonToValue
import org.apache.kafka.clients.producer.ProducerRecord

infix fun ProducerRecord<String, String>.shouldHaveKey(expected: String): String = key() shouldBe expected

inline fun <reified T> ProducerRecord<String, String>.shouldHaveValue(noinline matcher: (T) -> Unit) {
    shouldNotThrowAny { jsonToValue<T>(value()) }.should(matcher)
}

package no.nav.hjelpemidler.oebs.listener

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    val registry: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val ordrekvitteringCounter: Counter = registry.counter("ordrekvittering.mottatt")

    fun scrape(): String = if (registry is PrometheusMeterRegistry) registry.scrape() else ""
}

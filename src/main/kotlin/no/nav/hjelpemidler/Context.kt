package no.nav.hjelpemidler

import no.nav.helse.rapids_rivers.MessageContext
import no.nav.hjelpemidler.metrics.SensuMetrics

class Context(
    private val messageContext: MessageContext,
) : MessageContext by messageContext {
    val metrics: SensuMetrics = SensuMetrics(messageContext)
}

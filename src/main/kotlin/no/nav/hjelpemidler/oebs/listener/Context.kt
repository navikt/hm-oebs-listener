package no.nav.hjelpemidler.oebs.listener

import no.nav.helse.rapids_rivers.MessageContext

class Context(
    private val messageContext: MessageContext,
) : MessageContext by messageContext {
    fun <T> publish(
        key: String,
        message: T,
    ) = publish(key, jsonMapper.writeValueAsString(message))
}

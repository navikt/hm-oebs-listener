package no.nav.hjelpemidler.oebs.listener.model

import no.nav.hjelpemidler.oebs.listener.api.ServiceforespørselEndring
import java.time.LocalDateTime
import java.util.UUID

data class ServiceforespørselEndringMessage(
    val data: ServiceforespørselEndring,
    override val eventId: UUID = UUID.randomUUID(),
    override val opprettet: LocalDateTime = LocalDateTime.now(),
) : Message {
    override val eventName: String = "hm-EndretSF-oebs-v2"
}

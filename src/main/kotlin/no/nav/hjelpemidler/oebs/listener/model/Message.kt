package no.nav.hjelpemidler.oebs.listener.model

import no.nav.hjelpemidler.domain.id.UUID
import java.time.LocalDateTime

@Suppress("unused")
interface Message {
    val eventId get() = UUID()
    val eventName: String
    val opprettet: LocalDateTime get() = LocalDateTime.now()
}

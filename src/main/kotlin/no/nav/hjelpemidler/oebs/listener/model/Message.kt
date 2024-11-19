package no.nav.hjelpemidler.oebs.listener.model

import no.nav.hjelpemidler.domain.id.UUID
import java.time.LocalDateTime

@Suppress("unused")
abstract class Message(val eventName: String) {
    val eventId = UUID()
    val opprettet: LocalDateTime = LocalDateTime.now()
}

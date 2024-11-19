package no.nav.hjelpemidler.oebs.listener.model

import java.time.LocalDateTime
import java.util.UUID

interface Message {
    val eventId: UUID
    val eventName: String
    val opprettet: LocalDateTime
}

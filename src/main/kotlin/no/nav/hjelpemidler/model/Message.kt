package no.nav.hjelpemidler.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.UUID

data class Message(
    val eventId: UUID,
    val eventName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opprettet: LocalDateTime,
    val fnrBruker: String,
    val data: Ordrelinje,
)



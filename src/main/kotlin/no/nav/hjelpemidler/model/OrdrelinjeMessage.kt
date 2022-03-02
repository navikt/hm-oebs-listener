package no.nav.hjelpemidler.model

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.hjelpemidler.api.ServiceForespørselEndring
import java.time.LocalDateTime
import java.util.UUID

data class OrdrelinjeMessage(
    val eventId: UUID,
    val eventName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opprettet: LocalDateTime,
    val fnrBruker: String,
    val data: Ordrelinje,
)

data class SfMessage(
    val eventId: UUID,
    val eventName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opprettet: LocalDateTime,
    val data: ServiceForespørselEndring,
)

data class UvalidertOrdrelinjeMessage(
    val eventId: UUID,
    val eventName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val eventCreated: LocalDateTime,
    val orderLine: OrdrelinjeOebs,
)

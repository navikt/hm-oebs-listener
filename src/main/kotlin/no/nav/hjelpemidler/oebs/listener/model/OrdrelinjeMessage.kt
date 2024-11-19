package no.nav.hjelpemidler.oebs.listener.model

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.hjelpemidler.domain.id.UUID
import java.time.LocalDateTime
import java.util.UUID

interface OrdrelinjeMessage<T : Ordrelinje> : Message {
    val fnrBruker: String
    val data: T
}

data class HotsakOrdrelinjeMessage(
    override val fnrBruker: String,
    override val data: HotsakOrdrelinje,
    override val eventId: UUID = UUID(),
    override val opprettet: LocalDateTime = LocalDateTime.now(),
) : OrdrelinjeMessage<HotsakOrdrelinje> {
    override val eventName: String = "hm-NyOrdrelinje-hotsak"

    constructor(oebs: OrdrelinjeOebs) : this(oebs.fnrBruker, HotsakOrdrelinje(oebs))
}

data class InfotrygdOrdrelinjeMessage(
    override val fnrBruker: String,
    override val data: InfotrygdOrdrelinje,
    override val eventId: UUID = UUID(),
    override val opprettet: LocalDateTime = LocalDateTime.now(),
) : OrdrelinjeMessage<InfotrygdOrdrelinje> {
    override val eventName: String = "hm-NyOrdrelinje"

    constructor(oebs: OrdrelinjeOebs) : this(oebs.fnrBruker, InfotrygdOrdrelinje(oebs))
}

class UvalidertOrdrelinjeMessage(
    @JsonProperty("orderLine")
    val data: RåOrdrelinje,
    override val eventId: UUID = UUID(),
    override val opprettet: LocalDateTime = LocalDateTime.now(),
) : Message {
    override val eventName: String = "hm-uvalidert-ordrelinje"

    val eventCreated = opprettet
}

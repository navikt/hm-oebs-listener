package no.nav.hjelpemidler.oebs.listener.model

import no.nav.hjelpemidler.oebs.listener.api.ServiceforespørselEndring

data class OrdrelinjeMessage(
    override val eventName: String,
    val fnrBruker: String,
    val data: Ordrelinje,
) : Message

data class ServiceforespørselEndringMessage(val data: ServiceforespørselEndring) : Message {
    override val eventName = "hm-EndretSF-oebs-v2"
}

data class UvalidertOrdrelinjeMessage(val orderLine: RåOrdrelinje) : Message {
    override val eventName = "hm-uvalidert-ordrelinje"

    @Suppress("unused") // leses kanskje nedstrøms
    val eventCreated = opprettet
}

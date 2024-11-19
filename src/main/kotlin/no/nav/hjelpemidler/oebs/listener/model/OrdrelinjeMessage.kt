package no.nav.hjelpemidler.oebs.listener.model

import no.nav.hjelpemidler.oebs.listener.api.ServiceforespørselEndring

class OrdrelinjeMessage<T : Ordrelinje>(
    eventName: String,
    val fnrBruker: String,
    val data: T,
) : Message(eventName)

class ServiceforespørselEndringMessage(val data: ServiceforespørselEndring) :
    Message(eventName = "hm-EndretSF-oebs-v2")

class UvalidertOrdrelinjeMessage(val orderLine: RåOrdrelinje) :
    Message(eventName = "hm-uvalidert-ordrelinje") {
    @Suppress("unused") // leses kanskje nedstrøms
    val eventCreated = opprettet
}

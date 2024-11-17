package no.nav.hjelpemidler.oebs.listener.test

import no.nav.hjelpemidler.oebs.listener.api.SFEndringType
import no.nav.hjelpemidler.oebs.listener.api.ServiceforespørselEndring

object Fixtures {
    fun lagServiceforespørselEndring() =
        ServiceforespørselEndring(
            system = "HOTSAK",
            id = "1",
            sfnummer = "2",
            saksnummer = "3",
            antallKostnadslinjer = "1",
            ordre = emptyList(),
            status = SFEndringType.OPPRETTET,
        )
}

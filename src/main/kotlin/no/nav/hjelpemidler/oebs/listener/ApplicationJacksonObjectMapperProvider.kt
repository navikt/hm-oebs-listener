package no.nav.hjelpemidler.oebs.listener

import no.nav.hjelpemidler.serialization.jackson.JacksonObjectMapperProvider
import no.nav.hjelpemidler.serialization.jackson.defaultJsonMapper
import no.nav.hjelpemidler.service.LoadOrder
import tools.jackson.core.StreamReadFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.ObjectMapper

/**
 * Sikrer at vi bruker samme [ObjectMapper] i hotlibs og i hm-oebs-listener.
 */
@LoadOrder(0)
class ApplicationJacksonObjectMapperProvider : JacksonObjectMapperProvider {
    override fun invoke(): ObjectMapper =
        defaultJsonMapper {
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        }
}

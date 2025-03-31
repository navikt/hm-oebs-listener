package no.nav.hjelpemidler.oebs.listener

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.hjelpemidler.serialization.jackson.JacksonObjectMapperProvider
import no.nav.hjelpemidler.serialization.jackson.defaultJsonMapper
import no.nav.hjelpemidler.service.LoadOrder

/**
 * Sikrer at vi bruker samme [ObjectMapper] i hotlibs og i hm-oebs-listener.
 */
@LoadOrder(0)
class ApplicationJacksonObjectMapperProvider : JacksonObjectMapperProvider {
    override fun invoke(): ObjectMapper =
        defaultJsonMapper {
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).configure(
                JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION,
                true,
            )
        }
}

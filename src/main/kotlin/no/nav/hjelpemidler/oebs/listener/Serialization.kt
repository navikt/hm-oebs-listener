package no.nav.hjelpemidler.oebs.listener

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.serialization.jackson.JacksonObjectMapperProvider
import no.nav.hjelpemidler.serialization.jackson.defaultJsonMapper
import no.nav.hjelpemidler.serialization.jackson.jsonMapper
import no.nav.hjelpemidler.service.LoadOrder
import org.intellij.lang.annotations.Language

/**
 * Sikrer at vi bruker samme [ObjectMapper] i hotlibs og i hm-oebs-listener.
 */
@LoadOrder(0)
class ApplicationJacksonObjectMapperProvider : JacksonObjectMapperProvider {
    override fun invoke(): ObjectMapper =
        defaultJsonMapper {
            enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        }
}

inline fun <reified T> jsonToValue(
    @Language("JSON") content: String,
) = jsonMapper.readValue<T>(content)

package no.nav.hjelpemidler.oebs.listener

import com.fasterxml.jackson.core.StreamReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.MapperBuilder
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.configuration.Environment
import org.intellij.lang.annotations.Language

val jsonMapper: JsonMapper =
    jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .configure()
        .build()

val xmlMapper: ObjectMapper =
    XmlMapper.xmlBuilder()
        .addModules(kotlinModule(), JavaTimeModule())
        .configure()
        .build()

inline fun <reified T> jsonToValue(
    @Language("JSON") content: String,
) = jsonMapper.readValue<T>(content)

inline fun <reified T> xmlToValue(
    @Language("XML") content: String,
) = xmlMapper.readValue<T>(content)

private fun <M : ObjectMapper, B : MapperBuilder<M, B>> MapperBuilder<M, B>.configure(): B =
    this
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION, !Environment.current.tier.isProd)

package no.nav.hjelpemidler.oebs.listener

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.hjelpemidler.serialization.defaultJsonMapper
import org.intellij.lang.annotations.Language

val jsonMapper: JsonMapper = defaultJsonMapper { enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS) }

inline fun <reified T> jsonToValue(
    @Language("JSON") content: String,
) = jsonMapper.readValue<T>(content)

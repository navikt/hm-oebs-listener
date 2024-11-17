package no.nav.hjelpemidler.oebs.listener

import com.fasterxml.jackson.annotation.JsonValue
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.http.createHttpClient

object Ntfy {
    private val log = KotlinLogging.logger {}
    private val client =
        createHttpClient {
            expectSuccess = true
        }

    suspend fun publish(notification: Notification) =
        runCatching {
            val response =
                client.post(Configuration.NTFY_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(notification.copy(tags = notification.tags + setOf(Environment.current.toString())))
                }
            when (response.status) {
                HttpStatusCode.OK -> Unit
                else -> {
                    val body = response.body<Map<String, Any?>>()
                    log.warn { "Feil under publisering til ntfy: $body" }
                }
            }
        }.getOrElse {
            log.warn(it) { "Feil under publisering til ntfy" }
        }

    data class Notification(
        val topic: String = Configuration.NTFY_TOPIC,
        val title: String? = null,
        val message: String? = null,
        val priority: Priority = Priority.DEFAULT,
        val tags: Set<String> = emptySet(),
        val click: String? = null,
        val icon: String? = null,
        val actions: Set<Action> = emptySet(),
        val attach: String? = null,
        val filename: String? = null,
        val email: String? = null,
        val delay: String? = null,
    )

    data class Action(
        val action: ActionType,
        val label: String,
        val clear: Boolean = false,
        val url: String? = null,
        val method: String? = null,
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val intent: String? = null,
        val extras: Map<String, String> = emptyMap(),
    )

    enum class Priority(
        @JsonValue val value: Int,
    ) {
        MIN(1),
        LOW(2),
        DEFAULT(3),
        HIGH(4),
        MAX(5),
    }

    enum class ActionType(
        @JsonValue val value: String,
    ) {
        VIEW("view"),
        BROADCAST("broadcast"),
        HTTP("http"),
    }
}

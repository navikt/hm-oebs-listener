package no.nav.hjelpemidler

import com.fasterxml.jackson.annotation.JsonValue
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.hjelpemidler.configuration.Environment
import no.nav.hjelpemidler.http.createHttpClient

object Ntfy {
    private val log = KotlinLogging.logger {}
    private val client = createHttpClient()

    fun publish(notification: Notification) =
        runCatching {
            runBlocking(Dispatchers.IO) {
                val response =
                    client.post(NTFY_URL) {
                        contentType(ContentType.Application.Json)
                        setBody(notification.copy(tags = notification.tags + setOf(Environment.current.toString())))
                    }
                when (response.status) {
                    HttpStatusCode.OK -> Unit
                    else -> log.warn("Feil under publisering til ntfy: ${response.body<Map<String, Any?>>()}")
                }
            }
        }.getOrElse {
            log.warn(it) { "Feil under publisering til ntfy" }
        }

    data class Notification(
        val topic: String = NTFY_TOPIC,
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

package no.nav.hjelpemidler

import com.fasterxml.jackson.annotation.JsonValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

object Ntfy {
    private val log = KotlinLogging.logger {}
    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            jackson()
        }
    }

    fun publish(notification: Notification) = runBlocking(Dispatchers.IO) {
        val response = client.post(Configuration.ntfyUrl) {
            contentType(ContentType.Application.Json)
            setBody(notification)
        }
        when (response.status) {
            HttpStatusCode.OK -> Unit
            else -> log.warn("Feil ved publisering til ntfy: ${response.body<Map<String, Any?>>()}")
        }
    }

    data class Notification(
        val topic: String = Configuration.ntfyTopic,
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

    enum class Priority(@JsonValue val value: Int) {
        MIN(1),
        LOW(2),
        DEFAULT(3),
        HIGH(4),
        MAX(5),
    }

    enum class ActionType(@JsonValue val value: String) {
        VIEW("view"),
        BROADCAST("broadcast"),
        HTTP("http"),
    }
}

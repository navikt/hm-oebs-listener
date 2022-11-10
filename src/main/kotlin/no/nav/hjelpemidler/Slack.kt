package no.nav.hjelpemidler

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

object Slack {
    private val log = KotlinLogging.logger {}
    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            jackson()
        }
    }

    fun post(url: String = Configuration.slackHook, text: String, channel: String) = runCatching {
        runBlocking(Dispatchers.IO) {
            val response = client
                .post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "text" to text,
                            "channel" to channel,
                            "username" to "hm-oebs-listener",
                            "icon_emoji" to ":this-is-fine-fire:"
                        )
                    )
                }
            when (response.status) {
                HttpStatusCode.OK -> Unit
                else -> log.info(response.body<String>())
            }
        }
    }.getOrElse {
        log.warn(it) { "Feil under publisering til Slack" }
    }
}

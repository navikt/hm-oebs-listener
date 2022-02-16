package no.nav.hjelpemidler.slack

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class PostToSlack {
    private val username = "hm-oebs-listener"
    private val icon_emoji = ":this-is-fine-fire:"
    private val objectMapper = jacksonObjectMapper()

    fun post(hookUrl: String, alertText: String, channel: String) {
        val values = mapOf(
            "text" to alertText,
            "channel" to channel,
            "username" to username,
            "icon_emoji" to icon_emoji
        )

        val requestBody: String = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(values)
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(hookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        println(response.body())
    }
}

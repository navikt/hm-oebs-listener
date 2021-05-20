package no.nav.hjelpemidler.slack

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class PostToSlack {

    val username = "hm-oebs-listener"
    val icon_emoji = ":this-is-fine-fire:"

    fun post(hookUrl: String, alertText: String, channel: String) {

        val values = mapOf(
            "text" to alertText,
            "channel" to channel,
            "username" to username,
            "icon_emoji" to icon_emoji
        )

        val objectMapper = ObjectMapper()
        val requestBody: String = objectMapper.writeValueAsString(values)

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

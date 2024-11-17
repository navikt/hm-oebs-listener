package no.nav.hjelpemidler.oebs.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.http.slack.slackIconEmoji

object Slack {
    private val log = KotlinLogging.logger {}
    private val client = slack()

    suspend fun post(
        text: String,
        channel: String,
    ) = runCatching {
        client.sendMessage(
            username = "hm-oebs-listener",
            icon = slackIconEmoji(":this-is-fine-fire:"),
            channel = channel,
            message = text,
        )
    }.getOrElse {
        log.warn(it) { "Feil under publisering til Slack" }
    }
}

package no.nav.hjelpemidler.oebs.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.hjelpemidler.http.slack.slack
import no.nav.hjelpemidler.http.slack.slackIconEmoji

object Slack {
    private val log = KotlinLogging.logger {}
    private val client = slack()

    fun post(
        text: String,
        channel: String,
    ) = runCatching {
        runBlocking(Dispatchers.IO) {
            client.sendMessage(
                username = "hm-oebs-listener",
                icon = slackIconEmoji(":this-is-fine-fire:"),
                channel = channel,
                message = text,
            )
        }
    }.getOrElse {
        log.warn(it) { "Feil under publisering til Slack" }
    }
}

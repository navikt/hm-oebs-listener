package no.nav.hjelpemidler.oebs.listener

import no.nav.hjelpemidler.configuration.EnvironmentVariable

object Configuration {
    private val OEBSTOKEN by EnvironmentVariable
    val OEBS_TOKEN by this::OEBSTOKEN
    val SLACK_RECIPIENT by EnvironmentVariable

    val NTFY_URL by EnvironmentVariable
    val NTFY_TOPIC by EnvironmentVariable
}

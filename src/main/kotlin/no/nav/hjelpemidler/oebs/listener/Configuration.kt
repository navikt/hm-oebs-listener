package no.nav.hjelpemidler.oebs.listener

import no.nav.hjelpemidler.configuration.EnvironmentVariable

object Configuration {
    val OEBSTOKEN by EnvironmentVariable
    val SLACK_RECIPIENT by EnvironmentVariable

    val NTFY_URL by EnvironmentVariable
    val NTFY_TOPIC by EnvironmentVariable
}

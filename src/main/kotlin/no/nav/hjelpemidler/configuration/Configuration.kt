package no.nav.hjelpemidler.configuration

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

internal object Configuration {

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
        "prod-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
        else -> {
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
        }
    }

    private val prodProperties = ConfigurationMap(
        mapOf(
            "HTTP_PORT" to "8080",

            "kafka.aiven.topic" to "teamdigihot.hm-soknadsbehandling-v1",
            "kafka.reset.policy" to "latest",

            "application.profile" to "prod",
            "SENSU_URL" to "https://digihot-proxy.prod-fss-pub.nais.io/sensu",
        )
    )

    private val devProperties = ConfigurationMap(
        mapOf(
            "HTTP_PORT" to "8080",

            "kafka.aiven.topic" to "teamdigihot.hm-soknadsbehandling-v1",
            "kafka.reset.policy" to "latest",

            "application.profile" to "dev",
            "SENSU_URL" to "https://digihot-proxy.dev-fss-pub.nais.io/sensu",
        )
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "HTTP_PORT" to "8085",
            "OEBSTOKEN" to "abc",

            "kafka.reset.policy" to "earliest",
            "KAFKA_TRUSTSTORE_PATH" to "",
            "KAFKA_CREDSTORE_PASSWORD" to "",
            "KAFKA_KEYSTORE_PATH" to "",
            "kafka.brokers" to "host.docker.internal:9092",
            "kafka.aiven.topic" to "teamdigihot.hm-soknadsbehandling-v1",
            "kafka.reset.policy" to "earliest",
            "application.profile" to "local",
            "SENSU_URL" to "https://test",
            "SLACK_HOOK" to "https://test/slack"
        )
    )

    val rapidConfig: Map<String, String> = mapOf(
        "RAPID_KAFKA_CLUSTER" to "gcp",
        "RAPID_APP_NAME" to "hm-oebs-listener",
        "KAFKA_BROKERS" to config()[Key("kafka.brokers", stringType)],
        "KAFKA_CONSUMER_GROUP_ID" to "hm-oebs-listener-v1",
        "KAFKA_RAPID_TOPIC" to config()[Key("kafka.aiven.topic", stringType)],
        "KAFKA_RESET_POLICY" to config()[Key("kafka.reset.policy", stringType)],
        "KAFKA_TRUSTSTORE_PATH" to config()[Key("KAFKA_TRUSTSTORE_PATH", stringType)],
        "KAFKA_KEYSTORE_PATH" to config()[Key("KAFKA_KEYSTORE_PATH", stringType)],
        "KAFKA_CREDSTORE_PASSWORD" to config()[Key("KAFKA_CREDSTORE_PASSWORD", stringType)],
        "HTTP_PORT" to config()[Key("HTTP_PORT", stringType)],
    ) + System.getenv().filter { it.key.startsWith("NAIS_") }

    val application: Map<String, String> = mapOf(
        "APP_PROFILE" to config()[Key("application.profile", stringType)],
        "SENSU_URL" to config()[Key("SENSU_URL", stringType)],
        "OEBSTOKEN" to config()[Key("OEBSTOKEN", stringType)],
        "SLACK_HOOK" to config()[Key("SLACK_HOOK", stringType)],
    ) + System.getenv().filter { it.key.startsWith("NAIS_") }
}

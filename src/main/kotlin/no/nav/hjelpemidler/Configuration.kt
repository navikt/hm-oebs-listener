package no.nav.hjelpemidler

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

internal object Configuration {
    private val prodProperties = ConfigurationMap(
        Profile.PROD.entry(),

        "HTTP_PORT" to "8080",

        "kafka.aiven.consumer" to "hm-oebs-listener-v1",
        "kafka.aiven.topic" to "teamdigihot.hm-soknadsbehandling-v1",
        "kafka.reset.policy" to "latest",

        "SENSU_URL" to "https://digihot-proxy.prod-fss-pub.nais.io/sensu",
    )

    private val devProperties = ConfigurationMap(
        Profile.DEV.entry(),

        "HTTP_PORT" to "8080",

        "kafka.aiven.consumer" to "hm-oebs-listener-v2",
        "kafka.aiven.topic" to "teamdigihot.hm-soknadsbehandling-v1",
        "kafka.reset.policy" to "latest",

        "SENSU_URL" to "https://digihot-proxy.dev-fss-pub.nais.io/sensu",
    )

    private val localProperties = ConfigurationMap(
        Profile.LOCAL.entry(),

        "HTTP_PORT" to "8085",

        "NAIS_APP_NAME" to "hm-oebs-listener",
        "NAIS_CLUSTER_NAME" to Profile.LOCAL.name,
        "NAIS_NAMESPACE" to "teamdigihot",

        "OEBSTOKEN" to "token",

        "kafka.aiven.consumer" to "hm-oebs-listener-v1",
        "kafka.aiven.topic" to "teamdigihot.hm-soknadsbehandling-v1",
        "kafka.reset.policy" to "earliest",
        "kafka.brokers" to "host.docker.internal:9092",
        "kafka.reset.policy" to "earliest",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",

        "SENSU_URL" to "https://test/sensu",
        "SLACK_HOOK" to "https://test/slack"
    )

    private val config = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding devProperties
        "prod-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding prodProperties
        else -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding localProperties
    }

    operator fun get(name: String): String = config[Key(name, stringType)]

    val rapidConfig: Map<String, String> = mapOf(
        "RAPID_KAFKA_CLUSTER" to "gcp",
        "RAPID_APP_NAME" to "hm-oebs-listener",
        "KAFKA_BROKERS" to get("kafka.brokers"),
        "KAFKA_CONSUMER_GROUP_ID" to get("kafka.aiven.consumer"),
        "KAFKA_RAPID_TOPIC" to get("kafka.aiven.topic"),
        "KAFKA_RESET_POLICY" to get("kafka.reset.policy"),
        "KAFKA_KEYSTORE_PATH" to get("KAFKA_KEYSTORE_PATH"),
        "KAFKA_TRUSTSTORE_PATH" to get("KAFKA_TRUSTSTORE_PATH"),
        "KAFKA_CREDSTORE_PASSWORD" to get("KAFKA_CREDSTORE_PASSWORD"),
        "HTTP_PORT" to get("HTTP_PORT"),
    ) + System.getenv().filter { it.key.startsWith("NAIS_") }

    val application = get("NAIS_APP_NAME")
    val cluster = get("NAIS_CLUSTER_NAME")
    val namespace = get("NAIS_NAMESPACE")

    val profile: Profile = get("application.profile").let { Profile.valueOf(it) }
    val sensuUrl = get("SENSU_URL")
    val oebsToken = get("OEBSTOKEN")
    val slackHook = get("SLACK_HOOK")
    val slackRecipient = get("SLACK_RECIPIENT")
    const val ntfyUrl = "https://ntfy.sh"
    const val ntfyTopic = "teamdigihot_hm-oebs-listener"

    enum class Profile {
        PROD, DEV, LOCAL;

        fun entry(): Pair<String, String> = "application.profile" to name
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.hotlibs.core)
    implementation(libs.hotlibs.http)
    implementation(libs.hotlibs.kafka)
    implementation(libs.hotlibs.logging)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)

    // Ktor
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.netty)
}

application {
    applicationName = "hm-oebs-listener"
    mainClass.set("no.nav.hjelpemidler.oebs.listener.ApplicationKt")
}

spotless {
    kotlin { ktlint() }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest(libs.versions.kotlin.asProvider())
            dependencies {
                implementation(libs.hotlibs.test)
                implementation(libs.kotest.assertions.json)
                implementation(libs.ktor.server.test.host)
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.shadowJar { mergeServiceFiles() }

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.rapidsAndRivers)
    implementation(libs.hm.http)

    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.bundles.logging.runtime)

    // Jackson
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.datatype.jsr310)

    // Ktor
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)
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

kotlin { jvmToolchain(21) }

tasks.test { useJUnitPlatform() }

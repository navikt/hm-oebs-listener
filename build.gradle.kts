plugins {
    kotlin("jvm") version "1.9.0"
    id("com.diffplug.spotless") version "6.2.1"
}

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

val ktlint_version = "0.43.2"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // rapids-and-rivers
}

dependencies {
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("org.influxdb:influxdb-java:2.23")
    implementation("com.influxdb:influxdb-client-kotlin:6.10.0")
    implementation("com.github.navikt:rapids-and-rivers:2023101613431697456627.0cdd93eb696f") {
        exclude(group = "ch.qos.logback")
    }

    // Logging
    implementation("io.github.microutils:kotlin-logging:3.0.4")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.4")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.2")

    // Jackson
    val jacksonVersion = "2.14.0"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Ktor
    fun ktor(name: String) = "io.ktor:ktor-$name:2.3.3"
    implementation(ktor("serialization-jackson"))
    implementation(ktor("server-auth"))
    implementation(ktor("server-content-negotiation"))
    implementation(ktor("server-call-logging"))
    implementation(ktor("client-core"))
    implementation(ktor("client-cio"))
    implementation(ktor("client-content-negotiation"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(ktor("server-test-host"))
    testImplementation("io.mockk:mockk:1.13.2")
}

spotless {
    kotlin {
        ktlint(ktlint_version)
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlint_version)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "no.nav.hjelpemidler.ApplicationKt"
    }
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )
}

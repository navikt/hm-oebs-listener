import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
}

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // rapids-and-rivers
}

dependencies {
    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("org.influxdb:influxdb-java:2.22")
    implementation("com.github.navikt:rapids-and-rivers:2022.05.11-08.56.814d601f572e") {
        exclude(group = "ch.qos.logback")
    }

    // Logging
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.1.1")

    // Jackson
    val jacksonVersion = "2.13.3"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Ktor
    val ktorVersion = "2.0.1"
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Testing
    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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

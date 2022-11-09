import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
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
    implementation("com.github.navikt:rapids-and-rivers:2022110411121667556720.8a951a765583") {
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
    val ktorVersion = "2.1.3"
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:1.13.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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

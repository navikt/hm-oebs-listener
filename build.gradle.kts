import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val rapids_and_rivers_version: String by project
val kotlin_logging_version: String by project
val konfig_version: String by project
val brukernotifikasjon_schemas_version: String by project
val kafka_version: String by project
val kafka_avro_version: String by project
val influxdb_version: String by project
val jackson_version: String by project

plugins {
    kotlin("jvm") version "1.6.10"
}

group = "no.nav.hjelpemidler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io") // Rapids and Rivers
    maven("https://packages.confluent.io/maven/") // Kafka Avro
}

fun ktor(name: String) = "io.ktor:ktor-$name:1.6.7"

dependencies {
    testImplementation(kotlin("test-junit"))

    implementation("com.github.navikt:rapids-and-rivers:$rapids_and_rivers_version")
    implementation("io.github.microutils:kotlin-logging:$kotlin_logging_version")
    implementation("com.natpryce:konfig:$konfig_version")
    implementation("com.github.navikt:brukernotifikasjon-schemas:$brukernotifikasjon_schemas_version")
    implementation("org.apache.kafka:kafka-clients:$kafka_version")
    implementation("io.confluent:kafka-avro-serializer:$kafka_avro_version")
    implementation("org.influxdb:influxdb-java:$influxdb_version")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation(ktor("jackson"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

val fatJar = task("fatJar", type = org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "no.nav.hjelpemidler.ApplicationKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}

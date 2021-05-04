package no.nav.hjelpemidler

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.KafkaConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.hjelpemidler.configuration.Configuration
import no.nav.hjelpemidler.metrics.SensuMetrics
import no.nav.hjelpemidler.model.Ordrelinje
import no.nav.hjelpemidler.model.OrdrelinjeOebs
import no.nav.hjelpemidler.model.toOrdrelinje
import java.net.InetAddress
import java.time.LocalDateTime
import java.util.UUID

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private val mapperJson = jacksonObjectMapper().registerModule(JavaTimeModule())
private val mapperXml = XmlMapper().registerModule(JavaTimeModule())

// Unngå "inappropriate blocking method call" for objectmapper.writeValueAsString
@Suppress("BlockingMethodInNonBlockingContext")
fun main() {

    var rapidApp: RapidsConnection? = null
    rapidApp = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig(
            Configuration.rapidConfig["RAPID_APP_NAME"],
            InetAddress.getLocalHost().hostName,
            Configuration.rapidConfig["KAFKA_RAPID_TOPIC"]!!,
            emptyList(),
            KafkaConfig(
                Configuration.rapidConfig["KAFKA_BROKERS"]!!,
                Configuration.rapidConfig["KAFKA_CONSUMER_GROUP_ID"]!!,
                Configuration.rapidConfig["KAFKA_CLIENT_ID"]!!,
                null,
                null,
                Configuration.rapidConfig["KAFKA_TRUSTSTORE_PATH"]!!,
                Configuration.rapidConfig["KAFKA_CREDSTORE_PASSWORD"]!!,
                "jks",
                "PKCS12",
                Configuration.rapidConfig["KAFKA_KEYSTORE_PATH"]!!,
                Configuration.rapidConfig["KAFKA_CREDSTORE_PASSWORD"]!!,
                Configuration.rapidConfig["KAFKA_RESET_POLICY"]!!,
                false,
                null,
                null,
            ),
            Configuration.rapidConfig["HTTP_PORT"]!!.toInt(),
        )
    ).withKtorModule {
        routing {
            post("/push") {
                logg.info("incoming push")
                val authHeader = call.request.header("Authorization").toString()
                if (!authHeader.startsWith("Bearer ") || authHeader.substring(7) != Configuration.application["OEBSTOKEN"]!!) {
                    call.respond(HttpStatusCode.Unauthorized, "unauthorized")
                    return@post
                }

                var incomingFormatType = "JSON"
                if (call.request.header("Content-Type").toString().contains("application/xml")) {
                    incomingFormatType = "XML"
                }

                val requestBody: String = call.receiveText()
                SensuMetrics().meldingFraOebs()
                if (Configuration.application["APP_PROFILE"] != "prod") {
                    sikkerlogg.info("Received $incomingFormatType push request from OEBS: $requestBody")
                }

                // Check for valid json request
                val ordrelinje: OrdrelinjeOebs?
                try {
                    if (incomingFormatType == "XML"){
                        ordrelinje = mapperXml.readValue(requestBody)
                    } else {
                        ordrelinje = mapperJson.readValue(requestBody)
                    }
                    if (Configuration.application["APP_PROFILE"] != "prod") {
                        sikkerlogg.info(
                            "Parsing incoming $incomingFormatType request successful: ${
                            mapperJson.writeValueAsString(
                                ordrelinje
                            )
                            }"
                        )
                    }
                    SensuMetrics().oebsParsingOk()
                } catch (e: Exception) {
                    // Deal with invalid json/xml in request
                    sikkerlogg.info("Parsing incoming $incomingFormatType request failed with exception (responding 4xx): $e")
                    if (Configuration.application["APP_PROFILE"] != "prod") {
                        sikkerlogg.info(
                            "$incomingFormatType in failed parsing: ${mapperJson.writeValueAsString(requestBody)}"
                        )
                    }
                    SensuMetrics().oebsParsingFeilet()
                    call.respond(HttpStatusCode.BadRequest, "bad request: $incomingFormatType not valid")
                    return@post
                }

                if (ordrelinje!!.serviceforespørseltype != "Vedtak Infotrygd") {
                    if (ordrelinje.serviceforespørseltype == "") {
                        logg.info(
                            "Mottok melding fra oebs som ikke er en SF. Avbryter prosesseringen og returnerer"
                        )
                        SensuMetrics().sfTypeBlank()
                    } else {
                        logg.info(
                            "Mottok melding fra oebs med sf-type ${ordrelinje.serviceforespørseltype} og sf-status ${ordrelinje.serviceforespørselstatus}. " +
                                "Avbryter prosesseringen og returnerer"
                        )
                        SensuMetrics().sfTypeUlikVedtakInfotrygd()
                    }

                    call.respond(HttpStatusCode.OK)
                    return@post
                } else {
                    SensuMetrics().sfTypeVedtakInfotrygd()
                }

                if (ordrelinje.hjelpemiddeltype != "Hjelpemiddel" &&
                    ordrelinje.hjelpemiddeltype != "Individstyrt hjelpemiddel"
                ) {
                    logg.info("Mottok melding fra oebs med hjelpemiddeltype ${ordrelinje.hjelpemiddeltype}.")
                    SensuMetrics().irrelevantHjelpemiddeltype()
                    call.respond(HttpStatusCode.OK)
                    return@post
                } else {
                    SensuMetrics().rettHjelpemiddeltype()
                }

                val melding = Message(
                    eventId = UUID.randomUUID(),
                    eventName = "hm-NyOrdrelinje",
                    opprettet = LocalDateTime.now(),
                    fnrBruker = ordrelinje.fnrBruker,
                    data = ordrelinje.toOrdrelinje()
                )

                // Publish the received json/xml to our rapid as json
                try {
                    if (Configuration.application["APP_PROFILE"] != "prod") {
                        logg.info { "Publiserer ordrelinje til rapid i miljø ${Configuration.application["APP_PROFILE"]}" }
                        rapidApp!!.publish(ordrelinje.fnrBruker, mapperJson.writeValueAsString(melding))
                        SensuMetrics().meldingTilRapidSuksess()
                    } else {
                        // TODO: Reaktiver prod. videresending av OEBS data.
                        ordrelinje.fnrBruker = "MASKERT"
                        sikkerlogg.info { "Ordrelinje mottatt i prod som ikkje blir sendt til rapid: ${mapperJson.writeValueAsString(ordrelinje)}" }
                    }
                } catch (e: Exception) {
                    if (Configuration.application["APP_PROFILE"] != "prod") {
                        SensuMetrics().meldingTilRapidFeilet()
                    }
                    sikkerlogg.error("Sending til rapid feilet, exception: $e")
                    call.respond(HttpStatusCode.InternalServerError, "Feil under prosessering")
                    return@post
                }

                call.respond(HttpStatusCode.OK)
            }
        }
    }.build()

    // Run our rapid and rivers implementation facing hm-rapid
    logg.info("Starting Rapid & Rivers app towards hm-rapid")
    rapidApp.start()
    logg.info("Application ending.")
}

data class Message(
    val eventId: UUID,
    val eventName: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opprettet: LocalDateTime,
    val fnrBruker: String,
    val data: Ordrelinje,
)

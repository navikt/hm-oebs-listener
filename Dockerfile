FROM ghcr.io/navikt/baseimages/temurin:17
COPY build/libs/hm-oebs-listener-1.0-SNAPSHOT.jar app.jar

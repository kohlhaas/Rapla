FROM alpine:3.18

WORKDIR /app

RUN apk add --no-cache \
    openjdk11-jre \
    wget \
    tar && \
    wget -O - https://github.com/rapla/rapla/releases/download/2.0-RC8/rapla-2.0-RC8.tar.gz | tar -xz

COPY target/*.war /app/webapps/rapla.war

COPY data/data.xml /app/data/data.xml

COPY keystore.p12 /app/keystore.p12

COPY rapla.ks /app/rapla.ks

# COPY src/test/etc/jetty.xml /app/etc/jetty.xml

CMD ./raplaserver.sh run
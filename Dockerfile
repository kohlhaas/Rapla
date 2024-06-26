FROM alpine:3.18

WORKDIR /app

RUN apk add --no-cache \
    openjdk11-jre \
    wget \
    tar


COPY target/distribution/rapla-*.tar.gz rapla.tar.gz

RUN tar -xzvf rapla.tar.gz

COPY target/*.war /app/webapps/rapla.war

COPY data/data.xml /app/data/data.xml

COPY keystore.p12 /app/keystore.p12

CMD ./raplaserver.sh run
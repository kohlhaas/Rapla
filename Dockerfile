FROM alpine:3.18

WORKDIR /app

RUN apk add --no-cache \
    openjdk11-jre \
    wget \
    tar && \
    wget -O - https://github.com/rapla/rapla/releases/download/2.0-RC8/rapla-2.0-RC8.tar.gz | tar -xz

COPY target/*.war /app/webapps/rapla.war

CMD ./raplaserver.sh run
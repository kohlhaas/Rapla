FROM ubuntu:22.04

WORKDIR /app

RUN apt-get update && apt-get install -y \
    wget \
    default-jre

RUN wget -O - https://github.com/rapla/rapla/releases/download/2.0-RC8/rapla-2.0-RC8.tar.gz | tar -xz

COPY target/*.war /app/webapps/rapla.war

CMD ./raplaserver.sh run
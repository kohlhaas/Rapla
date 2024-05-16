FROM ubuntu:22.04

WORKDIR /app

RUN apt-get update

RUN apt-get install wget -y

RUN apt-get install -y default-jre

RUN wget https://github.com/rapla/rapla/releases/download/2.0-RC8/rapla-2.0-RC8.tar.gz

RUN tar -xvf rapla-2.0-RC8.tar.gz

CMD ["./src/main/distribution/templates/scripts/raplaserver.sh", "run"]


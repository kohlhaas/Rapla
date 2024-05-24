FROM ubuntu:22.04

WORKDIR /app

RUN apt-get update && apt-get install -y \
    wget \
    inotify-tools \
    default-jre \
    screen \
    nano \
    dos2unix

RUN wget -O - https://github.com/rapla/rapla/releases/download/2.0-RC8/rapla-2.0-RC8.tar.gz | tar -xz

COPY filelistener.sh filelistener.sh

RUN dos2unix filelistener.sh

CMD screen -d -m -S rapla_session ./raplaserver.sh run && ./filelistener.sh
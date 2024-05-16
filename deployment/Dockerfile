FROM ubuntu:22.04

WORKDIR /app

RUN apt-get update

RUN apt-get install wget -y

RUN apt-get install inotify-tools -y

RUN apt-get install -y default-jre

RUN apt-get install -y screen

RUN apt-get install -y nano

RUN wget https://github.com/rapla/rapla/releases/download/2.0-RC8/rapla-2.0-RC8.tar.gz

RUN tar -xvf rapla-2.0-RC8.tar.gz

COPY filelistener.sh filelistener.sh

CMD screen -d -m -S rapla_session ./raplaserver.sh run && sleep infinity
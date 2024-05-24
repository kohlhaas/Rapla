# Setup Container

navigate to the deployment folder

```bash
cd /path/to/rapla/deployment
```

change access rights for the filelistener script (for mac only)

```bash
chmod 777 filelistener.sh
```

build image from Dockerfile (only needed once)

```bash
podman build -t rapla .
```

start container with .yaml file

```bash
podman compose up
```

## General Docker Commands

enter container

```bash
podman exec -it [CONTAINER-ID] /bin/sh
```

# How to build the .war file

## Install Maven

Mac:

```bash
brew install maven
```

Windows:

```bash
choco install maven
```

## Build the .war File

navigate to the rapla folder

```bash
cd /path/to/rapla
```

build .war file

```bash
mvn package -DskipTests
```

.war located in target folder

```bash
cd /path/to/rapla/target
```

commit

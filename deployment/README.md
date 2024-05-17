

im deployment folder

```bash 
podman build -t rapla .

podman compose up
```


enter container
```bash
podman exec -it [CONTAINER-ID] /bin/sh
```


für mac we need to chmod for the script
```bash
chmod 777 filelistener.sh
```

# How to build the .war file

## Install Libraries
Mac:

```bash
brew install maven
```

Windows:

```bash
choco install maven
```


## Build the .war File
First you have to go into the project directory:

```bash
cd /path/to/rapla
```

Then you can build the .war file with the following command:

```bash
mvn package -DskipTests
```


The .war file will be located in the target folder of the project directory.
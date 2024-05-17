

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

```brew install maven```

Windows:

```choco install maven```


## Build the .war File
First you have to go into the project directory:

```cd /path/to/project```

Then you can build the .war file with the following command:

```mvn package -DskipTests```


The .war file will be located in the target folder of the project directory.
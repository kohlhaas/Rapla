

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
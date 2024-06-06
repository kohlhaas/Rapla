
connect to vm in our case 
```bash
ssh root@wwiappdev8.dhbw-stuttgart.de
```

generate ssh key 
```bash
ssh-keygen -t rsa
```
id_rsa.pub is the public key, this key needs to be added to the authorized_keys file on the vm in `.ssh/authroized_keys`

TBD: ssh key auf gitlab!!!!


###
add user steve and grad sudo access + password

### RUNNER INSTALLIEREN
im root
gitlab settings , cicd, runner expanden
-> new project runner, tags: rapla
operating system Linux 
-> runner installieren wie auf gitlab beschrieben
-> runner registrieren wie auf gitlab beschrieben (executer: docker, default image: ruby:2.7)

/etc/gitlab-runner/config.toml -> privileged = true


auf user git installieren 
sudo yum install git


auf dem user bei GITLAB anmelden
-> über container regestiry -> sudo docker login git.dhbw-stuttgart.de:XY (entnehmen aus registry - anmelden mit user und access token)


### DOCKER AUF RHL9
auf dem user
https://docs.docker.com/engine/install/rhel/
-> passt alles


### vars
auf gitlab: 
die vars setten!!!
4 stück
DEPLOY_HOST (ip der VM)
DEPLOY_USER (user mit dem der runner läuft)
SSH_PRIVATE_KEY (private key der VM (path dahin))
USER_PASSWORD (password des users)



### ports 

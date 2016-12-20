# KT Advance SonarQube Plugin Docker

## SonarQube with H2 database
To run bare SonarQube with only KT-Advance plugin pre-installed, you need either to build the Docker image
```
$ docker build -t compartia/kt-sonarqube:5.5.7 .
```
OR
to pull the image from Docker Hub

```
$ docker pull compartia/kt-sonarqube:5.5.7
```
Then run it with this command:
```
$ docker run -d --name kt-sonarqube -p 9000:9000 -p 9092:9092 compartia/kt-sonarqube:5.5.7
```
#### Docker image
https://hub.docker.com/r/compartia/kt-sonarqube/
#### KT-Advance plugin for SonarQube
https://github.com/kestreltechnology/sonar-kt-advance/releases/download/5.5.7/sonar-kt-advance-plugin-5.5.7.jar

## SonarQube with Postgres database
To run SonarQube with Postgres pre-filled with Redis project analysis,
just run docker composite:
```
$ docker-compose up
```

After seeing in the console something like
```
sonarqube_1  | INFO  ce[o.s.ce.app.CeServer] Compute Engine is up
sonarqube_1  | INFO  app[o.s.p.m.Monitor] Process[ce] is up
```
you may navigate to [http://localhost:9000](http://localhost:9000)

#### Docker images
the Docker composite is built of 2 containers. One for SonarQube, other for Posgres DB. The corresponding images are:
- https://hub.docker.com/r/compartia/kt-sonarqube-pg/
- https://hub.docker.com/r/compartia/kt-sonarqube-postgres-redis/

## Known issues
- there's a small chance that Posgres DB is not yet started at the moment when SonarQube needs it. In this case just re-start the composite. *Most likely it is fixed, unable to reproduce after employing docker/wait-for-it.sh script*

## Running the SonarQube scanner
To run the scanner on your KT-analyzed C project run `sonar-scanner` in the project dir. (In case you don’t have `sonar-scanner CLI`, please get it from (http://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) ).
To log more debug info into console, you may run the scanner in verbose mode: `sonar-scanner -X`.

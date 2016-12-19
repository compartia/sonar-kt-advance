# KT Advance SonarQube Plugin Docker

## SonarQube with H2 database
Firstly, you need either to build the Docker image
```
$ docker build -t compartia/kt-sonarqube:5.5.6 .
```
OR
to pull it from Docker Hub

```
$ docker pull compartia/kt-sonarqube:5.5.6
```
Then run it with this command:
```
$ docker run -d --name kt-sonarqube -p 9000:9000 -p 9092:9092 compartia/kt-sonarqube:5.5.6
```

## SonarQube with Postgres database
To run SonarQube with Postgres pre-filled with Redis project analysis,
Just run docker composite:
```
$ docker-compose up
```

After seeing in the console something like
```
sonarqube_1  | INFO  ce[o.s.ce.app.CeServer] Compute Engine is up
sonarqube_1  | INFO  app[o.s.p.m.Monitor] Process[ce] is up
```
you may navigate to `localhost:9000`.

## Known issues
1. there's a small chance that Posgres DB is not yet started at the moment when SonarQube needs it. In this case just re-start the composite. *Most likely it is fixed, unable to reproduce after employing docker/wait-for-it.sh script*

## Running the scanner
To run the scanner on your KT-analyzed C project run `sonar-scanner` in the project dir. (In case you don’t have `sonar-scanner CLI`, please get it from (http://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) ).
To log more debug info into console, you may run the scanner in verbose mode: `sonar-scanner -X`.

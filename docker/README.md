# KT Advance SonarQube Plugin Docker

## SonarQube with H2 database
```
docker build -t kt-sonarqube-plugin .
docker run -d --name kt-sonarqube-plugin -p 9000:9000 -p 9092:9092 kt-sonarqube-plugin
```

## SonarQube with Postgres database
```
docker build -t kt-sonarqube-plugin .
docker-compose up
```

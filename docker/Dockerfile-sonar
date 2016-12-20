FROM sonarqube:5.6.4-alpine

MAINTAINER Artem Zaborskiy “artem@toptal.com"

RUN apk --no-cache add --repository http://dl-cdn.alpinelinux.org/alpine/edge/community wget ca-certificates

ENV PLUGINS_DIR /opt/sonarqube/extensions/plugins

WORKDIR $PLUGINS_DIR
RUN wget https://github.com/kestreltechnology/sonar-kt-advance/releases/download/5.5.7/sonar-kt-advance-plugin-5.5.7.jar

WORKDIR $SONARQUBE_HOME

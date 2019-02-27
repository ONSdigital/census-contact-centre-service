FROM openjdk:11-jre-slim

ARG JAR_FILE=contactcentresvc*.jar
RUN apt-get update
RUN apt-get -yq clean
RUN groupadd -g 983 concensvc && \
    useradd -r -u 983 -g concensvc concensvc
USER concensvc
COPY target/$JAR_FILE /opt/contactcentresvc.jar

ENTRYPOINT [ "java", "-jar", "/opt/contactcentresvc.jar" ]


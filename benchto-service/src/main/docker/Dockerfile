FROM java:8
VOLUME /tmp
ADD benchto-service-${project.version}.jar benchto-service.jar
RUN bash -c 'touch /benchto-service.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/benchto-service.jar"]

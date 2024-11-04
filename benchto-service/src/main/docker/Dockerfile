FROM eclipse-temurin:21.0.5_11-jre-ubi9-minimal
VOLUME /tmp
ADD /maven/benchto-service-${project.version}.jar benchto-service.jar
RUN bash -c 'touch /benchto-service.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx2g","-jar","/benchto-service.jar"]

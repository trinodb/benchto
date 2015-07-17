FROM java:8
VOLUME /tmp
ADD benchmark-service-1.0.0-SNAPSHOT.jar benchmark-service.jar
RUN bash -c 'touch /benchmark-service.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/benchmark-service.jar"]
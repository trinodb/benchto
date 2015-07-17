FROM java:8
VOLUME /tmp
ADD presto-monitor-service-1.0.0-SNAPSHOT.jar presto-monitor-service.jar
RUN bash -c 'touch /presto-monitor-service.jar'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/presto-monitor-service.jar"]
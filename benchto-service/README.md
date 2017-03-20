# Benchto-service

Service for storing/showing benchmark results.

## Prerequisites

* Java 8

* PostgreSQL:

For local development use docker:

```
$ docker run --name benchto-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

## Running integration-tests

```
$ ./mvnw verify
```

## Running service

```
$ env SERVER_PORT=8081 ./mvnw spring-boot:run -pl benchto-service

    ____                  __    __      
   / __ )___  ____  _____/ /_  / /_____ 
  / __  / _ \/ __ \/ ___/ __ \/ __/ __ \
 / /_/ /  __/ / / / /__/ / / / /_/ /_/ /
/_____/\___/_/ /_/\___/_/ /_/\__/\____/ 
--- The macro-benchmarking framework ---

11:22:30.170 INFO  com.teradata.benchmark.service.App - Starting App v1.0.0-SNAPSHOT on latitude with PID 8659 (/home/sogorkis/repos/benchmark-service/target/service-1.0.0-SNAPSHOT.jar started by sogorkis in /home/sogorkis/repos/benchmark-service)
...
```

Go to: [http://localhost:8081/](http://localhost:8081/)

## Creating environment

To create environment PRESTO-DEVENV you need to run:

```
$ curl -H 'Content-Type: application/json' -d '{
    "dashboardType": "grafana",
    "dashboardURL": "http://localhost:3000/dashboard/db/presto-devenv",
    "prestoURL": "http://presto-master:8080/"
}' http://localhost:8081/v1/environment/PRESTO-DEVENV
```

## Creating tag

To create tag for environment PRESTO-DEVENV you need to run:

```
$ curl -H 'Content-Type: application/json' -d '{
    "name": "Short tag desciption",
    "description": "Very long but optional tag description"
}' http://localhost:8081/v1/tag/PRESTO-DEVENV

```

Note that `description` field is optional.

## Building docker image

```
$ ./mvnw package -pl benchto-service -P docker-images
$ docker images
REPOSITORY                             TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
teradata-labs/benchto-service          latest              427f3e1f4777        13 seconds ago      879.3 MB
...
$ docker save teradata-labs/benchto-service | gzip > /tmp/benchto-service.tar.gz
```

## Cleaning up stale benchmark runs

Benchmark runs that are older then 24 hours and have not finished will be periodically cleaned up and automatically failed.

## Protecting API write access

Benchto-service does support basic authorization method for using REST API calls that do write to service database.
It uses basic HTTP auth with user and password provided as part of `application.yaml`.

To enable protected write operations following section should be added to `application.yaml`:
```
benchto:
  security:
    api:
      protected: true
      login: <username>
      password: <password>
```

After enabling this security feature calls to api using `curl` should use `-u` flag to provide http auth login and password.
eg.
```
$ curl -u user:password -H 'Content-Type: application/json' -d '{
    "name": "Short tag desciption",
    "description": "Very long but optional tag description"
}' http://localhost:8081/v1/tag/PRESTO-DEVENV
```

## Benchto-service over HTTPS

Benchto-service may make use of provided SSL certificate in order to provide connection over secure protocol using TLS 1.2.
This goal may be achieved by adding follwoing section to `application.yaml`.
```
server:
  port: <HTTPS bind port>
  ssl:
    key-store: <path to keystore file>
    key-store-password: <password to keystore file>
    keyStoreType: <key store type eg. PKCS12>
    keyAlias: <alias of service key>
```


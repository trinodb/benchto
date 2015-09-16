# Benchto-service

Service for storing/showing benchmark results.

## Prerequisites

* Java 8

* PostgreSQL:

For local development use docker:

```
$ docker run --name benchmark-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

## Running integration-tests

```
$ mvn verify
```

## Running service

```
$ mvn spring-boot:run

------------------------------------------------------------------

      Copyright 2013-2015, Teradata, Inc. All rights reserved.

               Benchmark-service  (v1.0.0-SNAPSHOT)

------------------------------------------------------------------

11:22:30.170 INFO  com.teradata.benchmark.service.App - Starting App v1.0.0-SNAPSHOT on latitude with PID 8659 (/home/sogorkis/repos/benchmark-service/target/service-1.0.0-SNAPSHOT.jar started by sogorkis in /home/sogorkis/repos/benchmark-service)
...
```

Go to: [http://localhost:8080/](http://localhost:8080/)

## Creating environment

```
$ curl -X POST -H 'Content-Type: application/json' -d '{
    "dashboardType": "grafana",
    "dashboardURL": "http://10.25.17.79:3000/dashboard/db/hdp-cluster",
    "prestoURL": "http://10.25.17.79:8090/"
}' http://localhost:8080/v1/environment/HDP-r3
```

## Building docker image

```
$ mvn docker:build
$ docker images
REPOSITORY                             TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
teradata-labs/benchmark-service        latest              427f3e1f4777        13 seconds ago      879.3 MB
...
$ docker save teradata-labs/benchmark-service | gzip > /tmp/benchmark-service.tar.gz
```

## Cleaning up stale benchmark runs

Benchmark runs that are older then 24 hours and have not finished will be periodcally cleaned up and automatically failed.

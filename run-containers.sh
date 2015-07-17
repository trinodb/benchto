#!/bin/bash

echo "Starting benchto-postgres container"
docker run --name benchto-postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres

echo "Starting benchto-graphite container"
docker run --name benchto-graphite -p 2003:2003 -p 8088:80 \
        -v `pwd`/config/etc/graphite/storage-schemas.conf:/opt/graphite/conf/storage-schemas.conf \
        -d hopsoft/graphite-statsd

echo "Starting benchto-grafana container"
docker run --name benchto-grafana --link benchto-graphite:benchto-graphite -p 3000:3000 -d grafana/grafana

echo "Starting benchto-service container"
docker run --name benchto-service --link benchto-postgres:benchto-postgres \
        -e "SPRING_DATASOURCE_URL=jdbc:postgresql://benchto-postgres:5432/postgres" \
        -p 80:8080 -d teradata-labs/benchmark-service

# Benchto integration test

Benchto integration test runs a non-mocked integration test. It starts the benchto-driver that
executes simple benchmark against Trino. Trino, benchto-service and benchto-service backend
database are run in Docker using testcontainers.

## Executing integration test

Integration test is not executed by default - it triggered by activation of maven _it_ profile.
It also require locally built docker image of benchto-service. To start test, run:

```shell
./mvnw clean install -DskipTests -P docker-images
./mvnw test -pl benchto-it -P it
```
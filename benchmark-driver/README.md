# benchmark-driver

Benchmark driver is standalone java application which sql statements using JDBC.

## Configuration

TODO

## Usage

You should have _application.properties_ file and _sql_ directory containing sql benchmark queries:

```
$ ls
... sql application.properties
$ java -jar target/benchmark-driver-1.0.0-SNAPSHOT.jar --runs 5
```

## Properties

```
--runs COUNT  - number of query runs (default: 3)
--sql DIR - sql queries directory (default: sql)
```

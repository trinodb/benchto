# benchmark-tests-presto

Project containing benchmark tests for presto.

## Configuration (local development)

To run tests locally from IDE create _application.yaml_ properties file. You can use sample:

```
$ cp application.yaml-sample application.yaml
$ vim application.yaml
```

More information about configuration properties can be found in [https://github-bdch.td.teradata.com/bdch/benchmark-driver](benchmark-driver project README).

## Running benchmarks

You need to provide correct profile: `benchmark-hdp`, `benchmark-cdh`, `benchmark-td-hdp`

For CDH cluster configuration call:
```
$ mvn -Pbenchmark-cdh package exec:java
...
[INFO] --- exec-maven-plugin:1.4.0:java (exec-benchmark) @ benchmark-tests-presto ---
...
```

In case you select particular benchmark to run you can:
```
$ mvn -Pbenchmark-hdp package exec:java -DactiveBenchmarks=presto/linear-scan/selectivity=10.yaml 
```

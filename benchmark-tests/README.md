# benchmark-tests

Project containing benchmark tests.

## Configuration

To run tests create application.yaml properties file. You can use sample:

```
$ cp application.yaml-sample application.yaml
$ vim application.yaml
```

More information about configuration properties can be found in [https://github-bdch.td.teradata.com/bdch/benchmark-driver](benchmark-driver project README).

## Running benchmarks

```
$ mvn test -Pbenchmark
...
[INFO] --- exec-maven-plugin:1.4.0:java (exec-benchmark) @ benchmark-tests ---
...
```

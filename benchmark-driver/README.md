# benchmark-driver

Benchmark driver is standalone java application which sql statements using JDBC.

## Driver runtime configuration

It is most convenient to run benchmark driver using maven. Declare dependency to `benchmark-driver` and any
jdbc drivers you want to use. Then use maven exec plugin to run benchmark driver main class 
`com.teradata.benchmark.driver.DriverApp`:

```
    <dependencies>
        <dependency>
            <groupId>com.teradata.benchmark</groupId>
            <artifactId>benchmark-driver</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.facebook.presto</groupId>
            <artifactId>presto-jdbc</artifactId>
        </dependency>
    </dependencies>

    <profiles>
        <profile>
            <id>benchmark</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>exec-maven-plugin</artifactId>
                        <version>1.4.0</version>
                        <executions>
                            <execution>
                                <id>exec-benchmark</id>
                                <phase>test</phase>
                                <goals>
                                    <goal>java</goal>
                                </goals>
                            </execution>
                        </executions>
                        <configuration>
                            <mainClass>com.teradata.benchmark.driver.DriverApp</mainClass>
                        </configuration>
                    </plugin>
                </plugins>
            </build>

        </profile>
    </profiles>
```

Please take a look into `benchmark-test-example` module for examples.

## Global properties configuration

Global runtime properties are configured through `application.yaml` file. Sample configuration file:

```
data-sources:       # data-sources section which lists all jdbc drivers which can be used in benchmarks
  presto:           # presto section with jdbc connection properties
    url: jdbc:presto://example.com:8888
    username: example
    password: example
    driver-class-name: com.facebook.presto.jdbc.PrestoDriver
  teradata:
    url: jdbc:teradata://example.com
    username: example
    password: example
    driver-class-name: com.teradata.jdbc.TeraDriver

environment:        # environment on which benchmarks are run - it should map to environment mapped in benchmark-service
  name: TD-HDP

benchmark-service:
  url: http://example.com:18080       # url on benchmark-service endpoint

presto:
  url: http://example.com:8888        # optional parameter - presto coordinator endpoint
  metrics.collection.enabled: true    # feature toggle which enables presto query metrics collection

graphite:
  url: http://graphite:18088          # graphite endpoint
  event.reporting.enabled: true       # feature toggle which enables reporting of events in graphite
  metrics.collection.enabled: true    # feature toggle which enables cluster metrics collection
  resolution.seconds: 10              # graphite resolution - must be set if metrics collection is enabled
  metrics:                            # list of graphite expressions which gathers cpu, memory and network cluster metrics
    cpu: asPercent(sumSeries(collectd.TD_HDP-*.cpu.percent-{user,system}.value), sumSeries(collectd.TD_HDP-*.cpu.*.value))
    memory: collectd.CLOUD10HD01-2-*.memory
    network: sumSeries(collectd.TD_HDP-*.interface-*.if_octets.{rx,tx})
```

## Define benchmarks

TODO

## Properties

```
--sql DIR - sql queries directory (default: sql)
--benchmarks DIR - benchmark descriptors directory (default: benchmarks)
--activeBenchmarks BENCHMARK_NAME,... - list of active benchmarks (default: all benchmarks)
--executionSequenceId SEQUENCE_ID - sequence id of benchmark execution
```

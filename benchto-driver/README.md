# Benchto-driver

Benchto driver is standalone java application which sql statements using JDBC.

## Driver runtime configuration

It is most convenient to run benchmark driver using maven. Declare dependency to `benchto-driver` and any
jdbc drivers you want to use. Then use maven exec plugin to run benchmark driver main class
`io.trino.benchto.driver.DriverApp`:

```
    <dependencies>
        <dependency>
            <groupId>io.trino.benchto</groupId>
            <artifactId>benchto-driver</artifactId>
        </dependency>
        <dependency>
            <groupId>io.prestosql</groupId>
            <artifactId>presto-jdbc</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>1.2.5.RELEASE</version>
                 <configuration>
                    <mainClass>io.trino.benchto.driver.DriverApp</mainClass>
                    <layout>ZIP</layout>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```

Then issue following query to build and run benchto driver:

```
$ mvn package
$ java -jar target/your-benchto-benchmarks-*.jar 
```

## Global properties configuration

Global runtime properties are configured through `application.yaml` file. Sample configuration file:

```
data-sources:       # data-sources section which lists all jdbc drivers which can be used in benchmarks
  presto:           # presto section with jdbc connection properties
    url: jdbc:presto://example.com:8888
    username: example
    password: example
    driver-class-name: io.prestosql.jdbc.PrestoDriver
  teradata:
    url: jdbc:teradata://example.com
    username: example
    password: example
    driver-class-name: com.teradata.jdbc.TeraDriver

environment:        # environment on which benchmarks are run - it should map to environment mapped in benchmark-service
  name: TD-HDP

benchmark-service:
  url: http://example.com:18080       # url on benchmark-service endpoint

macroExecutions:
  healthCheck: disk-usage-check       # defines that 'disk-usage-check' macro should be used as a health check
  beforeAll: MACRO-NAME               # macro executed before all benchmarks
  afterAll: MACRO-NAME                # macro executed after all benchmarks

macros:             # defines list of macros which are executed using 'bash'
  drop-caches:      # macro running fabric that drop caches on benchmark cluster
    command: fabric execute_on_cluster "echo 3 > /proc/sys/vm/drop_caches"
  disk-usage-check:  # macro running fabric that performs disk health check on nodes
    command: fabric execute_on_cluster "disk-usage-check.sh"

presto:
  url: http://example.com:8888        # optional parameter - presto coordinator endpoint

graphite:
  url: http://graphite:18088          # graphite endpoint
  resolution.seconds: 10              # graphite resolution - must be set if metrics collection is enabled
  metrics:                            # list of graphite expressions which gathers cpu, memory and network cluster metrics
    cpu: asPercent(sumSeries(collectd.TD_HDP-*.cpu.percent-{user,system}.value), sumSeries(collectd.TD_HDP-*.cpu.*.value))
    memory: collectd.CLOUD10HD01-2-*.memory
    network: sumSeries(collectd.TD_HDP-*.interface-*.if_octets.{rx,tx})

benchmark:
  feature:
    graphite:
      event.reporting.enabled: true     # feature toggle which enables reporting of events in graphite
      metrics.collection.enabled: true  # feature toggle which enables cluster metrics collection
    presto:
      metrics.collection.enabled: true  # feature toggle which enables presto query metrics collection
```

## Benchmark descriptor

Benchmark descriptor is used to configure execution of particular benchmark. It is YAML file with various
properties and user defined variables. It is possible to configure multiple variants of benchmark with
different variables. It is possible to use variable substitution in this file. Example:

```
datasource: presto
query-names: presto/linear-scan/selectivity-${selectivity}.sql
runs: 3
variables:
  1:
    selectivity: 0, 2, 10, 100
    schema: sf100, sf1000
    database: tpch
    suite-prewarm-runs: 3
  2:
    selectivity: 0, 2, 10, 100
    schema: tpch_100gb_orc, tpch_100gb_text, tpch_1tb_orc, tpch_1tb_text
    database: hive
    suite-prewarm-runs: 3
```

List of keywords:

| Keyword | Required | Default value | Comment |
|---|---|---|---|
| datasource          | True  |       | Name of the datasource defined in `application.yaml` file.                         |
| query-names         | True  |       | Paths to the queries.                                                              |
| runs                | False | 3     | Number of runs each query should be executed.                                      |
| suite-prewarm-runs  | False | 0     | Number of prewarm runs of queries before whole benchmark suite.                    |
| benchmark-prewarm-runs  | False | 2     | Number of prewarm runs of queries before each benchmark.                    |
| concurrency         | False | 1     | Number of concurrent workers - 1 sequential benchmark, >1 concurrency benchmark.   |
| before-benchmark    | False | none  | Names of macros executed before benchmark.                                         |
| after-benchmark     | False | none  | Names of macros executed after benchmark.                                          |
| before-execution    | False | none  | Names of macros executed before benchmark executions.                              |
| after-execution     | False | none  | Names of macros executed after benchmark executions.                               |
| variables           | False | none  | Set of combinations of variables.                                                  |
| quarantine          | False | false | Flag which can be used to quarantine benchmark using `--activeVariables` property. |
| frequency           | False | none  | tells how frequent given benchmark can be executed (in days). 1 - once per day, 7 once per week. |
| quey-results        | False | none  | Triggers results verification against specified result files                       |

## SQL files

SQL query files reside in `sql` directory. User defined variables from benchmark descriptor can be used as template
variables in sql file. You can also use `execution_sequence_id` variable set automatically by driver. [Freemarker](http://freemarker.org/)
library is used to render query templates. Example:

```
SELECT 100.00 * sum(CASE
                    WHEN p.type LIKE 'PROMO%'
                      THEN l.extendedprice * (1 - l.discount)
                    ELSE 0
                    END) / sum(l.extendedprice * (1 - l.discount)) AS promo_revenue
FROM
  "${database}"."${schema}"."lineitem" AS l,
  "${database}"."${schema}"."part" AS p
WHERE
  l.partkey = p.partkey
  AND l.shipdate >= DATE '1995-09-01'
  AND l.shipdate < DATE '1995-09-01' + INTERVAL '1' MONTH
```

SQL query files used to setup data before benchmarks can be executed on different data source then the benchmark it self, by defining
query file property named `datasource`. Example:

```
--! datasource: presto
DROP TABLE IF EXISTS blackhole.default.lineitem_${splits_count}m;
CREATE TABLE blackhole.default.lineitem_${splits_count}m
    WITH (splits_count=${splits_count},pages_per_split=1000,rows_per_page=1000)
    AS SELECT * FROM tpch.tiny.lineitem;
```

## Results verification

Benchmark optional descriptor's property `query-results` may point to files containing unquoted CSV files with
query results. Paths to these files are relative to global runtime property `query-results-dir`.

Results of first warm-up run are compared to content of the result file for specific query. If verification 
fails, whole benchmark is marked as failure. 

Results verification should be used only for queries with stable results - for example with sorted output.

If benchmark has no pre-warm runs, verification is skipped.

## Overrides

It is possible to override benchmark top level variables by specifying
overrides YAML file:
```
--overrides path_to_overrides_file
```

An example overrides file:
```
runs: 5
suite-prewarm-runs: 10
```

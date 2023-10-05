# Benchto [![Build Status](https://travis-ci.com/trinodb/benchto.svg?branch=master)](https://travis-ci.com/trinodb/benchto)

The purpose of _Benchto_ project is to provide easy and manageable way to define, run and analyze _macro benchmarks_
in clustered environment. Understanding behaviour of distributed systems is hard and requires good visibility into
state of the cluster and internals of tested system. This project was developed for repeatable benchmarking of
Hadoop SQL engines, most importantly [Trino](https://trino.io/).

_Benchto_ consists of two main components: `benchto-service` and `benchto-driver`. To utilize all features of _Benchto_,
it is also recommended to configure _Graphite_ and _Grafana_. Image below depicts high level architecture:

![Benchto high level architecture](high-level-architecture.png?raw=true "Benchto high level architecture")

- _benchto-service_ - persistent data store for benchmark results. It exposes REST API and stores results in relational
DB (_Postgres_). Driver component calls API to store benchmark execution details which are later displayed by webapp
which is bundled with service. Single instance of _benchto-service_ can be shared by multiple benchmark drivers.

- _benchto-driver_ - standalone java application which loads definitions of benchmarks (_benchmark descriptors_) and
executes them against tested distributed system. If cluster monitoring is in place, driver collects various metrics
(cpu, memory, network usage) regarding cluster resource utilization. Moreover it adds graphite events which can be
later displayed as [Grafana annotations](http://docs.grafana.org/reference/annotations/). All data is stored in service
for later analysis.

- _monitoring_ - cluster monitoring is optional, but highly recommended to fully understand performance characteristics
of tested system. It is assumed that _Graphite/Carbon_ is used as metrics store and _Grafana_ for clusters dashboards.
There is no limitation on metric agents deployed on cluster hosts.


## Query profiler

It is possible to profile every query execution with:
* Java Flight Recorder
* async profiler
* perf (linux)

### Java Flight Recorder
To use Java Flight Recorder one should add following configuration:
```yaml
benchmark:
    feature:
        profiler:
          profiled-coordinator: # pod name of coordinator
          enabled: true
          jfr:
            enabled: true
            output-path: /tmp     # path where jfr recording files will be saved
            jmx.port: ${jmx.port} # JMX port of profiled JVM
```

### async profiler
To use async profiler one should add following configuration:
```yaml
benchmark:
    feature:
        profiler:
          profiled-coordinator: # pod name of coordinator
          enabled: true
          async:
            enabled: true
            output-path: /tmp     # path where jfr recording files will be saved
            jmx.port: ${jmx.port} # JMX port of profiled JVM
            async-library-path:   # path to libasyncProfiler shared library
          events:                 # list of async events like wall, cpu, lock, alloc and so on
          - cpu
```

### perf profiler
To use async profiler one should add following configuration:
```yaml
benchmark:
    feature:
        profiler:
          profiled-coordinator: # pod name of coordinator
          enabled: true
          async:
            enabled: true
            output-path: /tmp     # path where jfr recording files will be saved
            jmx.port: ${jmx.port} # JMX port of profiled JVM
            async-library-path:   # path to libasyncProfiler shared library
          events:
          - cpu
```

q
# Benchto [![Build Status](https://travis-ci.org/prestodb/benchto.svg?branch=master)](https://travis-ci.org/prestodb/benchto)

The purpose of _Benchto_ project is to provide easy and manageable way to define, run and analyze _macro benchmarks_
in clustered environment. Understanding behaviour of distributed systems is hard and requires good visibility into
state of the cluster and internals of tested system. This project was developed for repeatable benchmarking of
Hadoop SQL engines, most importantly [Presto](https://prestodb.io/).

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

- _benchto-generator_ - map reduce job for generating benchmark data. You can configure number of rows, type of the
row (ex. _BIGINT_, _INT_, _DOUBLE_, _DECIMAL(38,8)_), output format (_ORC_, _TEXT_) and number of output files. More
details can be found in _benchto-generator_ module README file.

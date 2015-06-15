# benchmark-tests-querygrid

Project containing benchmark tests for querygrid.

## Configuration

To run tests create application.yaml properties file. You can use sample:

```
$ cp application.yaml-sample application.yaml
$ vim application.yaml
```

More information about configuration properties can be found in [https://github-bdch.td.teradata.com/bdch/benchmark-driver](benchmark-driver project README).

## Running benchmarks

```
$ To install querygrid on local td_express which will use the local presto cluster execute: 
$
$ hfab tdexpress querygrid.install:/projects/swarm-teradata-querygrid/package/build/distribution/presto-teradata-driver.zip 
$ hfab tdexpress querygrid.create_server:server_name=presto_server,presto_master_ip=172.16.2.11,hdfs_master_ip=172.16.2.10,presto_schema=tpch_1gb,server_options=TemporaryTablesHdfsPath\\\(\\\'hdfs:///user/hive/warehouse\\\'\\\)
$
$ To install querygrid on remote perf cluster use:
$
$ hfab other:querygrid_td_perf_cluster querygrid.install:local_path=/home/andrii/projects/swarm-teradata-querygrid/package/build/distribution/presto-teradata-driver.zip,user_perm_space=1e13,user_spool_space=1e13,user_temp_space=1e13
$ hfab other:querygrid_td_perf_cluster  querygrid.create_server:server_name=presto_server,presto_master_ip=cloud10hd01-2-2.labs.teradata.com,hdfs_master_ip=cloud10hd01-2-2.labs.teradata.com,presto_schema=tpch_1gb,presto_master_port=8888,server_options=TemporaryTablesHdfsPath\\\(\\\'hdfs:///app/hive/warehouse\\\'\\\)
$
$ mvn test -Pbenchmark
...
[INFO] --- exec-maven-plugin:1.4.0:java (exec-benchmark) @ benchmark-tests-querygrid ---
...
```

#!/usr/bin/env bash
set -e;

hfab $1 querygrid.drop_server:server_name=presto_server || true
hfab $1 querygrid.drop_server:server_name=hive || true
hfab $1 querygrid.drop_server:server_name=tpch || true

hfab $1 querygrid.create_server:server_name=presto_server,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4
hfab $1 querygrid.create_server:server_name=hive,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4,presto_catalog=hive
hfab $1 querygrid.create_server:server_name=tpch,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4,presto_catalog=tpch,presto_schema=tiny
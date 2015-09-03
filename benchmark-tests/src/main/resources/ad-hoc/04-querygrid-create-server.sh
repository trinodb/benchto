#!/usr/bin/env bash
set -e -x;

hfab $1 querygrid.create_server:server_name=hdfs_pushdown,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)
hfab $1 querygrid.create_server:server_name=hdfs_hive,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4,presto_catalog=hive,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)
hfab $1 querygrid.create_server:server_name=hdfs_tpch,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4,presto_catalog=tpch,presto_schema=tiny,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)
hfab $1 querygrid.create_server:server_name=hdfs_blackhole,presto_master_ip=$2,presto_master_port=$3,hdfs_master_ip=$4,presto_catalog=blackhole,presto_schema=default,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)

hfab $1 querygrid.create_greyhound_server:server_name=greyhound_pushdown,presto_master_ip=$2,presto_master_port=$3,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)
hfab $1 querygrid.create_greyhound_server:server_name=greyhound_hive,presto_master_ip=$2,presto_master_port=$3,presto_catalog=hive,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)
hfab $1 querygrid.create_greyhound_server:server_name=greyhound_tpch,presto_master_ip=$2,presto_master_port=$3,presto_catalog=tpch,presto_schema=tiny,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)
hfab $1 querygrid.create_greyhound_server:server_name=greyhound_blackhole,presto_master_ip=$2,presto_master_port=$3,presto_catalog=blackhole,presto_schema=default,server_options=LOGGING_LEVEL\\\(\\\'DEBUG\\\'\\\)

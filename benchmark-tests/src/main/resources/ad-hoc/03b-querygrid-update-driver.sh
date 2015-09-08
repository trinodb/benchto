#!/usr/bin/env bash
set +e

hfab $1 querygrid.drop_server:server_name=hdfs_pushdown
hfab $1 querygrid.drop_server:server_name=hdfs_hive
hfab $1 querygrid.drop_server:server_name=hdfs_tpch
hfab $1 querygrid.drop_server:server_name=hdfs_blackhole

hfab $1 querygrid.drop_server:server_name=greyhound_pushdown
hfab $1 querygrid.drop_server:server_name=greyhound_hive
hfab $1 querygrid.drop_server:server_name=greyhound_tpch
hfab $1 querygrid.drop_server:server_name=greyhound_blackhole

set -e;

hfab $1 querygrid.update:local_path=$2

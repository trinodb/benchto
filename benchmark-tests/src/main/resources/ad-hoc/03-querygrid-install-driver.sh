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

hfab $1 querygrid.uninstall

set -e;

hfab $1 querygrid.install:local_path=$2,user_perm_space=1e13,user_spool_space=3e13,user_temp_space=1e13

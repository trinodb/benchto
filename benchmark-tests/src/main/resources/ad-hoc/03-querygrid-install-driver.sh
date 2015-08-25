#!/usr/bin/env bash
set -e;

hfab $1 querygrid.drop_server:server_name=hdfs_pushdown || true
hfab $1 querygrid.drop_server:server_name=hdfs_hive || true
hfab $1 querygrid.drop_server:server_name=hdfs_tpch || true

hfab $1 querygrid.drop_server:server_name=greyhound_pushdown || true
hfab $1 querygrid.drop_server:server_name=greyhound_hive || true
hfab $1 querygrid.drop_server:server_name=greyhound_tpch || true

hfab $1 querygrid.uninstall || true

hfab $1 querygrid.install:local_path=$2,user_perm_space=1e13,user_spool_space=3e13,user_temp_space=1e13
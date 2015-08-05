#!/usr/bin/env bash
set -e;

hfab $1 querygrid.drop_server:server_name=presto_server || true
hfab $1 querygrid.drop_server:server_name=hive || true
hfab $1 querygrid.drop_server:server_name=tpch || true
hfab $1 querygrid.uninstall || true

hfab $1 querygrid.install:local_path=$2,user_perm_space=1e13,user_spool_space=3e13,user_temp_space=1e13
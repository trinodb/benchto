#!/bin/bash

cat << EOF | PGPASSWORD=postgres psql -U postgres -p 15432 -h localhost postgres
delete from benchmark_run_measurements;
delete from execution_measurements;
delete from execution_attributes;
DELETE from executions;
delete from benchmark_runs_attributes;
delete from benchmark_runs;
delete from measurements;
EOF

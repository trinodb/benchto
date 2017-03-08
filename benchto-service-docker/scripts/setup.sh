#!/bin/bash -x
set -euo pipefail

# wait for Grafana to startup
sleep 30

# add benchto-graphite data source to Grafana
curl "http://admin:admin@benchto-grafana:3000/api/datasources" \
  -X POST \
  -H 'Content-Type: application/json;charset=UTF-8' \
  --data-binary \
  '{"name":"Benchto graphite",
    "type":"graphite",
    "url":"http://benchto-graphite:80",
    "access":"proxy",
    "isDefault":true,
    "basicAuth":true,
    "basicAuthUser":"guest",
    "basicAuthPassword":"guest"}'

#!/bin/bash

curl -X POST -H 'Content-Type: application/json' -d '{
    "dashboardType": "grafana",
    "dashboardURL": "http://benchto.td.teradata.com:3000/dashboard/db/td-hdp-cluster",
    "prestoURL": "http://cloud10hd01-2-2.labs.teradata.com:8888/"
}' http://benchto.td.teradata.com/v1/environment/TD-HDP

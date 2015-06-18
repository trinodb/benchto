#!/bin/bash

curl -X POST -H 'Content-Type: application/json' -d '{
    "dashboardType": "grafana",
    "dashboardURL": "http://10.25.17.79:3000/dashboard/db/hdp-cluster",
    "prestoURL": "http://10.25.17.79:8090/"
}' http://10.25.17.79:18080/v1/environment/HDP

curl -X POST -H 'Content-Type: application/json' -d '{
    "dashboardType": "grafana",
    "dashboardURL": "http://10.25.17.79:3000/dashboard/db/cdh-cluster",
    "prestoURL": "http://10.25.17.79:8090/"
}' http://10.25.17.79:18080/v1/environment/CDH

curl -X POST -H 'Content-Type: application/json' -d '{
    "dashboardType": "grafana",
    "dashboardURL": "http://10.25.17.79:3000/dashboard/db/td-hdp-cluster",
    "prestoURL": "http://cloud10hd01-2-2.labs.teradata.com:8888/"
}' http://10.25.17.79:18080/v1/environment/TD-HDP

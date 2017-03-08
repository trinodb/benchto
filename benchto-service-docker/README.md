# benchto-service-docker

This module contains docker compose definition for quickly setting up `benchto-service`
and related services. Apart from `benchto-service` the following services are started:
- `benchto-postgres`: PostgreSQL database for storing benchmark results
- `benchto-graphite`: Graphite service for storing cluster metrics
- `benchto-grafana`: Grafana service for visualizing cluster metrics

To start dockerized Benchto ecosystem use the following command from `benchto-service-docker` directory:
```
docker-compose -f docker-compose.yml up
```
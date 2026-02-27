# Banking Microservices Local Platform (Free/Open Source)

This repository provides a production-style local platform for a banking microservices system using only free services.

## Included Services
- PostgreSQL: transactional data + per-service databases
- Redis: cache/session store
- RabbitMQ: async messaging
- Kafka (KRaft): event streaming
- Keycloak: IAM/OIDC for auth
- Vault (dev mode): secrets management
- MinIO: S3-compatible object storage
- Zipkin: distributed tracing backend
- Prometheus + Grafana: metrics and dashboards

## 1) Start the platform

```bash
cp .env.example .env
docker compose --env-file .env up -d
# or: make up
```

## 2) Access URLs
- Grafana: http://localhost:3000 (`admin` / `admin` unless changed in `.env`)
- Prometheus: http://localhost:9090
- Zipkin: http://localhost:9411
- Keycloak: http://localhost:8081 (`admin` / `admin` unless changed)
- RabbitMQ UI: http://localhost:15672
- MinIO Console: http://localhost:9001
- Vault: http://localhost:8200

## 3) Core connection endpoints
- Postgres: `localhost:5432` (`bank_admin` / `bank_admin_pass`)
- Redis: `localhost:6379`
- Kafka bootstrap: `localhost:9092`
- RabbitMQ AMQP: `localhost:5672`
- Zipkin endpoint for Spring: `http://localhost:9411/api/v2/spans`

## 4) Spring Boot starter dependencies per service
Typical minimum for each microservice:
- `spring-boot-starter-web`
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- `spring-boot-starter-data-jpa` (if DB-backed)
- `spring-kafka` or `spring-boot-starter-amqp`
- `spring-cloud-starter-openfeign` (optional)
- `spring-boot-starter-oauth2-resource-server`

## 5) Suggested banking microservices
- `api-gateway`
- `auth-service`
- `customer-service`
- `account-service`
- `payment-service`
- `loan-service`
- `notification-service`
- `audit-service`

## 6) Recommended next setup
1. Create each service with Spring Boot 3.5+ and Java 21.
2. Enable `management.endpoints.web.exposure.include=*` and Prometheus actuator.
3. Wire JWT validation to Keycloak realm.
4. Externalize secrets in Vault (switch from dev mode before non-local usage).
5. Add Testcontainers for integration tests.

## Notes
- Vault is in dev mode for local convenience only.
- `scripts/init-postgres.sql` pre-creates sample databases.
- Add your microservice metrics endpoints to `infra/prometheus/prometheus.yml`.

## 7) Jenkins CI (Homebrew Jenkins at localhost:8080)
1. Create a new **Pipeline** job in Jenkins.
2. Point it to this repository (or use **Pipeline script from SCM**).
3. Use `Jenkinsfile` from repo root.
4. Ensure Jenkins agent has `java`, `mvn`, `docker`, and `docker compose` in PATH.

Pipeline behavior:
- Always validates Docker Compose and infra YAML files.
- Auto-detects Maven modules (`pom.xml`) and runs `mvn clean verify` for each.
- Publishes JUnit reports and archives jars/wars.
- Builds Docker images when `Dockerfile` files are present.

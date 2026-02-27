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

## 8) Dev/Test/Prod promotion model with code review

### Branching strategy
- `feature/*` -> CI + optional deploy to `dev`
- `develop` -> deploy to `dev`
- `release/*` or `qa/*` -> deploy to `test`
- `main`/`master` -> deploy to `prod` (with manual approval in Jenkins)

### Code review policy (recommended)
In GitHub branch protection:
1. Protect `main` (or `master`).
2. Require a pull request before merging.
3. Require at least 1-2 approving reviews.
4. Require status checks to pass (Jenkins job).
5. Dismiss stale approvals on new commits.

This enforces review before production deployment.

### Jenkins parameters for promotion
- `DEPLOY_TO_MINIKUBE=true` enables K8s deployment flow.
- `TARGET_ENV=auto` runs sequential promotion to `dev` then `test`.
- Set `TARGET_ENV=prod` explicitly for production deployment.
- `REQUIRE_PROD_APPROVAL=true` requires manual click approval before prod deploy.

### Environment values files
- `deploy/helm/banking-platform/environments/dev-values.yaml`
- `deploy/helm/banking-platform/environments/test-values.yaml`
- `deploy/helm/banking-platform/environments/prod-values.yaml`

### Build with parameters example
1. Open Jenkins job -> **Build with Parameters**.
2. Set `DEPLOY_TO_MINIKUBE=true`.
3. Leave `TARGET_ENV=auto`.
4. Keep chart path as `deploy/helm/banking-platform`.
5. Run build.

If Helm chart is not yet added at that path, deploy stage will be skipped.

## 9) Low-Memory Local Runbook (Single-Environment Mode)
If your laptop/Docker Desktop memory is limited, run one environment at a time in Minikube.

### Start/verify Minikube
```bash
minikube start --driver=docker --cpus=2 --memory=3072 --disk-size=15g --kubernetes-version=v1.33.1
kubectl config use-context minikube
kubectl get nodes
```

### Deploy to dev
In Jenkins **Build with Parameters**:
- `DEPLOY_TO_MINIKUBE=true`
- `TARGET_ENV=dev`

Verify:
```bash
kubectl get all -n banking-dev
```

Cleanup before next environment:
```bash
helm uninstall banking-platform -n banking-dev
```

### Deploy to test
In Jenkins **Build with Parameters**:
- `DEPLOY_TO_MINIKUBE=true`
- `TARGET_ENV=test`

Verify:
```bash
kubectl get all -n banking-test
```

Cleanup before next environment:
```bash
helm uninstall banking-platform -n banking-test
```

### Deploy to prod
In Jenkins **Build with Parameters**:
- `DEPLOY_TO_MINIKUBE=true`
- `TARGET_ENV=prod`
- keep `REQUIRE_PROD_APPROVAL=true`
- approve when prompted

Verify:
```bash
kubectl get all -n banking-prod
```

Optional cleanup:
```bash
helm uninstall banking-platform -n banking-prod
```

### Pause/resume local cluster
```bash
minikube stop
minikube start
```

## 10) Jenkinsfile variants
- `Jenkinsfile`: current pipeline (supports `TARGET_ENV=auto` for `dev -> test` and explicit gated `prod`).
- `Jenkinsfile.old`: previous pipeline version kept as backup/reference.

To use a specific file in Jenkins:
1. Job `Configure` -> `Pipeline`.
2. Keep `Pipeline script from SCM`.
3. Set `Script Path` to `Jenkinsfile` or `Jenkinsfile.old`.
4. Save and run a new build.

## 11) Local Security/Governance Hardening

### Trivy image scanning in Jenkins (optional)
- Pipeline parameters:
  - `ENABLE_TRIVY_SCAN=true` enables image vulnerability scanning after deploy.
  - Scans deployed images for `HIGH,CRITICAL` vulnerabilities and fails build on findings.
- Install Trivy on Jenkins host (if missing):
```bash
brew install trivy
```

### Release tags (optional)
- Pipeline parameters:
  - `CREATE_RELEASE_TAG=true`
  - `TARGET_ENV=prod`
  - `RELEASE_TAG_PREFIX=release` (customize as needed)
- Result: pipeline creates and pushes a git tag like `release-<build>-<shortsha>`.

### Basic NetworkPolicy
- Helm chart now includes `templates/networkpolicy.yaml`.
- Default behavior: allow ingress to app pods on port `80` from same namespace.
- Controlled via chart values:
  - `networkPolicy.enabled`
  - `networkPolicy.ingressFromSameNamespaceOnly`

## 12) Local Backup/Restore (PostgreSQL)

### Create backup
```bash
make backup-db
```
Output files are stored in `backups/postgres/`.

### Restore backup
```bash
make restore-db FILE=backups/postgres/<backup-file.sql.gz>
```

### Direct script usage
```bash
./scripts/postgres-backup.sh
./scripts/postgres-restore.sh backups/postgres/<backup-file.sql.gz>
```

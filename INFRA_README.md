# Banking Platform Local Infrastructure Handbook (Inception -> Current State)

This document is the complete technical history and operating model of this repository's local enterprise-style infrastructure setup.

It is intentionally exhaustive and written as a long-form engineering reference.

## 1) Objective and Scope

### 1.1 Original objective
Build a local, free, enterprise-like infrastructure foundation for a banking microservices platform, with:
- reproducible local runtime services,
- CI/CD automation,
- environment promotion controls,
- observability,
- security baseline checks,
- operational runbooks.

### 1.2 Why this matters for banking systems
Banking systems require:
- strong isolation of responsibilities,
- controlled release flow,
- auditable change process,
- layered defense (auth, secrets, network, scanning),
- observability for reliability and incident response.

Even in local development, modeling these patterns early reduces architecture rework when moving to cloud.

### 1.3 Scope boundary achieved
Completed locally:
- platform services stack,
- Kubernetes deployment pipeline,
- dev/test/prod promotion model,
- prod approval gate,
- smoke testing,
- vulnerability scanning hook (Trivy),
- backup/restore scripts,
- basic network policy and probe support.

Not fully achievable locally (future cloud phase):
- true HA/DR across zones/regions,
- enterprise IAM/KMS/audit integrations,
- regulatory segregation of duties at organization scale.

---

## 2) Toolchain Baseline (Verified)

The following local tools were confirmed and used:
- Java (OpenJDK 21+)
- Maven
- Docker + Docker Compose
- Git
- kubectl
- Helm
- Jenkins (Homebrew service)
- Minikube
- Trivy

These are the minimal layers for:
- application build (`mvn`),
- container runtime and compose stack (`docker compose`),
- Kubernetes packaging and deploy (`helm` + `kubectl`),
- CI/CD orchestration (Jenkins),
- security scanning (Trivy).

---

## 3) Infrastructure Stack Installed in `docker-compose.yml`

Source: [docker-compose.yml](/Users/sivaprasad/Desktop/my-java-project/docker-compose.yml)

### 3.1 Services and real-time purpose
1. PostgreSQL
- persistent transactional datastore,
- separate DBs per bounded context.

2. Redis
- cache/session/state acceleration.

3. RabbitMQ
- queue-based asynchronous messaging.

4. Kafka (KRaft)
- event streaming backbone.

5. Keycloak
- OAuth2/OIDC identity provider and token issuance.

6. Vault (dev mode locally)
- centralized secret lifecycle simulation.

7. MinIO
- S3-compatible object storage for documents/artifacts.

8. Zipkin
- distributed tracing backend.

9. Prometheus
- metrics scraping and alert-rule base.

10. Grafana
- dashboard and visualization layer.

### 3.2 Why free/open-source stack
- zero license barrier for local iteration,
- architecture parity with common production equivalents,
- fast onboarding and reproducibility.

---

## 4) Database Initialization Strategy

Source: [scripts/init-postgres.sql](/Users/sivaprasad/Desktop/my-java-project/scripts/init-postgres.sql)

Pre-created databases:
- `keycloak`
- `accounts_db`
- `payments_db`
- `loans_db`
- `customers_db`

Why:
- aligns with database-per-service pattern,
- prevents accidental cross-domain coupling,
- supports independent schema evolution and ownership.

---

## 5) Observability Bootstrap

Sources:
- [infra/prometheus/prometheus.yml](/Users/sivaprasad/Desktop/my-java-project/infra/prometheus/prometheus.yml)
- [infra/grafana/provisioning/datasources/datasource.yml](/Users/sivaprasad/Desktop/my-java-project/infra/grafana/provisioning/datasources/datasource.yml)
- [infra/grafana/provisioning/dashboards/dashboards.yml](/Users/sivaprasad/Desktop/my-java-project/infra/grafana/provisioning/dashboards/dashboards.yml)
- [infra/grafana/dashboards/spring-boot-overview.json](/Users/sivaprasad/Desktop/my-java-project/infra/grafana/dashboards/spring-boot-overview.json)

Why this was done early:
- observability-first engineering,
- immediate visibility once services publish metrics,
- lower debugging cost when adding real microservices.

---

## 6) Repo Operational Scaffolding

### 6.1 Environment template
Source: [.env.example](/Users/sivaprasad/Desktop/my-java-project/.env.example)

Purpose:
- centralize local credentials/config defaults,
- avoid hardcoding sensitive values in code.

### 6.2 Make helper targets
Source: [Makefile](/Users/sivaprasad/Desktop/my-java-project/Makefile)

Includes:
- stack lifecycle commands (`up`, `down`, `logs`, `clean`)
- DB operations (`backup-db`, `restore-db`)

### 6.3 Security hygiene
Source: [.gitignore](/Users/sivaprasad/Desktop/my-java-project/.gitignore)

Purpose:
- keep `.env` and local artifacts out of version control.

---

## 7) CI/CD Evolution in Jenkins

Primary source: [Jenkinsfile](/Users/sivaprasad/Desktop/my-java-project/Jenkinsfile)
Backup source: [Jenkinsfile.old](/Users/sivaprasad/Desktop/my-java-project/Jenkinsfile.old)

### 7.1 Why pipeline-as-code
- reproducibility,
- auditability,
- branch-governed evolution,
- lower configuration drift than UI-only pipelines.

### 7.2 Current pipeline capabilities
1. Toolchain verification
2. Compose/YAML validation
3. Maven project discovery + optional build/test
4. Kubernetes precheck
5. Helm deploy
6. Smoke test
7. Optional Trivy scanning
8. Optional release tag creation
9. Controlled prod approval

### 7.3 Environment promotion model
Parameters:
- `TARGET_ENV=auto` => sequential `dev -> test`
- `TARGET_ENV=prod` => explicit prod deployment (approval gate)

Rationale:
- maximize automation in lower environments,
- preserve change control for production.

---

## 8) Helm Packaging and Runtime Policy

Chart root: [deploy/helm/banking-platform](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform)

### 8.1 Chart files and purpose
- [Chart.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/Chart.yaml): chart metadata
- [values.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/values.yaml): defaults
- [environments/dev-values.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/environments/dev-values.yaml): dev-specific settings
- [environments/test-values.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/environments/test-values.yaml): test-specific settings
- [environments/prod-values.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/environments/prod-values.yaml): prod-specific settings (tuned for local memory)
- [templates/deployment.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/templates/deployment.yaml): workload
- [templates/service.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/templates/service.yaml): service abstraction
- [templates/configmap.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/templates/configmap.yaml): env metadata
- [templates/networkpolicy.yaml](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/templates/networkpolicy.yaml): ingress control baseline
- [templates/_helpers.tpl](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform/templates/_helpers.tpl): naming helpers

### 8.2 Why probes and strategy tuning were added
- readiness/liveness probes improve rollout safety and health semantics,
- rolling strategy (`maxSurge: 0`) reduced memory pressure in constrained local cluster,
- prevented upgrade deadlocks under low-memory Minikube.

---

## 9) Security Hardening Added Locally

### 9.1 Trivy in pipeline
- optional stage controlled by `ENABLE_TRIVY_SCAN`.
- scans deployed images for `HIGH,CRITICAL` vulnerabilities.
- fails build when severe issues are found.

Why:
- shift-left vulnerability detection,
- establishes baseline for future policy gates.

### 9.2 Network policy baseline
Template introduced in chart to limit ingress scope.

Why:
- introduces least-privilege networking early,
- models zero-trust direction even in local environments.

### 9.3 Prod approval gate
Manual gate retained for prod release.

Why:
- enforces human control point,
- aligns with banking-grade release governance patterns.

---

## 10) DR/Governance Simulation Added Locally

### 10.1 Backup/restore scripts
Sources:
- [scripts/postgres-backup.sh](/Users/sivaprasad/Desktop/my-java-project/scripts/postgres-backup.sh)
- [scripts/postgres-restore.sh](/Users/sivaprasad/Desktop/my-java-project/scripts/postgres-restore.sh)

Purpose:
- practice recovery procedures locally,
- make data lifecycle operations explicit and repeatable.

### 10.2 Release tagging stage (optional)
Jenkins can create/push release tags for prod runs (`CREATE_RELEASE_TAG`).

Purpose:
- stronger traceability from deployment to commit,
- basic release governance discipline.

---

## 11) Major Issues Encountered and Fixes

### 11.1 Jenkins local `file://` checkout blocked
Problem:
- Git plugin security blocked local directory remotes.
Fix:
- switched to GitHub HTTPS + credentials.

### 11.2 `docker: command not found` in Jenkins
Problem:
- Jenkins service PATH differed from shell PATH.
Fix:
- explicit Docker binary resolution and usage in pipeline.

### 11.3 Env var propagation issues (`unbound variable`)
Problem:
- cross-step variable persistence was unreliable.
Fix:
- recompute critical values in shell stage logic.

### 11.4 Helm chart missing
Problem:
- deploy stage failed due to missing `Chart.yaml`.
Fix:
- scaffolded complete minimal chart.

### 11.5 Prod rollout failures (`Progress deadline exceeded`)
Problem:
- Minikube memory exhaustion and rollout strategy constraints.
Fix:
- tuned resource requests/replicas and rollout strategy.

### 11.6 Smoke test pod timeout
Problem:
- extra smoke pod could not schedule under low memory.
Fix:
- switched to endpoint + `kubectl exec` from existing app pod.

### 11.7 Trivy invalid image parse
Problem:
- image list concatenation caused invalid image reference.
Fix:
- robust image extraction per line and app pod label filtering.

---

## 12) Full Command Runbook (Canonical)

### 12.1 Start stack
```bash
cd /Users/sivaprasad/Desktop/my-java-project
cp .env.example .env
docker compose --env-file .env up -d
docker compose ps
```

### 12.2 Minikube (low-memory profile)
```bash
minikube start --driver=docker --cpus=2 --memory=3072 --disk-size=15g --kubernetes-version=v1.33.1
kubectl config use-context minikube
kubectl get nodes
```

### 12.3 Jenkins safe defaults
- `DEPLOY_TO_MINIKUBE=true`
- `TARGET_ENV=auto` (dev->test)
- `REQUIRE_PROD_APPROVAL=true`
- `ENABLE_TRIVY_SCAN=true`
- `CREATE_RELEASE_TAG=false`

### 12.4 Explicit prod release
- `TARGET_ENV=prod`
- approve prompt in Jenkins UI
- optional `CREATE_RELEASE_TAG=true`

### 12.5 DB backup/restore
```bash
make backup-db
make restore-db FILE=backups/postgres/<backup.sql.gz>
```

---

## 13) Why each core file exists

- [docker-compose.yml](/Users/sivaprasad/Desktop/my-java-project/docker-compose.yml): local platform runtime dependencies.
- [Jenkinsfile](/Users/sivaprasad/Desktop/my-java-project/Jenkinsfile): CI/CD automation and governance logic.
- [Jenkinsfile.old](/Users/sivaprasad/Desktop/my-java-project/Jenkinsfile.old): prior pipeline backup reference.
- [Makefile](/Users/sivaprasad/Desktop/my-java-project/Makefile): operational shortcuts for stack and DB lifecycle.
- [scripts/init-postgres.sql](/Users/sivaprasad/Desktop/my-java-project/scripts/init-postgres.sql): bootstrap DB boundaries.
- [scripts/postgres-backup.sh](/Users/sivaprasad/Desktop/my-java-project/scripts/postgres-backup.sh): export full PG state.
- [scripts/postgres-restore.sh](/Users/sivaprasad/Desktop/my-java-project/scripts/postgres-restore.sh): restore full PG state.
- [deploy/helm/banking-platform/*](/Users/sivaprasad/Desktop/my-java-project/deploy/helm/banking-platform): deployment packaging and env controls.
- [infra/prometheus/*](/Users/sivaprasad/Desktop/my-java-project/infra/prometheus): metrics scrape model.
- [infra/grafana/*](/Users/sivaprasad/Desktop/my-java-project/infra/grafana): dashboard provisioning model.
- [README.md](/Users/sivaprasad/Desktop/my-java-project/README.md): quick-start and operational docs.
- [INFRA_README.md](/Users/sivaprasad/Desktop/my-java-project/INFRA_README.md): comprehensive system history and reference.

---

## 14) Installation-to-Problem Mapping

1. Installed Docker + Compose
- solved: reproducible multi-service local runtime.

2. Installed Minikube + kubectl + Helm
- solved: Kubernetes packaging/deploy rehearsal before cloud.

3. Installed Jenkins
- solved: repeatable, auditable CI/CD orchestration.

4. Installed Trivy
- solved: automated vulnerability gating capability.

5. Added observability stack
- solved: telemetry blindness and late troubleshooting costs.

6. Added backup scripts
- solved: lack of deterministic recovery exercises.

7. Added network policy/probes
- solved: weak network boundaries and poor readiness semantics.

---

## 15) Current Maturity and Next Phase

### 15.1 Current maturity
Local infrastructure foundation is complete and operational for iterative development.

### 15.2 Next phase (application layer)
- replace placeholder workload with real `account-service`,
- add schema migrations and domain tests,
- add contract testing as service mesh grows,
- migrate to cloud for full enterprise-grade HA/compliance controls.

---

## 16) Practical Notes

1. This setup intentionally balances realism and laptop constraints.
2. For low RAM, run one environment at a time if needed.
3. Keep prod gate enabled even in local practice to preserve release discipline.
4. Treat this repository as the reference blueprint for cloud transition.

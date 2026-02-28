# Account-Service and Local Platform Service Handbook (Inception -> Current)

This document is a complete technical walkthrough of what was implemented for the current `account-service` and how it integrates with local infrastructure, CI/CD, Kubernetes, and Keycloak.

It is intended as a workshop-grade reference: architecture intent, exact files, command catalog, incidents, and why each step exists in real-world banking systems.

## 1. Program Objective

Build a local but production-shaped banking service delivery flow:

1. Develop a real Spring Boot microservice (`account-service`) instead of placeholder nginx.
2. Package it as a Docker image and deploy through Helm to Minikube.
3. Promote across environments (`dev`, `test`, controlled `prod`) using Jenkins pipeline-as-code.
4. Integrate auth (Keycloak OIDC/JWT), schema migrations (Flyway), and observability (Actuator/Prometheus/Zipkin).
5. Add operational controls: smoke tests, optional image scanning, network policy, backup/restore runbooks.

## 2. What Was Built (End State)

Current end state that is now working:

1. `account-service` builds and tests in Jenkins.
2. Image is built as `local/account-service:<build-number>` and loaded into Minikube.
3. Helm deploys to namespace `banking-dev` (and supports `test` and `prod`).
4. App boots with Postgres + Flyway migration.
5. `/actuator/health` returns `UP`.
6. Business APIs work:
   - `POST /api/accounts`
   - `GET /api/accounts/{id}`
7. JWT mode works when `APP_SECURITY_ENABLED=true`.
8. OIDC auth smoke test in pipeline can obtain token from Keycloak and call protected API.

## 3. Technology Stack and Real-Time Use Cases

| Layer | Technology | Where Used | Why Used | Real-Time Banking Use Case |
|---|---|---|---|---|
| Runtime language | Java 21 | `account-service` build/runtime | LTS, mature ecosystem | Stable core services with strict type safety |
| Service framework | Spring Boot 3.3.x | `account-service` | Fast microservice bootstrap, actuator/security/data integration | Standard service baseline for regulated domains |
| Build tool | Maven | Jenkins + local build | Deterministic dependency/build lifecycle | Reproducible audited builds |
| Container engine | Docker | local image builds + compose infra | Runtime parity with cloud images | Standard artifact promotion between envs |
| Local orchestrator | Minikube | K8s deployment target | Local Kubernetes with low ops overhead | Validate deployment model before cloud |
| Package manager | Helm | chart deploys | Parameterized environment deployments | Controlled release templates per environment |
| CI/CD | Jenkins | `Jenkinsfile` | Pipeline-as-code, gated approvals | Release governance and traceability |
| DB | PostgreSQL | app persistence | ACID transactions, relational integrity | Ledger/account persistence patterns |
| DB migration | Flyway | app startup migration | Versioned schema evolution | Auditable schema change history |
| IAM/OIDC | Keycloak | JWT issuing/validation | Standards-based authn/authz flows | Tokenized service authorization |
| Metrics | Micrometer + Prometheus | Actuator scrape | Operational visibility | SLO/error budget monitoring |
| Tracing | Micrometer tracing + Zipkin | distributed tracing export | Request path observability | Incident triage and latency root cause |
| Image security | Trivy (optional) | Jenkins stage | Shift-left vulnerability check | Block known severe CVEs in release path |
| Network controls | Kubernetes NetworkPolicy | Helm template | Namespace ingress restriction baseline | East-west traffic hardening baseline |

## 4. Code Map: What Was Written and Why

### 4.1 Service Source Files

| File | Why It Was Created/Updated | Problem Solved |
|---|---|---|
| `account-service/pom.xml` | Define app dependencies for web, JPA, security, OIDC resource server, Flyway, metrics/tracing, tests | Establishes complete service runtime/build contract |
| `account-service/src/main/java/com/bank/account/AccountServiceApplication.java` | Spring Boot bootstrap class | App entrypoint for jar runtime |
| `account-service/src/main/java/com/bank/account/config/SecurityConfig.java` | Dual security mode (`app.security.enabled` true/false) | Allows gradual rollout from open mode to JWT-protected mode |
| `account-service/src/main/java/com/bank/account/controller/AccountController.java` | REST endpoints for create/get account | Exposes business API surface |
| `account-service/src/main/java/com/bank/account/service/AccountService.java` | Domain logic and conflict/not-found handling | Centralizes account creation/read behavior |
| `account-service/src/main/java/com/bank/account/entity/Account.java` | JPA entity for `accounts` table | Maps persistent model with constraints |
| `account-service/src/main/java/com/bank/account/repository/AccountRepository.java` | Data access abstraction | Enables repository query patterns (`findByAccountNumber`) |
| `account-service/src/main/java/com/bank/account/dto/CreateAccountRequest.java` | Input validation rules | Prevents malformed payloads entering domain logic |
| `account-service/src/main/java/com/bank/account/dto/AccountResponse.java` | API response contract | Stable outward response schema |
| `account-service/src/main/resources/application.yml` | Runtime config for DB, Flyway, JWT issuer, actuator, tracing | Externalized env-driven configuration and observability |
| `account-service/src/main/resources/db/migration/V1__create_accounts.sql` | Initial schema migration | Deterministic DB bootstrap |
| `account-service/src/test/java/com/bank/account/AccountServiceApplicationTests.java` | Context load test with test profile | CI guard for wiring regressions |
| `account-service/src/test/resources/application-test.yml` | Isolated H2 test profile | Removes external DB dependency from unit/context test |
| `account-service/Dockerfile` | Multi-stage image build | Produces runnable app container for K8s |

### 4.2 Helm and Deployment Files

| File | Why It Was Created/Updated | Problem Solved |
|---|---|---|
| `deploy/helm/banking-platform/values.yaml` | Default image/env/resources/probes/network policy values | Single source for deploy defaults |
| `deploy/helm/banking-platform/environments/dev-values.yaml` | Dev overrides (`APP_SECURITY_ENABLED=true`, low resources) | Environment-specific behavior without template duplication |
| `deploy/helm/banking-platform/environments/test-values.yaml` | Test sizing/security overrides | Independent environment profile |
| `deploy/helm/banking-platform/environments/prod-values.yaml` | Local-prod safe resources/security | Controlled prod-like profile under laptop constraints |
| `deploy/helm/banking-platform/templates/deployment.yaml` | Workload template with env/probes/resources | Standardized app rollout config |
| `deploy/helm/banking-platform/templates/networkpolicy.yaml` | Baseline ingress restriction | Network hardening starting point |

### 4.3 CI/CD and Ops Files

| File | Why It Was Created/Updated | Problem Solved |
|---|---|---|
| `Jenkinsfile` | Full pipeline: build, image, deploy, smoke, auth smoke, scan, tagging | Automated and governed delivery flow |
| `Jenkinsfile.old` | Preserve prior pipeline behavior | Safe rollback/reference copy |
| `README.md` | Operator runbook and parameter guidance | Fast onboarding and repeatable workflow |
| `INFRA_README.md` | Full infra handbook | Detailed architecture/operations documentation |
| `scripts/postgres-backup.sh` | Postgres dump automation | Local recovery practice |
| `scripts/postgres-restore.sh` | Postgres restore automation | Validates backup usefulness |
| `docker-compose.yml` | Local infra services and ports | Reproducible local dependencies for app + auth + observability |

## 5. Jenkins Pipeline Stages and Purpose

Source: `Jenkinsfile`

| Stage | Purpose | Why It Exists |
|---|---|---|
| Checkout | Pull exact commit | Reproducibility |
| Toolchain Check | Validate `docker`, `java`, `mvn`, `git` | Fast fail on host/tool issues |
| Discover Projects | Detect `pom.xml` modules | Supports mono-repo style growth |
| Static Validation | Validate compose/yaml | Prevents invalid infra config promotion |
| Kubernetes Precheck | Verify minikube/helm/kubectl context | Prevents deploy to wrong/unready cluster |
| Maven Build & Unit Tests | `mvn clean verify` | Quality gate before image/deploy |
| Container Build | Build/tag/load image to Minikube | Ensures deployable artifact exists |
| Helm Deploy | Deploy per target env | Environment promotion |
| Kubernetes Smoke Test | In-pod health check with retries | Runtime sanity immediately after rollout |
| Image Security Scan (Optional) | Trivy scan deployed image | Security baseline gate |
| OIDC Auth Smoke Test (Optional) | Token fetch + protected API call | Validates auth configuration end-to-end |
| Create Release Tag (Optional) | Git tag on successful path | Release traceability |

## 6. Command Catalog (What You Ran and Why)

This table captures the core commands used in the lifecycle and troubleshooting.

| Command | Why Run It | Typical Success Signal | Failure Pattern You Hit | Resolution |
|---|---|---|---|---|
| `git status` | inspect workspace state | `working tree clean` | modified/untracked files hidden from commit | stage required files explicitly |
| `git add ...` | stage intended changes | no errors | multiline/path split in shell | run as one single-line command |
| `git commit -m "..."` | record change | commit hash created | `nothing to commit` | changes were already committed/pushed |
| `git push origin main` | trigger SCM update for Jenkins | remote updated | no pipeline trigger | configure webhook/polling in Jenkins |
| `minikube start ...` | bring up local k8s | node `Ready` | memory/version constraints | align k8s version, fit Docker Desktop memory |
| `kubectl get nodes` | cluster health check | `Ready` node | not ready/context wrong | `kubectl config use-context minikube` |
| `helm lint <chart>` | chart validation | `0 chart(s) failed` | template/value mismatch | fix chart values/template |
| `helm upgrade --install ...` | deploy app | `STATUS: deployed` | progress deadline/rollout not ready | adjust resources, probes, memory, DB/auth config |
| `kubectl get all -n <ns>` | inspect workload state | pod running, deploy available | `Pending`, `CrashLoopBackOff` | inspect events/logs, fix root cause |
| `kubectl describe deploy ...` | rollout diagnosis | stable conditions | scaling/probe failures | tune strategy/probes/resources |
| `kubectl get events ...` | scheduler/runtime diagnosis | normal events | `Insufficient memory`, probe failures | reduce replicas/resources, one-env-at-time |
| `kubectl logs ...` | app boot diagnosis | startup complete | DB auth/flyway/auth issues | fix DB creds, flyway module, issuer |
| `docker compose --env-file .env up -d postgres` | local DB dependency | container healthy | `.env` missing, port bind fail | create `.env`, move host port to 5433 |
| `docker compose ps postgres keycloak` | validate auth+db deps | containers `Up` | service not listening | restart compose service |
| `curl ... /actuator/health` | app liveness check | JSON `status":"UP"` | connection refused/empty reply | ensure port-forward and app readiness |
| `kubectl port-forward ... 18080:8080` | local access to service | forwarding started | local port already in use | use alternate local port or stop process |
| `curl POST /api/accounts` | business API test | `201` + account JSON | `400 Bad Request` (payload typo/line break) | send valid JSON in one line |
| `curl GET /api/accounts/{id}` | business read path test | `200` account | `404` wrong id | use exact returned UUID |
| `curl .../token` (Keycloak) | get JWT token | `access_token` present | `invalid_grant`/`unauthorized_client` | fix client secret/user setup |
| `docker exec ... kcadm.sh ...` | inspect/fix Keycloak realm user/client | user/client data returned | shell quoting errors | quote arguments, use supported flags |
| `helm get values ... -a` | verify effective runtime values | expected env values | mismatch vs local files | redeploy with correct namespace/flags |

## 7. Key Incidents and How They Were Solved

| Symptom | Root Cause | Evidence | Fix Applied |
|---|---|---|---|
| Helm rollout `Progress deadline exceeded` | Minikube memory pressure + rolling strategy | events: `Insufficient memory` | lower resource profile and single-environment flow |
| Pod `CrashLoopBackOff` on account-service | wrong Postgres credentials | logs: `password authentication failed for user bank_admin` | align datasource creds + running postgres dependency |
| Postgres container could not start | host port 5432 already occupied | docker error bind `5432` | moved compose host mapping to `5433` |
| Flyway startup failure on PG16 | missing db-specific flyway module | logs: `Unsupported Database: PostgreSQL 16.13` | added `flyway-database-postgresql` dependency |
| JWT calls failed with malformed token | placeholder token or empty token var | `Bearer token is malformed` / `TOKEN=[]` | capture real token from keycloak response |
| JWT issuer mismatch | token `iss` != configured issuer | `iss claim is not valid` | used host `host.minikube.internal` consistently |
| Jenkins smoke test exit code 4 | one-shot in-container health curl/wget too brittle | stage failed at `kubectl exec ... exit code 4` | implemented retry logic in smoke stage |
| OIDC smoke test `HTTP_CODE=000` | port-forward race before endpoint available | curl code 52 / `000` | added port-forward readiness wait + retries |

## 8. Why Keycloak Was Installed (Real-Time Use Case)

In real banking systems, APIs cannot rely on open endpoints between channels/services.

Keycloak provides:

1. OIDC-compliant token issuer.
2. Centralized user/client management.
3. Standard JWT claims and issuer semantics.
4. Path to role-based and scope-based authorization expansion.

For this project:

1. `account-service` acts as OAuth2 Resource Server and validates JWTs.
2. Dev/test/prod behavior is controlled with `APP_SECURITY_ENABLED`.
3. Jenkins OIDC smoke test proves that protected API access requires a valid token from configured issuer.

## 9. Source References for Daily Work

Use these files as your primary references:

1. Platform overview and runbook: `README.md`
2. Infra deep handbook: `INFRA_README.md`
3. Service deep handbook (this file): `SERVICE_README.md`
4. Pipeline logic: `Jenkinsfile`
5. Helm chart and env values: `deploy/helm/banking-platform/**`
6. Service implementation: `account-service/src/main/**`
7. Service tests: `account-service/src/test/**`
8. Local infra runtime: `docker-compose.yml`

## 10. From-Scratch Rebuild Procedure (Condensed)

1. Start infra:
   - `cp .env.example .env`
   - `docker compose --env-file .env up -d`
2. Ensure Minikube up and context set:
   - `minikube start --driver=docker --cpus=2 --memory=3072 --disk-size=15g --kubernetes-version=v1.33.1`
   - `kubectl config use-context minikube`
3. Push code to main:
   - `git add ...`
   - `git commit -m "..."`
   - `git push origin main`
4. Run Jenkins with parameters:
   - `DEPLOY_TO_MINIKUBE=true`
   - `TARGET_ENV=dev` (or `auto`)
   - optional `ENABLE_TRIVY_SCAN=true`
   - optional `ENABLE_AUTH_SMOKE_TEST=true` with Keycloak creds
5. Verify:
   - `kubectl get all -n banking-dev`
   - `kubectl port-forward -n banking-dev svc/banking-platform 18080:8080`
   - `curl -s http://localhost:18080/actuator/health`

## 11. Local-Only Run (Without Minikube)

This is the exact path you validated for running `account-service` directly on your laptop with local PostgreSQL.

### 11.1 Preconditions

1. PostgreSQL is running and reachable on `localhost:5432`.
2. Database `accounts_db` exists.
3. You have valid DB credentials (example used: `postgres` / `Sample@24`).

Quick check:

```bash
pg_isready -h localhost -p 5432
```

### 11.2 Start app locally (security off for browser testing)

```bash
cd /Users/sivaprasad/Desktop/my-java-project/account-service && SERVER_PORT=18081 SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/accounts_db' SPRING_DATASOURCE_USERNAME='postgres' SPRING_DATASOURCE_PASSWORD='Sample@24' APP_SECURITY_ENABLED=false mvn spring-boot:run
```

Why `18081`:
- avoids collision with Jenkins or old port-forward listeners on `8080/18080`.

### 11.3 Verify local app health

```bash
curl -i http://localhost:18081/actuator/health
```

Expected: `HTTP/1.1 200` and JSON with `"status":"UP"` and `"db":{"status":"UP"}`.

### 11.4 Create account in local DB

```bash
curl -s -X POST "http://localhost:18081/api/accounts" -H "Content-Type: application/json" --data-binary "{\"customerId\":\"11111111-1111-1111-1111-111111111111\",\"accountNumber\":\"ACCLOCAL01\",\"currency\":\"USD\",\"openingBalance\":1500.00}"
```

Expected: JSON response containing new `id`, for example:
- `f6695195-06f3-4393-96ed-a3e17a57234c`

### 11.5 Open account URL in browser

Use the returned ID:

```text
http://localhost:18081/api/accounts/<returned-id>
```

Validated example:

```text
http://localhost:18081/api/accounts/f6695195-06f3-4393-96ed-a3e17a57234c
```

Expected: `200` with account JSON (no `401` because local run used `APP_SECURITY_ENABLED=false`).

### 11.6 Common local-only issues and fixes

| Symptom | Cause | Fix |
|---|---|---|
| `No goals have been specified` or `zsh: command not found: spring-boot:run` | multi-line command split by shell | run the full command on a single line |
| `Web server failed to start. Port 8080 was already in use` | Jenkins/another app already on `8080` | run with `SERVER_PORT=18081` |
| browser shows `401` on account URL | request hitting secured Minikube service/port-forward | stop port-forward and use local app URL on `18081` |
| browser shows `404` for old ID | ID belongs to different environment DB | create a new account locally and use returned local ID |

## 12. Practical Limits of Local Setup

This local setup is strong for architecture, CI/CD, auth, observability, and operational patterns.

It is not a substitute for cloud production controls such as:

1. Multi-zone HA/DR and tested failover.
2. Enterprise key management and hardened secret rotation workflows.
3. Regulatory controls integrated with enterprise IAM, SIEM, and audit pipelines.

That cloud phase will build on this foundation rather than replace it.

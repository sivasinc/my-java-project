# Phased Master Task List (Basic -> Enterprise)

Status legend:
- `DONE`: implemented in current repository/workflow
- `NEXT`: should be implemented in near-term sprints
- `LATER`: advanced phase after core maturity

## Phase 0: Foundation and Local Platform

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 1 | Initialize mono-repo with infra + service folders | reproducible development | stable repo layout | DONE |
| 2 | Add root docs (`README`, infra handbook, service handbook) | onboarding + repeatability | documented runbook | DONE |
| 3 | Add `.env.example` and local env strategy | avoid hardcoded env in source | consistent local config | DONE |
| 4 | Add `.gitignore` for local artifacts and secrets | prevent accidental leaks | clean VCS history | DONE |
| 5 | Add Docker Compose stack for platform dependencies | local enterprise-like infrastructure | runnable local platform | DONE |
| 6 | Provision Postgres in compose | transactional store | DB service | DONE |
| 7 | Provision Redis in compose | cache layer readiness | Redis service | DONE |
| 8 | Provision Kafka in compose | streaming readiness | Kafka broker | DONE |
| 9 | Provision RabbitMQ in compose | queue-based async readiness | RabbitMQ broker | DONE |
| 10 | Provision Keycloak in compose | OAuth2/OIDC identity | IdP available | DONE |
| 11 | Provision Vault (dev mode) in compose | secrets workflow simulation | local Vault | DONE |
| 12 | Provision MinIO in compose | S3-compatible storage | local object store | DONE |
| 13 | Provision Prometheus in compose | metrics collection | scrape backend | DONE |
| 14 | Provision Grafana in compose | visualization | dashboards | DONE |
| 15 | Provision Zipkin in compose | tracing backend | trace ingestion | DONE |
| 16 | Add DB init SQL (`accounts_db`, etc.) | db-per-service separation | pre-created databases | DONE |
| 17 | Add Makefile ops commands | operator productivity | standard shortcuts | DONE |

## Phase 1: First Real Service (`account-service`)

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 18 | Scaffold Spring Boot service | first real microservice | runnable app | DONE |
| 19 | Add account create/get REST APIs | business baseline | `/api/accounts` endpoints | DONE |
| 20 | Add DTO validation | input quality | request validation | DONE |
| 21 | Add JPA entity + repository | persistence abstraction | DB CRUD | DONE |
| 22 | Add service-layer logic and error handling | deterministic behavior | conflict/not-found handling | DONE |
| 23 | Add Flyway migration V1 | versioned schema | `accounts` table migration | DONE |
| 24 | Add env-driven `application.yml` | portability | externalized config | DONE |
| 25 | Add actuator health/readiness/liveness | operability | health endpoints | DONE |
| 26 | Add Prometheus metrics dependency | observability | metrics exposure | DONE |
| 27 | Add tracing deps + Zipkin endpoint | traceability | distributed tracing | DONE |
| 28 | Add dual security mode (`APP_SECURITY_ENABLED`) | progressive hardening | open/secure modes | DONE |
| 29 | Add OAuth2 resource-server JWT validation | auth control | bearer token enforcement | DONE |
| 30 | Add test profile with H2 | CI isolation | stable local test runtime | DONE |
| 31 | Add context-load test | baseline quality gate | initial test coverage | DONE |
| 32 | Add multi-stage Dockerfile | deployable artifact | container image | DONE |

## Phase 2: Kubernetes Packaging and Delivery

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 33 | Add Helm chart scaffold | K8s packaging | chart structure | DONE |
| 34 | Add deployment template | workload rollout | K8s Deployment | DONE |
| 35 | Add service template | service discovery | K8s Service | DONE |
| 36 | Add configmap template | deploy metadata | K8s ConfigMap | DONE |
| 37 | Add env-specific values (`dev/test/prod`) | env isolation | per-env values files | DONE |
| 38 | Add resource requests/limits tuning | scheduling stability | reduced memory failures | DONE |
| 39 | Add readiness/liveness probes | safer rollout | health-gated startup | DONE |
| 40 | Add basic NetworkPolicy | baseline hardening | ingress restrictions | DONE |

## Phase 3: Jenkins CI/CD and Release Flow

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 41 | Add pipeline-as-code `Jenkinsfile` | reproducible CI/CD | automated pipeline | DONE |
| 42 | Toolchain check stage | fail fast | env validation | DONE |
| 43 | Static validation stages | config sanity | compose/yaml checks | DONE |
| 44 | Maven discover/build/test stages | code quality | compile + tests | DONE |
| 45 | Docker image build stage | artifact generation | build-tagged image | DONE |
| 46 | Minikube image load stage | cluster availability | image load in cluster | DONE |
| 47 | Helm deploy stage | environment deployment | namespace rollout | DONE |
| 48 | Kubernetes smoke stage | runtime verification | post-deploy checks | DONE |
| 49 | Smoke retries hardening | reduce flakiness | resilient smoke stage | DONE |
| 50 | Optional Trivy stage | vuln scanning baseline | CVE scanning gate | DONE |
| 51 | Optional OIDC auth smoke stage | auth end-to-end validation | token-based API check | DONE |
| 52 | OIDC retries + port-forward readiness | remove race failures | stable auth smoke | DONE |
| 53 | Optional release tag stage | release traceability | git tags | DONE |
| 54 | Keep `Jenkinsfile.old` backup | rollback/reference | previous pipeline file | DONE |

## Phase 4: Local Operations and Troubleshooting Outcomes

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 55 | Resolve Postgres host port conflicts | compose stability | moved compose path to 5433 | DONE |
| 56 | Validate local-only app run on `18081` | avoid port conflicts | direct local mode | DONE |
| 57 | Validate local create/get account flow | service correctness | successful local CRUD | DONE |
| 58 | Validate secured mode with Keycloak | auth correctness | JWT-protected API success | DONE |
| 59 | Add backup/restore scripts | DR baseline | manual backup/restore capability | DONE |
| 60 | Document low-memory and env-by-env runbook | laptop reliability | constrained-resource operations | DONE |

## Phase 5: Immediate Enterprise Gaps (P0/P1)

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 61 | Add SonarQube service and Jenkins sonar stage | static quality governance | quality report + gate | DONE |
| 62 | Add JaCoCo coverage thresholds | enforce test depth | coverage fail gate | NEXT |
| 63 | Add unit tests for service logic branches | regression prevention | deterministic unit suite | DONE |
| 64 | Add controller tests (`@WebMvcTest`) | API behavior confidence | 200/201/400/404/409 tests | DONE |
| 65 | Add repository tests (`@DataJpaTest`) | query correctness | repository confidence | DONE |
| 66 | Add Testcontainers integration tests | real DB integration confidence | e2e DB-backed tests | DONE |
| 67 | Add Cucumber BDD tests | business-readable acceptance | executable feature specs | DONE |
| 68 | Add contract tests | inter-service compatibility | provider/consumer gates | NEXT |
| 69 | Add performance tests | capacity baseline | latency/throughput profile | NEXT |
| 70 | Add resilience tests | failure-mode confidence | outage/retry behavior tests | NEXT |
| 71 | Move secrets to K8s Secrets/Vault path | security hardening | no plain secrets in values | NEXT |
| 72 | Add secret rotation process | credential lifecycle | rotation SOP | NEXT |
| 73 | Add role/scope authorization rules | least-privilege authz | claim-based access control | NEXT |
| 74 | Add security authorization tests | prevent auth regressions | auth test coverage | NEXT |
| 75 | Add OpenAPI generation/publishing | API governance | discoverable contract | NEXT |
| 76 | Add standardized error schema | consistent client integration | stable error contract | NEXT |
| 77 | Add API versioning/deprecation policy | compatibility governance | versioning policy | NEXT |
| 78 | Add idempotency key support for writes | retry safety | duplicate-proof write APIs | NEXT |
| 79 | Add optimistic locking strategy | concurrency safety | conflict-safe updates | NEXT |
| 80 | Add immutable audit trail | compliance and forensics | action audit records | NEXT |
| 81 | Add structured logs with trace IDs | diagnosability | correlated logs | NEXT |
| 82 | Define SLI/SLO targets | reliability governance | SLO baseline | NEXT |
| 83 | Configure alert rules by SLO | actionable ops | alert policies | NEXT |
| 84 | Add incident runbooks | operational maturity | response playbooks | NEXT |

## Phase 6: Streaming, Caching, and Batch Analytics

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 85 | Implement Kafka producer (`account-created`) | real-time eventing | emitted domain events | NEXT |
| 86 | Implement Kafka consumer service | real-time processing | running consumer | NEXT |
| 87 | Add retry topic + DLQ strategy | poison-message handling | robust stream processing | NEXT |
| 88 | Add event schema governance/versioning | safe event evolution | managed schemas | NEXT |
| 89 | Integrate Redis cache in service | latency optimization | cached account reads | NEXT |
| 90 | Add cache invalidation strategy | consistency | correct cache behavior | NEXT |
| 91 | Add cache metrics | observability | hit/miss visibility | NEXT |
| 92 | Add Airflow to compose | batch orchestration | scheduler platform | NEXT |
| 93 | Add daily DAG(s) | automation | scheduled batch pipelines | NEXT |
| 94 | Add Spark cleaning/feature jobs | DS readiness | curated datasets | NEXT |
| 95 | Define data zones (`raw/clean/feature/serving`) | governed lifecycle | lakehouse structure | NEXT |
| 96 | Persist curated Parquet to MinIO/S3 | analytics-friendly storage | reusable datasets | NEXT |
| 97 | Add data quality checks in DAG | dataset trust | DQ reports | NEXT |
| 98 | Add Metabase/Superset for business analytics | business reporting | KPI dashboards | NEXT |
| 99 | Keep Grafana for ops dashboards | SRE monitoring | ops dashboard suite | NEXT |

## Phase 7: ML/GenAI and Advanced Governance

| ID | Task | Why | Output | Status |
|---|---|---|---|---|
| 100 | Build first ML baseline pipeline | predictive capability | model v1 | LATER |
| 101 | Add MLflow tracking + registry | experiment governance | model versioning | LATER |
| 102 | Add batch scoring workflow | operational model usage | scored outputs | LATER |
| 103 | Add drift detection | model reliability | drift alerts | LATER |
| 104 | Add GenAI RAG assistant on banking corpus | domain assistant | grounded QA | LATER |
| 105 | Add GenAI guardrails (PII/policy/refusal) | safe outputs | controlled assistant behavior | LATER |
| 106 | Add human approval for high-risk AI outputs | risk mitigation | HITL flow | LATER |
| 107 | Generate SBOM in CI | supply-chain transparency | SBOM artifacts | LATER |
| 108 | Sign images/artifacts and verify provenance | trust chain | verified artifacts | LATER |
| 109 | Add policy-as-code (OPA/Kyverno) | platform enforcement | admission policies | LATER |
| 110 | Add GitOps (Argo CD/Flux) | declarative reconciled delivery | drift control | LATER |
| 111 | Harden RBAC/quotas/limits by namespace | multi-team safety | governance baseline | LATER |
| 112 | Enforce Pod Security Standards | runtime hardening | secure pod posture | LATER |
| 113 | Automate backup schedules + restore drills | DR readiness | measured RTO/RPO | LATER |
| 114 | Build compliance evidence pipeline | audit readiness | compliance artifacts | LATER |
| 115 | Add cloud IaC and env isolation blueprint | cloud transition | deployable cloud setup | LATER |
| 116 | Integrate managed secrets/KMS/IAM | enterprise cloud security | managed secret controls | LATER |
| 117 | Expand to more services (payments/loans/etc.) | domain completeness | multi-service platform | LATER |
| 118 | Add saga + inter-service contract governance | distributed consistency | reliable orchestration | LATER |
| 119 | Add chaos/fault-injection program | resilience proof | controlled failure tests | LATER |
| 120 | Add release-readiness checklist + ADR process | engineering governance | formal change control | LATER |

## Suggested Execution Order (Practical)

1. Complete Phase 5 tasks `61-74` first (quality + testing + security/authz).
2. Then implement Phase 6 tasks `85-99` (Kafka/Redis/Airflow/Spark/analytics).
3. Then move to Phase 7 (ML/GenAI/governance/cloud transition).

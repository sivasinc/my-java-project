# Command Reference (Daily Use)

## 1) Repo and Git
```bash
cd /Users/sivaprasad/Desktop/my-java-project
git status
git add <files>
git commit -m "<message>"
git push origin main
```

## 2) Local Infra
```bash
cp -n .env.example .env
docker compose --env-file .env up -d
docker compose ps
docker compose logs -f <service>
```

## 3) SonarQube
```bash
docker compose --env-file .env up -d sonarqube
curl -I http://localhost:9002
```

## 4) Account Service Tests
```bash
cd /Users/sivaprasad/Desktop/my-java-project/account-service
mvn -U -B -ntp clean test
mvn -U -B -ntp clean verify
```

## 5) Run Account Service Locally (local Postgres on 5432)
```bash
cd /Users/sivaprasad/Desktop/my-java-project/account-service && \
SERVER_PORT=18081 \
SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/accounts_db' \
SPRING_DATASOURCE_USERNAME='postgres' \
SPRING_DATASOURCE_PASSWORD='Sample@24' \
APP_SECURITY_ENABLED=false \
mvn spring-boot:run
```

## 6) Local App Checks
```bash
curl -i http://localhost:18081/actuator/health
curl -s -X POST "http://localhost:18081/api/accounts" -H "Content-Type: application/json" --data-binary "{\"customerId\":\"11111111-1111-1111-1111-111111111111\",\"accountNumber\":\"ACCLOCAL01\",\"currency\":\"USD\",\"openingBalance\":1500.00}"
```

## 7) Kubernetes Checks
```bash
kubectl config use-context minikube
kubectl get nodes
kubectl get all -n banking-dev
kubectl logs -n banking-dev <pod-name> --tail=200
kubectl get events -n banking-dev --sort-by=.metadata.creationTimestamp | tail -n 40
```

## 8) Helm Deploy (manual)
```bash
helm upgrade --install banking-platform deploy/helm/banking-platform \
  -n banking-dev \
  -f deploy/helm/banking-platform/environments/dev-values.yaml \
  --set-string image.repository=local/account-service \
  --set-string image.tag=latest
```

## 9) Keycloak Token and Auth Check
```bash
TOKEN=$(curl -s -X POST 'http://host.minikube.internal:8081/realms/banking/protocol/openid-connect/token' \
  --resolve 'host.minikube.internal:8081:127.0.0.1' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=account-service-client' \
  --data-urlencode 'client_secret=HtA2ioCbLpyLZyoxoL4Z8zuGXZDv4D7C' \
  --data-urlencode 'username=bankuser' \
  --data-urlencode 'password=bankpass' | jq -r '.access_token')

curl -i -H "Authorization: Bearer $TOKEN" \
  "http://localhost:18080/api/accounts/c0ee6b00-41d9-4ab0-96ed-1b9fbfd9cc4a"
```

## 10) Troubleshooting
```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:5432 -sTCP:LISTEN
pg_isready -h localhost -p 5432
```

## 11) Jenkins Build (No Deploy)
In Jenkins UI -> **Build with Parameters**:
- `DEPLOY_TO_MINIKUBE=false`
- `ENABLE_SONARQUBE_SCAN=false` (or true if Sonar server is up)
- keep other defaults

Expected Maven stage behavior:
- runs `mvn clean verify`
- executes unit/controller/repository/Cucumber BDD tests
- Testcontainers integration test skips if Docker is unavailable

# Service Flow After Infra Setup (Kid-Friendly)

This is what we built after infrastructure was ready.

## 1) We made a real service: `account-service`
- What: A small app that handles bank account data.
- Why: We needed a real "account machine" to do useful work.

## 2) We added two main APIs
- `POST /api/accounts` -> create a new account
- `GET /api/accounts/{id}` -> read an account by ID
- Why: First we must create data, then read it back.

## 3) We added safety checks for input
- Example: bad UUID, empty account number, negative opening balance are rejected.
- Why: Bad input should be blocked early.

## 4) We connected service to database
- Added entity + repository + Flyway migration for `accounts` table.
- Why: Data must be saved permanently.

## 5) We added business rules in service layer
- If account number already exists -> return `409 Conflict`
- If account ID not found -> return `404 Not Found`
- Why: App should respond correctly for normal and error cases.

## 6) We made config environment-based
- App settings come from environment variables.
- Why: Same code can run in local, dev, test, and prod with different settings.

## 7) We added health and monitoring basics
- Health/readiness/liveness endpoints
- Metrics and tracing dependencies
- Why: We must know if app is alive and healthy.

## 8) We added security mode switch
- Open mode for local learning
- JWT-secured mode for real enterprise flow
- Why: Easy workshop testing now, stronger security later.

## 9) We added test layers
- Unit tests for service logic
- Controller tests (`@WebMvcTest`) for `200/201/400/404/409`
- Repository tests (`@DataJpaTest`)
- Integration test with Testcontainers + Postgres
- Why: Tests prove behavior and prevent future breakage.

## 10) We fixed local test stability
- Added Mockito mock-maker config for this local JDK setup.
- Why: Tests were failing locally before this fix.

## 11) We validated end-to-end manually
- Created account with `POST`
- Read same account with `GET` using returned ID
- Why: Final proof that service works in real local run.

## 12) What we added now (file-by-file)
- `account-service/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- Why: force Mockito to use subclass mock-maker, so tests run on local JDK without bytecode self-attach issues.

- `account-service/src/test/java/com/bank/account/controller/AccountControllerTest.java`
- Why: added missing API behavior checks (`409` create conflict and `200` get success), so controller contract is complete.

- `account-service/src/test/java/com/bank/account/repository/AccountRepositoryTest.java`
- Why: added missing negative repository test (empty result when account number does not exist).

- `account-service/src/test/java/com/bank/account/integration/AccountApiIntegrationTest.java`
- Why: made Testcontainers integration test safe for machines without Docker (`disabledWithoutDocker = true`), so local runs stay stable.

- `account-service/src/test/resources/features/account_api.feature`
- Why: added business-readable acceptance scenario (create account, then fetch by ID).

- `account-service/src/test/java/com/bank/account/bdd/CucumberAcceptanceTest.java`
- Why: Cucumber test runner entry point so feature files are executable.

- `account-service/src/test/java/com/bank/account/bdd/CucumberSpringConfiguration.java`
- Why: Cucumber + Spring test context config with test profile/H2/MockMvc setup.

- `account-service/src/test/java/com/bank/account/bdd/AccountApiStepDefinitions.java`
- Why: step-by-step Java actions for the Cucumber scenario.

- `account-service/pom.xml`
- Why: added Cucumber/JUnit suite dependencies so BDD tests compile and run in Maven.

- `PHASED_MASTER_TASKLIST.md`
- Why: status tracking update after implementation (`61`, `63`, `64`, `65`, `66`, `67` moved to `DONE`).

## 13) Super Simple Teaching Flow (6th Grade Style)
1. We built a small "bank account robot" (`account-service`).
2. We taught it 2 jobs:
- Create an account (`POST /api/accounts`)
- Find an account by ID (`GET /api/accounts/{id}`)
3. We gave it rules:
- If input is bad -> say "Bad Request" (`400`)
- If account number already exists -> say "Conflict" (`409`)
- If ID does not exist -> say "Not Found" (`404`)
4. We connected it to a notebook (database) so it remembers accounts.
5. We added checkups (tests) to make sure the robot behaves correctly:
- Unit test = check one small brain rule
- Controller test = check API door behavior
- Repository test = check DB lookup behavior
- Integration test = check full app + DB together
- BDD test = check business story (create then fetch) in simple language
6. We fixed local test problems so tests run on your machine.
7. We proved it works by creating an account and fetching the same account by ID.
8. Then we marked completed tasks in the master task list.

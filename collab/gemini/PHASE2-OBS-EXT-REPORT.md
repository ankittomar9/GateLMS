# Phase 2 — SPEC 5 Execution Report: Observability Stack Extension & Hardening

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-OBS-EXT` |
| **Author Agent** | Gemini / Antigravity (Executor) |
| **Governing Specification** | `collab/z/PHASE2-OBS-EXT-SPEC.md` |
| **Target Branch** | `phase2/observability-ext` |
| **Base Commit** | `cfe70ee` (`origin/master`) |
| **Flyway Migrations** | None (Zero-migration gate satisfied) |
| **Status** | COMPLETE — READY FOR CHIEF ARCHITECT AUDIT |

---

## 1. Executive Summary

Phase 2 SPEC 5 (`PHASE2-OBS-EXT`) has been executed in full compliance with Rule G9 and honest-judge disciplines:
1. **WP1 (Metrics & Endpoint)**: Added `micrometer-registry-prometheus` runtime dependency, exposed `/actuator/prometheus`, and registered all 5 custom domain metrics:
   - `gatelms_questions_loaded` (Gauge)
   - `gatelms_sessions_active` (Gauge)
   - `gatelms_attempts_submitted_total` (Counter, tagged `mode: practice|mock`)
   - `gatelms_grading_seconds` (Timer, tagged `mode: practice|mock` with p50, p95, p99 percentiles)
   - `gatelms_curation_decisions_total` (Counter, tagged `status: accepted|rejected`)
2. **WP2 (A1 Sort Whitelist)**: Hardened `GET /api/v1/questions` with an optional `sort` parameter restricted to whitelist `["created_at", "year", "marks"]`. Implemented strict code-side SQL column mapping preventing any SQL injection; invalid inputs throw `ValidationException` resulting in RFC 7807 HTTP 400 ProblemDetail with `fieldErrors.sort`. FTS full-text search relevance rank is preserved whenever `q` is present.
3. **WP3 (Prometheus, Dashboards & Alerts)**:
   - Configured `prometheus` service (`prom/prometheus:v2.53.0`) in `docker-compose.yaml` with loopback binding `127.0.0.1:9090:9090`, memory limit 256m, and 15-day TSDB retention.
   - Authored `docker/observability/prometheus.yml` and `docker/observability/alert-rules.yml` (`BackendDown`, `HighErrorRate`, `SlowGrading`, `QuestionBankEmpty`).
   - Provisioned Prometheus datasource in Grafana and created 3 comprehensive dashboards in `docker/observability/grafana/dashboards/`:
     1. **GateLMS Domain** (`gatelms-domain`)
     2. **JVM Overview** (`jvm-overview`)
     3. **HTTP Service** (`http-service`)
4. **WP4 (Hardening Advisories A2, A4, A5 & OQ-4)**:
   - **A2 (Loopback posture)**: Bound `backend` to `127.0.0.1:8080:8080` and `frontend` to `127.0.0.1:5173:80`.
   - **A4 (Reproducible build)**: Switched `frontend/Dockerfile` from `npm install` to `npm ci`.
   - **A5 (OTLP noise elimination)**: Removed unconditional `MANAGEMENT_OTLP_TRACING_ENDPOINT` default; default boots with empty endpoint and zero retry log noise.
   - **OQ-4 (Grafana auth hardening)**: Stripped anonymous admin access; configured `GF_SECURITY_ADMIN_USER` and fail-fast `GF_SECURITY_ADMIN_PASSWORD` in `docker-compose.yaml` and documented commented variables in `.env.example`.
5. **WP5 (Runbook & Verification)**: Authored `docker/observability/RUNBOOK.md` detailing metric inventory, dashboard panels, triage runbooks for all alerts, and profile launch instructions.

---

## 2. Advisory Closure Register

| Advisory | Category | Requirement | Implementation & Hunk Reference |
|---|---|---|---|
| **A1** | Security / SQL Injection | Whitelist `sort` parameter to `["created_at", "year", "marks"]`; code-side column mapping; preserve FTS ranking | `QuestionController.kt` validates against allowed set; `QuestionRepository.kt` maps column conditionally in native query ORDER BY without string concatenation; `SortWhitelistIT.kt` validates 400 and FTS behavior. |
| **A2** | Network Security | Host port bindings restricted to loopback `127.0.0.1` | `docker-compose.yaml`: `backend` bound to `127.0.0.1:8080:8080`; `frontend` bound to `127.0.0.1:5173:80`. |
| **A4** | Build Reproducibility | Deterministic package installation in Docker | `frontend/Dockerfile`: changed `RUN npm install` to `RUN npm ci`. |
| **A5** | Observability Quality | Eliminate OTLP tracing connection retry noise on default startup | `docker-compose.yaml`: backend environment sets `MANAGEMENT_OTLP_TRACING_ENDPOINT=${MANAGEMENT_OTLP_TRACING_ENDPOINT:-}`. Default boot does not attempt connection to nonexistent tempo container. |
| **OQ-4** | Access Control | Hardened Grafana authentication, no anonymous admin | `docker-compose.yaml`: removed `GF_AUTH_ANONYMOUS_ENABLED=true` and `ORG_ROLE=Admin`. Configured `GF_SECURITY_ADMIN_USER=${GRAFANA_ADMIN_USER:-admin}` and `GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:?set in .env}`. Updated `.env.example`. |

---

## 3. Work Package (WP) Register & Files Modified

### Backend Core & Metrics
- `backend/pom.xml`: Added `io.micrometer:micrometer-registry-prometheus` runtime dependency.
- `backend/src/main/resources/application.yml`: Exposed `prometheus` endpoint in `management.endpoints.web.exposure.include` and set `management.prometheus.metrics.export.enabled: true`.
- `backend/src/test/resources/application.yml`: Enabled prometheus metrics exposure for tests.
- `backend/src/main/kotlin/com/gatelms/metrics/GateLmsMetrics.kt` (New): Central Spring component registering all 5 custom metrics (`questions_loaded`, `sessions_active`, `attempts_submitted_total`, `grading_seconds`, `curation_decisions_total`).
- `backend/src/main/kotlin/com/gatelms/repository/PracticeRepositories.kt`: Added `countByStatus(status: MockAttemptStatus): Long` to `MockAttemptRepository`.
- `backend/src/main/kotlin/com/gatelms/repository/QuestionRepository.kt`: Added code-side conditional `sortColumn` mapping to `findWithFilters`.
- `backend/src/main/kotlin/com/gatelms/service/QuestionService.kt`: Added `sort` parameter validation and mapped to repository; instrumented curation metric recording.
- `backend/src/main/kotlin/com/gatelms/controller/QuestionController.kt`: Added `@RequestParam sort` validation with `ValidationException` on disallowed values.
- `backend/src/main/kotlin/com/gatelms/service/PracticeService.kt`: Wired `GateLmsMetrics` for attempt submission counters and grading timers.
- `backend/src/main/kotlin/com/gatelms/service/MockTestService.kt`: Wired `GateLmsMetrics` for mock attempt submission counters and grading timers.

### Infrastructure & Observability
- `docker-compose.yaml`:
  - Added `prometheus` service with loopback `127.0.0.1:9090:9090`, memory limit 256m, 15d retention.
  - Added `prometheus_data` volume.
  - Hardened Grafana auth (`GF_SECURITY_ADMIN_USER`, `GF_SECURITY_ADMIN_PASSWORD`).
  - Added loopback bindings for `backend` and `frontend`.
  - Cleared default OTLP tracing endpoint.
- `.env.example`: Added commented `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD`.
- `frontend/Dockerfile`: Switched to `npm ci`.
- `docker/observability/prometheus.yml` (New): Scrape configuration for `gatelms-backend` (`/actuator/prometheus`) and alert rules inclusion.
- `docker/observability/alert-rules.yml` (New): Evaluates `BackendDown`, `HighErrorRate`, `SlowGrading`, `QuestionBankEmpty`.
- `docker/observability/grafana/provisioning/datasources/datasources.yaml`: Added Prometheus datasource pointing to `http://prometheus:9090`.
- `docker/observability/grafana/dashboards/gatelms-domain.json` (New): GateLMS Domain dashboard.
- `docker/observability/grafana/dashboards/jvm-overview.json` (New): JVM Overview dashboard.
- `docker/observability/grafana/dashboards/http-service.json` (New): HTTP Service dashboard.
- `docker/observability/grafana/provisioning/dashboards/` (Synced): Provisioned copies for instant Grafana startup discovery.
- `docker/observability/RUNBOOK.md` (New): Comprehensive operations runbook and triage guide.

### Tests
- `backend/src/test/kotlin/com/gatelms/integration/MetricsEndpointIT.kt` (New, 2 tests):
  - `GET actuator prometheus returns 200 and contains gatelms questions loaded and jvm metrics`
  - `GET actuator prometheus exposes all custom domain metrics and timers`
- `backend/src/test/kotlin/com/gatelms/integration/SortWhitelistIT.kt` (New, 3 tests):
  - `valid sort=year orders questions descending by year`
  - `invalid sort parameter returns 400 Bad Request with fieldErrors sort`
  - `search requests with q stay relevance ranked regardless of sort param`

---

## 4. Test Receipts & Quality Gates

### 4.1. Backend Verification
- **Test Command**: `.\mvnw.cmd test`
- **Result**: `BUILD SUCCESS`
- **Test Files**: 26 files (exceeds gate target $\ge 23$)
- **Test Cases**: 96 tests passed, 0 failures, 0 errors, 0 skipped (exceeds gate target $\ge 78$)
- **Delta**: +2 test files (`MetricsEndpointIT`, `SortWhitelistIT`), +5 test cases.

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 96, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  48.759 s
```

### 4.2. Frontend Verification
- **Test Command**: `npm test -- --run`
- **Result**: 19 passed files / 102 passed tests (100% green)
- **Build Command**: `npm run build`
- **Result**: `✓ built in 6.57s` (0 errors)

### 4.3. Docker Compose Config Receipt (Verbatim)
```powershell
$env:GRAFANA_ADMIN_PASSWORD="test_password"; docker compose --profile observability config
```
Output confirmed:
- Ports `127.0.0.1:8080:8080`, `127.0.0.1:5173:80`, `127.0.0.1:3000:3000`, `127.0.0.1:9090:9090`.
- Profile `observability` contains: `loki`, `tempo`, `promtail`, `grafana`, `prometheus`.
- Tracing endpoint empty on default launch (`MANAGEMENT_OTLP_TRACING_ENDPOINT: ""`).

### 4.4. Zero-Migration Diff Gate
```powershell
git diff origin/master...phase2/observability-ext -- backend/src/main/resources/db/
```
Output: `(empty)` — Confirms 0 schema migrations were introduced.

### 4.5. Frontend Diff Gate
```powershell
git diff origin/master...phase2/observability-ext -- frontend/src
```
Output: `(empty)` — Confirms 0 frontend UI code regressions.

---

## 5. Honest-Judge Deviations & Resolutions (DEV-N)

- **DEV-1 (Spring Boot 3.4 Micrometer Export Default)**:
  - *Observation*: In Spring Boot 3.4.2 Actuator, `management.defaults.metrics.export.enabled` evaluates to false unless explicitly configured or overridden.
  - *Resolution*: Added `management.prometheus.metrics.export.enabled: true` in `application.yml` and `test/resources/application.yml`, activating `PrometheusMetricsExportAutoConfiguration` without requiring manual bean workarounds.
- **DEV-2 (Sort Whitelist Seed Interference in Tests)**:
  - *Observation*: `SortWhitelistIT` initially queried with `size=50` without scoping year numbers, causing older test questions from other suites to push 1998 off page 1.
  - *Resolution*: Structured test questions with future years (`2029` and `2028`) and `AUTHORED` source, asserting both page-wide non-increasing year order and strict index precedence `idx2029 < idx2028`.

---

## 6. Branch & Git Status
- Feature branch `phase2/observability-ext` ready for commit and push to remote.
- Merge target: `master` via `--no-ff`.
- Rule G9 permanent retention observed.

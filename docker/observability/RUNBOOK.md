# GateLMS Observability Stack Runbook

This document is the operational guide and runbook for the GateLMS Observability stack (Prometheus, Grafana, Loki, Tempo, Promtail).

---

## 1. Architecture & Services

The GateLMS observability platform runs on Docker under the `observability` profile:

| Service | Port / Loopback | Purpose | Configuration / Storage |
|---|---|---|---|
| **Prometheus** | `127.0.0.1:9090` | Scrapes Spring Boot metrics every 15s; evaluates alert rules; TSDB retention 15d | `docker/observability/prometheus.yml`, `alert-rules.yml` |
| **Grafana** | `127.0.0.1:3000` | Unified visualization for metrics, logs, and traces; admin authenticated via `.env` | Provisioned dashboards & datasources |
| **Loki** | `127.0.0.1:3100` | Structured JSON log aggregation store | `docker/observability/loki-config.yaml` |
| **Tempo** | `127.0.0.1:3200` / `4318` | Distributed tracing engine with OTLP HTTP receiver on port 4318 | `docker/observability/tempo.yaml` |
| **Promtail** | N/A | Tails host `./logs/gatelms-backend.jsonl` into Loki | `docker/observability/promtail.yaml` |

---

## 2. Metric Catalog

GateLMS instruments both standard JVM / HTTP metrics and domain-specific counters, gauges, and timers:

| Metric Name | Type | Tags | Unit | Description |
|---|---|---|---|---|
| `gatelms_questions_loaded` | Gauge | None | count | Total number of questions in GateLMS question bank |
| `gatelms_sessions_active` | Gauge | None | count | Total number of currently active (`IN_PROGRESS`) mock/practice sessions |
| `gatelms_attempts_submitted_total` | Counter | `mode: practice \| mock` | submissions | Total number of submitted exam and practice attempts |
| `gatelms_grading_seconds` | Timer | `mode: practice \| mock` | seconds | Time taken to grade submissions (publishes p50, p95, p99 percentiles) |
| `gatelms_curation_decisions_total` | Counter | `status: accepted \| rejected` | decisions | Total number of question moderation / curation decisions |
| `http_server_requests_seconds` | Timer | `uri`, `status`, `method` | seconds | Spring MVC HTTP request latency and invocation counts |
| `jvm_memory_used_bytes` | Gauge | `area: heap \| nonheap`, `id` | bytes | JVM memory consumption |
| `jvm_memory_max_bytes` | Gauge | `area: heap \| nonheap`, `id` | bytes | JVM configured maximum memory limit |
| `jvm_gc_pause_seconds` | Timer | `action`, `cause` | seconds | JVM Garbage Collection pause frequency and duration |
| `jvm_threads_live_threads` | Gauge | None | threads | Current live Java threads |
| `process_cpu_usage` | Gauge | None | ratio (0-1) | CPU percentage consumed by the backend JVM process |

---

## 3. Provisioned Dashboards Inventory

Grafana is provisioned automatically with four dashboards:

1. **GateLMS Domain (`uid: gatelms-domain`)**:
   - Total Questions Loaded (Stat / Gauge)
   - Active Practice/Mock Sessions (Stat / Gauge)
   - Attempts Submission Rate by Mode (Timeseries)
   - Grading Latency p95 by Mode (Timeseries)
   - Curation Decisions Rate by Status (Timeseries)
2. **JVM Overview (`uid: jvm-overview`)**:
   - JVM Heap Memory Used vs Max (Timeseries)
   - CPU Usage: Process vs Host System (Timeseries)
   - GC Pause Time Rate (Timeseries)
   - Live and Daemon Thread Counts (Timeseries)
3. **HTTP Service (`uid: http-service`)**:
   - Request Rate segmented by URI (Timeseries)
   - HTTP 5xx Error Ratio with 1% / 5% threshold lines (Timeseries)
   - Average and Max Request Latency (Timeseries)
   - Tomcat Busy vs Max Thread Pool capacity (Timeseries)
4. **GateLMS Backend — Logs & Traces (`uid: gatelms-logs-traces`)**:
   - Real-time Log Stream with TraceID click-through linking to Tempo traces.

---

## 4. Alerting & Incident Runbook

Alert rules are defined in `docker/observability/alert-rules.yml` and monitored via Prometheus and the Grafana Alerting panel.

### 4.1. `BackendDown` (Critical)
- **Condition**: `up{job="gatelms-backend"} == 0` for 1 minute.
- **Impact**: The backend API is unreachable; users cannot access questions or submit tests.
- **Triage Steps**:
  1. Check backend container status: `docker compose ps backend`.
  2. Inspect backend logs: `docker compose logs --tail=100 backend`.
  3. Verify Postgres connection: `docker compose exec postgres pg_isready -U postgres -d gatelms_db`.
  4. Restart backend: `docker compose restart backend`.

### 4.2. `HighErrorRate` (Warning)
- **Condition**: 5xx HTTP responses exceed 5% of total requests over 5 minutes.
- **Impact**: Users experiencing request failures.
- **Triage Steps**:
  1. Open Grafana $\to$ **GateLMS Logs & Traces** dashboard.
  2. Filter by `level="ERROR"`.
  3. Identify failing URI and inspect stack trace in logs or Tempo trace.
  4. Verify database pool health in **JVM Overview** / Hikari metrics.

### 4.3. `SlowGrading` (Warning)
- **Condition**: `gatelms_grading_seconds{quantile="0.95"} > 2s` over 5 minutes.
- **Impact**: Users experience noticeable delays upon submitting test papers.
- **Triage Steps**:
  1. Check CPU load in **JVM Overview** dashboard.
  2. Inspect database query latency for `AttemptAnswer` batch persistence.
  3. Verify whether large mock exams (65 questions) are experiencing locking contention.

### 4.4. `QuestionBankEmpty` (Critical)
- **Condition**: `gatelms_questions_loaded == 0` for 5 minutes.
- **Impact**: Zero questions available in question bank; practice mode is broken.
- **Triage Steps**:
  1. Verify question seed migration completed: check `flyway_schema_history`.
  2. Run seed runner manually if necessary: `POST /api/v1/admin/seed` or restart with `SPRING_PROFILES_ACTIVE=seed`.
  3. Query database directly: `SELECT count(*) FROM questions;`.

---

## 5. Security & Authentication Setup

### 5.1. Grafana Admin Authentication (OQ-4)
Anonymous admin access is **disabled**. To log into Grafana:
1. In `.env`, configure credentials:
   ```env
   GRAFANA_ADMIN_USER=admin
   GRAFANA_ADMIN_PASSWORD=your_strong_password_here
   ```
2. When starting the observability profile, docker compose enforces that `GRAFANA_ADMIN_PASSWORD` is set (fail-fast syntax `:?`).
3. Open `http://localhost:3000` and sign in with the configured credentials. Anonymous login will be rejected with HTTP 401.

### 5.2. Loopback Posture (A2)
All observability ports (`3000`, `9090`, `3100`, `3200`, `4318`) and core ports (`8080`, `5173`) bind to `127.0.0.1` on the host, preventing unauthorized external network access.

---

## 6. How to Run

### Running Without Observability (Default Mode — Zero Noise)
```bash
docker compose up -d
```
In default mode, tracing OTLP export is inactive and Prometheus/Grafana containers are not started. No OTLP connection retry noise will appear in backend logs.

### Running With Observability Active
1. Ensure `.env` contains `GRAFANA_ADMIN_PASSWORD`:
   ```bash
   cp .env.example .env
   # Edit .env and uncomment GRAFANA_ADMIN_PASSWORD
   ```
2. Start all services including observability:
   ```bash
   docker compose --profile observability up -d
   ```
3. Verify all endpoints:
   - Backend Actuator: `http://localhost:8080/actuator/prometheus`
   - Prometheus Targets: `http://localhost:9090/targets` (shows `gatelms-backend` as `UP`)
   - Grafana UI: `http://localhost:3000` (login with your credentials)

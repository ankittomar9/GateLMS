# GateLMS Observability Stack Runbook (Grafana + Loki + Tempo)

This directory contains the configurations and provisioned datasources for the GateLMS Observability foundation (Phase 1, Work Package 4).

---

## 1. Architecture Overview

- **Backend Telemetry (Spring Boot + Kotlin on Host)**:
  - Tracing: Spring Boot Actuator + Micrometer Tracing bridge with OpenTelemetry (`micrometer-tracing-bridge-otel`).
  - Trace Exporter: Pushes OTLP spans over HTTP/protobuf to `http://localhost:4318/v1/traces`.
  - Structured Logging: Logback with `LogstashEncoder` writes JSON log events with MDC fields (`traceId`, `spanId`, `level`, `message`) into `./logs/gatelms-backend.jsonl`.
- **Containers (Dockerized under `profiles: ["observability"]`)**:
  - **Loki (`:3100`)**: Log aggregation engine storing JSON log lines.
  - **Promtail**: Scrapes host `./logs/gatelms-backend.jsonl` via read-only volume mount, parses JSON fields, and pushes to Loki.
  - **Tempo (`:3200`, `:4318`)**: High-volume distributed tracing store receiving spans on port `4318`.
  - **Grafana (`:3000`)**: Visualization UI with pre-provisioned Loki & Tempo datasources and trace-to-log correlation links.

---

## 2. Quickstart Instructions

### Step 1: Start the Observability Stack

```powershell
docker compose --profile observability up -d
```

Verify running containers:
```powershell
docker compose ps
```

### Step 2: Access Endpoints

| Service | Port / URL | Credentials |
|---|---|---|
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Anonymous Admin enabled (Zero config) |
| **Tempo API / UI** | [http://localhost:3200](http://localhost:3200) | N/A |
| **Tempo OTLP Receiver** | `http://localhost:4318` | N/A (OTLP/HTTP for backend) |
| **Loki** | [http://localhost:3100](http://localhost:3100) | N/A |

### Step 3: Run Core Backend with Observability Active

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

All incoming REST requests (e.g. `/api/v1/questions`, `/api/v1/subjects`) will generate trace spans shipped to Tempo (`:4318`) and JSON log lines written to `logs/gatelms-backend.jsonl`.

---

## 3. End-to-End Trace Correlation Guide

1. Open **Grafana** at `http://localhost:3000`.
2. Navigate to **Dashboards** $\to$ **GateLMS** $\to$ **GateLMS Backend — Logs & Traces**.
3. In the live log panel at the bottom:
   - Expand any log entry.
   - Look for the `traceId` field.
   - Click the **TraceID** derived link.
4. Grafana will automatically open the **Tempo trace viewer** showing:
   - Span timeline
   - Total latency of SQL queries and HTTP handler execution
   - Deep links back into matching log lines.

---

## 4. Teardown Instructions

To stop and remove observability containers:

```powershell
docker compose --profile observability down
```

To preserve logs and traces for later analysis, data volumes (`loki_data`, `tempo_data`, `grafana_data`) are retained by default.

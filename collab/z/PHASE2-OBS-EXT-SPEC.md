# [PHASE2-OBS-EXT-SPEC] — Prometheus Metrics, Grafana Dashboards & Alerts, Grafana Auth, Hardening A1–A5

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-OBS-EXT` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase2/seed-bulk` (SPEC 1) — independent of SPECs 2–4 otherwise |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | PHASE1-SPEC OQ-1 (Prometheus timing — now) + OQ-4 (Grafana auth); PHASE1-FIXUP-AUDIT advisories A1, A2, A4, A5 |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase2/observability-ext` · **No migration** · Effort 5–7 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Complete the observability stack (OQ-1): **Prometheus scraping + provisioned Grafana dashboards + alert rules**, harden **Grafana auth** (OQ-4 — no more anonymous admin), and land the four open hardening advisories from the PHASE1-FIXUP audit. Closes A1, A2, A4, A5.

## 2. Backend — Metrics (binding)

1. Dependency: `io.micrometer:micrometer-registry-prometheus` (runtime).
2. `application.yml`: expose `prometheus` under `management.endpoints.web.exposure.include` (health,info,prometheus — keep existing list and append).
3. **Custom metrics (binding names, documented in the runbook)**:
   - `gatelms_questions_loaded` (Gauge — total question count from stats service, refreshed 60 s)
   - `gatelms_attempts_submitted_total` (Counter, tagged `mode: practice|mock`)
   - `gatelms_grading_seconds` (Timer, tag `mode`)
   - `gatelms_curation_decisions_total` (Counter, tag `status: accepted|rejected`)
   - `gatelms_sessions_active` (Gauge — IN_PROGRESS attempts)
4. JVM/HTTP metrics come free via micrometer — no custom work.

## 3. Hardening (binding, closes advisories)

- **A1 — sort whitelist**: `GET /api/v1/questions` gains optional `sort` param validated against a whitelist `["created_at","year","marks"]` (default `created_at`). Mapping into the native query ORDER BY is by **code-side column mapping only** (map param → literal SQL column inside a `when` block; never string-concatenate the param). Invalid value → 400 ProblemDetail `fieldErrors.sort`. The internal FTS-relevance-first ordering is preserved when `q` is present (sort applies only to non-search requests; searches stay relevance-ranked).
- **A2 — loopback posture**: `docker-compose.yaml` binds `backend` → `127.0.0.1:8080:8080` and `frontend` → `127.0.0.1:5173:80` (postgres and GLT ports already loopback).
- **A4 — reproducible frontend image**: `frontend/Dockerfile` build stage switches `npm install` → `npm ci` (lockfile is committed and current — verify `npm ci` succeeds locally first).
- **A5 — OTLP noise**: remove the unconditional `MANAGEMENT_OTLP_TRACING_ENDPOINT` default from the `backend` service env. Tracing endpoint is set **only** in the observability profile: add a compose `x-observability-env` anchor or profile-specific override documented in the runbook; default (no profile) boots with tracing export disabled and zero retry-log noise.
- **OQ-4 — Grafana auth**: remove `GF_AUTH_ANONYMOUS_ENABLED=true` / `ORG_ROLE=Admin`. Provision admin via `GF_SECURITY_ADMIN_USER=${GRAFANA_ADMIN_USER:-admin}` + `GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:?set in .env}` (fail-fast if unset when profile used). Add `.env.example` entries (both keys, commented defaults) — never real secrets in git (G5).

## 4. Infrastructure (observability profile, binding)

- **`prometheus` service** (profile `observability`, loopback `127.0.0.1:9090:9090`, mem limit 256m): image `prom/prometheus:v2.53.0`; config `docker/observability/prometheus.yml` — scrape `backend:8080/actuator/prometheus` every 15 s, honor labels, 15-day retention (command flags).
- **Grafana provisioning** (existing profile): datasource `prometheus` (provisioned, pointing at `http://prometheus:9090`), plus **3 dashboards** as JSON under `docker/observability/grafana/dashboards/`:
  1. **GateLMS Domain** — panels: questions_loaded gauge, attempts rate by mode, grading p95 latency, curation decisions rate, active sessions.
  2. **JVM Overview** — heap used vs max, GC pause rate, thread count, CPU.
  3. **HTTP Service** — request rate by uri, p50/p95/p99 latency, error (5xx) ratio, tomcat threads.
  Each dashboard: uid-stable, `$__rate_interval`-correct panels, meaningful units, no default "Panel title" leftovers.
- **Alert rules** (`docker/observability/grafana/alerting/` or Prometheus rule file — pick one, document): `BackendDown` (up==0 for 1 m, critical), `HighErrorRate` (5xx ratio > 5% over 5 m, warning), `SlowGrading` (gatelms_grading_seconds p95 > 2 s over 5 m, warning), `QuestionBankEmpty` (gatelms_questions_loaded == 0 for 5 m, critical). Alert delivery = Grafana UI only (no email/pager — Operator watches the Alerting panel; documented in runbook).
- **Runbook**: extend `docker/observability/RUNBOOK.md` (created in WP1 Phase 1 — keep the file, append sections): metric catalog with names/units, dashboard inventory, alert list with runbook-style diagnosis steps for each, Grafana auth setup steps (`.env`), how to run with/without the profile.

## 5. Verification & Test Gates

- **Backend delta** ≥ +2 files / +5 cases (target ≥ 23 / ≥ 78): `MetricsEndpointIT` ≥ 2 (`GET /actuator/prometheus` returns 200 and body contains `gatelms_questions_loaded` + `jvm_memory_used` — Testcontainers slice with metrics on), `SortWhitelistIT` ≥ 3 (valid `sort=year` changes ordering; invalid `sort=; DROP` → 400 with fieldErrors.sort; search requests stay relevance-ranked regardless of sort param).
- **Infra receipts (executor, quoted verbatim in report)**: `docker compose --profile observability config` (valid parse), `curl localhost:8080/actuator/prometheus | grep gatelms_` output, `curl localhost:9090/api/v1/targets` showing the backend target UP, Grafana dashboard list via API (`/api/search?type=dash-db`) after provisioning.
- **Frontend**: zero changes; `npm run build` + vitest stay green (guards against accidental breakage).
- **Diff gates**: `git diff ... -- frontend/src` empty (except none expected); `docker-compose.yaml` diffs limited to §3/§4 changes; `.env.example` gains only the two commented Grafana keys.

## 6. Report & Acceptance

Report `collab/gemini/PHASE2-OBS-EXT-REPORT.md`: WP register; G3 per-file receipts; §5 infra receipts verbatim; advisory closure table (A1/A2/A4/A5 → commit hunk references); honest-judge register (DEV-N). Operator acceptance: `docker compose --profile observability up -d`, log into Grafana with the `.env` credentials (anonymous login must FAIL), open the 3 dashboards with live data, break something (stop backend) and watch `BackendDown` fire, verify `?sort=year` and `?sort=evil` behaviors on the questions API, and confirm the default (no-profile) boot has no OTLP retry noise.

**Out of scope**: metrics on the frontend app, distributed tracing UI (Tempo already integrated — only dashboards for traces if trivial), long-term metrics storage beyond 15 days, email/Slack alert delivery, multi-tenant Grafana.

**Work packages**: WP1 metrics + prometheus endpoint (1.5 h) · WP2 A1 sort whitelist + tests (1.5 h) · WP3 prometheus service + dashboards + alerts (2 h) · WP4 Grafana auth + A2/A4/A5 hardening (1 h) · WP5 runbook + receipts + report (1 h).

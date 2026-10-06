# Spec: PHASE1 — Syllabus Taxonomy, Question Bank Core, Seed Pipeline & Observability Foundation

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE1` (work packages `PHASE1-WP0` … `PHASE1-WP4`) |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | `1c06274` |
| **Governing Inputs** | `SPEC.md` §5 Phase 1 · Operator directive 2026-10-07 · `collab/z/PHASE0-FIXUP-AUDIT.md` residuals R1–R4 |
| **Executor** | Gemini / Antigravity (primary; split-safe per §3.4) |
| **Status** | `APPROVED_FOR_EXECUTION` (awaiting Operator directive to Gemini) |
| **Estimated Effort** | ~11–14 focused hours (see §12.3) |

---

## 1. Purpose & Operator Directives

This spec operationalizes **Phase 1** of `SPEC.md` §5 ("Syllabus Taxonomy & Question Bank Core") and incorporates the Operator's directives from the 2026-10-07 session:

1. **Build the full LMS core now** — backend + frontend working end-to-end, exercised through the multi-agent pipeline (Gemini executes, Qwen reviews independently, Z.ai audits, Operator manually tests the running application).
2. **Observability is in scope**: a Grafana + Loki + Tempo (GLT) stack must be provisioned (WP4), with tracing/log foundations landing in WP1 so all later phases are debuggable from day one.
3. **Prepare the 2,500-question GATE Overflow seeding, but do not bulk-seed yet.** Phase 1 delivers the import pipeline, validation contract, and a synthetic sample pack (WP3). Sourcing and bulk import of real PYQ data is a post-Phase-1 task owned by the Operator + Gemini.
4. **Long-session execution preferred.** Gemini should execute this spec in one long session if it can; the spec is deliberately split-safe at work-package boundaries so multiple agents can complete remaining packages without re-planning (§3.4).

## 2. Scope & Non-Goals (Scope-Creep Defense)

**In scope (Phase 1 definition of done):**
- Read APIs for syllabus taxonomy (subjects + topics) and mastery updates.
- Question bank CRUD + full-text search + two-pass status transitions + bookmarking.
- Flyway `V3` (integrity/locking columns) and `V4` (FTS) migrations.
- Frontend: real routing, syllabus explorer page, question browser page with filters/search/pagination, question detail page with KaTeX rendering and reveal-solution UX.
- Seed import pipeline (JSONL contract + idempotent runner + validation report + ≥25-question synthetic sample pack).
- Observability: JSON structured logs, Micrometer/OTel tracing wired to Tempo, log shipping to Loki, Grafana with provisioned datasources (GLT compose profile).
- Backend test harness (unit + Testcontainers integration) and frontend Vitest harness.

**Explicit non-goals (deferred, do not build):**
- Authentication/authorization (single-user local tool; revisit only if ever exposed beyond localhost).
- Mock exam engine, palette UI, scoring, virtual calculator (Phase 3).
- Dashboard analytics & mastery visualizations beyond a minimal `/stats/summary` endpoint (Phase 2).
- Mistake notebook (Phase 4), notes vault/PDF upload (Phase 5).
- Scraping/sourcing the real 2,500 GATE Overflow questions (post-Phase-1; requires Operator's data decisions).
- Prometheus/metrics long-term storage (open question OQ-1, §15 — actuator metrics endpoints suffice for Phase 1).
- Any AI/NLP features. No new UI frameworks, no state-management libraries beyond what §10 lists.

## 3. Work Package Plan

### 3.1 Execution order and dependency graph

```text
WP0 (governance sweep, ~30 min)
  └─> WP1 (backend domain core + V3/V4 + tests + tracing foundations)
        └─> WP2 (frontend pages + Vitest)
        └─> WP3 (seed pipeline + sample pack)      [depends on WP1 entities/repos]
              └─> WP4 (GLT observability stack)    [depends on WP1 log/tracing output]
```

### 3.2 Work package register

| WP | ID | Title | One-line scope | Acceptance anchor |
|---|---|---|---|---|
| 0 | `PHASE1-WP0` | Governance sweep | R1 prompt-label fix + R2 loopback port binding (see §3.5) | §3.5 |
| 1 | `PHASE1-WP1` | Backend domain core | Entities, repos, services, DTOs, controllers, error model, V3+V4, actuator/tracing/JSON-logging, full backend test suite | §4, §5, §6, §11 |
| 2 | `PHASE1-WP2` | Frontend LMS shell & pages | Router, TanStack Query, typed API client, syllabus + questions + detail pages, KaTeX MathText, filters, skeletons, Vitest suite | §7, §11 |
| 3 | `PHASE1-WP3` | Question seed pipeline | JSONL contract, idempotent upsert runner, validation report, ≥25 synthetic sample questions, import verification test | §8, §11 |
| 4 | `PHASE1-WP4` | Observability (GLT) | Compose `observability` profile (Loki, Tempo, Promtail, Grafana), provisioned datasources, correlation config, runbook | §9 |

### 3.3 Commit & push discipline (G7)

One atomic commit per work package, pushed immediately after its local verification passes. Message convention:

```text
feat(phase1): WP0 governance sweep
feat(phase1): WP1 backend domain core (entities, APIs, V3/V4, tracing)
feat(phase1): WP2 frontend syllabus & question bank pages
feat(phase1): WP3 question seed pipeline + sample pack
feat(phase1): WP4 observability stack (Grafana/Loki/Tempo)
```

### 3.4 Split protocol (multi-agent completion)

If Gemini's session must end early, it stops **only at a WP boundary** (all acceptance anchors for that WP met, committed, pushed) and publishes `collab/gemini/PHASE1-WP{n}-REPORT.md` for the completed packages, marking the remaining ones `PENDING`. Any agent that picks up the remaining packages must: re-read this spec + the latest WP report, implement the remaining WPs in order, and publish its own report file(s) under the same naming scheme (`PHASE1-WP{n}-REPORT.md`, author = completing agent). A consolidated `collab/gemini/PHASE1-REPORT.md` (or `PHASE1-COMBINED-REPORT.md` if split) closes the task with all receipts. Executors must never edit another agent's report file (G8).

### 3.5 WP0 detail — governance sweep (PHASE0-FIXUP-AUDIT residuals)

1. **R1**: in `collab/STANDARDIZED-AGENT-PROMPTS.md`, relabel the two "G7 (Worklog Scope)" citations (Qwen prompt §5 and Z.ai prompt §5) to "G8 (Worklog Scope)" — the rule now lives in G8; G7 is Shell Execution Discipline. Root governance files (`AGENTS.md`, `SPEC.md`, `collab/README.md`, `STANDARDIZED-AGENT-PROMPTS.md`) may be edited by the Executor **only** under an explicit spec condition + Operator directive (precedent: the G8 codification in PHASE0-FIXUP, commit `03339c2`); agent folders remain strictly isolated.
2. **R2**: in `docker-compose.yaml`, bind the Postgres port to loopback — `"127.0.0.1:5432:5432"` — so the dev database is never LAN-exposed while zero-config startup is preserved (the `.env.example` from PHASE0-FIXUP covers credential overrides).
3. Do **not** touch `collab/operator/` — recording `PHASE0-ACCEPTANCE.md` (R5) is the Operator's own action.


## 4. Database Migrations (exact DDL; forward-only, never edit V1/V2 — the F9 in-place exception is closed per PHASE0-FIXUP-AUDIT R3)

### 4.1 `backend/src/main/resources/db/migration/V3__phase1_integrity_and_locking.sql`

```sql
-- PHASE1-WP1: integrity + optimistic-locking foundations
-- F10 (PHASE0-SETUP-AUDIT): 5-state TCS iON palette integrity
ALTER TABLE attempt_answers
    ADD CONSTRAINT chk_attempt_answer_state
    CHECK (state IN ('NOT_VISITED', 'NOT_ANSWERED', 'ANSWERED',
                     'MARKED_FOR_REVIEW', 'ANSWERED_AND_MARKED_FOR_REVIEW'));

-- Qwen OBS-01 (endorsed): optimistic locking + modification audit
ALTER TABLE questions
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE topics
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0;
```

### 4.2 `backend/src/main/resources/db/migration/V4__phase1_fulltext_search.sql`

```sql
-- PHASE1-WP1: PostgreSQL full-text search over the question bank
ALTER TABLE questions
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(statement, '')), 'A')
        || setweight(to_tsvector('english',
             coalesce(external_id, '') || ' ' || coalesce(explanation, '')), 'B')
    ) STORED;

CREATE INDEX idx_questions_fts ON questions USING GIN (search_vector);
```

Implementation notes (binding):
- `to_tsvector('english', …)` with an explicit configuration is immutable — required for `GENERATED … STORED`.
- The JPA `Question` entity must **not** write `search_vector` (map it read-only or omit it; the DB owns it).
- Query predicate: `search_vector @@ websearch_to_tsquery('english', :q)`; rank with `ts_rank`. Treat a blank/absent `q` as "no FTS predicate" at the service layer (an empty `websearch_to_tsquery` matches nothing).

## 5. REST API Contract (all under `/api/v1`)

Conventions: JSON only; timestamps ISO-8601 with offset; pagination follows Spring's `Page<T>` shape (`content`, `page`, `size`, `totalElements`, `totalPages`); errors use Spring's `ProblemDetail` (RFC 7807) with `detail` human-readable and `properties.fieldErrors` map for validation failures. Status codes: 200/201/204 success, 400 validation, 404 not found, 409 version conflict, 500 unhandled (never leak stack traces).

| # | Method & Path | Purpose | Request → Response (essentials) |
|---|---|---|---|
| 1 | `GET /api/v1/subjects` | Subject list | → `[{id, name, slug, cluster, avgWeightage, priorityTier, standardBookRef, topicCount}]` |
| 2 | `GET /api/v1/subjects/{idOrSlug}` | Subject + topics | → `{…subject fields, topics: [{id, name, slug, bookChapterRef, masteryLevel, lastRevisedAt, nextRevisionDue, version}]}` |
| 3 | `GET /api/v1/topics?subjectId=&subjectSlug=` | Topic list (one filter required) | → `[TopicDto]`; 400 if neither param |
| 4 | `PATCH /api/v1/topics/{id}` | Update mastery/revision | `{masteryLevel?, nextRevisionDue?, lastRevisedAt?, version}` → 200 `TopicDto`; 409 on stale `version` |
| 5 | `GET /api/v1/questions` | Browse/filter/search | Query params: `page(0), size(20, max 100), subjectId, topicId, type(MCQ|MSQ|NAT), year, pass1Status, pass2Status, bookmarked, q` (FTS) → `Page<QuestionSummaryDto>`; ordering: FTS rank when `q` present, else `created_at DESC, id DESC` |
| 6 | `GET /api/v1/questions/{id}` | Full detail | → `QuestionDto` (summary + `optionsJson, correctOptions, numericMin/Max, explanation, gateOverflowUrl, version`) |
| 7 | `POST /api/v1/questions` | Create | `QuestionUpsertDto` → 201 + `QuestionDto`; validation rules V-Q below |
| 8 | `PUT /api/v1/questions/{id}` | Full update | `QuestionUpsertDto` + `version` → 200; 409 stale version |
| 9 | `PATCH /api/v1/questions/{id}/status` | Two-pass transitions | `{pass1Status?, pass2Status?, isBookmarked?}` → 200 `QuestionDto`; enum-validated |
| 10 | `GET /api/v1/stats/summary` | Counts for shell | → `{totalQuestions, bySubject: [{subjectId, count}], byType: {MCQ, MSQ, NAT}, pass1: {SOLVED, ASSISTED, FAILED, PENDING}}` |

**Validation rules V-Q (binding, enforced via jakarta validation + service checks):**
- `type ∈ {MCQ, MSQ, NAT}`; `marks ∈ {1, 2}`; `statement` non-blank; `subjectId` and `topicId` must exist (404-style field error otherwise).
- MCQ: `optionsJson` has exactly 4 entries (A–D); `correctOptions` exactly 1.
- MSQ: 4 options; `correctOptions` 1–4 entries; duplicates rejected.
- NAT: `numericMin` and `numericMax` present and `numericMin ≤ numericMax`; options/correctOptions must be null.
- MCQ/MSQ: `numericMin/Max` must be null.
- `gateOverflowUrl`, when present, must be an http(s) URL.
- `QuestionSummaryDto` never exposes `correctOptions`/`numericMin/Max`/`explanation` (list views are key-safe; detail view is used post-reveal).

## 6. Backend Architecture & Conventions (WP1)

**Package layout** (inside `com.gatelms`): `controller` / `service` / `repository` / `entity` / `dto` / `error` / `config` / `seed`. `GateLmsApplication.kt` keeps only the main class; `HealthController` moves to `controller/HealthController.kt` (version via `BuildProperties`, not hardcoded).

- Entities: JPA with explicit `@Column` mappings; `@Version version: Int` on `Question`/`Topic`; `@UpdateTimestamp updatedAt`; constructor injection only; zero Lombok; DTOs are immutable `data class`es; mapping via small manual mapper functions in the service layer (no MapStruct — zero churn).
- Controllers are thin (`@RestController`, `@RequestMapping("/api/v1/…")`, `@Valid`); services hold all logic; repositories are Spring Data JPA interfaces.
- Question filtering: **one native query path** (not Specification + native dual paths). Native `@Query` with optional named parameters (`(:subjectId IS NULL OR subject_id = :subjectId)` pattern, `cast(:q as text)` guards for null-typed params), plus matching `countQuery`; FTS predicate and `ts_rank` ordering per §4.2. Service normalizes blank `q` to null.
- Error handling: `@RestControllerAdvice` producing `ProblemDetail` for: `MethodArgumentNotValidException` (400 + fieldErrors), `EntityNotFoundException` (404), `ObjectOptimisticLockingFailureException` (409), fallback `Exception` (500, generic message).
- CORS: allow `http://localhost:5173` (config-prop `app.cors.allowed-origins`) in addition to the existing Vite proxy.
- Actuator: expose `health, info, metrics` only. Tracing: `micrometer-tracing-bridge-otel` + OTel OTLP exporter → `http://localhost:4318/v1/traces`, sampling probability `1.0` (dev). Logging: `logback-spring.xml` with JSON console appender **and** rolling file `logs/gatelms-backend.jsonl` (daily, 7-day keep, 50 MB cap); `traceId`/`spanId` flow via MDC (micrometer-tracing). `logs/` is already git-ignored.

## 7. Frontend Architecture (WP2)

**New dependencies (see §10 manifest):** `react-router-dom`, `@tanstack/react-query`, plus Vitest toolchain. Structure: `src/api/` (typed client + models), `src/pages/` (route-level components), `src/components/` (shared presentational), `src/hooks/`.

- **Routing**: `createBrowserRouter` with lazy routes; layout keeps the existing Sidebar/Header/RightSidebar shell; nav links become routes: `/` (current dashboard placeholder), `/syllabus`, `/questions`, `/questions/:id`. No nested tabs beyond this.
- **Server state**: one `QueryClient` (`staleTime: 30_000`, `retry: 1`); queries via custom hooks (`useSubjects`, `useQuestions(filters)`, `useQuestion(id)`, `useStatsSummary`); mutations for status PATCH/PUT with invalidation.
- **API client**: `fetch` wrapper with generics, JSON headers, `AbortController` support, error normalization to a typed `ApiError` (status + message + fieldErrors). Base URL `/api/v1` (Vite proxy already wired).
- **`MathText` component** (critical, KaTeX): splits text into plain / `$…$` inline / `$$…$$` display segments; renders math with `katex.renderToString({strict: false, throwOnError: false})`; on KaTeX failure renders the segment as a neutral styled `<code>` chip (never crashes, never shows unrendered raw LaTeX as body text). Display math block-centered.
- **Questions page**: debounced (300 ms) search box, subject/topic (cascaded) selects, type/year/pass-status filters, bookmark toggle filter, pagination controls (page size 20); list rows show type badge (MCQ amber / MSQ cyan / NAT emerald), marks, year, subject, pass chips; skeleton rows while loading; empty state and error state components; URL search params mirror filter state (shareable/back-button-safe).
- **Question detail page**: statement via `MathText`; options list; **answer key + explanation hidden behind an explicit "Reveal solution" toggle** (two-pass honesty: default hidden); after reveal, correct options highlighted; NAT range shown; Pass1/Pass2 status controls (PENDING/SOLVED/ASSISTED/FAILED chips → PATCH), bookmark star; external GATE Overflow link; version conflict (409) surfaces a non-destructive "reload latest" toast.
- **Syllabus page**: subject cards from API (weightage, priority tier, book ref, topic count) → drill into topic list with mastery indicators (0–5 dots) and inline mastery stepper (PATCH) with optimistic-lock 409 handling.
- Responsive: existing breakpoints; grids collapse `lg→sm`; right rail behavior unchanged. All TUF tokens (§2 of AGENTS.md) reused — no new palette values.
- **Vitest harness** (resolves F5): jsdom + Testing Library; `vitest run` in CI-equivalent local gate; tests enumerated in §11.

## 8. WP3 — Question Seed Pipeline Contract

**File**: `backend/seed-data/questions.jsonl` (one JSON object per line; the directory is git-tracked — sample pack only — while future bulk data of thousands of lines will be reviewed for size before committing; if > 2 MB, keep bulk files out of git and load from an untracked path).

**JSONL schema (binding contract for future 2,500-question sourcing):**

```json
{"externalId":"GO-CS-2023-42","year":2023,"type":"NAT","marks":2,
 "subjectSlug":"computer-organization","topicSlug":"instruction-pipelining",
 "statement":"A 5-stage pipeline has stage delays 2, 3, 5, 4 and 2 ns. The speedup over a non-pipelined design is ____ (round to 2 decimals).",
 "numericMin":2.99,"numericMax":3.01,"explanation":"…",
 "gateOverflowUrl":"https://gateoverflow.in/example"}
```

Fields: `externalId` (unique, required), `year`, `type`, `marks`, `subjectSlug`, `topicSlug` (must resolve to seeded subjects/topics), `statement`, `options` `{A,B,C,D}` (MCQ/MSQ only), `correctOptions` `["A",…]` (MCQ: exactly 1; MSQ: 1–4), `numericMin/numericMax` (NAT only, min ≤ max), optional `explanation`, `gateOverflowUrl`, `pass1Status`. Same type rules as V-Q.

**Runner**: `com.gatelms.seed.QuestionSeedRunner` — a `@Profile("seed")` `ApplicationRunner` reading `app.seed.path` (default `backend/seed-data/questions.jsonl`), executed via `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed`. Semantics: **idempotent upsert** keyed on `external_id` (insert or update; never duplicate). Output receipt (stdout + log): `total, inserted, updated, rejected` plus up to 20 rejection reasons (line number + reason). Non-zero exit is NOT triggered by rejections (imports are best-effort with report).

**Sample pack requirement**: ≥25 questions authored by Gemini (synthetic, exam-realistic, *not* copied PYQ content), covering all 10 subjects, all 3 types (≥5 MSQ with multi-correct, ≥8 NAT with tolerance ranges), ≥8 containing LaTeX (including ≥2 with display `$$…$$`), ≥5 with explanations, ≥1 deliberately-latent edge case per type.

## 9. WP4 — Observability: Grafana + Loki + Tempo

**Topology constraint honored (G1)**: backend runs on the host via Maven, only data/obs containers are Dockerized. The bridge: (a) backend pushes traces via **OTLP/HTTP to `localhost:4318`** published from the Tempo container; (b) backend writes JSON logs to `./logs/gatelms-backend.jsonl`, which **Promtail scrapes via a read-only bind mount** into Docker and ships to Loki.

**`docker-compose.yaml` additions** (all services carry `profiles: ["observability"]`; ports bound to loopback):

```yaml
  loki:
    image: grafana/loki:<current 3.x>
    ports: ["127.0.0.1:3100:3100"]
    volumes: [loki_data:/loki, ./docker/observability/loki-config.yaml:/etc/loki/config.yaml:ro]
  tempo:
    image: grafana/tempo:<current 2.x>
    command: ["-config.file=/etc/tempo.yaml"]
    ports: ["127.0.0.1:3200:3200", "127.0.0.1:4318:4318"]   # 3200 UI, 4318 OTLP/HTTP
    volumes: [tempo_data:/tempo, ./docker/observability/tempo.yaml:/etc/tempo.yaml:ro]
  promtail:
    image: grafana/promtail:<current 3.x>
    volumes: [./logs:/var/log/gatelms:ro, ./docker/observability/promtail.yaml:/etc/promtail/config.yaml:ro]
  grafana:
    image: grafana/grafana:<current LTS>
    ports: ["127.0.0.1:3000:3000"]
    environment: [GF_AUTH_ANONYMOUS_ENABLED=true, GF_AUTH_ANONYMOUS_ORG_ROLE=Admin]
    volumes: [grafana_data:/var/lib/grafana, ./docker/observability/grafana/provisioning:/etc/grafana/provisioning:ro]
```

Config files live under `docker/observability/` (loki: filesystem store, short retention, `reject_old_samples`; tempo: OTLP HTTP receiver on 4318, local backend storage, 7-day retention; promtail: scrape `/var/log/gatelms/*.jsonl`, JSON parsing, `trace_id`/`span_id` extracted as labels; grafana: provisioned datasources Loki + Tempo with Tempo **derived field** on the `trace_id` log label → deep-link to trace explore).

**Grafana starter dashboard (provisioned, minimal):** "GateLMS Backend — Logs & Traces": panel 1 = error-log rate (LogQL `rate` over `{app="gate-lms-backend"} |= "ERROR"`), panel 2 = log volume by level, panel 3 = Tempo service graph / recent traces table. No metrics panels (see OQ-1).

**Application wiring recap (lands in WP1):** `management.tracing.sampling.probability=1.0`, `management.otlp.tracing.endpoint=http://localhost:4318/v1/traces`, actuator `health,info,metrics`, JSON logging with MDC trace/span fields, log field `app=gate-lms-backend`, `service.name=gate-lms-backend` resource attribute.

**Runbook:** a short `docker/observability/README.md` — how to start (`docker compose --profile observability up -d`), URLs (Grafana `:3000`, Tempo `:3200`), how to follow a trace end-to-end (grep a request log line by `trace_id` → Explore → Tempo), and how to stop (`docker compose --profile observability down`).

## 10. Dependency Manifest (churn control — additions are exhaustive; nothing else may be added)

**Backend (`pom.xml`)** — versions managed by the Spring Boot 3.4.2 parent where possible; explicit only where noted:

| Dependency | Scope | Justification |
|---|---|---|
| `spring-boot-starter-actuator` | runtime | health/info/metrics + tracing hooks (WP1/WP4) |
| `io.micrometer:micrometer-tracing-bridge-otel` | runtime | bridge Micrometer observations → OTel spans (parent-managed) |
| `io.opentelemetry:opentelemetry-exporter-otlp` | runtime | OTLP/HTTP trace export to Tempo (pin via `opentelemetry-bom`, current 1.4x line) |
| `net.logstash.logback:logstash-logback-encoder` | runtime | JSON structured logs for Loki (explicit version, current 8.x) |
| `org.springframework.boot:spring-boot-testcontainers` | test | Testcontainers integration support |
| `org.testcontainers:postgresql` | test | clean-DB integration tests (Pillar 2 proof) |
| `io.mockk:mockk` | test | idiomatic Kotlin mocking for service unit tests (explicit 1.13.x) |

**Frontend (`package.json`)**:

| Dependency | Type | Justification |
|---|---|---|
| `react-router-dom` (^7) | dep | real routing for syllabus/questions pages |
| `@tanstack/react-query` (^5) | dep | server-state cache/refetch/mutations across all future phases |
| `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom` | dev | F5 test harness (G3 frontend metrics) |

Removals: none. No CSS framework changes, no component libraries, no axios (fetch wrapper suffices), no form libraries (small controlled forms only).

## 11. Testing & Verification Gates (G3 + G4 discipline)

### 11.1 Required test matrix (binding; every row must exist and pass)

| Test class | Layer | Honest-judge pair (G4) |
|---|---|---|
| `QuestionServiceTest` (mockk) | unit | valid MCQ create → persisted; **deliberate-wrong**: NAT with `min > max` → rejected; MSQ 0 correct → rejected |
| `TopicServiceTest` (mockk) | unit | mastery update with fresh version → 200; **deliberate-wrong**: stale version → 409 path |
| `SyllabusApiIT` (Testcontainers, `@Tag("integration")`) | integration | subjects list returns the 10 seeded rows; **deliberate-wrong**: unknown subject id → 404 ProblemDetail |
| `QuestionApiIT` | integration | CRUD round-trip (create→read→update→status patch→verify); validation 400 with fieldErrors for each V-Q rule; pagination totals correct |
| `QuestionSearchIT` | integration | FTS: search `"pipeline cache"` returns the seeded COA question, ranked; **deliberate-wrong**: search `"quantum entanglement"` returns 0 rows; blank `q` returns unfiltered page |
| `MigrationIntegrityIT` | integration | V1–V4 apply cleanly on a fresh container; **deliberate-wrong**: raw insert with invalid `attempt_answers.state` violates `chk_attempt_answer_state` |
| `SeedRunnerIT` | integration | import 25-sample pack: `inserted=25, rejected=0`; re-run: `updated=25, inserted=0`; corrupt fixture: 3 valid + 2 invalid → `rejected=2` with reasons |
| `MathText.test.tsx` | frontend (Vitest) | `$x^2+1$` renders KaTeX markup (`.katex` node present); **deliberate-wrong**: malformed `$\frac{$` renders fallback chip, does not crash, body text does not contain raw `frac` |
| `QuestionsPage.test.tsx` | frontend | mocked page renders rows + pagination; filter interaction issues request with correct query params; empty state on 0 rows |
| `QuestionDetail.test.tsx` | frontend | answer key hidden by default; after "Reveal" click, correct options visible; status PATCH called with correct payload |
| `apiClient.test.ts` | frontend | 400 ProblemDetail normalizes to `ApiError` with fieldErrors; network error normalizes; abort respected |

Minimum quantitative gate: **backend ≥ 6 test files / ≥ 25 cases; frontend ≥ 5 test files / ≥ 20 cases; zero failures.**

### 11.2 Receipt format (mandatory in every report; G3)

```text
backend : files=N test-cases=N assertions=N failures=N   (./mvnw clean test)
frontend: files=N test-cases=N assertions=n/r failures=N (npx vitest run)  [Vitest reports cases; use n/r for assertions unless typed counting is added]
```

Include toolchain lines (Java vendor/version, Maven 3.9.16 wrapper-pinned, Node/npm versions) and the exact commands run. Any skipped group (e.g., integration tests on a Docker-less agent box via `-Dgroups='!integration'`) must be stated explicitly with the reason and re-run by the Operator.

### 11.3 Local verification commands (what Executor runs per WP)

```bash
# backend (integration tests need Docker)
cd backend && ./mvnw clean test
# frontend
cd frontend && npm ci && npm run build && npx vitest run
# full stack smoke (Operator / Executor with Docker)
docker compose up -d postgres && ./mvnw spring-boot:run &   # then curl /api/v1/health, /api/v1/subjects
# observability
docker compose --profile observability up -d   # then open http://localhost:3000
# seed pipeline
./mvnw spring-boot:run -Dspring-boot.run.profiles=seed
```

## 12. Execution Plan for Gemini

### 12.1 Session strategy

One long session, WP0 → WP4 strictly in order. Per WP: implement → self-verify against that WP's acceptance anchors + relevant test matrix rows → commit (§3.3 message) → push. Do not start a new WP with uncommitted or failing state. If the session must stop, follow §3.4.

### 12.2 Report requirements

- Per-WP mini-receipts inside the final `collab/gemini/PHASE1-REPORT.md` (or per-WP report files if split, per §3.4): what was built, commands run, G3 blocks, G4 pairs exercised, deviations from this spec (must be justified or avoided), open issues.
- Any deviation from a **binding** clause (§4 DDL, §5 contract, §8 schema, §10 manifest, §11 matrix) must be called out as `DEVIATION:` with reason — silent drift will be flagged in audit.
- Include a short "Operator smoke script" section reproducing §11.3 with expected outputs (the Operator will follow it verbatim, per their workflow of reading code + testing the app manually).

### 12.3 Effort estimates (planning only, not a gate)

WP0 ≈ 0.5 h · WP1 ≈ 4–5 h · WP2 ≈ 3–4 h · WP3 ≈ 1.5–2 h · WP4 ≈ 2–3 h · total ≈ **11–14 h**. This is at the upper bound of the lean-sprint ethos; it is accepted because the Operator explicitly expanded Phase 1 scope (observability + seed pipeline). Do not compensate by cutting tests.

## 13. Definition of Done & Operator Acceptance Checklist

Phase 1 is done when ALL of: every WP acceptance anchor met; §11 matrix green with receipts; both build pillars green; spec deviations ≤ justified ones; Qwen review published; Z.ai audit published; **Operator manual acceptance** below passed.

Operator checklist (browser + terminal, ~15 min):
1. `docker compose up -d postgres && ./mvnw spring-boot:run` → `/api/v1/health` returns `UP`; Flyway log shows 4 migrations applied.
2. `npm run dev` → app at `:5173`; navigate Sidebar → Syllabus: 10 subject cards render with weightages; drill into a subject; bump a topic mastery; refresh → persisted.
3. Questions page: filters + search return seeded rows (after seed import); open a question; **solution hidden**; Reveal → key + explanation visible; set Pass1 = SOLVED; refresh → persisted.
4. Run seed profile once → `/api/v1/stats/summary` counts match import receipt.
5. `docker compose --profile observability up -d` → Grafana `:3000`; Loki datasource has `app=gate-lms-backend` logs from the last minutes; click a log's `trace_id` → Tempo trace opens; the trace shows the `/api/v1/questions` span.
6. Confirm no secrets in git (`git grep -iE 'postgres_password *= *[^$ ]'` finds nothing real).

## 14. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Testcontainers unavailable on some agent boxes | `@Tag("integration")` split; Docker-less runs use `-Dgroups='!integration'` and MUST be declared; Operator re-runs full suite |
| Native-query null-param typing on PG (`ERROR: could not determine data type`) | `cast(:param as text/int)` guards mandated in §6; covered by `QuestionApiIT` filter rows |
| Generated tsvector column rejected (non-immutable expression) | explicit `'english'` config (§4.2); `MigrationIntegrityIT` proves on clean DB |
| React 19 + router/query major-version friction | pinned majors in §10; upgrade nothing mid-phase |
| KaTeX font loading regressions | already proven locally in PHASE0-FIXUP; `MathText` tests assert `.katex` presence |
| GLT image version drift | `<current>` tags in §9 resolved by Gemini to concrete minor versions at implementation time and pinned; report must list chosen tags |
| Log volume / noise | JSON logs with level filtering; sampling 1.0 is dev-only (documented in runbook) |
| Seed data quality (synthetic) | sample pack constraints in §8; sourcing real 2,500 PYQs is explicitly out of scope |

## 15. Open Questions for Operator (non-blocking; defaults chosen, confirm to override)

- **OQ-1 — Metrics backend:** spec delivers GLT (traces + logs) + actuator metrics endpoints, no Prometheus. Default: defer Prometheus until Phase 2 when there is traffic worth graphing. Confirm or request now.
- **OQ-2 — Question authoring UI:** backend CRUD + status controls only in Phase 1 (no create-question form in the UI). Default: form lands in Phase 2 polish. Confirm.
- **OQ-3 — Bulk seed data in git:** default = keep the sample pack tracked; the real 2,500-question JSONL (likely > 2 MB) stays untracked and is loaded from a local path. Confirm.
- **OQ-4 — Grafana auth:** anonymous Admin for localhost convenience (loopback-bound). Confirm or lock down with a local admin password via `.env`.

*End of `PHASE1-SPEC.md` — Z.ai, 2026-10-07. Executor: proceed per §12 upon Operator directive.*

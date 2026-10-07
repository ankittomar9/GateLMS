# [PHASE5-PERFORMANCE-SPEC] — Scale Hardening: Index Receipts, Query Audits, Route-Split Bundles

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE5-PERFORMANCE` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase5/auth-scoping` (SPEC 1) — indexes assume V14 per-user columns exist |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Bank scale ≥ 2,500 (Phase 2) + PYP (Phase 3) + per-user scoping (V14); every perf claim must carry a **measured receipt** (G3 doctrine applied to performance) |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase5/performance` · Migration **V15** (indexes only) · Effort 8–11 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Make "fast" a **measured property, not a feeling**: index the grown bank with EXPLAIN ANALYZE before/after receipts at ≥2,500 seeded rows, eliminate N+1 and unbounded reads, bound every list endpoint, gzip responses, and split the frontend into route-level lazy bundles with size receipts. No claim lands in the report without a number beside it.

## 2. Backend (binding)

### 2.1 Migration V15 (binding index DDL — Flyway transactional CREATE INDEX is acceptable at this scale; documented)

```sql
CREATE INDEX idx_questions_assembly ON questions(subject_id, type, curation_status);
CREATE INDEX idx_q_progress_user    ON question_progress(user_id, pass1_status);
CREATE INDEX idx_attempts_user_time ON mock_attempts(user_id, submitted_at DESC);
CREATE INDEX idx_mistakes_user_due  ON mistake_logs(user_id, resolved, next_review_at);
CREATE INDEX idx_notes_user_topic   ON notes(user_id, topic_id);
CREATE INDEX idx_rollups_user_date  ON daily_rollups(user_id, activity_date DESC);
CREATE INDEX idx_questions_pyp_year ON questions(year) WHERE source = 'GATE_PYP';
CREATE INDEX idx_questions_active   ON questions(pass1_status) WHERE pass1_status <> 'SOLVED';
```

(The executor may drop/adjust an index only with an EXPLAIN receipt showing it unused — recorded as DEV-N. `idx_questions_active`/`idx_questions_pyp_year` on questions are partial-index patterns over columns that remain global; V14 note: if `pass1_status` columns moved to `question_progress`, these two become progress-table partials accordingly — the receipt, not the name, is binding.)

### 2.2 Query receipts (binding — Testcontainers, ≥ 2,500-row seeded fixture via the existing bulk JSONL, quoted verbatim)

| Query | Receipt requirement |
|---|---|
| FTS search `q` + subject/type/curation filters, page 0/20 | EXPLAIN ANALYZE before vs after — index/plan change + timing, both quoted |
| Question list filtered by subject + type + curation (assembly path) | same |
| Mock attempt history for a user (attempts + result aggregates) | same |
| Mistake due queue (user + resolved + next_review_at) | same |
| Readiness computation (all subjects, mastery + accuracy + coverage) | total SQL wall-time on the fixture, before/after; if > 500 ms, a materialization proposal is recorded as an advisory (NOT implemented silently — aggregation tables change G3 accounting) |

`EXPLAIN (ANALYZE, BUFFERS)` output pasted into the report per query, with a one-line interpretation each. **Numbers are machine-generated, not typed from memory.**

### 2.3 Bounded-reads + N+1 audit (binding)

- **Pageable sweep**: every list-shaped endpoint is paged or hard-limited — questions (✓ existing), mistakes (✓ V7), notes (✓ V10), notifications (✓ V13), plans list (✓ V12), attempts history, curation stats (bounded by definition), engagement achievements (bounded by code table). Sweep receipt = a table of endpoint × limit × proof. Any unbounded read found → fixed in-branch (its test asserts the limit).
- **N+1 audit**: enable Hibernate `generate_statistics` in a test profile; the integration suite asserts **zero** queries beyond an expected count per endpoint call (repository-level `@Fetch(JOIN)`/`@EntityGraph` or batch-size fixes where found; each fix = a test that fails before it).
- **HikariCP binding**: `maximum-pool-size: 10`, `connection-timeout: 5s` — with a load receipt (§2.4) proving no starvation at target concurrency.
- **HTTP**: nginx `gzip on` for text/JSON/JS/CSS (`gzip_types` list, `gzip_min_length 1024`) — receipt = before/after `Content-Length` for a paged questions response and the main JS bundle over the proxy.
- **Query timeout**: `spring.jpa.properties.jakarta.persistence.query.timeout=10000` — a runaway query fails fast, never hangs the pool (documented in runbook).

### 2.4 Load smoke (binding, scripted: `scripts/load_smoke.sh`)

- k6 (preferred) or a `wrk`/`ab` fallback — a script committed to the repo, 60 s at 20 virtual users against `GET /api/v1/questions?subjectId=…&page=0` + `GET /api/v1/stats/readiness` (authenticated: the script logs in first and reuses the session cookie), run against the **local dockerized stack** on the executor machine.
- **Gates**: p95 < 300 ms per endpoint on the fixture bank; 0 5xx; error ratio < 0.1%. Receipt (p50/p95/p99, RPS, error rate) quoted verbatim. A miss is a finding with a follow-up plan — never silently relaxed.

## 3. Frontend (binding)

- **Route-level code splitting**: `React.lazy` + `Suspense` for every route component (dashboard, syllabus, questions, question detail, practice runner, mistakes, mocks, result, curation, author, insights, notes, revision, pyp, planner, notifications, login) — route skeleton fallback token-styled.
- **Vendor chunking**: Vite `build.rollupOptions.output.manualChunks` — `react-vendor` (react, react-dom, router), `query-vendor` (@tanstack), `charts-vendor` (recharts), `katex-vendor` — so a chart-page visit doesn't re-download KaTeX, etc.
- **Receipts (report-verbatim)**: `npm run build` per-chunk size table before vs after (single-bundle baseline measured on master first, quoted), gzip sizes, initial-JS estimate for `/` (chunk graph, not hand-waving), and **Lighthouse Performance ≥ 85** on `/` (executor-run against the local dockerized frontend; score + metrics table quoted; LCP/CLS/TBT visible).
- Vitest suite unchanged-green; SW (Phase 4) picks up hashed chunks by strategy — no PWA regression (a cached-asset smoke test re-run).
- **Diff gate**: no behavior changes — `frontend/src` diff limited to router/lazy wrappers, manualChunks, and skeleton components; zero changes to page components' internals.

## 4. Verification & Test Gates

- **Backend delta** ≥ +2 files / +6 cases (target ≥ 46 / ≥ 156): `PaginationBoundsIT` ≥ 3 (every swept endpoint asserts its limit — includes one previously-unbounded endpoint if found, else the three largest), `NPlusOneIT` ≥ 3 (statistics-based query-count assertions on the 3 hottest read paths; one deliberate canary: removing a `@EntityGraph` makes the test fail — the honest-judge proof).
- **Frontend delta** ≥ +1 file / +4 cases (target ≥ 36 / ≥ 187): `lazyRoutes.test` ≥ 2 (every route element is lazy-wrapped — a source-level structural assertion, honest and cheap), `chunkConfig.test` ≥ 2 (manualChunks maps the four groups; no page component is imported eagerly in router).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.
- **Receipts (the real gate)**: §2.2 EXPLAIN table, §2.3 sweep table + gzip before/after, §2.4 load numbers, §3 bundle table + Lighthouse — all verbatim, all machine-generated.

## 5. Report & Acceptance

Report `collab/gemini/PHASE5-PERFORMANCE-REPORT.md`: WP register; G3 per-file receipts; the four receipt tables; every dropped/adjusted index with its justification; N+1 canary proof; honest-judge register (DEV-N). Operator acceptance: run `scripts/load_smoke.sh` against the running stack and reproduce the p95 numbers (±25%); open the app cold and feel the split loads (devtools network: route chunks load on navigation); re-run Lighthouse locally and compare; verify gzip via `curl -H "Accept-Encoding: gzip" -sI` on a paged questions response.

**Out of scope**: horizontal scaling/replicas, CDN, Redis caching layer, materialized aggregate tables (advisory only — G3 accounting change requires a SPEC), frontend virtualization of large lists (pagination bounds it), k6 scenarios beyond the two smoke endpoints.

**Work packages**: WP1 V15 + EXPLAIN receipts (2.5 h) · WP2 bounded-reads sweep + N+1 audit (2.5 h) · WP3 gzip + Hikari + query timeout + load script + run (1.5–2 h) · WP4 frontend lazy/chunks + receipts (2 h) · WP5 Lighthouse + tests + report (1–1.5 h).

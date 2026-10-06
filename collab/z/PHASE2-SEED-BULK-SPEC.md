# [PHASE2-SEED-BULK-SPEC] — Bulk Question Ingestion (≥2,500 SO-Derived GATE Questions) + Curation Queue

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-SEED-BULK` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase2/design-system-toggle` (SPEC 0) — cut from its tip |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directive: "afterwards seed ~2,500 Stack Overflow GATE questions tagged by topic and evaluate in the UI" |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase2/seed-bulk` · Migration **V6** · Effort 8–11 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Grow the question bank from 26 to **≥ 2,500 GATE CSE practice questions derived from Stack Overflow content, tagged by subject and topic**, importable idempotently, and **evaluable in the UI** via a curation queue (accept/reject/annotate). The Operator personally curates quality — the pipeline must make that evaluation fast.

## 2. Sourcing Architecture (binding decision framework)

Two-track pipeline; both script tracks are committed to the repo:

**Track A — Fetch (deterministic): `scripts/so_fetch.py`**
- Queries Stack Exchange API 2.2 (`https://api.stackexchange.com/2.2/questions`) with `order=desc&sort=votes`, paginated, per subject tag map (binding):
  `algorithms→[algorithm, complexity-theory]`, `data-structures→[data-structures, sorting]`, `dbms→[database, sql, dbms]`, `operating-systems→[operating-system, process, concurrency, deadlock]`, `computer-networks→[networking, tcp, computer-networks]`, `theory-of-computation→[automata, turing-machine]`, `compilers→[compiler-construction, parsing]`, `digital-logic→[boolean-algebra, logic-gates]`, `discrete-mathematics→[discrete-mathematics, graph-theory, combinatorics]`, `aptitude→[]` (skip — no SO source; authored instead, see below).
- Optional key via env `SO_API_KEY` (anonymous quota ≈300/day is insufficient; the script must respect `backoff` on HTTP 400 and log quota). Raw JSON cached to `seed-data/so-raw/<subject>.json` — **gitignored** (add `seed-data/so-raw/` to `.gitignore`).
- Aptitude quota (≥300 questions) is **authored directly** by the executor in GATE General Aptitude style (verbal + quantitative), externalId prefix `APT-`.

**Track B — Transform (LLM-assisted, executor-driven):**
- The executor transforms cached SO posts into GATE-format questions. The deterministic parts are scripted in `scripts/so_transform.py` (HTML→text stripping, tag→subject mapping, dedup by SO post id, JSONL emission, stats). The **editorial parts** (rewriting the stem into a crisp GATE question, writing 4 plausible options with exactly one correct answer, composing the explanation, choosing marks/difficulty/type) are authored by the executor in batches, then validated by the script.
- Every derived question records the SO link in the existing `questions.gate_overflow_url` column (it is the general "source link" column) and `source='SO_DERIVED'`.
- **Deterministic contract regardless of source**: final artifact = `seed-data/so-gate-bank.jsonl` in the existing `SeedQuestionJson` schema (externalId, year, type, marks, subjectSlug, topicSlug, statement, optionsJson, correctOptions, numericMin/Max, explanation, gateOverflowUrl) + `source`/`difficulty` passthrough (runner extended, §3).

If the SO API is unavailable/rate-capped at execution time, the executor may author more questions directly (prefix `GEN-`), **recorded as DEV-N**. The ≥2,500 total and all distribution gates remain binding.

## 3. Backend (V6 + API)

### 3.1 Migration V6 (binding DDL)

```sql
ALTER TABLE questions
    ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'SAMPLE'
        CHECK (source IN ('SAMPLE','SO_DERIVED','AUTHORED','GATE_PYP','APT')),
    ADD COLUMN curation_status VARCHAR(20) NOT NULL DEFAULT 'UNREVIEWED'
        CHECK (curation_status IN ('UNREVIEWED','ACCEPTED','REJECTED')),
    ADD COLUMN curation_note TEXT,
    ADD COLUMN difficulty VARCHAR(10) CHECK (difficulty IN ('EASY','MEDIUM','HARD'));
CREATE INDEX idx_questions_curation ON questions(curation_status);
CREATE INDEX idx_questions_source ON questions(source);
```

Entities/DTOs extended accordingly (`source`, `curationStatus`, `curationNote`, `difficulty`); seed runner passes `source`/`difficulty` through and defaults new imports to `curation_status='UNREVIEWED'`. Seed runner must accept an explicit **path argument** (env `app.seed.path` already exists — keep it, add `--spring.run.arguments` guidance in runbook) and stream-process 2,500+ lines without loading all into memory at once (line-by-line parse — current implementation already does; verify and keep).

### 3.2 New/Extended Endpoints (binding)

| Endpoint | Contract |
|---|---|
| `PATCH /api/v1/questions/{id}/curation` | body `{status: ACCEPTED\|REJECTED\|UNREVIEWED, note?: string}`; 200 returns updated DTO; invalid status → 400 ProblemDetail `fieldErrors.curationStatus`; curation of nonexistent id → 404 |
| `GET /api/v1/questions` | extend filters: `curationStatus`, `source`, `difficulty` (optional, null = unfiltered) — same native query pattern |
| `GET /api/v1/stats/curation` | counts by curation_status overall **and per subject**; returns `{totalQuestions, byStatus: {…}, bySubject: [{subjectId, name, unreviewed, accepted, rejected}]}` |
| `GET /api/v1/stats/distribution` | counts by subject × type × difficulty — the §6 distribution receipt is served from this endpoint (Operator sees the table live) |

## 4. Frontend — Curation Queue (`/curation`)

- New route + sidebar entry "Curation" (icon `ClipboardCheck`) in all designs (token-styled, SPEC 0 layer).
- **CurationQueuePage**: filter bar (subject, curation status, source, difficulty, FTS q), paginated question cards showing statement (MathText), type badge, subject/topic, difficulty chip, source link (external, `target=_blank rel=noopener`), and current curation state.
- Per card: **Accept** / **Reject** buttons + optional note input (expandable). Accept/Reject optimistically updates via TanStack mutation + rollback on error; keyboard shortcuts `A` (accept), `R` (reject), `N` (focus note).
- Bulk mode: select N visible → apply Accept/Reject with one note (calls the endpoint per question in sequence, progress indicator).
- Dashboard tile (all designs): curation progress (accepted/total) linking to `/curation`.
- QuestionDetailPage: show source badge + link + curation state; QuestionsPage filter chips gain curation-status quick filter.

## 5. Distribution & Quality Gates (binding, verified from real data)

| Gate | Requirement |
|---|---|
| Total | ≥ 2,500 questions imported (receipt `inserted ≥ 2500` on first import; re-import 100% idempotent: 0 inserted / all updated / 0 rejected) |
| Per subject | every subject ≥ 150; algorithms, dbms, operating-systems, data-structures ≥ 250 each |
| Type mix | MCQ 50–65%, MSQ 15–25%, NAT 20–30% |
| Explanations | ≥ 70% of new questions have non-empty explanation |
| Display math | ≥ 100 new questions contain `$$…$$` |
| Dedup | 0 duplicate `externalId` in the JSONL (script-verified) |
| Validation | 0 schema/V-Q violations (script + runner rejection counters = 0) |
| FTS sanity | spot check: 5 sampled search terms each return > 0 results (manual smoke, recorded in report) |

## 6. Verification & Test Gates

- **Backend delta**: ≥ +4 files / +13 cases (target ≥ 13 files / ≥ 40 total): `CurationApiIT` ≥ 4 (accept/reject/invalid/404 — honest-judge pair), `QuestionFilterIT` ≥ 3 (curation/source/difficulty filters compose with existing filters), `StatsCurationIT` ≥ 3 (counts correct after known mutations), `SeedBulkIT` ≥ 3 (import ≥100-fixture bulk pack idempotently; corrupt line rejected with reason; UNREVIEWED default). Full `./mvnw test` green incl. all prior ITs.
- **Frontend delta**: ≥ +2 files / +10 cases (target ≥ 12 files / ≥ 55 total): `CurationQueuePage.test` ≥ 6 (renders filters + cards; accept mutation fires PATCH; reject with note; error rollback; keyboard shortcuts; bulk mode), `distributionStats` API-layer test ≥ 4.
- **Script receipts**: `python3 scripts/so_fetch.py --help`, `so_transform.py --stats` output (counts by subject/type/difficulty, dedup report) committed into the report verbatim.
- **Import receipt**: full runner 4-number receipt for the 2,500+ import.
- `npm run build` zero errors; hardcoded-color grep gate (SPEC 0 §9.3) stays green.

## 7. Report & Acceptance

Report `collab/gemini/PHASE2-SEED-BULK-REPORT.md`: WP register; the §5 distribution table with **real numbers**; G3 receipts (per-file test counts, both pillars); script receipts; import receipt; honest-judge register (DEV-N continuing); sourcing decision narrative (how much SO-fetched vs authored, with reason). Operator acceptance: run the app, open `/curation`, curate ~20 questions, check `/api/v1/stats/distribution` against the report.

**Out of scope**: changing V-Q validation rules, modifying existing sample pack content, auth/multi-user, deleting/downgrading existing 26 sample questions.

**Work packages**: WP1 V6 + entities + endpoints (2.5 h) · WP2 fetch/transform tooling (2 h) · WP3 bank generation + import + receipts (2.5–3.5 h) · WP4 curation UI (2.5 h) · WP5 tests + report (1.5 h).

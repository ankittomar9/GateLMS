# [PHASE2-MOCK-TESTS-SPEC] — Mock Test Engine: Blueprint Assembly, Timed TCS iON Runner, GATE Scoring & Analytics

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-MOCK-TESTS` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase2/practice-workflows` (SPEC 2) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator roadmap (Mock Tests nav item); GATE CSE official marking scheme; V1 schema (mock_exams.blueprint_json, mock_attempts pos/neg scores, attempt_answers 5-state) |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase2/mock-tests` · Migration **V8** · Effort 11–15 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Full-length **timed mock tests** assembled from the question bank by subject-weightage blueprint, run in a TCS iON-style interface (5-state question palette, section navigation, countdown), graded server-side with **GATE-accurate negative marking**, and reviewed with per-subject/topic analytics and score history. This is the centerpiece "exam simulation" loop.

## 2. Architecture (binding)

### 2.1 Migration V8 (binding DDL)

```sql
ALTER TABLE mock_attempts
    ADD COLUMN question_ids_json JSONB,
    ADD COLUMN duration_seconds INT,
    ADD COLUMN finished_reason VARCHAR(20)
        CHECK (finished_reason IN ('SUBMITTED','AUTO_SUBMITTED','ABANDONED'));

CREATE INDEX idx_mock_attempts_submitted ON mock_attempts(submitted_at);
```

`question_ids_json` = the **per-attempt frozen question list** (assembly snapshot — later bank edits don't alter a running attempt). `mock_exams` keeps its role: template metadata + `blueprint_json` (assembly rule) + `duration_minutes` + `total_marks` (existing columns).

### 2.2 Blueprint Contract (binding `blueprint_json`)

```json
{
  "title": "GATE CSE Full Mock #1",
  "sections": [
    {"subjectId": "subj_aptitude", "mcq": 5, "msq": 2, "nat": 3, "marks": 1},
    {"subjectId": "subj_algo", "mcq": 6, "msq": 2, "nat": 2, "marks": 2}
  ],
  "questionPool": {"curationStatus": ["ACCEPTED"], "difficulty": ["EASY", "MEDIUM", "HARD"]},
  "shuffle": true
}
```

Assembly (`MockAssemblyService`): for each section, query candidates by subject + type (+ curation/difficulty pool filters), order randomly in SQL (`ORDER BY random() LIMIT n`), take exactly `mcq+msq+nat` per counts; if the pool is short, the section is filled with what exists and the response carries `shortfallWarnings` (never silently pass). Assembly happens once per attempt and is stored in `question_ids_json`.

### 2.3 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `POST /api/v1/mocks` | create template (body: title, durationMinutes, blueprint per §2.2). Validates sections reference real subjectIds; 201 |
| `GET /api/v1/mocks` | list templates with computed question totals (from blueprint, or actual on last assembly) |
| `POST /api/v1/mocks/{id}/attempts` | assembles + freezes `question_ids_json`, sets duration_seconds = durationMinutes·60, creates attempt IN_PROGRESS; returns `{attemptId, durationSeconds, sections: [{subjectId, name, questionIds}], questions: [safe DTOs — no answers, same subset as SPEC 2], startedAt, serverTime}` |
| `PATCH /api/v1/attempts/{id}/answers/{questionId}` | upsert answer `{selectedOptions?, numericAnswer?, state}` — state transitions honor TCS iON 5-state (NOT_VISITED, NOT_ANSWERED, ANSWERED, MARKED_FOR_REVIEW, ANSWERED_AND_MARKED_FOR_REVIEW); persists `time_spent_seconds` deltas; **no grading yet**; 200 `{saved: true, state}` |
| `POST /api/v1/attempts/{id}/submit` | grades ALL answers (§2.4), sets positive_score/negative_score/total_score, status SUBMITTED, finished_reason SUBMITTED; returns result summary (score trio + counts). **Server-side timer enforcement**: if now > started_at + duration + 60s grace, finished_reason = AUTO_SUBMITTED and unanswered questions grade as 0 — late manual submissions cannot game the clock |
| `GET /api/v1/attempts/{id}/result` | full review: per-question `{questionId, yourAnswer, correctAnswer, isCorrect, marksAwarded, state, timeSpentSeconds, explanation, statement, optionsJson, type, marks}` + per-subject/topic aggregates + section score table |
| `GET /api/v1/mocks/{id}/attempts` | attempt history for a template (scores, dates) |
| `GET /api/v1/stats/mock-trend` | score trend across attempts: `[{attemptId, date, totalScore, maxScore, percentileLike: null}]` — feeds the trend chart |

In-flight attempts (IN_PROGRESS) older than duration + 1 h are auto-finalized as ABANDONED (scheduled cleanup, documented in runbook — a `@Scheduled` job every 10 min).

### 2.4 GATE Scoring Rules (binding — exhaustively tested)

| Case | Marks |
|---|---|
| Unattempted / NOT_VISITED / NOT_ANSWERED / MARKED_FOR_REVIEW without answer | 0 |
| MCQ correct (exact single-option match) | +marks |
| MCQ wrong (answered, incorrect) | **−marks/3** (1-mark → −0.33, 2-mark → −0.67; NUMERIC(5,2), round half-up) |
| MSQ exact set match | +marks |
| MSQ any mismatch (missing, extra, or partial) | 0 (no partial credit, no negative) |
| NAT within [numericMin, numericMax] inclusive | +marks |
| NAT out of range / missing | 0 (no negative) |
| **Aggregate floor** | total_score = max(0, positive_score − negative_score) — negative_score itself records the raw deduction |

`positive_score`/`negative_score` store raw sums; `total_score` applies the floor. Per-question `marks_awarded` is the signed value (may be −0.33/−0.67).

## 3. Frontend

- **MockTestsPage `/mocks`** (sidebar "Mock Tests" activates): template cards (title, duration, question count, blueprint summary chips, best score), "New template" builder form (sections table: subject select + mcq/msq/nat counts + marks; live total-marks/question computation; pool filters), "Start attempt" CTA, attempt history per template (score trend sparkline, recharts).
- **TestRunnerPage `/mocks/attempts/{attemptId}`**: full-screen exam shell — header (title, **server-synced countdown** via serverTime offset, submit button with confirm), left question navigator grid (numbers colored by 5-state: gray/green/red/amber/purple per TCS iON), main question card (statement MathText, options or NAT input, save-and-next, mark-for-review toggle), section tabs, answer persistence via debounced PATCH (1.5 s) + on navigation; tab-switch counter (visibilitychange, client-side, advisory); auto-submit on countdown zero (calls submit with finished_reason AUTO_SUBMITTED); connection-lost banner with retry.
- **ResultPage `/mocks/attempts/{attemptId}/result`**: score hero (total / positive / negative / max), per-subject table + recharts radar (accuracy by subject) and bar (time by section), question review list (filter: wrong only / unattempted only; your answer vs correct with diff highlighting; explanation; marks awarded signed), "Practice weak topics" CTA (deep-links to `/practice` preconfigured with the weak subject).
- Trend chart on MockTestsPage (line: totalScore% over attempts, recharts).

## 4. Verification & Test Gates

- **Backend delta** ≥ +4 files / +15 cases (target ≥ 20 / ≥ 70): `ScoringRulesTest` ≥ 8 (honest-judge core: every §2.4 row incl. −0.33/−0.67 rounding, MSQ partial=0, NAT inclusive bounds, floor at aggregate, signed per-question marks), `MockAssemblyIT` ≥ 4 (exact counts per section; shortfall warnings when pool short; frozen snapshot — editing the bank after assembly doesn't change attempt; shuffle respects pool filters), `AttemptFlowIT` ≥ 5 (answer upsert + state transitions incl. invalid transition 400; submit grades all; overtime → AUTO_SUBMITTED with unanswered=0; result payload complete), `MockTrendIT` ≥ 2 (trend ordering; empty history).
- **Frontend delta** ≥ +3 files / +15 cases (target ≥ 18 / ≥ 90): `TestRunnerPage.test` ≥ 7 (navigator palette states render; save-and-next PATCHes; mark-for-review transition; countdown auto-submit; options disabled post-submit; serverTime offset math; a11y roles), `MockTestsPage.test` ≥ 4 (template cards; builder computes totals; start creates attempt; trend chart renders with data), `ResultPage.test` ≥ 4 (score hero math from fixtures; wrong-only filter; signed marks display; radar/bar render).
- Full `./mvnw test`, `npm run build`, vitest green; SPEC 0 grep gates green.

## 5. Report & Acceptance

Report `collab/gemini/PHASE2-MOCK-TESTS-REPORT.md`: WP register; G3 per-file receipts; **scoring-rules → test-name traceability table** (every §2.4 row mapped to its test); assembly snapshot proof; timer-enforcement proof; honest-judge register (DEV-N). Operator acceptance: create a 20-question template, run it fully (answer mix: right, wrong, mark-for-review, skip), submit, verify scores against §2.4 by hand, review result page, check trend chart after a second attempt.

**Out of scope**: percentile/All-India rank simulation, section-wise cutoff enforcement, adaptive testing, resume-after-browser-crash (attempt continues server-side but the runner reloads state — acceptable v1), multi-user.

**Work packages**: WP1 V8 + assembly service (2.5 h) · WP2 attempt/answer/submit APIs + scoring (3 h) · WP3 result/analytics/trend APIs + abandonment job (2 h) · WP4 mock tests + builder frontend (2.5 h) · WP5 runner UI (3 h) · WP6 result UI + charts (2 h) · WP7 tests + report (1.5 h).

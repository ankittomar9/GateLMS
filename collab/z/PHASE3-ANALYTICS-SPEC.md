# [PHASE3-ANALYTICS-SPEC] — Progress Analytics: Readiness Score, Mastery Heatmap, Trends, GATE Countdown

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-ANALYTICS` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | P2-EXIT (merge of `phase2/observability-ext`) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator roadmap (dashboard numbers must become real); G3 anti-fabrication doctrine; V1 topics.mastery_level, mock_attempts scores; PHASE2-SPEC-3 mock-trend endpoint |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase3/analytics` · Migration **V9** · Effort 8–11 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Turn the dashboard's decorative numbers into **honest, data-derived study intelligence**: a subject-weighted **readiness score** with a fully specified formula, a subject×topic **mastery heatmap**, accuracy/volume/time **trends** fed by a daily rollup table, and a **GATE 2028 countdown**. Every displayed number must be traceable to real rows (G3: aggregates are computed from data at request time or from the rollup maintained by the same write path — never seeded, never invented).

## 2. Architecture (binding)

### 2.1 Migration V9 (binding DDL)

```sql
CREATE TABLE daily_rollups (
    activity_date DATE PRIMARY KEY,
    questions_answered INT NOT NULL DEFAULT 0,
    correct_answers INT NOT NULL DEFAULT 0,
    minutes_practiced INT NOT NULL DEFAULT 0,
    minutes_mocked INT NOT NULL DEFAULT 0,
    mock_attempts INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

`RollupService.recordAnswer(activityDate, correct, seconds, mode)` (idempotent upsert by date, `ON CONFLICT (activity_date) DO UPDATE SET ... = ... + excluded...`) is called from the **existing** write paths: the practice answer-grading service (per answer) and the mock submit grading service (bulk for all graded answers — count only states ANSWERED/ANSWERED_AND_MARKED, and `mock_attempts` +1 / `minutes_mocked` once per submit). No new write path duplicates grading. Rollups record only **completed grading events** — no projections, no estimates.

### 2.2 Readiness Score (binding formula — exhaustively tested)

For each subject `s` with syllabus weightage `w_s` (= `subjects.avg_weightage`):

```
R_s = 0.5·M_s + 0.3·A_s + 0.2·C_s            (each component ∈ [0,1]; subject with zero activity → R_s = 0)
M_s = avg(mastery_level/5) over topics of s with last_revised_at IS NOT NULL
A_s = correct / answered on questions of s (attempt_answers with is_correct NOT NULL)
C_s = distinct topics of s with ≥1 answered question / total topics of s
READINESS = round(100 · Σ(w_s·R_s) / Σ(w_s))   over ALL 10 subjects (inactive subjects count as 0 — honest)
```

Accuracy uses **latest distinct-question verdict** per question (the most recent attempt_answer with is_correct NOT NULL), not a raw sum over repeats. The endpoint returns the score **plus every component** (`perSubject: [{subjectId, name, weight, m, a, c, readiness}]`) so the UI and the audit can recompute it independently.

### 2.3 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `GET /api/v1/stats/readiness` | `{readiness, perSubject: [...], computedAt}` per §2.2; empty bank/activity → readiness 0 + all components 0 (never fabricated) |
| `GET /api/v1/stats/heatmap` | `{subjects: [{subjectId, name, topics: [{topicId, name, mastery, due: boolean, answered, correct}]}]}` — mastery grid source |
| `GET /api/v1/stats/trends?days=90` | `{days: [{date, questionsAnswered, correct, minutesPracticed, minutesMocked}], rolling7: {accuracy, volume}}` from `daily_rollups`; `days` clamped 7–365, default 90; missing dates are zero-filled (honest: no activity = 0) |
| `GET /api/v1/exam/countdown` | `{examDate, daysRemaining, weeksRemaining, source: "config"}` — `app.exam.date` (default `2028-02-12`, Operator-adjustable; GATE 2028 official date TBA — this is configuration, not a claim) |

### 2.4 Prometheus (extends PHASE2-OBS-EXT, binding)

- `gatelms_readiness_score` (Gauge, refreshed 60 s alongside `gatelms_questions_loaded`) — readiness on the GateLMS Domain dashboard as a stat panel.

## 3. Frontend — InsightsPage `/insights` + dashboard cards

- Sidebar entry "Insights" (icon `Activity`), route `/insights`, token-styled (SPEC 0 layer), all designs.
- **Layout**: score hero (readiness number + component chips M/A/C + "how is this computed?" popover quoting §2.2 verbatim), mastery heatmap (CSS grid: rows = subjects, cells = topics, background = `--ds-*`-derived 6-step mastery scale 0–5, due-for-revision cells ringed, cell tooltip = answered/correct), subject radar (recharts `RadarChart` — readiness per subject), 30/90-day trend lines (accuracy % + volume, recharts `LineChart`), minutes stacked bars (practice vs mock), GATE countdown card (days + weeks + exam date from endpoint).
- **Dashboard integration** (all designs, token-styled): readiness ring + countdown card on the dashboard hero area; "Due this week" count chip linking to `/mistakes`. Numbers come from the new endpoints — the existing decorative placeholders are **replaced, not decorated** (no static 0s where an endpoint exists).
- Empty states: honest "no activity yet — take a practice session" CTAs, never fake zeros with charts.

## 4. Verification & Test Gates

- **Backend delta** ≥ +3 files / +10 cases (target ≥ 26 / ≥ 88): `ReadinessComputationTest` ≥ 5 (honest-judge core: §2.2 hand-computable fixture — mixed mastery/accuracy/coverage; inactive subject contributes 0; clamp components; rounding; empty-bank → 0 with zero components), `RollupServiceIT` ≥ 3 (practice answer increments counts; mock submit bulk-increments incl. one attempt + minutes; upsert idempotence on same date), `TrendsApiIT` ≥ 2 (zero-fill for gap dates; days clamp 400 outside 7–365 — ProblemDetail `fieldErrors.days`).
- **Frontend delta** ≥ +2 files / +12 cases (target ≥ 21 / ≥ 108): `InsightsPage.test` ≥ 8 (score hero renders readiness + components from fixture; heatmap cell classes map mastery 0–5; due cells ringed; radar renders subjects; trend zero-fill renders; countdown math from fixed date; empty-state CTA; popover quotes formula), `readinessApi` layer test ≥ 4 (endpoint shapes; days param; error rollback; cache key).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.
- **Receipt**: a fixture-based readiness computation table in the report — inputs, hand-computed expectation, test assertion, actual output (the honest-judge paper trail).

## 5. Report & Acceptance

Report `collab/gemini/PHASE3-ANALYTICS-REPORT.md`: WP register; G3 per-file receipts both pillars; the §4 computation-receipt table; rollup write-path proof (diff hunks showing the two call sites — practice + mock submit); honest-judge register (DEV-N continuation with predecessor quote). Operator acceptance: complete one practice + one mock, open `/insights`, verify the score hero matches a hand recomputation from `/api/v1/stats/readiness` components, check heatmap due-rings against `/mistakes` queue, see the countdown.

**Out of scope**: percentile/All-India rank (no population data — explicitly never fabricated), per-user scoping (Phase 5 V14), forecasting/prediction models, export of analytics.

**Work packages**: WP1 V9 + RollupService + write-path hooks (2 h) · WP2 readiness/heatmap/trends/countdown APIs + metric (2.5 h) · WP3 InsightsPage + dashboard cards (2.5 h) · WP4 tests + report (1.5–2 h).

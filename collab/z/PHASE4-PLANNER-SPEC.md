# [PHASE4-PLANNER-SPEC] — Study Planner: Weightage-Aware Plan Generation, Calendar, Daily Targets, Auto-Progress

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-PLANNER` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | P3-EXIT (merge of `phase3/pyp-import`) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | `subjects.avg_weightage` + `priority_tier` (V1); Phase 3 readiness/heatmap; GATE 2028 countdown (config `app.exam.date`); Operator's study workflow (dashboard HeroPlanner placeholder exists in chrome) |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase4/study-planner` · Migration **V12** · Effort 9–12 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Convert "16 months to GATE" into an executable plan: **generate** week-by-week study plans weighted by syllabus weightage × current weakness, see them on a **calendar**, get a **today view** on the dashboard, and have practice/mock/mistake-review activity **auto-update plan progress** through the existing write paths. The planner computes from real state — a freshly installed system generates a valid weightage-only plan (honest default), and weakness terms activate as data accrues.

## 2. Architecture (binding)

### 2.1 Migration V12 (binding DDL)

```sql
CREATE TABLE study_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    weekly_target_questions INT NOT NULL DEFAULT 150,
    weekly_target_minutes INT NOT NULL DEFAULT 600,
    strategy VARCHAR(30) NOT NULL DEFAULT 'WEIGHTED'
        CHECK (strategy IN ('WEIGHTED', 'WEAKNESS_FIRST', 'LINEAR')),
    generated_reasoning JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT plan_date_range CHECK (end_date >= start_date)
);

CREATE TABLE plan_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES study_plans(id) ON DELETE CASCADE,
    plan_date DATE NOT NULL,
    subject_id VARCHAR(50) REFERENCES subjects(id) ON DELETE CASCADE,
    topic_id VARCHAR(100) REFERENCES topics(id) ON DELETE CASCADE,
    target_questions INT NOT NULL DEFAULT 10,
    target_minutes INT NOT NULL DEFAULT 30,
    done_questions INT NOT NULL DEFAULT 0,
    done_minutes INT NOT NULL DEFAULT 0,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'DONE', 'SKIPPED')),
    UNIQUE (plan_id, plan_date, topic_id)
);
CREATE INDEX idx_plan_items_date ON plan_items(plan_date);
```

### 2.2 Plan Generation Algorithm (binding — deterministic, testable)

`POST /api/v1/plans/generate` body `{title?, weeks (1–52), strategy: WEIGHTED|WEAKNESS_FIRST|LINEAR, weeklyTargetQuestions?, weeklyTargetMinutes?}`.

1. **Subject share** `S_s = w_s · (1 + 0.5·(1 − readiness_s))` for WEIGHTED (weightage × weakness multiplier); WEAKNESS_FIRST sorts subjects by `readiness_s` ascending and front-loads the weakest 40% of capacity; LINEAR = weightage only. If readiness has no data (fresh system), all strategies degenerate to weightage-only — recorded in `generated_reasoning` as `{mode: "weightage_only", reason: "no_activity"}` (honest).
2. **Topic share** within a subject: `T_t = 1 + topic_weakness_t` where weakness = `1 − mastery/5` (+0.5 boost if `next_revision_due` is past). Topics with zero questions in the bank are **excluded** (cannot study what you cannot practice — listed in `generated_reasoning.excludedTopics`).
3. **Distribution**: weeks → 6 study days each (1 rest day per week, Sunday default) → per day 1–3 plan items; `target_questions` per item 5–15 (clamped), scaled so the week sums to `weekly_target_questions`. Items carry `subject_id` + `topic_id` only — never fixed question lists (the plan says *what* to cover, practice assembles *which* questions).
4. `generated_reasoning` JSONB records the inputs and mode (subject shares, excluded topics, strategy) — the report's receipt and the UI's "why this plan?" popover both read it. **No randomness** (deterministic given inputs — testable).

### 2.3 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `POST /api/v1/plans/generate` | §2.2; 201 with full plan + items summary; invalid weeks → 400 `fieldErrors.weeks` |
| `GET /api/v1/plans` | list (title, range, progress %, item counts) |
| `GET /api/v1/plans/{id}` | full items grouped by date + `generated_reasoning` |
| `GET /api/v1/plans/today` | today's items + progress `{items, doneQuestions, targetQuestions, minutesDone, minutesTarget}` (404-free: empty array when no active plan — honest) |
| `PATCH /api/v1/plan-items/{id}` | `{status: DONE\|SKIPPED\|OPEN}` manual override; 404 unknown; progress snapshot in response |
| `POST /api/v1/plans/{id}/regenerate` | regenerates **from the current date forward** with fresh readiness (old future items replaced, past DONE items preserved — binding) |

**Auto-progress hook (binding, single write path)**: the existing practice-finish and mock-submit services call `PlanProgressService.onQuestionsAnswered(date, topicId, count, minutes)` — increments `done_questions`/`done_minutes` on matching OPEN items (same date, same topic; nearest OPEN item on that topic if the date has none, else no-op) and flips status DONE at ≥ target. Mistake reviews add 1 to the matching topic item. The hook is fire-and-forget-safe (failure logs, never breaks grading — tested).

### 2.4 Frontend — PlannerPage `/planner` + dashboard today card

- Sidebar entry "Planner" (icon `CalendarRange`), token-styled, all designs.
- **Generation form**: weeks, strategy (radio, one-line explainer each), weekly targets; result preview (subject distribution chips + excluded-topics note) before confirm.
- **Calendar month grid**: day cells show target/done (e.g. `12/15`), color by completion (token palette), rest days dimmed; click a day → item list with topic chips + "Drill" CTA (deep-links `/practice` preconfigured topic) + status toggle.
- **Plan detail**: progress bar, per-subject progress, "why this plan?" popover (generated_reasoning rendered), regenerate CTA with confirm.
- **Dashboard**: "Today's plan" card (all designs — replaces the decorative HeroPlanner placeholder with real items; empty-state = "Generate a plan" CTA linking `/planner`).
- SyllabusPage gains per-subject readiness column (from Phase 3) next to weightage — planner context.

## 3. Verification & Test Gates

- **Backend delta** ≥ +4 files / +12 cases (target ≥ 37 / ≥ 125): `PlanGenerationTest` ≥ 5 (honest-judge core: WEIGHTED shares on hand-computable fixture; WEAKNESS_FIRST front-loads weakest subject; fresh-system degenerates to weightage_only + reasoning recorded; empty-bank topic excluded + listed; deterministic — two calls, same output), `PlanProgressIT` ≥ 4 (practice finish increments matching item + DONE flip at target; topic-no-item no-op; hook failure does not fail grading (simulated throw, grade still 200); mistake review +1), `PlanApiIT` ≥ 3 (generate + list + today shapes; PATCH transitions + 404; regenerate preserves past DONE).
- **Frontend delta** ≥ +3 files / +14 cases (target ≥ 29 / ≥ 149): `PlannerPage.test` ≥ 7 (generation form validation + submit; preview chips; calendar day cells math; day drill deep-link; status toggle mutation; regenerate confirm; empty state), `TodayCard.test` ≥ 4 (items + progress math from fixture; DONE state; empty-state CTA; error rollback), `plannerApi.test` ≥ 3 (payload shapes; today cache key; optimistic status toggle).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.

## 4. Report & Acceptance

Report `collab/gemini/PHASE4-PLANNER-REPORT.md`: WP register; G3 per-file receipts; generation receipt (fixture → hand-computed shares → test assertion → reasoning JSON); auto-progress proof (write-path call-site diff hunks, both hooks); honest-judge register (DEV-N). Operator acceptance: generate a 4-week plan, check the preview against syllabus weightage, do one practice session on a planned topic, watch the today card tick up, mark an item done manually, regenerate and confirm past DONE items survive.

**Out of scope**: calendar integrations (iCal/Google), reminders via email/push (in-app notifications = SPEC 2), drag-and-drop rescheduling (regenerate covers v1), per-user plans (Phase 5 V14 adds `user_id`), time-of-day scheduling.

**Work packages**: WP1 V12 + entities + generation service (3 h) · WP2 APIs + auto-progress hooks (2.5 h) · WP3 planner frontend + today card (3 h) · WP4 tests + report (1.5–2 h).

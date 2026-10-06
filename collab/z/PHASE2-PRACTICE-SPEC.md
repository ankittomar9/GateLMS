# [PHASE2-PRACTICE-SPEC] — Practice Workflows: Solve Flow, Server-Side Grading, Mistake Notebook

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-PRACTICE` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase2/seed-bulk` (SPEC 1) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator roadmap: student practice loop, mistake notebook, spaced review; V1 schema pre-design (attempt_answers 5-state TCS iON, mistake_logs) |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase2/practice-workflows` · Migration **V7** · Effort 9–12 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Turn the question bank into a **usable study loop**: configure a practice session → answer questions one-by-one (TCS iON-style 5-state palette) → get server-graded verdicts → wrong answers feed the **Mistake Notebook** with categorized takeaways and a spaced-repetition review queue that updates topic mastery. Practice mode is untimed, no negative marking, reveal-on-grade.

## 2. Architecture (binding)

A practice session reuses the existing mock domain: a `mock_exams` row with `mode='PRACTICE'` and its blueprint describing the *selection rule* (not a fixed question list). `mock_attempts` + `attempt_answers` record the flow. **No answer leakage**: the question payload served during an active session must NOT contain `correctOptions`, `numericMin`, `numericMax`, or `explanation` — grading happens server-side, the verdict payload returns them only after the answer is submitted.

### 2.1 Migration V7 (binding DDL)

```sql
ALTER TABLE mock_exams
    ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'MOCK'
        CHECK (mode IN ('MOCK', 'PRACTICE', 'WEAKNESS_DRILL'));

ALTER TABLE mistake_logs
    ADD COLUMN attempt_answer_id UUID REFERENCES attempt_answers(id) ON DELETE SET NULL,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_review_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN resolved BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_mistake_logs_question ON mistake_logs(question_id);
CREATE INDEX idx_mistake_logs_next_review ON mistake_logs(next_review_at);
```

Entities + enums extended (`ExamMode`, mistake revision fields). Existing rows default correctly (mode=MOCK).

### 2.2 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `POST /api/v1/practice/sessions` | body `{subjectId?, topicId?, questionType?, marks?, curationStatus? (default ACCEPTED+UNREVIEWED), count (1–50, default 10)}` → creates PRACTICE exam + attempt, assembles questions via existing filter query, returns `{sessionId, attemptId, questions: [PracticeQuestionDto], totalQuestions}`. `PracticeQuestionDto` = safe subset (id, externalId, type, marks, statement, optionsJson, topicId, subjectId, year) — **no answers** |
| `POST /api/v1/practice/sessions/{attemptId}/answers` | body `{questionId, selectedOptions?: string, numericAnswer?: number, timeSpentSeconds: int}` → server grades (§2.3), persists `attempt_answers` (state ANSWERED), auto-updates `questions.pass1_status` (SOLVED if correct, FAILED if wrong), updates topic mastery (§2.4), returns `{correct: boolean, marksAwarded: number, correctOptions?, numericMin?, numericMax?, explanation, topicMastery}` — verdict reveals answer AFTER grade |
| `POST /api/v1/practice/sessions/{attemptId}/skip` | body `{questionId}` → persists state NOT_ANSWERED, pass1 stays PENDING |
| `POST /api/v1/practice/sessions/{attemptId}/reveal` | body `{questionId}` → returns explanation + correct answer WITHOUT grading; marks pass1 = ASSISTED, persists state NOT_ANSWERED (reveal = assisted) |
| `POST /api/v1/practice/sessions/{attemptId}/finish` | sets submitted_at + status SUBMITTED; returns summary `{correct, wrong, skipped, totalQuestions, timeSpentSeconds, byTopic: [{topicId, name, correct, total}]}` |
| `POST /api/v1/mistakes` | body `{questionId, attemptAnswerId?, errorCategory (CONCEPTUAL\|CALCULATION\|MSQ_TRAP\|MISREAD), description?, correctiveTakeaway}` → 201; invalid category → 400; next_review_at = now + 1 day |
| `GET /api/v1/mistakes` | filters `resolved`, `due=true` (next_review_at <= now), pagination |
| `PATCH /api/v1/mistakes/{id}/review` | increments review_count, schedules next interval from `[1,3,7,14,30,60]` days by review_count; body `{resolved?: boolean}` — resolving sets resolved + keeps history |
| `GET /api/v1/stats/mastery` | per-topic mastery levels + due-review counts (feeds Dashboard) |

All ProblemDetail error conventions, optimistic `@Version` handling (409 on stale), and tracing/JSON-logging follow PHASE1-SPEC §5/§6 patterns.

### 2.3 Practice Grading Rules (binding — tested exhaustively)

- **MCQ**: correct iff `selectedOptions` (single letter) equals `correctOptions` exactly. No negative marks in PRACTICE mode.
- **MSQ**: correct iff the selected set equals the correct set (set equality, order-insensitive, comma-normalized). Partial = wrong.
- **NAT**: correct iff `numericMin <= numericAnswer <= numericMax` (inclusive bounds — decimal answers allowed). Missing bounds or missing answer = wrong.
- `marksAwarded` = marks if correct else 0 (practice).

### 2.4 Mastery & Mistake Auto-Flow (binding)

- Wrong answer → `topics.mastery_level = max(0, current − 1)` and `last_revised_at = now`.
- Correct answer on a topic whose mastery was decremented by this question earlier in the same session → `mastery_level = min(5, current + 1)`.
- The verdict payload includes a `mistakeLogPrompt: boolean` (true when wrong) — the frontend uses it to offer the mistake-log form (§3), never auto-creates a mistake log without user input.

## 3. Frontend

- **PracticePage `/practice`** (sidebar "Practice Clusters" activates): config form (subject → topic cascading selects, type, count, marks, source filter), recent sessions list, "due for review" mistake count card. Start → navigates to runner.
- **PracticeRunnerPage `/practice/sessions/{attemptId}`**: question card (statement via MathText, options as clickable labels or NAT numeric input with keypad hints), per-question timer (client, advisory), submit → verdict panel inline (correct/ wrong states + explanation + correct answer + mastery delta + "Log mistake" form with 4 error-category buttons + takeaway textarea), skip / reveal buttons with confirm-on-reveal, progress dots showing 5-state palette (NOT_VISITED/NOT_ANSWERED/ANSWERED + MARKED_FOR_REVIEW + ANSWERED_AND_MARKED for review marking), finish → summary screen with per-topic table + re-drill weak topics CTA (starts a session filtered to topics with wrong answers).
- **MistakeNotebookPage `/mistakes`** (sidebar "Mistake Notebook" activates): review queue (due today first), mistake cards (question excerpt + error category chip + corrective takeaway + review_count + next review date), "Review now" flow (shows the question, re-answer, then PATCH review), resolved filter tab, empty states.
- All UI token-styled (SPEC 0), TanStack Query + mutations, a11y per existing patterns (`radiogroup`-style option groups, `aria-live` verdict announcements).

## 4. Verification & Test Gates

- **Backend delta** ≥ +5 files / +18 cases (target ≥ 16 / ≥ 55): `GradingServiceTest` ≥ 6 (honest-judge core: MCQ exact match; MSQ set-equality incl. order-insensitivity & partial=wrong; NAT inclusive bounds incl. boundary values at min/max; missing numericAnswer; practice zero negative), `PracticeSessionApiIT` ≥ 6 (session creation honors filters; **payload contains NO correctOptions/numericMin/numericMax/explanation — asserted explicitly**; answer → graded verdict; reveal marks ASSISTED without grading; skip; finish summary math), `MasteryUpdateIT` ≥ 3 (decrement floor 0; increment cap 5; correct leaves unchanged), `MistakeApiIT` ≥ 4 (create with categories; invalid category 400; due filter; review interval sequence 1→3→7).
- **Frontend delta** ≥ +3 files / +20 cases (target ≥ 15 / ≥ 75): `PracticeRunnerPage.test` ≥ 8 (option selection + submit flow; verdict panel renders explanation post-grade; mistake form posts category+takeaway; skip; reveal confirm; mastery delta chip; finish summary; **no answer data in rendered DOM before submit**), `PracticePage.test` ≥ 5 (cascading topic load; count clamp; start calls POST; due-review card; recent sessions), `MistakeNotebookPage.test` ≥ 7 (queue ordering by due; category chips; review flow PATCH; resolve; filters; empty state; error rollback).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates stay green.

## 5. Report & Acceptance

Report `collab/gemini/PHASE2-PRACTICE-REPORT.md`: WP register; G3 per-file receipts both pillars; grading-rules test map (each §2.3 rule → test name); honest-judge register (DEV-N); no-answer-leak proof (the IT assertion quoted). Operator acceptance: complete one 10-question practice session, get one wrong, log a mistake, see mastery drop on `/syllabus`, review the mistake next day (or force `next_review_at` via SQL), finish and read the summary.

**Out of scope**: mock-test mode, timers with server enforcement, negative marking (all SPEC 3), multi-user, leaderboard/gamification.

**Work packages**: WP1 V7 + entities (1.5 h) · WP2 practice session + grading service + APIs (3 h) · WP3 mastery + mistake APIs (2 h) · WP4 practice frontend (2.5 h) · WP5 mistake notebook frontend (2 h) · WP6 tests + report (1.5–2 h).

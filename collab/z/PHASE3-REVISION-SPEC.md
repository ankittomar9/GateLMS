# [PHASE3-REVISION-SPEC] — Revision Engine: Unified Queue, Pass-2 Workflow, Bookmarks, Weakness Drills

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-REVISION` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase3/analytics` (SPEC 1); may execute after SPEC 2 (program order flexible) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | V1 pre-design (`questions.pass2_status`, `is_bookmarked`); V7 mode enum `WEAKNESS_DRILL` (reserved, unused); V7 mistake SR intervals [1,3,7,14,30,60]; topics.next_revision_due |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase3/revision-engine` · Migration **V11** · Effort 7–10 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

One **revision queue** to rule the forgetting curve: everything that needs revisiting — due mistakes, due topics, pass-1 FAILED/ASSISTED questions, and bookmarks — surfaces in a single `/revision` cockpit, and drills run through the existing practice-session machinery in `WEAKNESS_DRILL` mode (the enum value V7 reserved for exactly this). Closes the `pass2_status` and `is_bookmarked` pre-designs.

## 2. Architecture (binding)

### 2.1 Migration V11 (binding DDL)

```sql
ALTER TABLE questions
    ADD COLUMN bookmarked_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_attempted_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_questions_pass2 ON questions(pass2_status);
CREATE INDEX idx_questions_bookmarked ON questions(is_bookmarked) WHERE is_bookmarked;
```

`is_bookmarked` (V1) is the boolean flag; `bookmarked_at` records when. `pass2_status` transitions (binding): a pass-1 **FAILED or ASSISTED** question enters pass 2 as `PENDING`; on a WEAKNESS_DRILL answer: correct → `pass2_status='SOLVED'`, wrong → `'FAILED'` (one-shot, no pass-3 v1; PENDING stays PENDING if skipped). Pass-2 updates flow through the same answer-grading service that already updates `pass1_status` (one hook, no duplicated grading logic).

### 2.2 Queue Composition (binding — the queue is a UNION of four real sources, each enumerated with its count)

| Source | Query basis |
|---|---|
| **Due mistakes** | `mistake_logs` with `resolved=false AND next_review_at <= now()` |
| **Due topics** | `topics` with `next_revision_due <= now()` (count = topics, not questions) |
| **Pass-2 backlog** | `questions` with `pass1_status IN ('FAILED','ASSISTED') AND pass2_status='PENDING'` |
| **Bookmarked** | `questions` with `is_bookmarked=true` |

`GET /api/v1/revision/overview` returns `{dueMistakes, dueTopics, pass2Pending, bookmarked, drillsCompleted, generatedAt}` — each number from its §table row, per-pillar honest (a count of 0 is reported as 0, never omitted).

### 2.3 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `GET /api/v1/revision/overview` | §2.2 payload — dashboard + cockpit feed |
| `POST /api/v1/revision/drills` | body `{source: "DUE_MISTAKES"\|"PASS2"\|"BOOKMARKED"\|"TOPIC", subjectId?, topicId?, count (1–50, default 15)}` → creates a practice session in **`mode='WEAKNESS_DRILL'`** reusing the PHASE2-PRACTICE machinery (safe DTO, server grading, verdict flow). Source semantics: DUE_MISTAKES → questions of unresolved due mistakes (ordered by next_review_at); PASS2 → pass-2 PENDING pool; BOOKMARKED → bookmarked pool; TOPIC → questions of due topics. Empty pool → 409 ProblemDetail `detail="No candidates for this drill source"` (honest, actionable) |
| `PATCH /api/v1/questions/{id}/bookmark` | body `{bookmarked: boolean}` → 200; sets `is_bookmarked` + `bookmarked_at`; only the flag + timestamp change — never statement/answers |
| `GET /api/v1/questions` | existing `bookmarked` filter already supported (V1) — verify, do not duplicate |
| `PATCH /api/v1/mistakes/{id}/review` | existing (V7) — mistake reviews increment `review_count`; drill answers on a due mistake question **do not auto-resolve** the mistake log (resolution stays a deliberate user action via the notebook) |
| `POST /api/v1/revision/mistakes/{id}/redrill` | creates a 1-question WEAKNESS_DRILL session for that mistake's question (deep-link CTA from the notebook) |

Topic `next_revision_due` maintenance (binding): on any wrong practice/drill answer the topic's `next_revision_due = now() + 3 days` (in addition to V7's mastery decrement); on a mistake review, `next_review_at` follows the SR ladder and **topic `next_revision_due` is NOT auto-touched** (topic scheduling stays mastery-driven only).

### 2.4 Frontend — RevisionPage `/revision`

- Sidebar entry "Revision" (icon `RotateCcw`), token-styled, all designs.
- **Cockpit**: four source cards (count + description + "Drill" CTA per §2.3 sources), "Due today" hero when any source > 0, generated-at timestamp, empty-state ("nothing due — take a mock" CTA).
- **Drill runner**: reuses the practice runner component as-is in drill mode (same 5-state palette, verdict, mistake form) — only the header badge shows "Weakness Drill · <source>".
- **Bookmark flow**: bookmark toggle button (icon `Bookmark`/`BookmarkCheck`) on QuestionDetailPage + question cards; `/questions?bookmarked=true` quick chip on QuestionsPage (filter param already exists); bookmarked page section on `/revision`.
- Dashboard: "Due for revision" card (sum of due mistakes + due topics) in all designs, linking `/revision`.

## 3. Verification & Test Gates

- **Backend delta** ≥ +3 files / +10 cases (target ≥ 32 / ≥ 109): `RevisionOverviewIT` ≥ 2 (counts per source against a known fixture — the honest-judge core: seed N of each kind, expect exactly N per row, 0 stays 0), `RevisionDrillIT` ≥ 5 (DUE_MISTAKES drill selects the due-mistake questions in next_review_at order; PASS2 pool honors pass1 FAILED/ASSISTED + pass2 PENDING; empty source → 409 with detail; drill correct answer moves pass2 PENDING→SOLVED; wrong → FAILED; skip leaves PENDING), `BookmarkApiIT` ≥ 3 (toggle on/off + bookmarked_at set; bookmark does not bump version-conflict on statement edit (independent field); list filter returns only bookmarked).
- **Frontend delta** ≥ +2 files / +10 cases (target ≥ 25 / ≥ 130): `RevisionPage.test` ≥ 6 (four source cards with counts from fixture; drill CTA posts correct source payload; due-today hero; empty state; drill runner badge; overview error rollback), `bookmarkFlow.test` ≥ 4 (toggle fires PATCH; card + detail states; filter chip navigation; optimistic rollback).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.
- **Diff gate**: practice grading service diff = the pass-2 transition hook + next_revision_due line only (no duplicated grading — reviewer verifies single write path).

## 4. Report & Acceptance

Report `collab/gemini/PHASE3-REVISION-REPORT.md`: WP register; G3 per-file receipts; source-coverage table (each §2.2 source → overview test assertion); pass-2 state-transition proof (fixture in → states out); honest-judge register (DEV-N). Operator acceptance: get a question wrong in practice, log the mistake, see it in `/revision` due queue (force `next_review_at` via SQL if not due), run the DUE_MISTAKES drill, answer correctly, verify pass2 SOLVED in the question detail, bookmark a question and find it under the bookmarked drill.

**Out of scope**: mistake auto-resolution by drill success, pass-3+ cycles, SM-2/FSRS-style adaptive intervals (fixed ladder v1), per-user scoping (Phase 5), email/push reminders (notifications land in Phase 4).

**Work packages**: WP1 V11 + pass-2 transition hook (1.5 h) · WP2 overview + drills + bookmark APIs (2.5 h) · WP3 frontend cockpit + runner reuse + bookmark UI (2 h) · WP4 tests + report (1.5 h).

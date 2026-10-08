# [PHASE7-M2-LEARNING-JOURNEY-SPEC] — Learning Journey M2: Subject Cards, Topic Roadmaps, Theory Primers, Similar Practice & Seeded Banks (BINDING)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE7-M2-LEARNING-JOURNEY` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Date** | 2026-10-09 |
| **Base Commit** | `e2dae6d` (master, clean tree) |
| **Operator Directive** | 2026-10-09 (session web-9406fb15): "If I log in to my portal, I see a card… I go to that DBMS and I see the tick marks… I can read about that topic, I can solve questions on that topic… You don't have to force me to start from foundation… Why will I have to wait for another month to unlock computer science? That's a bad design. I strongly against it… Right now I only see 26 questions in my LMS, not 3,100… So let's seed them, let's check them and let's build the system like this… If you are go, prepare the spec on this and I will have Gemini to execute this." |
| **Program Context** | Second milestone of the Learning Journey program. **Operator re-scope (binding):** the journey surface originally registered for M3 (topic launchpad, theory briefing, similar questions) is pulled forward into M2; the M1-register's "Bookshelf & Grounding" (textbook RAG pipeline) is deferred to the follow-up register. M2 binds the *visible journey*; RAG grounding follows later. |
| **Executor** | Gemini / Antigravity |
| **Migration Reservation** | **V16 is owned by this SPEC** (`topics.sequence` + `topic_primers` table, ships in WP2). V17 remains reserved for M3 Workbooks. Bookshelf tables (originally reserved for V16 under the old plan) are deferred unreserved to the follow-up register. |
| **Status / Verdict** | `APPROVED_FOR_EXECUTION` (Operator-directed; design pre-aligned across the 2026-10-09 Z/Gemini/Operator three-way review — Gemini's convergent blueprint is cross-referenced in §2) |

---

## 1. Purpose

M1 built the learning engine (anatomy dossiers, concept units, retrieval cards, SM-2-lite) and Phase 2–6 built the banks, drills, mocks, and AI layer — but the Operator's portal still *opens onto an assessment cockpit, not a journey*. Two concrete failures were diagnosed in the 2026-10-09 review:

1. **The bank is invisible.** The Operator's database shows **26 questions** (the `questions.jsonl` starter seed) while **3,131** student-visible questions exist in the repo's seed banks (2,550 GATE-Overflow-derived + 555 authored PYP + 26 starter). The bulk banks have never been loaded into the running instance.
2. **The journey has no surface.** `subjects`/`topics`/`question_progress`/topic-mastery/readiness math all exist (the 16.5/100 dial is live proof), but there is no LeetCode/Scalar-style path: *pick a subject → see its topics → read the theory → solve the topic's questions → watch ticks and cards advance*. A dead `SubjectCards.tsx` prototype (hardcoded, unwired) has sat in the tree since Phase 2 — someone dreamed this UI and never connected it.

M2 wires the existing engine into a visible journey: **10 subject cards home → topic roadmap → theory-first topic pages → similar practice → structured per-subject solved notebook**, backed by a fully seeded and verified bank.

## 2. Pedagogical Contract (binding product rules, Operator-adjudicated)

- **PC-1 — Free navigation, zero locks.** Every subject card and every topic is accessible at all times from the first login. "Recommended Start" may appear as a *badge on the Foundation cluster* (derived from the existing `subjects.cluster` / `priority_tier`), never as a gate, lock, or unlock condition. The Operator's ruling is binding and testable: *"Why will I have to wait for another month to unlock computer science? That's a bad design."* An honest-judge IT must prove a fresh student can open ANY subject/topic with zero preconditions.
- **PC-2 — Topic = Learn → Solve → Tick.** Each topic page is a stepper: ① Theory (primer + concepts + book reference) ② Questions (with M1 layered hints, ticks from real solve status) ③ Notebook (solved log). Navigation between any topics is always free; prev/next follows `topics.sequence` purely as *ordering*, not gating.
- **PC-3 — One solve moves three dials.** A single solved question must visibly advance: the topic progress bar, the subject card counters, and the overall readiness dial. All three already compute server-side; M2 surfaces them together (honest-judge IT: answer 1 question → all three deltas observed).
- **PC-4 — Theory is readable BEFORE the first attempt.** Unlike M1 anatomy (attempt-gated), topic primers are open on first visit — the Operator studies a topic *before* solving it. Primers remain AI-lazy, cached, DRAFT-badged, curated like all M1 content.
- **PC-5 — Similar practice is real questions first.** Tier-1 retrieval only in M2: unsolved real questions from the seeded banks (concept-tag match when coverage exists, topic match otherwise). AI-generated variants are **deferred to M3** (M1 register ruling unchanged; anti-fabrication risk unresolved).
- **PC-6 — Notebook = structured solved log, not a dump.** Per-subject solved history grouped by topic, star filter (reusing `question_progress.is_bookmark`), entry detail rendered from *existing persistent artifacts* (dossier tested-concept / invariant / trap) — zero new AI cost, zero fabricated takeaways.
- **PC-7 — Honest counts everywhere.** Card totals = student-visible questions only (UNREVIEWED-badged included, REJECTED excluded), verified against the WP1 seed receipt. No vanity numbers (PC-4/AF-1 of M1 carry over).

## 3. Scope & Work Packages

Three work packages, executed per G9 (one branch each, cut fresh from `origin/master`, `--no-ff` merges, branches never deleted):

| WP | Branch | Title | Delivers |
|---|---|---|---|
| WP1 | `phase7/m2-wp1-seed` | Seed & Verify the Full Bank | Multi-path seed runner (`app.seed.paths` list, backward compatible), load all three banks idempotently, admin inventory endpoint, per-subject verification receipt. **No migration.** |
| WP2 | `phase7/m2-wp2-journey` | Journey Surface | Migration V16 (`topics.sequence` + curated backfill + `topic_primers`), journey overview/roadmap/primer APIs, topic-primer generation pipeline (M1 pattern), JourneyHome cards, SubjectPage roadmap, TopicPage stepper, Dashboard relocation |
| WP3 | `phase7/m2-wp3-practice` | Similar Practice & Subject Notebooks | `/questions/{id}/similar` Tier-1 retrieval, SimilarQuestionsPanel in solve flows, per-subject solved-notebook API + UI with star filter |

**WP2 ships the complete V16 DDL** so WP3 never touches a migration (M1 precedent). Execution order: WP1 → WP2 → WP3 (WP2's honest-count UI is meaningful only against the fully seeded bank; WP3 builds on WP2's topic stepper).

**In scope:** lazy topic-primer generation + curation, journey read APIs, Tier-1 similar retrieval, solved-log notebook, dashboard route relocation.

**Out of scope (§10):** AI question variants, subject-wise mocks, bookshelf/RAG ingestion, anatomy bulk backfill job, journey-map mastery gates, workbook numbering.

## 4. Architecture Overview

No new modules — M2 extends existing services and reuses M1's generation contract end-to-end:

```text
Login ──► / (JourneyHome)
        │  GET /api/v1/journey/overview ──► 11 subject cards (subjects ⊕ question counts ⊕ topic mastery rollup)
        ▼
   SubjectPage (roadmap, ordered by topics.sequence — free navigation, badge-only recommendation)
        │  GET /api/v1/journey/subjects/{id}/topics
        ▼
   TopicPage (stepper)
        ├─ ① Theory   GET /journey/topics/{id}/primer ── cache? serve ── no: POST generate (lazy, DRAFT, quota)
        │             (primer ⊕ concept units via M1 dialogs ⊕ topics.book_chapter_ref ⊕ subjects.standard_book_ref)
        ├─ ② Questions  existing list API filtered by topic; ticks from question_progress (pass1_status)
        │             solve → M1 flow (hints, anatomy, card auto-enroll) → dials refresh (PC-3)
        │             post-solve: SimilarQuestionsPanel ◄── GET /api/v1/questions/{id}/similar?limit=5  (Tier-1)
        └─ ③ Notebook GET /journey/subjects/{id}/notebook (solved log grouped by topic; star = is_bookmark;
                      entry detail = existing anatomy sections rendered — no new AI call)
```

Topic primers flow through `AiChatService` + `AiRateLimiter` (new bucket `primer-generate`, 10/h/user) with the identical validate → 1 retry → DRAFT-with-`parse_error` never-500 contract as `AnatomyGenerationService`.

## 5. WP1 — Seed & Verify the Full Bank

### 5.1 Multi-Path Seed Runner (binding)

`QuestionSeedRunner` currently reads a single `app.seed.path` (`seed-data/questions.jsonl`, 26 items — the root cause of the Operator's "26 questions" portal). Changes:

- New config `app.seed.paths` (ordered list; `app.seed.path` remains honored as a single-entry fallback for backward compatibility).
- Runner iterates every path; per file it logs the honest receipt (imported / skipped-existing / rejected-with-reason counts). Idempotency stays keyed on `externalId` (`findByExternalId` upsert-or-skip — existing behavior, SeedBulkIT-proven).
- Default value for a fresh environment: `seed-data/questions.jsonl,seed-data/pyp-bank.jsonl,seed-data/so-gate-bank.jsonl`.
- Imported questions land `curation_status = UNREVIEWED` (existing default), student-visible with the UNREVIEWED badge per the established question-bank convention; REJECTED stays the only hidden status.

### 5.2 Verification & Inventory

- Admin endpoint `GET /api/v1/admin/seed/inventory` (role ADMIN): per subject → visible-question count, curation-status breakdown, per-topic counts. Backed by aggregate SQL (no N+1).
- Operator runbook step (docs in the Gemini report): boot with the 3-path list once; receipt must show **3,131 visible** (26 + 555 + 2,550) with zero rejections; re-boot → all skipped (idempotency proof).

### 5.3 WP1 Test Gates

- **Unit:** `QuestionSeedRunnerTest` extended — multi-path list ordering, per-file receipts, `app.seed.path` backward compat, duplicate `externalId` across files → second file skips.
- **IT (Docker):** `SeedInventoryIT` — import all three real bank files on a clean container DB; assert exact totals (3,131) and the per-subject sum; assert inventory endpoint matches; second run → 0 imported / 3,131 skipped.
- **G3 minimum:** ≥4 unit tests, ≥1 IT file.

## 6. WP2 — Journey Surface

### 6.1 Migration V16 DDL (binding, ships complete in WP2)

File: `backend/src/main/resources/db/migration/V16__learning_journey_m2.sql`

```sql
-- Migration V16: Learning Journey M2 — topic ordering + topic primers (PHASE7-M2)

-- 1. Roadmap ordering (display order ONLY — PC-1: never a gate)
ALTER TABLE topics ADD COLUMN sequence INT NOT NULL DEFAULT 0;
CREATE INDEX idx_topics_subject_sequence ON topics(subject_id, sequence);

-- 2. Curated backfill: deterministic per-subject order 1..N following the standard
--    GATE teaching sequence (Gemini authors the full UPDATE list from the syllabus
--    taxonomy; Foundation-cluster subjects' topics order first pedagogically).
--    Example shape:
-- UPDATE topics SET sequence = 1  WHERE id = 'dbms-er-models';
-- UPDATE topics SET sequence = 2  WHERE id = 'dbms-relational-algebra';
--    (… one UPDATE per topic; every topic ends with sequence ≥ 1)

-- 3. Topic-level theory primer (one per topic, global content; M1 concept-unit shape)
CREATE TABLE topic_primers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id        VARCHAR(100) NOT NULL UNIQUE REFERENCES topics(id) ON DELETE CASCADE,
    motivation      TEXT NOT NULL,
    core_idea       TEXT NOT NULL,
    invariant       TEXT,
    worked_example  TEXT NOT NULL,          -- markdown + KaTeX; PC-1 binding (≥150 words, numbered steps)
    traps           TEXT,                   -- markdown
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    parse_error     TEXT,
    generator_model VARCHAR(100),
    generated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_topic_primers_status ON topic_primers(status);
```

Entities map 1:1 (`TopicPrimer`, `topics.sequence` on `Topic`). A `MigrationV16IT` (pattern: `MigrationV14IT`) proves forward-runnability, the ≥1 backfill coverage, and deterministic ordering.

### 6.2 Topic-Primer Generation Contract (binding)

`TopicPrimerService` (in `com.gatelms.anatomy.service`, sibling of `ConceptService`) calling `AiChatService` with a strict JSON schema: `{motivation, core_idea, invariant, worked_example, traps}`. Model input: topic name + subject name + `book_chapter_ref` + a **sample of up to 10 question statements** from the topic (honest grounding on real exam data — AF-4 of M1 carries over: every numeric value must originate from the sample or be generic-definitional, never invented statistics).

Validation (server-side, in `AnatomyValidationService` — extend, do not fork): `worked_example` ≥ 150 words with numbered steps (a summary cannot contain a worked example — M1 PC-1 verbatim); AF-1 no student-population statistics; malformed JSON → 1 retry → DRAFT row with `parse_error`, never 500. Quota bucket `primer-generate` 10/h/user via `AiRateLimiter` (RFC 7807 429). Curation: DRAFT primers visible immediately, badged `AI-GENERATED — UNREVIEWED`; `CurationQueuePage` gains a Topic Primers tab (accept/reject/edit; REJECTED hidden from students, soft-delete only).

### 6.3 API Contracts (binding, under `/api/v1`)

| Endpoint | Method | Auth | Behavior |
|---|---|---|---|
| `/journey/overview` | GET | user | Array of ALL subjects (currently 11 incl. General Aptitude — render what the taxonomy holds, PC-7): `{subjectId, name, cluster, priorityTier, marksWeight, standardBookRef, questionsVisible, questionsSolved, topicsTotal, topicsTouched (≥1 solved), topicsCompleted, readinessPct}` |
| `/journey/subjects/{subjectId}/topics` | GET | user | Ordered by `(sequence, name)`: `{topicId, name, sequence, primerStatus, questionsVisible, questionsSolved, masteryLevel, completed}` |
| `/journey/topics/{topicId}/primer` | GET | user | Primer (DRAFT included, badged) + `conceptSlugs[]` for the topic (from `concept_tags.topic_id`) + `bookChapterRef` + `subjectStandardBookRef`. 404 if topic unknown. **No attempt gating (PC-4).** |
| `/journey/topics/{topicId}/primer/generate` | POST | user | Lazy generate-or-return-cache (idempotent on non-REJECTED row). 503 RFC 7807 after internal retry. 429 quota. |
| `/journey/continue` | GET | user | `{topicId, subjectId, questionId}` derived from the user's latest `question_progress.last_attempted_at` — powers the "Continue where you left off" strip. |

`completed` (topic) = 100% of student-visible questions in the topic have `pass1_status ∈ {SOLVED, ASSISTED}`; `questionsSolved` counts the same set. Mastery stays the existing 0–5 engine value, displayed as a chip — never merged into the tick logic (two honest signals, two displays).

### 6.4 Frontend (binding)

TUF dark palette, KaTeX via `MathText`, `@/` imports, collapsible rails from `e2dae6d` respected (JourneyHome renders full-width within the main workspace).

- **`JourneyHomePage.tsx`** — becomes `/` (router change; existing `DashboardPage` relocates to `/dashboard`, sidebar link "Dashboard" keeps working — no feature removed, only re-routed). Grid of subject cards grouped by `cluster` with a "Recommended Start" **badge** on Foundation (PC-1: badge only). Card body: solved/visible count, topics completed/total, marks weight subtext, progress ring. Top strip: "Continue where you left off" (`/journey/continue`), hidden when null. The dead `SubjectCards.tsx` is deleted (its visual language may be absorbed; hardcoded data must not survive).
- **`SubjectPage.tsx`** (`/subjects/:subjectId`) — the roadmap: ordered topic rows, each with tick state (progress bar solved/total + completed check), mastery chip, primer status icon, free click into any topic (PC-1 honest-judge in Vitest). Subject header shows book reference + marks weight.
- **`TopicPage.tsx`** (`/topics/:topicId`) — the stepper, three tabs, **all freely switchable**:
  - *Theory*: primer (lazy-render, generate button with loading/error states, DRAFT badge) + prerequisite concept units (each opens the M1 `ConceptUnitDialog`) + book chapter reference. Readable before any attempt (PC-4).
  - *Questions*: topic's questions via the existing list API; per-row tick from `question_progress`; opens the existing QuestionDetail/Practice flows (M1 hints, anatomy, card auto-enroll unchanged).
  - *Notebook*: this topic's solved entries (see 7.2 — same component as the subject notebook, topic-scoped).
  - Footer: prev/next topic by `sequence` + "back to roadmap".
- **`CurationQueuePage`** — Topic Primers tab (admin).
- Vitest (min 3 files): `JourneyHomePage` (cards honest data + continue strip + badge-not-lock), `SubjectPage` (ordering + ticks + free navigation), `TopicPage` (stepper tabs + primer lazy states + no gating before attempt).

### 6.5 WP2 Test Gates

- **Unit:** primer validator honest-judge (valid sample → PASS; 120-word worked example → FAIL; AF-1 fabricated-stat sample → FAIL; malformed JSON → DRAFT+parse_error path).
- **IT (Docker):** `JourneyApiIT` — overview counts match seeded fixture; **solve 1 question → overview `questionsSolved` +1, topic `touched` flips false→true, readiness delta > 0 (PC-3 three-dials honest-judge)**; roadmap ordered by sequence; primer lazy-generate → cached (single row, idempotent); **fresh student fetches any subject/topic/primer with zero preconditions → 200 (PC-1 zero-locks honest-judge)**; 429 quota; unauthenticated 401.
- **G3 minimum:** ≥8 unit tests, ≥2 IT files, ≥3 vitest files.

## 7. WP3 — Similar Practice & Subject Notebooks

### 7.1 Tier-1 Similar Questions (binding, PC-5)

`GET /api/v1/questions/{id}/similar?limit=5` (user): returns up to `limit` questions that are (a) student-visible, (b) **not the requester's solved questions** (pass1 SOLVED/ASSISTED excluded), (c) not the question itself. Ranking:

1. If ≥2 questions share a concept slug with the source question (`question_concept_tags` join, `source=ANATOMY`), rank by shared-slug count desc → `similarityBasis: "CONCEPT"`.
2. Else same-topic unsolved, newest year first → `similarityBasis: "TOPIC"` (the launch reality: concept coverage is ~0% until anatomy runs; topic basis is the honest floor, never a fabrication).

Response: existing `QuestionSummaryDto` list + `similarityBasis`. Frontend **`SimilarQuestionsPanel.tsx`** renders "Practice 5 more like this" in `QuestionDetailPage` (post-attempt) and `PracticeRunnerPage` review step, with an honest basis badge (`Same concept` / `Same topic`). No AI call, no generation, no new table.

### 7.2 Subject Notebooks (binding, PC-6)

`GET /api/v1/journey/subjects/{subjectId}/notebook?starredOnly=false` (user, paginated): solved entries grouped by topic, ordered by solve recency: `{topicId, topicName, entries: [{questionId, year, source, statementSnippet (≤200 chars), solvedAt, starred, anatomyAvailable}]}`. Starred filter = `question_progress.is_bookmarked` only (no new column — existing flag, new surface). Entry detail expansion renders the **existing** anatomy dossier sections (tested concept → invariant → trap) fetched via the M1 GET endpoint when `anatomyAvailable`; a plain "read the dossier" fallback when not. **Zero new AI generation in M2 notebooks.**

Frontend **`SubjectNotebookPage.tsx`** (`/subjects/:subjectId/notebook`, linked from SubjectPage header): topic-grouped list, `[All Solved]` / `[Starred Only]` toggle, expandable entries. Also embedded as the TopicPage *Notebook* tab (topic-scoped).

### 7.3 WP3 Test Gates

- **Unit:** similar-retrieval ranking pure function (shared-slug count ordering; solved-exclusion; self-exclusion; topic fallback when tags absent).
- **IT (Docker):** `SimilarApiIT` — same-topic unsolved set returned with basis TOPIC; after seeding two anatomy rows sharing a slug → basis CONCEPT and shared questions surface first; solved question disappears from results; empty topic → 200 with empty list. `NotebookApiIT` — solved log grouped by topic; star filter isolates bookmarked; entries exclude unsolved; pagination.
- **Vitest (min 2 files):** `SimilarQuestionsPanel` (basis badge, empty state), `SubjectNotebookPage` (grouping, star toggle, anatomy expansion fallback).
- **G3 minimum:** ≥6 unit tests, ≥2 IT files, ≥2 vitest files.

## 8. Cumulative Test-Gate Trajectory

Master baseline at `e2dae6d` (latest receipts): **backend static @Test 274 · backend IT files 37 · vitest 47 files / 249 tests · migrations V15.** Minimums at merge: WP1 +≥4 unit/+1 IT; WP2 +≥8 unit/+≥2 IT/+≥3 vitest files; WP3 +≥6 unit/+≥2 IT/+≥2 vitest files. G3 four-number reporting mandatory in the Gemini execution report; G4 dual honest-judge tests binding for PC-1 (zero locks), PC-3 (three dials), the primer validator, and similar-retrieval exclusion rules.

## 9. Governance (binding)

- Branches `phase7/m2-wp1-seed`, `phase7/m2-wp2-journey`, `phase7/m2-wp3-practice` cut fresh from `origin/master` **after WP1's parent commit lands**; one WP per branch; `--no-ff` merges only; branches pushed and preserved forever (G9).
- V16 ships complete in WP2; WP1 and WP3 touch zero migrations. V17 remains reserved for M3.
- G5 secret scan before every push. G6: `SPEC.md` phase register updated to bind M2 to this SPEC (bookshelf/RAG moved to the follow-up register). G8: Gemini reports in `collab/gemini/PHASE7-M2-*-REPORT.md`; Qwen review after each WP; Z.ai audit before each merge to master; Operator adjudicates.
- Execution order: WP1 → WP2 → WP3. No parallel cut: WP2/WP3 build on WP1's seeded reality.

## 10. Acceptance Checklist (audit rubric)

1. Seed receipt: 3,131 student-visible questions (26 + 555 + 2,550), zero rejected lines; re-boot idempotent (all skipped); inventory endpoint totals match SQL.
2. Zero locks (PC-1): fresh student account opens every subject roadmap, every topic page, every primer → 200/renders. No unlock state exists in the schema (audit: no lock/gate column introduced).
3. Three dials (PC-3): solving one question visibly moves topic bar, subject card counters, and overall readiness in one IT.
4. Roadmap ordered by `topics.sequence` (backfill coverage ≥1 per topic; deterministic).
5. Primer PC-1/M1: worked_example ≥150 words honest-judge test; AF-1 rejection test; lazy-generate idempotent; DRAFT badge shown; curation accept/reject flow.
6. Theory readable pre-attempt (PC-4): primer GET returns 200 with zero attempts on any question of the topic.
7. Similar (PC-5): topic-basis fallback works with zero anatomy coverage; concept-basis activates when tags exist; solved/self always excluded.
8. Notebook (PC-6): grouped by topic; star filter = bookmarks only; entry detail renders existing dossier data; no new AI calls.
9. TUF dark theme + KaTeX, zero raw-LaTeX leaks on all new components; dead `SubjectCards.tsx` deleted.
10. G3 four-number receipt in the execution report; G5 scan clean; V16 forward-runnable on clean Postgres 16 (`ddl-auto: validate` passes).

## 11. Out of Scope (follow-up register)

- **M3 DBMS Vertical Slice (V17):** numbered workbooks, AI question variants (Tier-2, with the anti-fabrication contract to be designed), similar-questions carousel polish, card+question mixed drills, **subject-wise mocks** (Operator: "this is the scope for M3 or M4").
- **Bookshelf & Grounding (deferred, unreserved):** books/book_sections ingestion, PDFBox pipeline, RAG-grounded generation, in-app PDF viewer, license dual-mode. M2 surfaces `book_chapter_ref`/`standard_book_ref` as display-only pointers — the seam for this register entry.
- **Anatomy bulk backfill job** (optional admin batch over 3,131 questions to accelerate concept-basis similarity; one-time AI cost — Operator decision, not M2-blocking).
- **Journey-map mastery gates** (M4 visual syllabus map; explicitly NOT a lock mechanism — PC-1 governs).
- Export of notebooks to PDF.

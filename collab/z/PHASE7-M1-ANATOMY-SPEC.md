# [PHASE7-M1-ANATOMY-SPEC] — Learning Journey M1: Question Anatomy Engine, Concept Units & Retrieval Cards (BINDING)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE7-M1-ANATOMY` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Date** | 2026-10-08 |
| **Base Commit** | `e8f004a` (master, clean tree) |
| **Operator Directive** | 2026-10-08 (session web-9406fb15): "push the SPEC M1 Schema to github — let's see the question bank in real and make this platform a truly learning journey one" |
| **Program Context** | First milestone of the Learning Journey program (M1 Anatomy → M2 Bookshelf & Grounding → M3 DBMS Vertical Slice → M4 Rollout & Journey Map). M2–M4 receive separate SPECs; this document binds M1 only. |
| **Executor** | Gemini / Antigravity |
| **Migration Reservation** | **V15 is owned by this SPEC (complete M1 DDL ships in WP1).** V16 reserved for M2 Bookshelf, V17 reserved for M3 Workbooks. No other agent may take these numbers. |
| **Status / Verdict** | `APPROVED_FOR_EXECUTION` (Operator-directed; pedagogical design pre-aligned with Operator across 2026-10-08 design session) |

---

## 1. Purpose

GateLMS today is an assessment engine: 3,105 seeded questions (2,550 GATE-Overflow-sourced + 555 authored PYP), mock tests, drills — but the learning loop is broken at its center. **0 of 3,105 questions carry an explanation longer than 250 characters.** A wrong answer produces "Incorrect. Correct option: B" and nothing else. There is no theory layer, no diagnosis, no recall mechanism. The Operator's ruling: this must become a learning journey, not a cold exam simulator.

M1 builds the engine that everything else in the journey program consumes:

1. **Question Anatomy Dossiers** — the per-question "examiner deconstruction": what the question tests, why every option exists, what misconception each distractor encodes, which concepts must be read first.
2. **Concept Tags** — the flywheel: every generated dossier emits concept-level tags (e.g. `bcnf-decomposition`) that M3's similar-questions radar and workbook banding will query. Tags are produced as a byproduct of usage, not by a separate hand-tagging campaign.
3. **Concept Units + Retrieval Cards** — the recall mechanism. Reading is entry, not retention; retention comes from scheduled retrieval. Units teach once (worked example mandatory); cards resurface forever.

## 2. Pedagogical Contract (binding product rules)

These rules came directly from the Operator's design review and are binding on implementation:

- **PC-1 — No summary theatre.** A concept unit is a teaching artifact, not a 100-word summary. The differentiator is the worked example: a summary cannot contain one. `worked_example` MUST walk the canonical GATE pattern step-by-step with concrete values.
- **PC-2 — Reading is entry, not retention.** Units are read once; retrieval cards then resurface on an expanding schedule. Failure of a question is diagnosed to the *concept* (via the dossier), and only that concept's cards are rescheduled — never the whole topic.
- **PC-3 — Two explanation layers, different jobs.** *Ephemeral* explanations (AI chat, per-question, per-student) unblock in the moment. *Persistent* assets (dossiers, units, cards) are global, curated, scheduled. M1 builds the persistent layer; the ephemeral layer already exists (Phase 6 AI Studio).
- **PC-4 — Zero fabricated statistics.** No dossier, unit, or UI string may state student-population claims ("60% of students pick B"). Numbers are either measured from `question_progress` / `mistake_log` or absent. M1 displays no population statistics at all.
- **PC-5 — No textbook citations in M1.** Grounding in the Operator's book library is M2 (`grounding_refs` column ships empty). M1 content must stand on the question's own data.

## 3. Scope & Work Packages

Two work packages, executed per G9 (one branch each, cut fresh from `origin/master`, `--no-ff` merge, branches never deleted):

| WP | Branch | Title | Delivers |
|---|---|---|---|
| WP1 | `phase7/wp1-anatomy` | Anatomy Engine | Migration V15 (full M1 DDL), dossier generation + validation + caching, concept-tag emission, REST endpoints, AnatomyPanel + LayeredHints frontend, mode-gating rules, curation extension |
| WP2 | `phase7/wp2-memory` | Concept Units & Retrieval Cards | Unit generation + validation, card emission, SM-2-lite scheduling service, card review API + RevisionPage flashcard flow, revision overview extension |

**WP1 ships the complete V15 DDL (including WP2's tables)** so WP2 never touches a migration — it implements services against the pre-created schema. This is binding.

**In scope:** lazy per-question dossier generation, on-demand unit generation, per-user card scheduling, study-mode hint layering, curation review flow for generated content.

**Out of scope (see §10):** bookshelf/RAG, workbooks, similar-questions endpoint, topic launchpad, journey map, batch pre-generation, AI question variants.

## 4. Architecture Overview

New backend module `com.gatelms.anatomy` (mirroring the Phase 6 `com.gatelms.ai` module layout) + extension of existing `RevisionService`. Generation flows through the existing **`AiChatService` provider matrix** (groq / gemini / openai / ollama) — no new provider, no new HTTP client. Rate limiting through the existing **`AiRateLimiter`** with two new buckets. Generated content is global (not per-user): one dossier per question, one unit per concept; students trigger lazy generation, everyone reuses the cache.

```text
Student answers question (study mode) ──► attempt recorded (question_progress)
        │
        ▼
GET /api/v1/questions/{id}/anatomy      [gated: attempted OR admin]
        │  cached row? ──yes──► serve dossier
        │  no
        ▼
POST /api/v1/questions/{id}/anatomy/generate   [rate-limited 30/h/user]
        │
        ▼
AiChatService → strict JSON dossier → server-side schema validation
        │                                  │ (fail → 1 retry → DRAFT w/ parse_error, never 500)
        ▼                                  ▼
persist question_anatomy (DRAFT)     + emit concept_tags + question_concept_tags
        │
        ▼
Dossier panel: prerequisites link → concept unit (lazy-generated) → emits retrieval_cards
        │
        ▼
Card review (RevisionPage): prompt → reveal → grade 0/1/2 → SM-2-lite reschedule
```

## 5. WP1 — Anatomy Engine

### 5.1 Migration V15 DDL (binding, ships complete in WP1)

File: `backend/src/main/resources/db/migration/V15__learning_journey_m1.sql`

```sql
-- Migration V15: Learning Journey M1 — Anatomy, Concept Tags, Concept Units, Retrieval Cards (PHASE7-M1)

-- 1. Concept tag registry (global content; slug-keyed like seeded entities)
CREATE TABLE concept_tags (
    slug        VARCHAR(120) PRIMARY KEY,
    label       VARCHAR(200) NOT NULL,
    topic_id    VARCHAR(100) NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_concept_tags_topic ON concept_tags(topic_id);

-- 2. Question <-> concept tag association (the flywheel output)
CREATE TABLE question_concept_tags (
    question_id      UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    concept_tag_slug VARCHAR(120) NOT NULL REFERENCES concept_tags(slug) ON DELETE CASCADE,
    source           VARCHAR(20) NOT NULL DEFAULT 'ANATOMY' CHECK (source IN ('ANATOMY','MANUAL')),
    confidence       REAL NOT NULL DEFAULT 1.0 CHECK (confidence BETWEEN 0.0 AND 1.0),
    tagged_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, concept_tag_slug)
);
CREATE INDEX idx_qct_tag ON question_concept_tags(concept_tag_slug);

-- 3. Anatomy dossier (one per question, global content)
CREATE TABLE question_anatomy (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id         UUID NOT NULL UNIQUE REFERENCES questions(id) ON DELETE CASCADE,
    tested_concept      TEXT NOT NULL,
    invariant_rule      TEXT,
    trap_analysis       JSONB NOT NULL DEFAULT '[]',
    prerequisites       JSONB NOT NULL DEFAULT '[]',
    similar_pattern_hint TEXT,
    grounding_refs      JSONB NOT NULL DEFAULT '[]',   -- reserved for M2; MUST stay '[]' in M1
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    parse_error         TEXT,                          -- non-null only for failed generations
    generator_model     VARCHAR(100),
    generated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE,
    version             INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_anatomy_status ON question_anatomy(status);

-- 4. Concept units (one per concept tag, global content)
CREATE TABLE concept_units (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_tag_slug VARCHAR(120) NOT NULL UNIQUE REFERENCES concept_tags(slug) ON DELETE CASCADE,
    title           VARCHAR(300) NOT NULL,
    motivation      TEXT NOT NULL,
    core_idea       TEXT NOT NULL,
    invariant       TEXT,
    worked_example  TEXT NOT NULL,          -- markdown + KaTeX; PC-1 binding
    traps           TEXT,                   -- markdown
    deep_link_ref   TEXT,                   -- free-text book pointer; structured refs in M2
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    parse_error     TEXT,
    generator_model VARCHAR(100),
    generated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         INT NOT NULL DEFAULT 0
);

-- 5. Retrieval cards (emitted per unit; 2–4 per unit)
CREATE TABLE retrieval_cards (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    concept_unit_id  UUID NOT NULL REFERENCES concept_units(id) ON DELETE CASCADE,
    prompt           TEXT NOT NULL,
    answer           TEXT NOT NULL,
    card_order       INT NOT NULL DEFAULT 0,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT','REVIEWED','ACCEPTED','REJECTED')),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_cards_unit ON retrieval_cards(concept_unit_id);

-- 6. Per-user card scheduling state (SM-2-lite; V14 per-user pattern)
CREATE TABLE retrieval_card_state (
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    card_id             UUID NOT NULL REFERENCES retrieval_cards(id) ON DELETE CASCADE,
    due_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    interval_days       REAL NOT NULL DEFAULT 0,
    ease                REAL NOT NULL DEFAULT 2.5,
    consecutive_correct INT NOT NULL DEFAULT 0,
    last_reviewed_at    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id, card_id)
);
CREATE INDEX idx_rcs_due ON retrieval_card_state(user_id, due_at);
```

Column conventions comply with AUDIT-PROTOCOL Pillar 2 (snake_case, PKs, FKs, forward-runnable from scratch). Entities map 1:1 in `entity/` with Kotlin data-class DTOs; jsonb columns use `@JdbcTypeCode(SqlTypes.JSON)` per the existing `Question.optionsJson` pattern.

### 5.2 AI Generation Contract (binding)

One new service `AnatomyGenerationService` calling `AiChatService` with a **strict JSON output schema**. Model input: statement, options, correct options / NAT range, type, marks, difficulty, subject + topic names, and the existing `explanation` field **marked as untrusted reference** (it may be wrong; the dossier must not inherit its errors silently).

Required JSON shape (validated server-side before persist; reject → one retry → persist DRAFT with `parse_error`, never a 500):

```json
{
  "tested_concept": "string (≤300 chars): the concept+skill this question probes",
  "invariant_rule": "string|null: the single rule that collapses the question",
  "trap_analysis": [
    { "option": "A", "verdict": "INCORRECT",
      "why_plausible": "why a student picks it",
      "misconception": "the exact confusion it encodes" }
  ],
  "prerequisites": [
    { "concept_slug": "kebab-case", "concept_label": "Human label",
      "topic_slug": "existing topic slug", "must_read": true }
  ],
  "concept_tags": ["kebab-case slugs, ≤4, within the question's topic"],
  "similar_pattern_hint": "one sentence on the question family"
}
```

**Anti-fabrication rules (binding, enforced in prompt + validator):**

- **AF-1** Dossiers containing student-population statistics (any `%`/count claim about students) are rejected by the validator.
- **AF-2** Distractor analysis must be derivable from option text + the key alone. If no trap logic exists, emit `"no distractor logic detected"` for that option — never invent one.
- **AF-3** `grounding_refs` stays `[]` in M1. Book/page citations are forbidden (M2 scope).
- **AF-4** Every numeric value in a dossier must originate from the question data itself.
- **AF-5** `concept_tags`: 1–4 slugs, kebab-case, regex `^[a-z0-9]+(-[a-z0-9]+)*$`, each auto-scoped to the question's `topic_id` in the registry.
- NAT questions: `trap_analysis` covers the common wrong-answer mechanism (off-by-one, boundary, unit) instead of option letters.

**Emission step (the flywheel):** on successful persist, each `concept_tags` slug is upserted into `concept_tags` (label + topic from the question) and linked in `question_concept_tags` (source `ANATOMY`). Emission is idempotent (ON CONFLICT DO NOTHING on the PK) and never fails the dossier persist.

**Quotas (binding):** `anatomy-generate` 30/hour/user, `concept-generate` 10/hour/user via new `AiRateLimiter` buckets; 429 with the Phase 6 RFC 7807 error contract. Timeouts reuse `app.ai.http.*`.

**Curation flow:** generated dossiers are `DRAFT` and student-visible immediately, badged `AI-GENERATED — UNREVIEWED` (matching the question-bank UNREVIEWED convention). `CurationQueuePage` gains a tab listing DRAFT dossiers with ACCEPT/REJECT + inline edit for admins. REJECTED dossiers remain queryable by admins but are not served to students.

### 5.3 API Contracts (binding, under `/api/v1`)

| Endpoint | Method | Auth | Behavior |
|---|---|---|---|
| `/api/v1/questions/{id}/anatomy` | GET | user | **Gating rule:** 200 with dossier iff requester has ≥1 recorded attempt on the question (`question_progress.last_attempted_at != null`) OR role ADMIN. Otherwise **403** body `{"code": "ANATOMY_LOCKED_UNTIL_ATTEMPT"}`. REJECTED status → 404 for non-admin. |
| `/api/v1/questions/{id}/anatomy/generate` | POST | user | Lazy generate-or-return-cache. Idempotent: existing non-REJECTED row → 200 (no regeneration). Generation failure → 503 RFC 7807 (retry once internally first). Rate limit 429. |
| `/api/v1/questions/{id}/anatomy` | DELETE | admin | Sets status REJECTED (soft reject; never hard-delete). |
| `/api/v1/concepts/{slug}` | GET | user | Unit + cards (DRAFT included, badged). 404 if absent. |
| `/api/v1/concepts/{slug}/generate` | POST | user | Lazy generate-or-return-cache for the unit (aggregates dossiers of questions tagged with the slug; ≥1 tagged question required, else 409 `NO_SOURCE_QUESTIONS`). Rate limit 10/h. |
| `/api/v1/concepts/{slug}/questions` | GET | user | Questions tagged with the concept (paginated, existing question DTO shape) — the M1 stand-in for the similar-questions radar. |

### 5.4 Frontend (binding)

TUF dark palette, KaTeX via the existing `MathText` component, absolute `@/` imports. New components:

- **`AnatomyPanel.tsx`** — rendered in `QuestionDetailPage` and in `PracticeRunnerPage` review step. Sections in order: *What this tests* → *The invariant* → *Distractor autopsy* (table: option / verdict / why plausible / misconception) → *Prerequisites* (each a link opening `ConceptUnitDialog`) → *Pattern hint* → status badge. No population statistics rendered anywhere (PC-4).
- **`LayeredHints.tsx`** — study mode only. Three progressive stages derived from the dossier (no extra AI call): L1 "This tests: {tested_concept}" → L2 "Key invariant: {invariant_rule}" → L3 "Read first: {first prerequisite label}". Never reveals the final answer or any option verdict. Reveals are client-tracked (count + confirm) with no M1 persistence. Hard-hidden in mock/test modes.
- **`ConceptUnitDialog.tsx`** — lazy-loads `/concepts/{slug}`; renders the 7-part unit (motivation → core idea → invariant → worked example → traps → self-check cards → deep link) with KaTeX.
- **Mode gating (binding):** dossier visible only after attempt in study mode; only in post-submission review in test/mock mode; hints only in study mode. Gating is enforced server-side (5.3) and mirrored in UI.
- **`CurationQueuePage`** — DRAFT dossiers + units tab (admin): accept/reject/edit.

Question list pages may show concept-tag chips (P3, optional).

### 5.5 WP1 Test Gates (G3/G4)

- **Unit:** `AnatomyJsonParserTest` — honest judge: schema-valid sample → parse PASS; malformed/truncated JSON → FAIL (DRAFT+parse_error path); AF-1 fabricated-stat sample → rejected; AF-5 bad slug → rejected. `EmissionTest` — idempotent upsert (same slug twice → one row, no exception).
- **IT (Docker machines):** `AnatomyApiIT` — generate→200; second generate→cached (single row, version unchanged); GET before any attempt → 403 `ANATOMY_LOCKED_UNTIL_ATTEMPT`; after attempt → 200; rate limit → 429; unauthenticated → 401; admin curation accept/reject flow.
- **Vitest:** `AnatomyPanel` renders dossier with KaTeX (no raw `$` leaks); `LayeredHints` stages hidden in test mode.

## 6. WP2 — Concept Units & Retrieval Cards

### 6.1 Unit Structure (binding, PC-1)

Generated units follow the 7-part anatomy agreed with the Operator: motivation (why the concept exists), core_idea (definition + intuition), invariant, **worked_example (≥150 words, explicit numbered steps, concrete values — validator rejects shorter: a summary cannot contain a worked example)**, traps (mined from tagged questions' dossiers), self-check via emitted retrieval cards (2–4 per unit: prompts that force active recall, not statement-recognition), deep_link_ref (free text until M2).

Generation input: the concept's label + topic + the aggregate of its tagged questions' dossiers (tested_concept, invariant, misconception texts). Same validation pipeline, quotas, and DRAFT-with-badge curation as WP1 (5.2).

### 6.2 Scheduling Algorithm (binding, SM-2-lite)

Grades: `0 = FORGOTTEN`, `1 = SHAKY`, `2 = SOLID`. First review schedules `due_at = now + 1 day` for all grades. Thereafter:

- **SOLID:** `consecutive_correct += 1`; ladder `[1, 3, 7, 14, 30]` days indexed by `min(consecutive_correct, 5)`; beyond the ladder, `interval_days *= ease`.
- **SHAKY:** `interval_days = max(1, interval_days * 1.2)`; `consecutive_correct = 0`; `ease = max(1.3, ease - 0.05)`.
- **FORGOTTEN:** `interval_days = 1`; `consecutive_correct = 0`; `ease = max(1.3, ease - 0.2)`.
- `ease` floor 1.3, initial 2.5. Deterministic and unit-testable — honest-judge tests required (known ladder → assert exact dates; FORGOTTEN → assert reset).

### 6.3 API + Frontend (binding)

- `GET /api/v1/revision/cards/due` — `{ dueCount, cards: [{cardId, prompt, answer, conceptSlug, conceptLabel, unitTitle, status}] }` (per-user scoping via V14 `CurrentUserProvider`).
- `POST /api/v1/revision/cards/{cardId}/review` — body `{"grade": 0|1|2}` → reschedules, returns `{nextDueAt, intervalDays, ease}`.
- `RevisionService.getOverview()` extended with `dueCards`; `RevisionPage` gains the **`CardReviewDrawer`** flashcard flow: prompt → user attempts aloud/on paper → reveal answer → self-grade 0/1/2 → next. Cards reviewed in the drawer (question drills unchanged in M1; mixing cards into question drills is M3 scope).
- **Auto-enrollment:** when a student's wrong attempt is diagnosed via a dossier, the dossier's prerequisite concepts' cards are enrolled (`retrieval_card_state` rows created, due tomorrow). This is the PC-2 loop closing: wrong answer → concept diagnosis → scheduled retrieval.
- Per-user isolation IT: user A's reviews never move user B's state.

### 6.4 WP2 Test Gates

- **Unit:** `SchedulingTest` — honest judge: SOLID ladder 1→3→7→14→30 exact; ladder-exit multiplies by ease; SHAKY 1.2× & ease decay; FORGOTTEN reset & ease floor 1.3. `CardEmissionTest` — 2–4 cards, prompt≠answer, no yes/no prompts.
- **IT (Docker):** `CardFlowIT` — due list empty → generate unit → auto-enroll after wrong attempt → review grade 2 → nextDueAt = +1d; per-user isolation.
- **Vitest:** `CardReviewDrawer` reveal/grade flow; overview tile renders `dueCards`.

## 7. Cumulative Test-Gate Trajectory

Current master baseline (per PHASE6 receipts): backend 196 static @Test, vitest 40 files / 206 tests. Minimums at merge: WP1 adds ≥8 unit tests + ≥1 IT file; WP2 adds ≥6 unit tests + ≥1 IT file + ≥2 vitest files. G3 four-number reporting mandatory in the Gemini report; G4 dual honest-judge tests for every gating rule, the validator, and the scheduler.

## 8. Governance (binding)

- Branches `phase7/wp1-anatomy` and `phase7/wp2-memory` cut fresh from `origin/master` after this SPEC lands; `--no-ff` merges only; branches preserved forever (G9).
- V15 ships complete in WP1; WP2 touches zero migrations. V16/V17 reserved (§3 table).
- G5 secret scan before every push; no AI provider keys in the tree; new quota buckets via `application.yml` env placeholders only.
- G6: `SPEC.md` gains a Phase 7 section pointing to this SPEC as the single source of truth for M1.
- Execution order: WP1 → WP2 (WP2 depends on WP1's schema + emission). Qwen review after each WP; Z.ai audit before each merge to master.

## 9. Acceptance Checklist (audit rubric)

1. V15 forward-runnable on clean Postgres 16; `ddl-auto: validate` passes against all six new tables.
2. Dossier generate→serve→cache lifecycle with single row per question; idempotent re-generation.
3. 403 `ANATOMY_LOCKED_UNTIL_ATTEMPT` enforced server-side before first attempt (honest-judge IT).
4. AF-1..AF-5 validator rejections proven by unit tests (fabricated stats sample must FAIL).
5. Concept-tag emission upserts are idempotent and scoped to the question's topic.
6. Worked-example minimum length enforced by the unit validator (short sample must FAIL).
7. SM-2-lite scheduler exact-ladder honest-judge test passes.
8. Wrong attempt → prerequisite card auto-enrollment visible in revision overview (IT).
9. TUF dark theme + KaTeX rendering with zero raw-LaTeX leaks on all new components.
10. G3 four-number receipt in the execution report; no skipped silently-failing tests.

## 10. Out of Scope (follow-up register)

- **M2 Bookshelf & Grounding:** books/book_sections/topic_book_refs tables (V16), PDFBox ingestion, RAG-grounded generation, personal/public license modes, in-app PDF viewer. `grounding_refs` is the M2 seam.
- **M3 DBMS Vertical Slice:** workbooks + numbered items (V17), similar-questions carousel (Jaccard on concept tags), topic launchpad, theory briefing tab, journey-map dashboard.
- **M4 Rollout:** remaining subjects, batch pre-generation, workbook re-test as custom mock, card+question mixed drills.
- AI-generated question variants (explicitly deferred; curation risk unresolved).
- Export of notebooks/dossiers to PDF.

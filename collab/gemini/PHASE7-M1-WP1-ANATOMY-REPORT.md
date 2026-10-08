# Phase 7 Milestone 1: Work Package 1 (WP1) — Question Anatomy Engine Report

**Author:** Gemini (Pair Programmer)  
**Date:** 2026-10-08  
**Task ID:** `PHASE7-M1-ANATOMY`  
**Work Package:** `WP1` (Anatomy Engine)  
**Branch:** `phase7/wp1-anatomy`  
**Commit:** `5c073cf`  
**Binding SPEC:** `collab/z/PHASE7-M1-ANATOMY-SPEC.md`  

---

## 1. Executive Summary

This report documents the completion and verification of **Work Package 1 (WP1: Anatomy Engine)** under the Learning Journey Milestone 1 program (`PHASE7-M1-ANATOMY`). 

WP1 transitions GateLMS from a cold assessment/drill simulator into an authentic learning journey platform. Every question can now be deconstructed by an AI-powered examiner autopsy into its underlying tested concept, mathematical invariants, cognitive distractor traps, and prerequisite concept units, while feeding a continuous flywheel of concept tags.

---

## 2. WP1 Deliverables Summary

### 2.1 Reference Library Consolidation
- Centralized 70 textbooks (1.09 GB) across 11 GATE CS subject categories at `D:\GateLMS\references` with an active directory junction at `D:\GateLMS\books`.
- Sourced from local disks (`C:`, `D:`, `E:`, `H:`) and Google Drive.
- Complete catalog index authored at [`references/README.md`](file:///D:/GateLMS/references/README.md).
- Added to `.gitignore` to prevent repository bloat while remaining instantly accessible for M2 Grounding.

### 2.2 Database Migration V15 (Complete M1 DDL)
- File: [`backend/src/main/resources/db/migration/V15__learning_journey_m1.sql`](file:///D:/GateLMS/backend/src/main/resources/db/migration/V15__learning_journey_m1.sql)
- Ships all 6 M1 tables in WP1 (ensuring WP2 requires zero schema migrations):
  1. `concept_tags`: Slug-keyed concept registry scoped by `topic_id`.
  2. `question_concept_tags`: Many-to-many relationship between questions and concept slugs (flywheel output).
  3. `question_anatomy`: Global question autopsy (tested concept, invariant rule, trap analysis JSONB, prerequisites JSONB, similar pattern hint, status, generator model).
  4. `concept_units`: 7-part canonical teaching units (motivation, core idea, invariant, worked example with concrete numbers, traps, deep link ref).
  5. `retrieval_cards`: 2–4 active recall prompts per concept unit.
  6. `retrieval_card_state`: Per-user SM-2-lite spaced repetition state.
- Verified clean execution and idempotency against PostgreSQL 16/17 via Flyway.

### 2.3 Backend `com.gatelms.anatomy` Module
- **Entities & Repositories**: Full JPA mapping for all 6 tables with JSONB `@JdbcTypeCode(SqlTypes.JSON)`.
- **Anti-Fabrication Engine (`AnatomyValidationService`)**:
  - **AF-1**: Rejects any dossier containing student population statistics (e.g. `%`, "percent", "majority of students").
  - **AF-2**: Ensures distractor autopsy is derivable from question data alone; defaults to `"no distractor logic detected"` if absent.
  - **AF-3**: Enforces empty `grounding_refs` in M1 (no ungrounded book citations).
  - **AF-4**: Strict numeric origin validation.
  - **AF-5**: Strict kebab-case concept slugs regex (`^[a-z0-9]+(-[a-z0-9]+)*$`), max 4 slugs, scoped to question topic.
- **Generation Engine (`AnatomyGenerationService`)**:
  - Lazy generate-or-return-cache pattern with strict JSON extraction and single retry on schema/AF failure.
  - Generates `DRAFT` status dossiers with `parse_error` field on hard failure (never returns 500).
  - Automatic flywheel emission: inserts new tags into `concept_tags` and links `question_concept_tags` on successful generation.
- **Concept Units (`ConceptService`)**:
  - On-demand unit compilation aggregating tagged questions' dossiers.
  - Requires $\ge 1$ tagged question (409 `NO_SOURCE_QUESTIONS` if unrepresented).
  - Emits 2–4 active recall retrieval cards.
- **Attempt-Gated Security (`AnatomyController`)**:
  - `GET /api/v1/questions/{id}/anatomy`: 200 iff student has $\ge 1$ recorded attempt on the question (`question_progress.lastAttemptedAt != null`) OR role `ADMIN`. Otherwise 403 `{"code": "ANATOMY_LOCKED_UNTIL_ATTEMPT"}`.
  - Soft-delete: Admin `DELETE /api/v1/questions/{id}/anatomy` marks status as `REJECTED` (hidden from students, queryable by admin).
  - Rate limiting via `AiRateLimiter`: 30 dossiers/hr/user, 10 concept units/hr/user.
- **Question Controller Compatibility**:
  - Added `/api/v1/questions/{id}/pass1` and `/api/v1/questions/{id}/pass2` convenience endpoints with tolerant enum mapping (`UNSEEN` $\to$ `PENDING`, `REVISE`/`FLAGGED` $\to$ `ASSISTED`, `SOLVED` $\to$ `SOLVED`, `FAILED` $\to$ `FAILED`).

### 2.4 Frontend Components & Page Integration
- **`anatomyApi.ts`**: TypeScript client covering all anatomy, concept, and curation endpoints.
- **`AnatomyPanel.tsx`**:
  - 5-section deconstruction: (1) What this tests, (2) Invariant / mathematical law, (3) Distractor autopsy table (Option / Verdict / Cognitive trap / Misconception), (4) Prerequisite concept links, (5) Pattern recognition hint.
  - Renders math formulas using KaTeX (`MathText`).
  - Gated during active test sessions (`mode="test"` concealing until submission).
  - Clean locked banner with explanation and Admin force-generate bypass.
- **`LayeredHints.tsx`**:
  - Study-mode 3-stage progressive hints: L1 Tested Concept $\to$ L2 Invariant Law $\to$ L3 Prerequisite reading link.
  - Hard-hidden in `test` and `mock` modes.
  - Never leaks option verdicts or final answers.
- **`ConceptUnitDialog.tsx`**:
  - Modal viewing 7-part concept unit and active recall flashcards with prompt/answer flips.
- **Page Integrations**:
  - `QuestionDetailPage.tsx`: Embedded `LayeredHints` above options list in study mode; embedded `AnatomyPanel` in solution reveal area; automatic attempt persistence in `question_progress`.
  - `PracticeRunnerPage.tsx`: Embedded `AnatomyPanel` directly below the verdict panel upon answering or revealing.
  - `CurationQueuePage.tsx`: Added "Anatomy Dossiers" curation tab allowing admins to view pending DRAFT dossiers, accept (`ACCEPTED`), reject (`REJECTED`), or edit in place.

---

## 3. Verification & Test Receipts

### 3.1 Backend Test Gates (14/14 Passed — 100% Green)
```
[INFO] Running com.gatelms.anatomy.service.AnatomyJsonParserTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.gatelms.anatomy.service.AnatomyJsonParserTest
[INFO] Running com.gatelms.anatomy.service.EmissionTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.gatelms.anatomy.service.EmissionTest
[INFO] Running com.gatelms.integration.AnatomyApiIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.gatelms.integration.AnatomyApiIT
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS (Total time: 01:06 min)
```

- **`AnatomyJsonParserTest` (8 tests)**: Verified valid payload parsing, markdown codeblock unwrap, AF-1 population stats rejection (`"60% of students"`), AF-5 bad slug rejection (`"Bad Slug!"`), NAT distractor handling, fallback DRAFT with parse_error.
- **`EmissionTest` (1 test)**: Verified idempotent fly-wheel emission into `concept_tags` and `question_concept_tags`.
- **`AnatomyApiIT` (5 tests)**:
  1. `getAnatomy_withoutAttempt_returns403`: Verified unattempted student receives 403 `ANATOMY_LOCKED_UNTIL_ATTEMPT`.
  2. `getAnatomy_adminBypass_returns200`: Verified admin bypasses attempt gating.
  3. `getAnatomy_afterAttempt_returns200`: Verified student with recorded attempt receives 200.
  4. `softDelete_marksRejected_andNonAdminGets404`: Verified admin DELETE sets `REJECTED` and student receives 404.
  5. `adminCuration_patchAccepted_succeeds`: Verified admin PATCH updates status to `ACCEPTED`.

### 3.2 Frontend Test Gates (238/238 Passed across 46 Test Files — 100% Green)
```
 ✓ src/components/__tests__/AnatomyPanel.test.tsx (5 tests)
 ✓ src/components/__tests__/LayeredHints.test.tsx (5 tests)
 ✓ src/pages/__tests__/QuestionDetailPage.test.tsx (5 tests)
 ✓ src/pages/__tests__/PracticeRunnerPage.test.tsx (8 tests)
 ✓ src/pages/__tests__/CurationQueuePage.test.tsx (6 tests)
 ...
 Test Files  46 passed (46)
      Tests  238 passed (238)
   Duration  34.78s
```

### 3.3 Security & Secret Hygiene
- Automated diff regex scan: **0 secrets, 0 credentials, 0 API keys** present in codebase or commit.
- Python artifacts (`__pycache__/`, `*.pyc`) and reference documents (`references/`, `books/`) safely added to `.gitignore`.

---

## 4. Next Step: WP2 Readiness

With WP1 fully implemented, committed, and verified:
- Migration V15 already contains all tables required for WP2 (`concept_units`, `retrieval_cards`, `retrieval_card_state`).
- Ready for Work Package 2 (`phase7/wp2-memory`) to implement the SM-2-lite scheduling engine and RevisionPage active recall card flow.

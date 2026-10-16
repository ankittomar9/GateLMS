# [PHASE8-WP2-REPORT] Concept Lessons, Topic Concepts UI & Primer Grounding Fix (WP2)

- **Work Package:** WP2 — Concept lessons + topic concepts UI + primer grounding fix
- **Executor:** Gemini Antigravity (OmniRush retired)
- **Base Commit:** `cea8ea6` (WP1 audit pass on master)
- **Branch:** `phase8/wp2-lessons-ui` (`e5838dd`)
- **Merge Commit:** (to be recorded post-merge)
- **Audit Regime:** Post-merge audit (≤24 h per SPEC §8.2; probation ended with 2/2 gates cleared)
- **Date:** 2026-10-17

---

## 1. Executive Summary & Scope Delivered

All tasks and acceptance gates outlined in `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §5.4, §7 (WP2), and Z.ai's WP1 post-merge audit advisories (`cea8ea6`) have been implemented, verified, and tested with honest reproduction receipts:

1. **`ConceptLessonService.kt` (New Service, `com.gatelms.anatomy.service`):**
   - Implements full primer-rails clone for canonical concept lessons: cache-first lookup, `force` regeneration parameter, per-user quota reuse (reuses existing primer limiter bucket without adding a second limiter), 1 internal retry on transient 5xx/parse failures, and never persists corrupt rows (throws 502 Bad Gateway with parse detail).
   - Hard validation (PC-2, AF-1 extended): word count strictly bounded to 300–600 words; `worked_example_external_id` validated to exist in the bank AND be mapped to the concept (`question_concept_tags`).
   - Admin-only curation: PUT update and DELETE endpoints gated behind `@PreAuthorize("hasRole('ADMIN')")`. DRAFT → ACCEPTED curation workflow intact.
   - God-Class Guardrail: 368 LOC, 9 constructor dependencies (strictly ≤600 LOC / ≤10 dependencies).

2. **Topic Primer Grounding Fix (PC-1):**
   - Extracted `TopicGroundingSampler.kt` (40 LOC): round-robin stratified sampling across `(type × difficulty)` up to 30 questions, full statements up to 1,500 characters each, and mapped canonical concept labels.
   - Replaced old first-10/200-char sampling in `TopicPrimerService.kt`.
   - Relabeled UI tab to "1. Topic Orientation" per PC-1 (honest primer scope — no implied coverage of every question).

3. **Frontend Concepts Tab & Concept Lesson Viewer (`TopicPage.tsx`, `TopicConceptsTab.tsx`, `ConceptLessonViewer.tsx`):**
   - Added Concepts tab (`data-testid="tab-concepts"`) to `TopicPage.tsx`, freely switchable across all 4 tabs (Orientation, Concepts, Practice Questions, Solved Notebook) with zero sequential locks (PC-5).
   - `TopicConceptsTab.tsx` (166 LOC): renders coverage summary bar (`conceptsTotal`, `conceptsWithMaterial`, `conceptsExplained`) and concept cards grid with coverage pills (`EXPLAINED`, `MAPPED`, `UNMAPPED`), question count, and lesson status.
   - `ConceptLessonViewer.tsx` (568 LOC): renders PC-2 sections (1. Motivation, 2. Core Idea & Mechanism with KaTeX, 3. Fundamental Invariant, 4. Worked Example citing `workedExampleExternalId` with navigation link, 5. Common Pitfalls & Traps) and the computed "Practice these questions" rail (`mappedQuestions`).
   - Admin curation controls (Edit, Regenerate, Delete, Quick Accept) strictly gated behind `isAdmin`.

4. **Question Review Concept Chips (PC-3):**
   - Extended `QuestionSummary` and `QuestionDetail` DTOs with `conceptTags?: string[]`.
   - In `QuestionDetailPage.tsx`, solution reveal panel displays tested concept chips (`data-testid="solution-concept-chips"` and `data-testid="concept-chip-${tag}"`). Clicking a chip opens the lesson dialog (`ConceptUnitDialog.tsx`, 332 LOC), linking questions directly back to canonical concepts.

5. **WP1 Audit Advisories Folded In:**
   - **A-1 & A-2:** Added `flush()` after tag deletion and `existsById` checks in `ConceptCoverageService.kt` to ensure safe, idempotent `removeTags` and conflict-free full replacement overlaps (`[A] -> [A, B] -> [B]`). Added regression test 6 to `ConceptCoverageIT.kt`.
   - **A-6:** Flipped template-saturation comparison operator in `QuestionSeedRunner.kt` from `< 3.0` to `> 3.0` with honest warning message (`> 3:1`).

---

## 2. Test Verification Receipts

### A. Backend Unit Tests (`mvnw test -Dtest=*Test`)
```
[INFO] Running com.gatelms.anatomy.service.ConceptLessonServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 7.785 s
[INFO] Running com.gatelms.anatomy.service.TopicPrimerPromptTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.496 s
[INFO] Running com.gatelms.anatomy.service.TopicPrimerValidationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.131 s
[INFO] Running com.gatelms.seed.ConceptSeedRunnerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.780 s
[INFO] Running com.gatelms.seed.QuestionSeedRunnerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.091 s
...
[INFO] Results:
[WARNING] Tests run: 148, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

### B. Backend Integration Tests (`mvnw test -Dtest=ConceptLessonIT,ConceptCoverageIT,JourneyApiIT`)
1. **`ConceptLessonIT` (4 Tests with Testcontainers PostgreSQL):**
   - `honest-judge PASS - negative authorization - students cannot access curation lesson endpoints (403)` (asserts 403 ProblemDetail on GET curation list, PUT lesson, DELETE lesson).
   - `honest-judge PASS - student can read concept lesson and mapped questions rail` (asserts lesson sections, `workedExampleExternalId`, and non-empty mapped questions rail).
   - `honest-judge PASS - lesson generation failure throws 502 Bad Gateway and does not persist corrupt row` (asserts zero corrupt rows saved on 503 upstream error).
   - `honest-judge PASS - admin curation update and delete workflows` (asserts status flip DRAFT → ACCEPTED, field mutation, and idempotent deletion).
   ```
   [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 141.2 s -- in com.gatelms.integration.ConceptLessonIT
   [INFO] BUILD SUCCESS
   ```

2. **`ConceptCoverageIT` (6 Tests):**
   - Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 — `BUILD SUCCESS` (including Advisory A-1 / A-2 overlap replace test).

3. **`JourneyApiIT` (9 Tests):**
   - Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 — `BUILD SUCCESS`.

### C. Frontend Vitest Suite (`npx vitest run`)
```
 ✓ src/pages/__tests__/TopicPage.test.tsx (10 tests)
 ✓ src/pages/__tests__/QuestionDetailPage.test.tsx (6 tests)
 ✓ src/components/__tests__/TopicConceptsTab.test.tsx (6 tests)
 ✓ src/components/__tests__/AnatomyPanel.test.tsx (5 tests)
 ...
 Test Files  53 passed (53)
      Tests  285 passed (285)
   Start at  00:05:34
   Duration  72.16s
```

### D. Frontend Production Build (`npm run build`)
```
✓ built in 18.62s
dist/assets/index-Bm7Q2nPn.js   1,614.63 kB
dist/assets/index-DMQh7G2Z.css    114.04 kB
```

---

## 3. Guardrails & Compliance Check

| Requirement | Target | Actual | Verdict |
|---|---|---|---|
| `ConceptLessonService.kt` LOC | ≤ 600 LOC | 368 LOC | PASS |
| `ConceptLessonService.kt` ctor deps | ≤ 10 deps | 9 deps | PASS |
| `TopicConceptsTab.tsx` LOC | ≤ 600 LOC | 166 LOC | PASS |
| `ConceptLessonViewer.tsx` LOC | ≤ 600 LOC | 568 LOC | PASS |
| `ConceptUnitDialog.tsx` LOC | ≤ 600 LOC | 332 LOC | PASS |
| `JourneyController.kt` deps | ≤ 10 deps | 5 deps | PASS |
| Sequential locks (PC-5) | 0 locks | 0 locks (all 4 tabs open) | PASS |
| Negative authorization test | 403 on student write | Verified in `ConceptLessonIT` | PASS |
| Grounding prompt stratification | 20–30 full statements | Verified in `TopicPrimerPromptTest` | PASS |
| Lesson word count validation | 300–600 words | Verified in `ConceptLessonServiceTest` | PASS |
| Worked example mapping validation | Resolvable + mapped | Verified in `ConceptLessonServiceTest` | PASS |

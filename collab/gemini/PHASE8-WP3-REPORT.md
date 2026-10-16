# [PHASE8-WP3-REPORT] Loop Closure & Similar-Question Quality (WP3)

- **Work Package:** WP3 — Loop closure + similar-question quality
- **Executor:** Gemini Antigravity (OmniRush retired)
- **Base Commit:** `44439ec` (WP2 merged & remote synced on master)
- **Branch:** `phase8/wp3-loop-closure`
- **Audit Regime:** Post-merge audit (≤24 h per SPEC §8.2)
- **Date:** 2026-10-17

---

## 1. Executive Summary & Scope Delivered

All tasks and acceptance gates outlined in `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §4 (PC-7), §7 (WP3), and §9 have been implemented, verified, and tested with honest reproduction receipts:

1. **Similar-Questions Dedup-Aware Ranking (Task 2, Quick Win):**
   - Extended `SimilarQuestionRanker.kt` with a normalized-statement pre-filter. Candidates sharing the exact same normalized statement as the source question (applying the canonical AF-1 intro-prefix stripping, lowercase normalization, and whitespace collapsing from `QuestionSeedRunner.normalizeStatement`) are eliminated prior to ranking.
   - Updated `SimilarQuestionsService.kt` to extract the source question's statement, compute its normalized form, and pass it to `rankCandidates`.
   - Updated `QuestionConceptTagRepository.findConceptCandidatesWithSharedSlugCount` and `QuestionRepository.findTopicCandidates` to project `statement` to supply the ranker with normalized candidate statements.
   - **Critical Fix:** Removed the hard-coded `AND qct.source = 'ANATOMY'` clause in `QuestionConceptTagRepository.findConceptCandidatesWithSharedSlugCount`. Because all Phase 8 curator-seeded tags carry `source = MANUAL`, this restriction previously prevented live pilot questions from matching concept similarity candidates. With this removed, curator-mapped tags participate fully, activating the `CONCEPT` basis (≥2 shared slugs) on real data.

2. **Retention Closes the Loop (Task 1, PC-7):**
   - Extended backend DTOs to surface concept references:
     - `PracticeQuestionDto.conceptTags`: populated on session creation.
     - `PracticeVerdictResponse.conceptTags`: returned upon answer submission and question reveal.
     - `MistakeDto.conceptTags` and `MistakeDto.topicId`: populated on mistake logging and review.
     - `MockQuestionReviewDto.conceptTags`: populated during mock test review.
     - `RevisionDrillRequest.conceptSlug`: supported in `RevisionService.kt` to filter weakness drills to a specific concept.
   - Updated services (`PracticeService.kt`, `MistakeService.kt`, `MockTestService.kt`, `RevisionService.kt`) with optional `QuestionConceptTagRepository` injection, strictly honoring test isolation and constructor signatures.

3. **Frontend Concept Routing & In-Place Dialogs (PC-7):**
   - **Practice Runner (`PracticeRunnerPage.tsx`):** On graded verdict (especially wrong answers where `mistakeLogPrompt` is triggered), tested concept chips (`data-testid="verdict-concept-chips"`) are displayed. Clicking a chip opens `ConceptUnitDialog` directly, allowing learners to review the lesson in-place without losing practice session state.
   - **Mistake Notebook (`MistakeNotebookPage.tsx`):** Each mistake card displays tested concept chips (`data-testid="mistake-concept-chips"`). Clicking a chip opens `ConceptUnitDialog` with the full lesson.
   - **Exam Review (`ResultPage.tsx`):** Reviewed questions display tested concept chips (`data-testid="review-concept-chips"`), opening `ConceptUnitDialog` for instant review.
   - **Deep-Linking Support (`TopicPage.tsx`):** Added URL query parameter support (`?tab=concepts&concept=<slug>`) via `useSearchParams` to route deep-links directly into the Concepts tab with the targeted lesson opened.

4. **Honest-Judge Integration Tests (Task 3):**
   - `SimilarApiIT`: Added `honest-judge PASS - similar results exclude normalized-duplicate siblings and return concept-tagged candidate (PC-7)`. Proves that a prefix-rotated clone is pre-filtered (0 normalized duplicates returned), while distinct-pattern questions sharing concept tags are ranked top.
   - `MistakeApiIT`: Added `honest-judge PASS - mistake log surfaces concept tags and topicId for wrong answer review (PC-7)`. Proves mistake creation and retrieval return `conceptTags` and `topicId`.
   - `PracticeSessionApiIT`: Added `honest-judge PASS - wrong answer submission returns concept tags in verdict response (PC-7)`. Proves wrong answer submission returns `conceptTags` in `PracticeVerdictResponse`.
   - `RevisionDrillIT`: Added `8 Revision drill filters candidates by conceptSlug (PC-7)`. Proves weakness drill candidate filtering by `conceptSlug`.

---

## 2. Test Verification Receipts

### A. Backend Targeted & Integration Tests (`mvnw test -Dtest=SimilarQuestionRankerTest,SimilarApiIT,MistakeApiIT,PracticeSessionApiIT,RevisionDrillIT`)
```
[INFO] Running com.gatelms.service.SimilarQuestionRankerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.018 s
[INFO] Running com.gatelms.integration.SimilarApiIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.58 s
[INFO] Running com.gatelms.integration.MistakeApiIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 13.91 s
[INFO] Running com.gatelms.integration.PracticeSessionApiIT
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 75.38 s
[INFO] Running com.gatelms.integration.RevisionDrillIT
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.057 s
[INFO] Results:
[INFO] Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### B. Backend Unit Tests Suite (`mvnw test -Dtest=*Test`)
```
[INFO] Running com.gatelms.service.SimilarQuestionRankerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gatelms.anatomy.service.ConceptLessonServiceTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gatelms.seed.ConceptSeedRunnerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gatelms.seed.QuestionSeedRunnerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[WARNING] Tests run: 151, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

### C. Frontend Vitest Suite (`cmd.exe /c "npx vitest run"`)
```
 ✓ src/pages/__tests__/PracticeRunnerPage.test.tsx (8 tests)
 ✓ src/pages/__tests__/MistakeNotebookPage.test.tsx (5 tests)
 ✓ src/pages/__tests__/ResultPage.test.tsx (7 tests)
 ✓ src/pages/__tests__/TopicPage.test.tsx (10 tests)
 ✓ src/components/__tests__/SimilarQuestionsPanel.test.tsx (5 tests)
 ...
 Test Files  53 passed (53)
      Tests  288 passed (288)
   Start at  01:00:28
   Duration  99.70s
```

### D. Frontend Production Build (`cmd.exe /c "npm run build"`)
```
✓ built in 25.87s
dist/assets/index-DMQh7G2Z.css    114.04 kB │ gzip:  21.95 kB
dist/assets/index-DiKTRDRg.js   1,617.63 kB │ gzip: 419.21 kB
```

---

## 3. Guardrails & Compliance Check

| Requirement | Target | Actual | Verdict |
|---|---|---|---|
| `SimilarQuestionRanker.kt` LOC | ≤ 600 LOC | 59 LOC | PASS |
| `SimilarQuestionsService.kt` LOC | ≤ 600 LOC | 98 LOC | PASS |
| `PracticeService.kt` LOC | ≤ 600 LOC | 394 LOC | PASS |
| `MistakeService.kt` LOC | ≤ 600 LOC | 168 LOC | PASS |
| `RevisionService.kt` LOC | ≤ 600 LOC | 132 LOC | PASS |
| Constructor dependencies | ≤ 10 deps | All ≤ 9 deps | PASS |
| Clones in similar results | 0 normalized duplicates | 0 (verified in `SimilarApiIT`) | PASS |
| Wrong answer concept routing | Surface tested concepts | Verified in `PracticeRunnerPage.test.tsx` & `PracticeSessionApiIT` | PASS |
| Mistake concept context | Tags & topic context in mistake flow | Verified in `MistakeNotebookPage.test.tsx` & `MistakeApiIT` | PASS |
| Non-locking navigation (PC-5) | 0 locks | Verified (all dialogs & tabs free) | PASS |

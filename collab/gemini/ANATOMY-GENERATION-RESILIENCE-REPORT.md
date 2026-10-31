# [ANATOMY-GENERATION-RESILIENCE] Question Anatomy Failure Resilience & Cache Lock Fix — Execution Report

- **Work Package:** Question Anatomy Generation Resilience, Cache Bypass & Retry
- **Executor:** Gemini Antigravity
- **Base Commit:** `f1e098c` (`origin/master`)
- **Branch:** `ai/anatomy-generation-resilience`
- **Date:** 2026-10-31

---

## 1. Problem Root Cause & Architectural Defects

### Root Cause Analysis
During upstream Google Gemini demand spikes (`503 Service Unavailable`), the AI Adapter layer correctly fell back from `gemini-3.8-flash` to `gemini-3.5-flash`. However, due to concurrent load across models and socket read timeouts, the subsequent internal retry emitted malformed JSON (unexpected character `'v'` at line 1, column 2).

This triggered two architectural defects in `AnatomyGenerationService.kt`:
1. **Corrupt Row Persistence:** Lines 150–165 persisted a placeholder DRAFT row with `testedConcept = "Generation parsing incomplete"` and `parseError != null` into PostgreSQL.
2. **Permanent Cache Lock:** Line 73 evaluated `if (existing.isPresent && existing.get().status != AnatomyStatus.REJECTED)`, which returned `true` for the corrupt DRAFT row because `status` was `DRAFT`. It did **not** verify that `parseError == null` or that `testedConcept != "Generation parsing incomplete"`, and did not provide a `force: Boolean = false` parameter to bypass cache.

As a consequence, the question was permanently locked into displaying `"Generation parsing incomplete"`, even after Gemini recovered.

---

## 2. Delivered Fixes & Hardening

### 1. Backend Service Layer (`AnatomyGenerationService.kt`)
- **Cache Check Hardening:** Updated `generateOrGetCached` cache validation to:
  ```kotlin
  if (!force && existing.isPresent && existing.get().status != AnatomyStatus.REJECTED &&
      existing.get().parseError == null && existing.get().testedConcept != "Generation parsing incomplete"
  ) {
      return toDto(existing.get())
  }
  ```
- **Never Persist Corrupt Rows:** Aligned with `TopicPrimerService` and `ConceptLessonService`. When `payload == null`, the service throws:
  ```kotlin
  throw ResponseStatusException(
      HttpStatus.BAD_GATEWAY,
      "Question anatomy generation failed: ${parseError ?: "AI service unavailable"}"
  )
  ```
  Corrupt placeholder rows are never written to PostgreSQL on generation failure.
- **In-Place Overwrite on Successful Regeneration:** When a legacy corrupted row already exists in the database, successful regeneration updates that entity in-place, clears `parseError = null`, saves the valid anatomy, and emits concept tags.

### 2. Backend Controller & Query Layer
- **`AnatomyController.kt`:** Added `@RequestParam(defaultValue = "false") force: Boolean` to `POST /api/v1/questions/{id}/anatomy/generate`.
- **`QuestionAnatomyRepository.kt`:** Updated `findQuestionIdsWithValidAnatomy` to filter out records where `parseError IS NOT NULL` or `testedConcept = 'Generation parsing incomplete'`.

### 3. Frontend Client & UI Layer
- **`anatomyApi.ts`:** Added `force?: boolean` option to `generateQuestionAnatomy(questionId, { force })`, appending query parameter `?force=true` when specified.
- **`AnatomyPanel.tsx`:**
  - Added incomplete/error banner (`data-testid="anatomy-error-banner"`) shown when `anatomy.parseError != null` or `anatomy.testedConcept === 'Generation parsing incomplete'`.
  - Prominent **"Retry Generation"** button with a refresh icon calling `handleGenerate(true)` with `force: true`.
  - Admin header button to force-regenerate dossiers when needed.
  - Section 1 ("1. What This Tests") is concealed when `testedConcept === 'Generation parsing incomplete'`.
- **`LayeredHints.tsx`:** Hardened hints so that incomplete or errored anatomies do not leak "Generation parsing incomplete" as Hint 1 during study practice.

---

## 3. Verification Matrix & Test Receipts

| Suite | Scope | Target | Result | Status |
|---|---|---|---|---|
| **`AnatomyGenerationServiceTest`** | Backend Unit Suite | 4 unit tests | **4 run, 0 failures, 0 errors, 0 skipped** (elapsed 9.38 s) | **PASS** |
| **Backend Unit Suite (`*Test`)** | Full Backend Unit Regression | 171 tests | **171 run, 0 failures, 0 errors, 1 skipped** (elapsed 2:34 min) | **PASS** |
| **`AnatomyPanel.test.tsx`** | Frontend Vitest | 6 tests | **6 passed / 6 passed** (incl. Test 6 retry test) | **PASS** |
| **Frontend Full Vitest** | Full Frontend Regression | 55 files / 300 tests | **55 passed / 300 passed** (0 failures) | **PASS** |
| **Frontend Production Build** | TypeScript + Vite Build | 2 chunks | **RichMarkdownText-*.js 184.22 kB, index-*.js 1,629.83 kB** | **PASS** |
| **Whitespace / CRLF** | Git diff check | All touched files | **0 errors (`git diff --check`)** | **PASS** |

### Detailed Receipts

#### 1. Backend Unit Regression (`mvn test -Dtest="*Test"`)
```
[INFO] Running com.gatelms.anatomy.service.AnatomyGenerationServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.385 s
...
[INFO] Results:
[INFO] 
[WARNING] Tests run: 171, Failures: 0, Errors: 0, Skipped: 1
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:34 min
```
*(Arithmetic: 167 baseline + 4 AnatomyGenerationServiceTest = 171 run).*

#### 2. Frontend Vitest Regression (`npm test -- --run`)
```
 Test Files  55 passed (55)
      Tests  300 passed (300)
   Start at  15:41:50
   Duration  98.54s
```
*(Arithmetic: 299 baseline + 1 AnatomyPanelTest = 300 passed).*

#### 3. Frontend Production Build (`npm run build`)
```
vite v6.4.4 building for production...
transforming...
✓ 2935 modules transformed.
rendering chunks...
computing gzip size...
dist/assets/RichMarkdownText-BTA6qU1J.js                    184.22 kB │ gzip:  56.58 kB
dist/assets/index-BiqC9-9L.js                             1,629.83 kB │ gzip: 423.04 kB
✓ built in 29.27s
```

---

## 4. Git & Branch Hygiene Compliance

All actions strictly executed per `AGENTS.md` G9 Branch-per-Work-Package Protocol:
1. Cut dedicated branch `ai/anatomy-generation-resilience` off `origin/master` (`f1e098c`).
2. Atomic commit on feature branch.
3. Pushed branch to `origin/ai/anatomy-generation-resilience`.
4. Merged into `master` via `--no-ff` merge commit.
5. Pushed `master` to remote `origin`.

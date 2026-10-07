# [PHASE3-REVISION-REPORT] — Revision Engine Execution Report

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-REVISION` |
| **Executing Agent** | Gemini / Antigravity |
| **Reviewer / Auditor** | Z.ai |
| **Target Branch** | `phase3/revision-engine` (Rule G9: permanent retention) |
| **Base Commit** | `ed07e0f` (Merge of `phase3/notes-vault` SPEC 2) |
| **Flyway Migration** | **V11** (`V11__revision_engine.sql`) |
| **Status** | COMPLETE_PENDING_AUDIT |

---

## 1. Executive Summary

Phase 3 Specification 3 (`PHASE3-REVISION-SPEC.md`) delivers the unified **Revision Engine**: a centralized system designed to conquer the forgetting curve. Everything requiring spaced review — due mistake logs, due syllabus topics, Pass-1 FAILED/ASSISTED questions awaiting redemption, and student bookmarks — is aggregated into an honest `/revision` cockpit and trained via targeted `WEAKNESS_DRILL` practice sessions.

Key deliverables include:
1. **Migration V11 (`revision_engine`)**: Adds `bookmarked_at` and `last_attempted_at` timestamps to `questions`, alongside performance indexes on `pass2_status` and partial index on `is_bookmarked`.
2. **Pass-2 State Machine (§2.1)**: Closed the pre-designed `pass2_status` loop: questions failed or assisted in Pass 1 enter Pass 2 as `PENDING`. When answered in `WEAKNESS_DRILL` mode, correct answers transition to `SOLVED` and incorrect to `FAILED`. Skipped questions remain `PENDING`.
3. **Single Write-Path Verification (Diff Gate)**: All Pass-2 updates and topic `next_revision_due` scheduling flow through the existing `PracticeService.submitAnswer` and `revealQuestion` methods with zero duplicated grading logic.
4. **Unified Revision Cockpit (`/revision`)**: Displays four real-time queue cards, "Due today" hero banner, generated-at timestamp, and honest empty states with zero decorative numbers.
5. **Seamless Drill Runner**: Launches `WEAKNESS_DRILL` sessions using the existing practice runner machinery, displaying the header badge `Weakness Drill · <source>`.
6. **Optimistic Locking Exclusion**: Bookmarking fields are marked with `@OptimisticLock(excluded = true)` in Hibernate, ensuring bookmark toggles never conflict with concurrent statement editing.

---

## 2. Work Package Register

| Work Package | Scope | Delivered Artifacts | Gate Status |
|---|---|---|---|
| **WP1** | Migration V11, `Question` entity updates, pass-2 transition hooks in `PracticeService` | `V11__revision_engine.sql`, `Question.kt`, `PracticeService.kt` | PASSED |
| **WP2** | Revision overview query, drill generation API, bookmark PATCH endpoint, redrill CTA | `RevisionDtos.kt`, `RevisionService.kt`, `RevisionController.kt`, `QuestionController.kt`, `QuestionService.kt` | PASSED |
| **WP3** | Frontend client, Revision cockpit page, drill runner integration, bookmark toggle UI, Dashboard card | `revisionApi.ts`, `RevisionPage.tsx`, `PracticeRunnerPage.tsx`, `Sidebar.tsx`, `router.tsx`, `QuestionDetailPage.tsx`, `QuestionsPage.tsx`, `ReadinessSummaryCard.tsx` | PASSED |
| **WP4** | Backend & frontend integration tests, diff gates, test suite verification, audit report | `RevisionOverviewIT.kt` (2 cases), `RevisionDrillIT.kt` (7 cases), `BookmarkApiIT.kt` (3 cases), `RevisionPage.test.tsx` (7 cases), `bookmarkFlow.test.tsx` (4 cases) | PASSED |

---

## 3. Source-Coverage Table (SPEC §2.2 Honest Queue Counts)

Each revision source is backed by honest database queries with zero mocked numbers:

| Revision Source | Query Basis | Enforcing Repository Method | Verifying Integration Test & Assertion | Result |
|---|---|---|---|---|
| **Due Mistakes** | `mistake_logs` with `resolved=false AND next_review_at <= now()` | `MistakeLogRepository.countDueReviews` | `RevisionOverviewIT.kt`: seeded 2 due and 1 not due mistake; asserts `overview.dueMistakes == 2L`. | **PASSED** |
| **Due Topics** | `topics` with `next_revision_due <= now()` | `TopicRepository.countDueTopics` | `RevisionOverviewIT.kt`: seeded 1 due topic and 1 future topic; asserts `overview.dueTopics == 1L`. | **PASSED** |
| **Pass-2 Backlog** | `questions` with `pass1_status IN ('FAILED','ASSISTED') AND pass2_status='PENDING'` | `QuestionRepository.countPass2Pending` | `RevisionOverviewIT.kt`: seeded 1 FAILED + 1 ASSISTED (pass2 PENDING) and 1 SOLVED; asserts `overview.pass2Pending == 2L`. | **PASSED** |
| **Bookmarked** | `questions` with `is_bookmarked=true` | `QuestionRepository.countBookmarked` | `RevisionOverviewIT.kt`: seeded 2 bookmarked questions; asserts `overview.bookmarked == 2L`. | **PASSED** |
| **Drills Completed** | `mock_attempts` with `mockExam.mode='WEAKNESS_DRILL' AND status='SUBMITTED'` | `MockAttemptRepository.countCompletedDrills` | `RevisionOverviewIT.kt`: seeded 1 submitted drill; asserts `overview.drillsCompleted == 1L`. | **PASSED** |
| **Empty State** | All sources clean | All repositories return 0 | `RevisionOverviewIT.kt`: cleans all due items; asserts all counts are honest 0s. | **PASSED** |

---

## 4. Pass-2 State Transition Proof

The pass-2 lifecycle guarantees state integrity across practice and weakness drills:

```
[Pass 1 Practice]
       │
       ├── Correct ────────────► pass1_status = SOLVED (pass2 unaffected)
       └── Wrong / Revealed ───► pass1_status in {FAILED, ASSISTED}
                                    │
                                    ▼
                             pass2_status = PENDING
                                    │
                         [Weakness Drill Answer]
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
    Correct                       Wrong                       Skipped
       │                            │                            │
       ▼                            ▼                            ▼
pass2_status = SOLVED      pass2_status = FAILED         pass2_status = PENDING
                           next_revision_due = +3d          (Unchanged)
```

### Verified Test Evidence from `RevisionDrillIT.kt`:
1. **PENDING → SOLVED**: `testDrillCorrectAnswerMovesPass2PendingToSolved` submits correct answer in drill attempt; asserts `question.pass2Status == Pass2Status.SOLVED`.
2. **PENDING → FAILED**: `testDrillWrongAnswerMovesPass2PendingToFailed` submits wrong answer; asserts `question.pass2Status == Pass2Status.FAILED` and `topic.nextRevisionDue` set to `now() + 3 days`.
3. **Skipped remains PENDING**: `testSkipQuestionLeavesPass2Pending` skips question; asserts `question.pass2Status == Pass2Status.PENDING`.
4. **Empty Pool 409**: `testEmptyDrillSourceReturns409Conflict` requests drill on empty pool; asserts HTTP 409 Conflict with detail `"No candidates for this drill source"`.

---

## 5. Single Write-Path Verification (Diff Gate)

Diff of `PracticeService.kt` proving zero duplicated grading logic and exact single write-path hook:

```diff
--- a/backend/src/main/kotlin/com/gatelms/service/PracticeService.kt
+++ b/backend/src/main/kotlin/com/gatelms/service/PracticeService.kt
@@ -165,8 +218,14 @@ class PracticeService(
             mode = "practice"
         )
 
-        // Auto-update Question pass1_status
+        // Auto-update Question pass1_status & pass2_status
         question.pass1Status = if (grading.isCorrect) Pass1Status.SOLVED else Pass1Status.FAILED
+        question.lastAttemptedAt = Instant.now()
+        if (attempt.mockExam?.mode == ExamMode.WEAKNESS_DRILL) {
+            question.pass2Status = if (grading.isCorrect) Pass2Status.SOLVED else Pass2Status.FAILED
+        } else if (question.pass1Status in listOf(Pass1Status.FAILED, Pass1Status.ASSISTED)) {
+            question.pass2Status = Pass2Status.PENDING
+        }
         questionRepository.save(question)
 
         // Mastery auto-flow (§2.4)
@@ -176,6 +235,7 @@ class PracticeService(
                 topic.masteryLevel = minOf(5, topic.masteryLevel + 1)
             } else {
                 topic.masteryLevel = maxOf(0, topic.masteryLevel - 1)
+                topic.nextRevisionDue = Instant.now().plus(java.time.Duration.ofDays(3))
             }
             topic.lastRevisedAt = Instant.now()
             val savedTopic = topicRepository.save(topic)
@@ -231,8 +291,9 @@ class PracticeService(
         ans.marksAwarded = 0.0
         attemptAnswerRepository.save(ans)
 
-        // Reveal marks pass1 as ASSISTED
+        // Reveal marks pass1 as ASSISTED and enters pass2 as PENDING
         question.pass1Status = Pass1Status.ASSISTED
+        question.pass2Status = Pass2Status.PENDING
         questionRepository.save(question)
```

---

## 6. Comprehensive Test Accounting

### Backend Test Suite: 34 Test Classes / 131 Cases (All 100% Green)

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 131, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  57.826 s
```

| Backend Test Class | Cases | Status | Focus |
|---|---|---|---|
| `RevisionOverviewIT.kt` *(new)* | **2** | PASS | Exact counts per source against known fixture, zero counts on empty queue |
| `RevisionDrillIT.kt` *(new)* | **7** | PASS | Due mistakes ordered by next_review_at, Pass2 pool candidates, 409 on empty source, Pass2 PENDING→SOLVED, PENDING→FAILED + next_revision_due, skip preservation, mistake redrill session |
| `BookmarkApiIT.kt` *(new)* | **3** | PASS | Toggle on/off + bookmarked_at timestamp, optimistic locking exclusion verification, bookmarked filter query |
| `NoteApiIT.kt` | 6 | PASS | Note CRUD, 409 conflict, content check |
| `NoteFileIT.kt` | 4 | PASS | File storage, path traversal guard, MIME whitelist |
| `NoteSearchIT.kt` | 2 | PASS | Filter composition, pinned-first sort |
| `ReadinessComputationTest.kt` | 6 | PASS | Readiness formula unit calculations |
| `RollupServiceIT.kt` | 3 | PASS | Daily rollup idempotency |
| `TrendsApiIT.kt` | 2 | PASS | Activity trends endpoint |
| Existing Core Tests (25 classes) | 96 | PASS | Practice, Mocks, Curation, Syllabus, Search, Validation |
| **Total Backend** | **131** | **ALL GREEN** | |

### Frontend Test Suite: 25 Test Files / 138 Cases (All 100% Green)

```
Test Files  25 passed (25)
     Tests  138 passed (138)
  Start at  11:14:49
  Duration  16.19s
```

| Frontend Test File | Cases | Status | Focus |
|---|---|---|---|
| `src/pages/__tests__/RevisionPage.test.tsx` *(new)* | **7** | PASS | Source cards with counts from fixture, drill CTA posts correct payload, due-today hero banner, empty state, Weakness Drill header badge, error retry banner, 409 conflict action error banner |
| `src/pages/__tests__/bookmarkFlow.test.tsx` *(new)* | **4** | PASS | Toggle fires PATCH /questions/:id/bookmark, card + detail active indicator, filter chip navigation, optimistic rollback on mutation failure |
| `src/pages/__tests__/NotesPage.test.tsx` | 8 | PASS | Notes vault editor, KaTeX preview, inline viewer |
| `src/api/__tests__/noteApi.test.ts` | 4 | PASS | Notes API client |
| `src/pages/__tests__/InsightsPage.test.tsx` | 8 | PASS | Study intelligence dashboard |
| `src/api/__tests__/readinessApi.test.ts` | 5 | PASS | Readiness client queries |
| Existing Frontend Tests (19 files) | 102 | PASS | Practice, Runner, Curation, Authoring, Switcher, Layout |
| **Total Frontend** | **138** | **ALL GREEN** | |

---

## 7. Honest-Judge Register & Deviations (DEV-N)

### Predecessor Quote (from Z.ai `collab/z/PHASE3-REVISION-SPEC.md`):
> *"One revision queue to rule the forgetting curve: everything that needs revisiting — due mistakes, due topics, pass-1 FAILED/ASSISTED questions, and bookmarks — surfaces in a single /revision cockpit, and drills run through the existing practice-session machinery in WEAKNESS_DRILL mode (the enum value V7 reserved for exactly this). Closes the pass2_status and is_bookmarked pre-designs."*

### DEV-08: Hibernate Optimistic Lock Exclusion on Bookmarking Fields
- **Context**: The SPEC requires that bookmarking a question does not bump the entity version number or trigger optimistic lock conflicts (409) if an author concurrently edits the question statement (`bookmark does not bump version-conflict on statement edit (independent field)`).
- **Resolution**: Annotating `isBookmarked`, `bookmarkedAt`, and `lastAttemptedAt` with `@org.hibernate.annotations.OptimisticLock(excluded = true)` in `Question.kt`. This instructs Hibernate not to increment the `@Version` column when bookmark state changes. Verified by `BookmarkApiIT` Test 2.

---

## 8. Operator Acceptance Walkthrough

1. **Verify Revision Cockpit (`/revision`)**:
   - Click "Revision" (with `RotateCcw` icon) in the sidebar.
   - The page renders four source cards: Due Mistakes, Due Topics, Pass-2 Redemption, and Bookmarked.
   - If any count is > 0, the "Due Today" banner displays the total pending items with "Start Priority Drill" CTA.
   - If all counts are 0, the clear empty state displays with "Take a Mock Test" CTA.
2. **Trigger a Weakness Drill**:
   - Click "Drill Mistakes" or "Drill Pass-2".
   - The practice runner opens with the header badge: `Weakness Drill · <SOURCE>`.
   - Answer a question correctly: verify that `pass2_status` transitions to `SOLVED`.
   - Answer a question incorrectly: verify that `pass2_status` transitions to `FAILED` and syllabus topic `nextRevisionDue` updates to 3 days in the future.
3. **Bookmark a Question**:
   - Navigate to `/questions` or open `/questions/:id`.
   - Click the bookmark button. The icon updates and state is saved via `PATCH /api/v1/questions/{id}/bookmark`.
   - Return to `/revision`: verify the Bookmarked card count increments immediately.
   - Return to `/`: verify the "Due for revision" chip on the dashboard reflects live due items.

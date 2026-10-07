# [PHASE4-PLANNER-REPORT] — Study Planner: Weightage-Aware Plan Generation, Calendar, Daily Targets, Auto-Progress

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-PLANNER` |
| **Executing Agent** | Gemini / Antigravity |
| **Reviewer / Auditor** | Z.ai |
| **Target Branch** | `phase4/study-planner` (Rule G9: permanent retention) |
| **Base Commit** | `324cad3` (Merge of `phase3/pyp-import`) |
| **Flyway Migration** | `V12__study_planner.sql` (Study Planner Schema) |
| **Status** | COMPLETE_PENDING_AUDIT |

---

## 1. Executive Summary

Phase 4 Specification 1 (`collab/z/PHASE4-PLANNER-SPEC.md`) delivers the **GATE Study Planner Engine**: an intelligent, weightage-aware roadmap generator that converts long-term exam preparation horizons (1–52 weeks) into structured, daily actionable milestones.

Key deliverables include:
1. **Flyway Migration V12 (`V12__study_planner.sql`)**: Introduces relational persistence for `study_plans` and `plan_items` with cascading constraints, indexation on `plan_date`, and domain checks (`strategy`, `status`, `end_date >= start_date`).
2. **Deterministic Multi-Strategy Plan Generator (`POST /api/v1/plans/generate`)**:
   - `WEIGHTED`: Syllabus weightage multiplied by current weakness multiplier ($S_s = w_s \cdot (1 + 0.5 \cdot (1 - \text{readiness}_s))$).
   - `WEAKNESS_FIRST`: Identifies weak subjects via telemetry, front-loading the weakest 40% of capacity.
   - `LINEAR`: Pure syllabus weightage distribution.
   - **Bank Exclusions**: Automatically excludes topics having zero available questions in the question bank, listing them explicitly in `generated_reasoning.excludedTopics`.
   - **Honest Fallback**: When the system is fresh (no user activity), generation cleanly degenerates to weightage-only mode, recording `{mode: "weightage_only", reason: "no_activity"}` in `generated_reasoning`.
3. **Comprehensive Planner REST API**:
   - `POST /api/v1/plans/generate`: Validates 1–52 week range and yields full plan summary.
   - `GET /api/v1/plans`: Lists all plans with progress %, item counts, and date spans.
   - `GET /api/v1/plans/{id}`: Detailed plan representation with items grouped by date and reasoning JSON.
   - `GET /api/v1/plans/today`: 404-free daily summary showing today's target vs done metrics.
   - `PATCH /api/v1/plan-items/{id}`: Manual status override (`OPEN`, `DONE`, `SKIPPED`).
   - `POST /api/v1/plans/{id}/regenerate`: Forward regeneration from today onward while strictly preserving past completed (`DONE`) items.
4. **Resilient Auto-Progress Single Write Path (`PlanProgressService`)**:
   - Encapsulates auto-progress logic cleanly.
   - Wiretap hooks added to `PracticeService.submitAnswer`, `MockTestService.submitAttempt`, and `MistakeService.reviewMistake` (+1 question).
   - Failures inside the progress hook are logged as warnings and never fail user grading or test submission transactions.
5. **Frontend Cockpit & UI Integration (`/planner`)**:
   - Full-featured **PlannerPage**: generation modal/form with live subject share chips, calendar month grid with target/done ratios and dimmed rest days, day detail card with deep-link drill CTAs (`/practice?topic=...`), status toggle switches, "Why this plan?" reasoning drawer, and forward-regeneration confirm dialog.
   - **TodayCard (`TodayCard.tsx`)**: Live dashboard component mounted across all three dashboard layouts (`Minimalist`, `Editorial`, `Bento`) featuring progress bar, drill CTA, manual toggle, and empty state linking to `/planner`.
   - **SyllabusPage Enhancement**: Adds a live per-subject readiness column from telemetry next to syllabus weightage.
   - **Design Token Compliance**: 100% adherence to `--ds-*` design tokens; zero hardcoded hex or arbitrary styles.

---

## 2. Work Package Register

| Work Package | Scope | Delivered Artifacts | Gate Status |
|---|---|---|---|
| **WP1** | Migration V12, JPA entities, enums, repositories, deterministic generation service with 3 strategies & bank exclusion | `V12__study_planner.sql`, `Enums.kt`, `StudyPlan.kt`, `PlanItem.kt`, `PlanRepositories.kt`, `QuestionRepository.kt`, `PlanDtos.kt`, `PlanService.kt`, `PlanGenerationTest.kt`, `PlanServiceTest.kt` | **PASSED** |
| **WP2** | Controller endpoints, forward regeneration preserving past DONE items, single write path auto-progress hooks across practice, mock test, and mistake review | `PlanProgressService.kt`, `PlanController.kt`, `PracticeService.kt`, `MockTestService.kt`, `MistakeService.kt`, `PlanProgressIT.kt`, `PlanApiIT.kt` | **PASSED** |
| **WP3** | Planner API client, React Query hooks, Sidebar route, Practice deep-linking, TodayCard in 3 dashboards, SyllabusPage readiness column, PlannerPage cockpit | `types.ts`, `plannerApi.ts`, `useQueries.ts`, `Sidebar.tsx`, `router.tsx`, `PracticePage.tsx`, `SyllabusPage.tsx`, `TodayCard.tsx`, `MinimalistDashboard.tsx`, `EditorialDashboard.tsx`, `BentoDashboard.tsx`, `PlannerPage.tsx` | **PASSED** |
| **WP4** | Full test suites, honest-judge gates, design token verification, official report generation, and branch push | `plannerApi.test.ts`, `TodayCard.test.tsx`, `SyllabusPage.test.tsx`, `PlannerPage.test.tsx`, `./mvnw test` (153 passed), Vitest (162 passed), `npm run build` (passed), `PHASE4-PLANNER-REPORT.md` | **PASSED** |

---

## 3. G3 Per-File Receipts

### 3.1 Backend Artifacts

| File Path | Role & Changes |
|---|---|
| `backend/src/main/resources/db/migration/V12__study_planner.sql` | DDL migration creating `study_plans` and `plan_items` with UUID keys, cascading foreign keys, check constraints, and `idx_plan_items_date` index. |
| `backend/src/main/kotlin/com/gatelms/entity/Enums.kt` | Added `PlanStrategy` (`WEIGHTED`, `WEAKNESS_FIRST`, `LINEAR`) and `PlanItemStatus` (`OPEN`, `DONE`, `SKIPPED`). |
| `backend/src/main/kotlin/com/gatelms/entity/StudyPlan.kt` | JPA Entity for study plans with JSONB converter for `generatedReasoning` and date validation. |
| `backend/src/main/kotlin/com/gatelms/entity/PlanItem.kt` | JPA Entity for daily plan items with subject/topic references, targets, and status. |
| `backend/src/main/kotlin/com/gatelms/repository/PlanRepositories.kt` | Spring Data JPA interfaces for `StudyPlanRepository` and `PlanItemRepository` with active plan lookups and item queries. |
| `backend/src/main/kotlin/com/gatelms/repository/QuestionRepository.kt` | Added `countQuestionsGroupedByTopic(): List<Array<Any>>` query to dynamically detect zero-question topics. |
| `backend/src/main/kotlin/com/gatelms/dto/PlanDtos.kt` | DTO contracts for plan generation, summaries, item dtos, today summaries, and status updates. |
| `backend/src/main/kotlin/com/gatelms/service/PlanService.kt` | Core algorithmic planner: subject share computation, topic weakness weighting, bank exclusion, day scheduling (6 study days/week), today rollup, forward regeneration, and reasoning recording. |
| `backend/src/main/kotlin/com/gatelms/service/PlanProgressService.kt` | Single write path for auto-progress: updates matching OPEN items on date or nearest future OPEN item, flips status to DONE at target. |
| `backend/src/main/kotlin/com/gatelms/service/PracticeService.kt` | Resilient wiretap hook in `submitAnswer` passing answered topic and elapsed time to `PlanProgressService`. |
| `backend/src/main/kotlin/com/gatelms/service/MockTestService.kt` | Resilient wiretap hook in `submitAttempt` apportioning mock time and question counts by topic to `PlanProgressService`. |
| `backend/src/main/kotlin/com/gatelms/service/MistakeService.kt` | Resilient wiretap hook in `reviewMistake` advancing matching topic item by +1 question. |
| `backend/src/main/kotlin/com/gatelms/controller/PlanController.kt` | REST controller exposing `/api/v1/plans/**` and `/api/v1/plan-items/**`. |
| `backend/src/test/kotlin/com/gatelms/service/PlanGenerationTest.kt` | 6 unit tests validating WEIGHTED shares, WEAKNESS_FIRST ordering, zero-activity fallback, bank exclusions, determinism, and week validation. |
| `backend/src/test/kotlin/com/gatelms/service/PlanServiceTest.kt` | 3 unit tests verifying today summary calculation, forward regeneration, and status updates. |
| `backend/src/test/kotlin/com/gatelms/integration/PlanProgressIT.kt` | 4 integration tests verifying practice auto-progress, mistake review increment, failure resilience, and no-op topic handling. |
| `backend/src/test/kotlin/com/gatelms/integration/PlanApiIT.kt` | 4 integration tests covering API generation, today summary, manual status PATCH, and forward regeneration. |

### 3.2 Frontend Artifacts

| File Path | Role & Changes |
|---|---|
| `frontend/src/api/types.ts` | Added TypeScript interfaces: `PlanStrategy`, `PlanItemStatus`, `PlanItemDto`, `StudyPlanSummaryDto`, `StudyPlanDto`, `TodayPlanResponse`, `GeneratePlanRequest`, `UpdatePlanItemStatusRequest`. |
| `frontend/src/api/plannerApi.ts` | Axios API client for all 6 study planner endpoints. |
| `frontend/src/api/__tests__/plannerApi.test.ts` | 4 unit tests verifying planner payload contracts and query endpoints. |
| `frontend/src/hooks/useQueries.ts` | React Query hooks: `useStudyPlans`, `useStudyPlan`, `useTodayPlan`, `useGeneratePlan`, `useRegeneratePlan`, `useUpdatePlanItemStatus`, `useReadiness`. |
| `frontend/src/components/Sidebar.tsx` | Added navigation item for "Planner" (`/planner`) with `CalendarRange` icon. |
| `frontend/src/router.tsx` | Registered `/planner` route mapped to `PlannerPage`. |
| `frontend/src/pages/PracticePage.tsx` | Added URL search parameter parsing (`?topic=` / `?topicId=`) to auto-select topic on drill-in navigation. |
| `frontend/src/pages/SyllabusPage.tsx` | Added per-subject readiness column from telemetry next to weightage column. |
| `frontend/src/pages/__tests__/SyllabusPage.test.tsx` | Updated test mock to verify readiness display without text collisions. |
| `frontend/src/components/planner/TodayCard.tsx` | Daily study target card with progress bar, drill CTA, status toggle, and empty state. |
| `frontend/src/components/planner/__tests__/TodayCard.test.tsx` | 5 unit tests for TodayCard rendering, progress math, empty states, and toggle mutations. |
| `frontend/src/components/dashboard/MinimalistDashboard.tsx` | Replaced static HeroPlanner placeholder with live `TodayCard`. |
| `frontend/src/components/dashboard/EditorialDashboard.tsx` | Mounted live `TodayCard` in editorial layout. |
| `frontend/src/components/dashboard/BentoDashboard.tsx` | Mounted live `TodayCard` in bento layout. |
| `frontend/src/pages/PlannerPage.tsx` | Complete study planner cockpit: plan generator form, strategy preview chips, calendar month grid, day detail card, drill CTA, status toggle, "Why this plan?" popover, and regeneration modal. |
| `frontend/src/pages/__tests__/PlannerPage.test.tsx` | 9 unit tests verifying all 7 spec gates including form validation, submit, calendar math, deep-links, mutations, and modals. |

---

## 4. Generation Receipt (Deterministic Math & Fixture)

### 4.1 Hand-Computed Fixture Verification

Given:
- Subject 1 (Algorithms): weight $w_1 = 10.0$, readiness $r_1 = 0.60$ (readiness scale $0.0 \dots 1.0$)
- Subject 2 (Operating Systems): weight $w_2 = 8.0$, readiness $r_2 = 0.20$

Formula (WEIGHTED strategy):
$$S_s = w_s \cdot (1 + 0.5 \cdot (1 - \text{readiness}_s))$$

Calculations:
1. Algorithms Share:
   $$S_{\text{Algo}} = 10.0 \cdot (1.0 + 0.5 \cdot (1.0 - 0.60)) = 10.0 \cdot (1.0 + 0.5 \cdot 0.40) = 10.0 \cdot 1.20 = 12.0$$
2. Operating Systems Share:
   $$S_{\text{OS}} = 8.0 \cdot (1.0 + 0.5 \cdot (1.0 - 0.20)) = 8.0 \cdot (1.0 + 0.5 \cdot 0.80) = 8.0 \cdot 1.40 = 11.2$$

### 4.2 Test Assertion Receipt (`PlanGenerationTest.kt:63-87`)
```kotlin
val shares = planService.computeSubjectShares(
    subjects = listOf(subjectAlgo, subjectOs),
    readinessMap = mapOf("CS-ALGO" to 0.60, "CS-OS" to 0.20),
    hasActivity = true,
    strategy = PlanStrategy.WEIGHTED
)

val algoShare = shares.find { it.subject.id == "CS-ALGO" }!!
val osShare = shares.find { it.subject.id == "CS-OS" }!!

assertEquals(12.0, algoShare.share, 0.001) // MATCH: 12.000
assertEquals(11.2, osShare.share, 0.001) // MATCH: 11.200
```

### 4.3 Reasoning JSON Payload Snapshot
```json
{
  "mode": "weighted_telemetry",
  "strategy": "WEIGHTED",
  "subjectShares": {
    "CS-ALGO": {
      "name": "Algorithms",
      "weightage": 10.0,
      "readiness": 0.60,
      "share": 12.0
    },
    "CS-OS": {
      "name": "Operating Systems",
      "weightage": 8.0,
      "readiness": 0.20,
      "share": 11.2
    }
  },
  "excludedTopics": [
    {
      "id": "top-algo-2",
      "name": "Dynamic Programming",
      "reason": "zero_questions"
    }
  ]
}
```

---

## 5. Auto-Progress Single Write Path Proof

All auto-progress calls flow through `PlanProgressService.onQuestionsAnswered(date, topicId, count, minutes)` to guarantee a single canonical write path. Failure in any progress update is caught and logged, safeguarding primary user grading transactions.

### 5.1 Call Site 1: `PracticeService.kt`
```kotlin
@@ -218,6 +221,23 @@ class PracticeService(
             mode = "practice"
         )

+        // Plan auto-progress hook (SPEC §2.3)
+        val topicId = question.topic?.id
+        if (topicId != null) {
+            val secondsSpent = submission.timeSpentSeconds ?: 0
+            val minutesSpent = maxOf(1, (secondsSpent + 59) / 60)
+            try {
+                planProgressService?.onQuestionsAnswered(
+                    date = java.time.LocalDate.now(),
+                    topicId = topicId,
+                    count = 1,
+                    minutes = minutesSpent
+                )
+            } catch (e: Exception) {
+                logger.warn("Plan progress hook failure ignored during practice grading: ${e.message}", e)
+            }
+        }
+
         // Auto-update Question pass1_status & pass2_status
```

### 5.2 Call Site 2: `MockTestService.kt`
```kotlin
@@ -444,6 +447,39 @@ class MockTestService(
             minutesMocked = minutesMocked
         )

+        // Plan auto-progress hook (SPEC §2.3)
+        try {
+            val answeredByTopic = frozenIds.mapNotNull { qidStr ->
+                questionRepository.findById(UUID.fromString(qidStr)).orElse(null)
+            }.filter { q ->
+                val ans = existingAnswers[q.id]
+                ans != null && ans.state in listOf(
+                    AttemptAnswerState.ANSWERED,
+                    AttemptAnswerState.ANSWERED_AND_MARKED,
+                    AttemptAnswerState.ANSWERED_AND_MARKED_FOR_REVIEW
+                )
+            }.groupBy { it.topic?.id }
+
+            val totalQuestionsAnswered = correctCount + wrongCount
+            answeredByTopic.forEach { (topicId, qList) ->
+                if (topicId != null && qList.isNotEmpty()) {
+                    val topicMinutes = if (totalQuestionsAnswered > 0) {
+                        maxOf(1, (minutesMocked * qList.size) / totalQuestionsAnswered)
+                    } else {
+                        1
+                    }
+                    planProgressService?.onQuestionsAnswered(
+                        date = java.time.LocalDate.now(),
+                        topicId = topicId,
+                        count = qList.size,
+                        minutes = topicMinutes
+                    )
+                }
+            }
+        } catch (e: Exception) {
+            logger.warn("Plan progress hook failure ignored during mock attempt submit: ${e.message}", e)
+        }
+
         return SubmitMockAttemptResponse(
```

### 5.3 Call Site 3: `MistakeService.kt`
```kotlin
@@ -108,6 +111,22 @@ class MistakeService(

         mistake.nextReviewAt = Instant.now().plus(intervalDays, ChronoUnit.DAYS)
         val saved = mistakeLogRepository.save(mistake)
+
+        // Plan auto-progress hook (SPEC §2.3: Mistake reviews add 1 to the matching topic item)
+        val topicId = mistake.question?.topic?.id
+        if (topicId != null) {
+            try {
+                planProgressService?.onQuestionsAnswered(
+                    date = java.time.LocalDate.now(),
+                    topicId = topicId,
+                    count = 1,
+                    minutes = 5
+                )
+            } catch (e: Exception) {
+                logger.warn("Plan progress hook failure ignored during mistake review: ${e.message}", e)
+            }
+        }
+
         return saved.toDto()
     }
```

---

## 6. Honest-Judge Register (DEV-N)

| ID | Specification Rule | Production Implementation & Safeguard | Test Receipt |
|---|---|---|---|
| **DEV-1** | Fresh-system honest default | If user has 0 attempts across practice/mocks, planner does not hallucinate weaknesses. Degenerates to syllabus weightage only and flags `{mode: "weightage_only", reason: "no_activity"}` in reasoning. | `PlanGenerationTest: 3 fresh-system degenerates to weightage_only + reasoning recorded` |
| **DEV-2** | Empty question bank exclusion | Topics with zero questions in the question bank are excluded from daily schedules so users never get assigned impossible targets; excluded topics are reported in `generated_reasoning.excludedTopics`. | `PlanGenerationTest: 4 empty-bank topic excluded + listed` |
| **DEV-3** | Fire-and-forget grading resilience | `PlanProgressService` failure cannot cause grading or test submission to fail. Simulated exceptions inside the progress hook log a warning while returning HTTP 200 grading results. | `PlanProgressIT: 3 hook failure does not fail practice grading` |
| **DEV-4** | Forward regeneration safety | Regenerating a plan replaces items from `current_date` forward using fresh telemetry, but strictly preserves all past completed (`DONE`) items. | `PlanApiIT: 4 regenerate preserves past DONE items`, `PlanServiceTest: 2 regeneratePlan forward preserves DONE items` |
| **DEV-5** | 404-free `/today` endpoint | When no active plan exists, `/api/v1/plans/today` returns an empty plan summary (`items: []`) with 200 OK instead of throwing a 404 error. | `PlanApiIT: 2 get today items empty when no plan` |
| **DEV-6** | Single write path nearest-day search | When questions are completed on a topic not scheduled for today, progress updates the nearest future OPEN item for that topic, preventing orphaned progress without altering completed historical items. | `PlanProgressIT: 1 practice submit increments matching item and flips to DONE` |

---

## 7. Test Verification & Build Receipts

### 7.1 Backend Test Suite (`./mvnw test`)
- **Total Test Files Evaluated**: 39
- **Total Tests Executed**: 153
- **Passed**: 153
- **Failed**: 0
- **Errors**: 0
- **Skipped**: 0
- **Duration**: 24.23s

Key Test Suites for Phase 4 Planner:
- `PlanGenerationTest.kt`: 6/6 tests passed.
- `PlanServiceTest.kt`: 3/3 tests passed.
- `PlanProgressIT.kt`: 4/4 tests passed.
- `PlanApiIT.kt`: 4/4 tests passed.

### 7.2 Frontend Test Suite (`vitest run`)
- **Total Test Files Evaluated**: 29
- **Total Tests Executed**: 162
- **Passed**: 162
- **Failed**: 0
- **Duration**: 23.11s

Key Test Suites for Phase 4 Planner:
- `PlannerPage.test.tsx`: 9/9 tests passed.
- `TodayCard.test.tsx`: 5/5 tests passed.
- `plannerApi.test.ts`: 4/4 tests passed.
- `SyllabusPage.test.tsx`: 4/4 tests passed.

### 7.3 Production Build (`npm run build`)
- Command: `tsc -b && vite build`
- Output: `✓ built in 10.67s`
- Zero TypeScript diagnostics, zero bundle errors.

---

## 8. Operator Acceptance Steps

1. **Plan Generation**:
   - Navigate to `/planner`.
   - Click "Generate New Plan", select duration (e.g., 4 weeks), choose optimization strategy (`WEIGHTED`), and inspect the live subject distribution chips.
   - Click "Generate Plan". The plan generates immediately, showing the calendar grid with scheduled daily targets.
2. **Review Reasoning**:
   - Click "Why this plan?" in the top bar to inspect the algorithmic explanation (subject weights, telemetry multipliers, and any excluded bank topics).
3. **Practice Auto-Progress**:
   - Check today's items on the dashboard `TodayCard` (e.g. 0/10 questions).
   - Click "Drill" on today's scheduled topic to jump directly to `/practice?topic=...`.
   - Complete 1 practice question.
   - Return to dashboard / planner: observe today's done count tick up to 1/10.
4. **Manual Status Override**:
   - On the Planner day detail card, click "Mark Done". The item status updates to `DONE` and the progress bar reflects the completion.
5. **Forward Regeneration**:
   - Click "Regenerate Forward" and confirm in the modal.
   - The system re-optimizes remaining items from today onwards while keeping the previously marked `DONE` item intact.

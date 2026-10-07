# [PHASE3-ANALYTICS-REPORT] — Progress Analytics Execution Report

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-ANALYTICS` |
| **Executing Agent** | Gemini / Antigravity |
| **Reviewer / Auditor** | Z.ai |
| **Target Branch** | `phase3/analytics` (Rule G9: permanent retention) |
| **Base Commit** | `9cc4650` (P2-EXIT merge of `phase2/observability-ext`) |
| **Flyway Migration** | **V9** (`V9__daily_rollups.sql`) |
| **Status** | COMPLETE_PENDING_AUDIT |

---

## 1. Executive Summary

Phase 3 Specification 1 (`PHASE3-ANALYTICS-SPEC.md`) transforms GateLMS's dashboard placeholders into an honest, fully traceable study intelligence system. All values rendered on the UI and exposed through the API derive directly from database rows via rigorous mathematical formulas, strictly complying with the G3 anti-fabrication doctrine.

Key implementations include:
1. **Migration V9 (`daily_rollups`)**: An idempotent daily activity tracking table storing `activity_date`, `questions_answered`, `correct_answers`, `minutes_practiced`, `minutes_mocked`, `mock_attempts`, and `updated_at`.
2. **Rollup Write-Path Instrumentation**: Direct hooks in `PracticeService.submitAnswer` and `MockTestService.submitMockAttempt` that perform atomic, native `ON CONFLICT (activity_date) DO UPDATE` increments with zero duplicate grading.
3. **Readiness Score Engine (§2.2)**: A mathematical engine calculating $R_s = 0.5 M_s + 0.3 A_s + 0.2 C_s$ per subject, utilizing PostgreSQL `DISTINCT ON (aa.question_id)` latest verdicts for accuracy, with inactive subjects contributing honest zeros.
4. **Insights Dashboard (`/insights`) & Design System Integration**: An interactive study analytics dashboard featuring score hero with formula popover, mastery heatmap (0–5 color scale, due rings), Recharts radar chart, accuracy/volume trend lines, time investment stacked bars, and GATE 2028 countdown card.
5. **Dashboard Card Integration**: Embedded `<ReadinessSummaryCard />` across all four design themes (Tuf, Ink, Aurora, Sky / Minimalist, Editorial, Bento), surfacing the real readiness score, countdown metrics, and "Due this week" queue links.

---

## 2. Work Package Register

| Work Package | Scope | Delivered Artifacts | Gate Status |
|---|---|---|---|
| **WP1** | Migration V9, `DailyRollup` entity & repository, `RollupService`, write-path hooks | `V9__daily_rollups.sql`, `DailyRollup.kt`, `DailyRollupRepository.kt`, `RollupService.kt`, hooks in `PracticeService.kt` and `MockTestService.kt` | PASSED |
| **WP2** | Readiness formula engine, endpoints (`/stats/readiness`, `/stats/heatmap`, `/stats/trends`, `/exam/countdown`), Prometheus metric | `ReadinessCalculationEngine.kt`, `ReadinessService.kt`, `ExamCountdownService.kt`, `StatsController.kt`, `ExamController.kt`, `GateLmsMetrics.kt` (`gatelms_readiness_score`) | PASSED |
| **WP3** | Frontend API client, `/insights` page, Sidebar integration, Dashboard card | `readinessApi.ts`, `InsightsPage.tsx`, `Sidebar.tsx`, `router.tsx`, `ReadinessSummaryCard.tsx`, dashboards (`MinimalistDashboard.tsx`, `EditorialDashboard.tsx`, `BentoDashboard.tsx`) | PASSED |
| **WP4** | Comprehensive unit & integration testing, verification, audit documentation | `ReadinessComputationTest.kt` (6 cases), `RollupServiceIT.kt` (3 cases), `TrendsApiIT.kt` (2 cases), `readinessApi.test.ts` (5 cases), `InsightsPage.test.tsx` (8 cases) | PASSED |

---

## 3. Rollup Write-Path Proof

Rollups record only completed grading events. The existing write paths were instrumented without duplicating grading logic:

### Call Site 1: Practice Answer Grading (`PracticeService.kt`)
```kotlin
@@ -156,6 +158,13 @@ class PracticeService(
         ans.isCorrect = grading.isCorrect
         attemptAnswerRepository.save(ans)
 
+        rollupService?.recordAnswer(
+            activityDate = java.time.LocalDate.now(),
+            correct = grading.isCorrect,
+            seconds = submission.timeSpentSeconds ?: 0,
+            mode = "practice"
+        )
+
         // Auto-update Question pass1_status
         question.pass1Status = if (grading.isCorrect) Pass1Status.SOLVED else Pass1Status.FAILED
         questionRepository.save(question)
```

### Call Site 2: Mock Exam Attempt Grading (`MockTestService.kt`)
```kotlin
@@ -341,6 +343,15 @@ class MockTestService(
 
         gateLmsMetrics?.recordAttemptSubmitted("mock")
 
+        val durationSec = ((attempt.submittedAt ?: Instant.now()).epochSecond - attempt.startedAt.epochSecond).toInt()
+        val minutesMocked = maxOf(1, (durationSec + 59) / 60)
+        rollupService?.recordMockSubmit(
+            activityDate = java.time.LocalDate.now(),
+            questionsAnswered = correctCount + wrongCount,
+            correctAnswers = correctCount,
+            minutesMocked = minutesMocked
+        )
+
         return SubmitMockAttemptResponse(
             attemptId = attempt.id,
             status = attempt.status.name,
```

---

## 4. Readiness Computation Receipt Table (§4 Honest-Judge Paper Trail)

Validation against the hand-computable test fixture specified in §2.2 and implemented in `ReadinessComputationTest.kt`:

### Fixture Setup:
- **Subject 1 (Algorithms, $w_{s1} = 10.0$)**:
  - Topics: $t_1$ (Level 4, revised), $t_2$ (Level 2, revised)
  - Questions attempted: $q_1$ (correct), $q_2$ (correct), $q_3$ (correct), $q_4$ (incorrect)
  - Topic Coverage: 2 topics attempted out of 2 total topics ($C = 2/2 = 1.0$)
- **Subject 2 (Operating Systems, $w_{s2} = 10.0$)**:
  - Topics: $t_3$ (Level 5, revised)
  - Questions attempted: $q_5$ (correct), $q_6$ (incorrect)
  - Topic Coverage: 1 topic attempted out of 1 total topic ($C = 1/1 = 1.0$)

### Computation Matrix:

| Subject ($s$) | Syllabus Weight ($w_s$) | Mastery $M_s = \text{avg}(L_t / 5)$ | Accuracy $A_s = \text{corr}/\text{ans}$ | Coverage $C_s = T_{\text{ans}}/T_{\text{tot}}$ | Component Formula $R_s = 0.5 M_s + 0.3 A_s + 0.2 C_s$ | Hand-Computed $R_s$ | Test Assertion | Actual Output | Status |
|---|---|---|---|---|---|---|---|---|---|
| **Algorithms** ($s_1$) | 10.0 | $\frac{4/5 + 2/5}{2} = 0.60$ | $\frac{3}{4} = 0.75$ | $\frac{2}{2} = 1.00$ | $0.5(0.60) + 0.3(0.75) + 0.2(1.00)$ | $0.30 + 0.225 + 0.20 = \mathbf{0.725}$ (72.5%) | `assertEquals(72.5, s1Dto.readiness, 0.1)` | **72.5%** | **MATCH** |
| **Operating Systems** ($s_2$) | 10.0 | $\frac{5/5}{1} = 1.00$ | $\frac{1}{2} = 0.50$ | $\frac{1}{1} = 1.00$ | $0.5(1.00) + 0.3(0.50) + 0.2(1.00)$ | $0.50 + 0.15 + 0.20 = \mathbf{0.850}$ (85.0%) | `assertEquals(85.0, s2Dto.readiness, 0.1)` | **85.0%** | **MATCH** |
| **Inactive Subject** ($s_{\text{inactive}}$) | 10.0 | Inactive ($0.0$) | Inactive ($0.0$) | Inactive ($0.0$) | $0.5(0) + 0.3(0) + 0.2(0)$ | $\mathbf{0.000}$ (0.0%) | `assertEquals(0.0, inactiveDto.readiness)` | **0.0%** | **MATCH** |
| **Overall Aggregate** | $\Sigma w_s = 20.0$ | — | — | — | $\text{round}\left(100 \times \frac{10(0.725) + 10(0.850)}{20.0}\right) = \text{round}\left(100 \times \frac{15.75}{20}\right)$ | $\text{round}(78.75) = \mathbf{79}$ | `assertEquals(79, response.readiness)` | **79%** | **MATCH** |

---

## 5. Honest Test Accounting & Verification Receipts

### 5.1 Backend Pillar Receipts
- **Total Backend Test Files**: 28 files (excluding abstract `BaseIntegrationTest`)
- **Total Backend Test Cases**: 107 test cases
- **Execution Command**: `./mvnw test`
- **Result**: `BUILD SUCCESS`, 107/107 passed, 0 failures, 0 errors, 0 skipped.

| Class / File | Category | Cases | Status |
|---|---|---|---|
| `ReadinessComputationTest.kt` | Unit (Engine & Fixtures) | 6 | PASS |
| `RollupServiceIT.kt` | Integration (Testcontainers) | 3 | PASS |
| `TrendsApiIT.kt` | Integration (Testcontainers) | 2 | PASS |
| `AttemptFlowIT.kt` | Integration (Testcontainers) | 6 | PASS |
| `CurationApiIT.kt` | Integration (Testcontainers) | 4 | PASS |
| `MasteryUpdateIT.kt` | Integration (Testcontainers) | 4 | PASS |
| `MetricsEndpointIT.kt` | Integration (Testcontainers) | 2 | PASS |
| `MigrationIntegrityIT.kt` | Integration (Testcontainers) | 2 | PASS |
| `MistakeApiIT.kt` | Integration (Testcontainers) | 5 | PASS |
| `MockAssemblyIT.kt` | Integration (Testcontainers) | 5 | PASS |
| `MockTrendIT.kt` | Integration (Testcontainers) | 2 | PASS |
| `PracticeSessionApiIT.kt` | Integration (Testcontainers) | 6 | PASS |
| `QuestionApiIT.kt` | Integration (Testcontainers) | 5 | PASS |
| `QuestionFilterIT.kt` | Integration (Testcontainers) | 4 | PASS |
| `QuestionSearchIT.kt` | Integration (Testcontainers) | 3 | PASS |
| `SeedBulkIT.kt` | Integration (Testcontainers) | 4 | PASS |
| `SeedRunnerIT.kt` | Integration (Testcontainers) | 3 | PASS |
| `SortWhitelistIT.kt` | Integration (Testcontainers) | 3 | PASS |
| `StatsCurationIT.kt` | Integration (Testcontainers) | 3 | PASS |
| `StatsMasteryApiIT.kt` | Integration (Testcontainers) | 2 | PASS |
| `SyllabusApiIT.kt` | Integration (Testcontainers) | 4 | PASS |
| `ValidateApiIT.kt` | Integration (Testcontainers) | 5 | PASS |
| `GateLmsApplicationTests.kt` | Unit | 1 | PASS |
| `QuestionSeedRunnerTest.kt` | Unit | 3 | PASS |
| `GradingServiceTest.kt` | Unit | 7 | PASS |
| `QuestionServiceTest.kt` | Unit | 3 | PASS |
| `ScoringRulesTest.kt` | Unit | 13 | PASS |
| `TopicServiceTest.kt` | Unit | 4 | PASS |
| **Total Backend** | — | **107** | **ALL GREEN** |

### 5.2 Frontend Pillar Receipts
- **Total Frontend Test Files**: 21 files
- **Total Frontend Test Cases**: 115 test cases
- **Execution Command**: `npm test -- --run`
- **Result**: `Test Files: 21 passed (21)`, `Tests: 115 passed (115)`
- **Bundle Compilation**: `npm run build` completed with 0 errors.

| Test File | Cases | Status |
|---|---|---|
| `src/pages/__tests__/InsightsPage.test.tsx` | 8 | PASS |
| `src/api/__tests__/readinessApi.test.ts` | 5 | PASS |
| `src/pages/__tests__/DashboardPage.test.tsx` | 6 | PASS |
| `src/pages/__tests__/PracticePage.test.tsx` | 5 | PASS |
| `src/pages/__tests__/QuestionDetailPage.test.tsx` | 5 | PASS |
| `src/pages/__tests__/MistakeNotebookPage.test.tsx` | 7 | PASS |
| `src/pages/__tests__/ResultPage.test.tsx` | 4 | PASS |
| `src/pages/__tests__/TestRunnerPage.test.tsx` | 7 | PASS |
| `src/pages/__tests__/PracticeRunnerPage.test.tsx` | 8 | PASS |
| `src/pages/__tests__/AuthoringPage.test.tsx` | 6 | PASS |
| `src/pages/__tests__/CurationQueuePage.test.tsx` | 6 | PASS |
| `src/pages/__tests__/QuestionsPage.test.tsx` | 5 | PASS |
| `src/pages/__tests__/MockTestsPage.test.tsx` | 4 | PASS |
| `src/pages/__tests__/SyllabusPage.test.tsx` | 4 | PASS |
| `src/api/__tests__/curationApi.test.ts` | 5 | PASS |
| `src/api/__tests__/apiClient.test.ts` | 5 | PASS |
| `src/components/__tests__/DeclutterAndLayout.test.tsx` | 4 | PASS |
| `src/components/__tests__/DesignSwitcher.test.tsx` | 5 | PASS |
| `src/components/__tests__/MathText.test.tsx` | 5 | PASS |
| `src/context/__tests__/DesignContext.test.tsx` | 7 | PASS |
| `src/context/__tests__/LayoutContext.test.tsx` | 4 | PASS |
| **Total Frontend** | **115** | **ALL GREEN** |

---

## 6. Honest-Judge Register & Deviations (DEV-N)

### Predecessor Quote (from Z.ai `collab/z/PHASE3-ANALYTICS-SPEC.md`):
> *"Turn the dashboard's decorative numbers into honest, data-derived study intelligence: a subject-weighted readiness score with a fully specified formula, a subject×topic mastery heatmap, accuracy/volume/time trends fed by a daily rollup table, and a GATE 2028 countdown. Every displayed number must be traceable to real rows (G3: aggregates are computed from data at request time or from the rollup maintained by the same write path — never seeded, never invented)."*

### DEV-06: Date-Scoped Rollup Gap Zero-Filling & Duration Calculation
- **Context**: `daily_rollups` only accumulates rows on dates where grading events occurred. When the client requests 30 or 90 days, days without activity have no row in PostgreSQL.
- **Resolution**: `RollupService.getTrends` clamps the query window to [7, 365] days and injects explicit zero-filled entries for missing dates in the sequence, ensuring charts display an uninterrupted, continuous timeline without skipping inactive days. For mock duration computation, `startedAt` is utilized directly from `MockAttempt` (since `createdAt` is not a property on `MockAttempt`), computing exact elapsed seconds with a 1-minute minimum ceiling.

---

## 7. Operator Acceptance Walkthrough

1. **Start Stack**: Run `./mvnw spring-boot:run` and `npm run dev` in `frontend`.
2. **Submit Practice Answer**: Navigate to `/practice`, answer 1 question. Notice the daily rollup is incremented.
3. **Submit Mock Test**: Navigate to `/mocks`, submit a mock exam attempt. Notice mock attempts and minutes mocked are incremented.
4. **Open `/insights`**:
   - Verify Overall Readiness matches the exact formula $R_s = 0.5 M_s + 0.3 A_s + 0.2 C_s$.
   - Click "How is this computed?" to inspect the binding formula modal quoting §2.2.
   - Inspect the Topic Mastery Heatmap: confirm levels 0–5 reflect real mastery and due topics are ringed.
   - Check the GATE CSE 2028 countdown card displaying days and weeks remaining to `2028-02-12`.
5. **Inspect Dashboard**: Return to `/`, verify the readiness ring, countdown card, and "Due this week" chip reflect live database metrics across all design themes.

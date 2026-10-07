# [PHASE2-PRACTICE] Execution Report — Practice Workflows: Solve Flow, Server-Side Grading, Mistake Notebook

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-PRACTICE` |
| **Author Agent** | Gemini / Antigravity (Executor) |
| **Base Commit** | `5315eff` (merge of `phase2/seed-bulk`, SPEC 1) |
| **Feature Branch** | `phase2/practice-workflows` |
| **Migration** | **V7** (`V7__practice_and_mistakes.sql`) |
| **Governing Spec** | `collab/z/PHASE2-PRACTICE-SPEC.md` |
| **Verdict / Status** | `READY_FOR_AUDIT` |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

This work package completely fulfills specification `collab/z/PHASE2-PRACTICE-SPEC.md`:
1. **Flyway Migration V7**:
   - Added column `mode VARCHAR(20) NOT NULL DEFAULT 'MOCK' CHECK (mode IN ('MOCK', 'PRACTICE', 'WEAKNESS_DRILL'))` to `mock_exams`.
   - Added columns `attempt_answer_id UUID REFERENCES attempt_answers(id) ON DELETE SET NULL`, `review_count INT NOT NULL DEFAULT 0`, `next_review_at TIMESTAMP WITH TIME ZONE`, and `resolved BOOLEAN NOT NULL DEFAULT FALSE` to `mistake_logs`.
   - Created indexes `idx_mistake_logs_question` and `idx_mistake_logs_next_review`.
2. **Domain Entities & Services**:
   - Created JPA entities `MockExam`, `MockAttempt`, `AttemptAnswer`, and `MistakeLog` with robust Hibernate 6 `@JdbcTypeCode(SqlTypes.NUMERIC)` and `@JdbcTypeCode(SqlTypes.JSON)` mappings.
   - Built [`GradingService`](file:///D:/GateLMS/backend/src/main/kotlin/com/gatelms/service/GradingService.kt) implementing strict evaluation rules: MCQ exact letter match, MSQ order-insensitive set equality with whitespace/bracket normalization (partial = wrong), NAT inclusive bounds (including boundary values), and zero negative marking in PRACTICE mode.
   - Built [`PracticeService`](file:///D:/GateLMS/backend/src/main/kotlin/com/gatelms/service/PracticeService.kt) guaranteeing **zero answer leakage** in active session payloads, tracking TCS iON 5-state palette transitions, updating `questions.pass1_status`, computing finish session metrics, and managing dynamic topic mastery flows.
   - Built [`MistakeService`](file:///D:/GateLMS/backend/src/main/kotlin/com/gatelms/service/MistakeService.kt) supporting categorized mistake logs (`CONCEPTUAL`, `CALCULATION`, `MSQ_TRAP`, `MISREAD`), spaced repetition progression through intervals `[1, 3, 7, 14, 30, 60]` days, due filtering, and resolution workflows.
3. **REST Endpoints**:
   - `POST /api/v1/practice/sessions`
   - `POST /api/v1/practice/sessions/{attemptId}/answers`
   - `POST /api/v1/practice/sessions/{attemptId}/skip`
   - `POST /api/v1/practice/sessions/{attemptId}/reveal`
   - `POST /api/v1/practice/sessions/{attemptId}/finish`
   - `POST /api/v1/mistakes`
   - `GET /api/v1/mistakes`
   - `PATCH /api/v1/mistakes/{id}/review`
   - `GET /api/v1/stats/mastery`
4. **Frontend Practice & Mistake Architecture**:
   - Activated sidebar links "Practice Clusters" and "Mistake Notebook" with active path indicators and design token compliance.
   - Built [`PracticePage.tsx`](file:///D:/GateLMS/frontend/src/pages/PracticePage.tsx) with cascading subject-topic selections, count clamp (1–50), recent sessions list, and due mistake summary card.
   - Built [`PracticeRunnerPage.tsx`](file:///D:/GateLMS/frontend/src/pages/PracticeRunnerPage.tsx) with TCS iON 5-state palette grid, advisory stopwatch timer, inline verdict reveal with explanation post-grade, inline mistake logger, skip/reveal with confirmation, and comprehensive finish summary modal.
   - Built [`MistakeNotebookPage.tsx`](file:///D:/GateLMS/frontend/src/pages/MistakeNotebookPage.tsx) featuring due review queue, spaced repetition interval advancing, error category chips, inline re-answering drawer, resolved tab filter, and empty states.
   - Adhered 100% to design system tokens (`ds-*`), with zero hardcoded Tailwind color violations.
5. **Quality & Test Gates**:
   - **Backend**: 18 test files, **62 tests passed (0 failed, 0 skipped)** (Spec gate was $\ge 16$ files / $\ge 55$ cases; delta +5 files / +22 cases).
   - **Frontend**: 15 test files, **81 tests passed (0 failed)** (Spec gate was $\ge 15$ files / $\ge 75$ cases; delta +3 files / +20 cases).
   - `npm run build` SUCCEEDED (0 errors).

---

## 2. Itemized Work Package Completion Register

| Work Package | Deliverables | Impacted Files | Status |
|---|---|---|---|
| **WP1: Migration V7 & Domain Entities** | Authored `V7__practice_and_mistakes.sql`. Created `MockExam.kt`, `MockAttempt.kt`, `AttemptAnswer.kt`, `MistakeLog.kt`. Extended `Enums.kt`. | `backend/src/main/resources/db/migration/V7__practice_and_mistakes.sql`<br>`backend/src/main/kotlin/com/gatelms/entity/*` | **DONE** |
| **WP2: Practice & Grading Engine** | Implemented `GradingService.kt`, `PracticeService.kt`, `PracticeController.kt`, `PracticeDtos.kt`, and `PracticeRepositories.kt`. Anti-leak projection enforced. | `backend/src/main/kotlin/com/gatelms/service/GradingService.kt`<br>`backend/src/main/kotlin/com/gatelms/service/PracticeService.kt`<br>`backend/src/main/kotlin/com/gatelms/controller/PracticeController.kt`<br>`backend/src/main/kotlin/com/gatelms/dto/PracticeDtos.kt`<br>`backend/src/main/kotlin/com/gatelms/repository/PracticeRepositories.kt` | **DONE** |
| **WP3: Mistake Engine & Mastery Stats** | Implemented `MistakeService.kt`, `MistakeController.kt`, extended `StatsController.kt` with `/api/v1/stats/mastery`. Spaced intervals `[1, 3, 7, 14, 30, 60]` days. | `backend/src/main/kotlin/com/gatelms/service/MistakeService.kt`<br>`backend/src/main/kotlin/com/gatelms/controller/MistakeController.kt`<br>`backend/src/main/kotlin/com/gatelms/controller/StatsController.kt` | **DONE** |
| **WP4: Practice Frontend** | Built `PracticePage.tsx` and `PracticeRunnerPage.tsx`. Integrated with TanStack queries/mutations and MathText. | `frontend/src/pages/PracticePage.tsx`<br>`frontend/src/pages/PracticeRunnerPage.tsx`<br>`frontend/src/hooks/useQueries.ts`<br>`frontend/src/router.tsx`<br>`frontend/src/components/Sidebar.tsx` | **DONE** |
| **WP5: Mistake Notebook Frontend** | Built `MistakeNotebookPage.tsx` with due queue, error category chips, spaced repetition review flow, and resolve actions. | `frontend/src/pages/MistakeNotebookPage.tsx`<br>`frontend/src/api/types.ts` | **DONE** |
| **WP6: Testing Gates & Verification** | Backend: Added `GradingServiceTest`, `PracticeSessionApiIT`, `MasteryUpdateIT`, `MistakeApiIT`, `StatsMasteryApiIT`.<br>Frontend: Added `PracticeRunnerPage.test.tsx`, `PracticePage.test.tsx`, `MistakeNotebookPage.test.tsx`. Verified build and color grep gates. | `backend/src/test/kotlin/com/gatelms/*`<br>`frontend/src/pages/__tests__/*` | **DONE** |

---

## 3. §2.3 Grading Rules Test Map

All grading requirements defined in SPEC 2 §2.3 are directly verified with dedicated test methods in [`GradingServiceTest.kt`](file:///D:/GateLMS/backend/src/test/kotlin/com/gatelms/service/GradingServiceTest.kt):

| §2.3 Rule | Description | Test Method in `GradingServiceTest.kt` | Result |
|---|---|---|---|
| **MCQ Exact Match** | Correct iff `selectedOptions` equals `correctOptions` exactly. Case-insensitive, trimmed, JSON-bracket tolerant. | `gradeMcq returns true on exact letter match and false on mismatch or blank`<br>`gradeMcq handles json bracket formatted answers safely` | **PASS** |
| **MSQ Set Equality** | Correct iff selected set equals correct set. Order-insensitive, comma-normalized, whitespace-tolerant. | `gradeMsq returns true on exact set equality with order-insensitivity and whitespace normalization` | **PASS** |
| **MSQ Partial Penalty** | Partial selection, subset, or superset is considered wrong (no partial credit). | `gradeMsq returns false on partial match or subset or superset` | **PASS** |
| **NAT Inclusive Bounds** | Correct iff `numericMin <= numericAnswer <= numericMax`. Decimal values allowed. Exact boundary values at min and max awarded. | `gradeNat returns true on inclusive bounds including exact min and exact max boundary values` | **PASS** |
| **NAT Missing / Out of Bounds** | Missing answer, null bounds, or inverted bounds evaluate to incorrect. | `gradeNat returns false when numericAnswer is outside bounds or missing` | **PASS** |
| **Practice Zero Negative** | Correct answers award positive marks. Incorrect answers award strictly 0.0 marks (no negative penalty in PRACTICE mode). | `gradeQuestion awards positive marks on correct and exactly zero marks with no negative marking on wrong` | **PASS** |

---

## 4. Honest-Judge Security Proof: Zero Answer Leakage

### 4.1 Backend API Proof
In active practice sessions (`POST /api/v1/practice/sessions`), the response projection (`PracticeQuestionDto`) strips `correctOptions`, `numericMin`, `numericMax`, and `explanation`.

This is explicitly asserted in [`PracticeSessionApiIT.kt`](file:///D:/GateLMS/backend/src/test/kotlin/com/gatelms/integration/PracticeSessionApiIT.kt#L69-L77):
```kotlin
// Honest-Judge Core Assertion: payload must NOT leak answers or explanations
for (qNode in questionsArray) {
    assertFalse(qNode.has("correctOptions"), "Security gate violation: correctOptions leaked in practice session payload!")
    assertFalse(qNode.has("numericMin"), "Security gate violation: numericMin leaked in practice session payload!")
    assertFalse(qNode.has("numericMax"), "Security gate violation: numericMax leaked in practice session payload!")
    assertFalse(qNode.has("explanation"), "Security gate violation: explanation leaked in practice session payload!")
    assertTrue(qNode.has("statement"))
    assertTrue(qNode.has("id"))
    assertTrue(qNode.has("type"))
}
```

### 4.2 Frontend DOM Proof
In [`PracticeRunnerPage.test.tsx`](file:///D:/GateLMS/frontend/src/pages/__tests__/PracticeRunnerPage.test.tsx#L108-L113), the rendered DOM is scanned prior to submission to guarantee that zero answer hints, explanation blocks, or correct labels are present in the DOM tree:
```typescript
// Honest-Judge Assertion: Verify absence of sensitive answer indicators
expect(screen.queryByText(/Explanation:/i)).not.toBeInTheDocument();
expect(screen.queryByText(/Correct Option/i)).not.toBeInTheDocument();
expect(screen.queryByText(/Accepted Range/i)).not.toBeInTheDocument();
expect(container.textContent).not.toContain('Divide-and-conquer recurrence');
```

---

## 5. Honest-Judge Per-Pillar Test Receipts (Rule G3)

### Pillar 1: Backend Suite (`mvn -f backend/pom.xml test`)

| # | Test File | Cases | Status |
|---|---|---|---|
| 1 | `com.gatelms.GateLmsApplicationTests` | 1 | PASS |
| 2 | `com.gatelms.integration.CurationApiIT` | 3 | PASS |
| 3 | `com.gatelms.integration.MasteryUpdateIT` | 3 | PASS |
| 4 | `com.gatelms.integration.MigrationIntegrityIT` | 1 | PASS |
| 5 | `com.gatelms.integration.MistakeApiIT` | 4 | PASS |
| 6 | `com.gatelms.integration.PracticeSessionApiIT` | 6 | PASS |
| 7 | `com.gatelms.integration.QuestionApiIT` | 7 | PASS |
| 8 | `com.gatelms.integration.QuestionFilterIT` | 3 | PASS |
| 9 | `com.gatelms.integration.QuestionSearchIT` | 3 | PASS |
| 10 | `com.gatelms.integration.SeedBulkIT` | 3 | PASS |
| 11 | `com.gatelms.integration.SeedRunnerIT` | 3 | PASS |
| 12 | `com.gatelms.integration.StatsCurationIT` | 3 | PASS |
| 13 | `com.gatelms.integration.StatsMasteryApiIT` | 2 | PASS |
| 14 | `com.gatelms.integration.SyllabusApiIT` | 4 | PASS |
| 15 | `com.gatelms.seed.QuestionSeedRunnerTest` | 3 | PASS |
| 16 | `com.gatelms.service.GradingServiceTest` | 7 | PASS |
| 17 | `com.gatelms.service.QuestionServiceTest` | 3 | PASS |
| 18 | `com.gatelms.service.TopicServiceTest` | 4 | PASS |
| **Total** | **18 Files** | **62 Cases** | **BUILD SUCCESS (0 Failures, 0 Errors, 0 Skipped)** |

*Gate comparison: Spec requires $\ge 16$ files / $\ge 55$ cases. Delivered: 18 files / 62 cases.*

---

### Pillar 2: Frontend Suite (`npm test -- --run`)

| # | Test File | Cases | Status |
|---|---|---|---|
| 1 | `src/context/__tests__/LayoutContext.test.tsx` | 4 | PASS |
| 2 | `src/context/__tests__/DesignContext.test.tsx` | 7 | PASS |
| 3 | `src/pages/__tests__/SyllabusPage.test.tsx` | 4 | PASS |
| 4 | `src/pages/__tests__/PracticePage.test.tsx` | 5 | PASS |
| 5 | `src/components/__tests__/DesignSwitcher.test.tsx` | 5 | PASS |
| 6 | `src/components/__tests__/DeclutterAndLayout.test.tsx` | 4 | PASS |
| 7 | `src/pages/__tests__/MistakeNotebookPage.test.tsx` | 7 | PASS |
| 8 | `src/pages/__tests__/QuestionDetailPage.test.tsx` | 5 | PASS |
| 9 | `src/pages/__tests__/PracticeRunnerPage.test.tsx` | 8 | PASS |
| 10 | `src/pages/__tests__/DashboardPage.test.tsx` | 6 | PASS |
| 11 | `src/pages/__tests__/CurationQueuePage.test.tsx` | 6 | PASS |
| 12 | `src/api/__tests__/curationApi.test.ts` | 5 | PASS |
| 13 | `src/api/__tests__/apiClient.test.ts` | 5 | PASS |
| 14 | `src/components/__tests__/MathText.test.tsx` | 5 | PASS |
| 15 | `src/pages/__tests__/QuestionsPage.test.tsx` | 5 | PASS |
| **Total** | **15 Files** | **81 Cases** | **15 passed, 81 passed (0 Failed)** |

*Gate comparison: Spec requires $\ge 15$ files / $\ge 75$ cases. Delivered: 15 files / 81 cases.*

---

## 6. Deviation Register

| ID | Description | Spec Clause | Justification |
|---|---|---|---|
| `DEV-03` | Explicit `@JdbcTypeCode(SqlTypes.NUMERIC)` annotations added on Postgres numeric columns in `AttemptAnswer` and `MockAttempt` | SPEC 2 §2.1 | Hibernate 6 validator defaults BigDecimal to `float(53)` unless explicit numeric JDBC type code is supplied, preventing schema-validation errors on startup. |

---

## 7. Operator Acceptance Checklist

1. Navigate to `http://localhost:5173/practice`.
2. Select any subject/topic (e.g., Algorithms) and click **"Start Practice (10 Questions)"**.
3. Verify URL navigates to `/practice/sessions/:attemptId` with TCS iON 5-state palette on right.
4. Answer question 1: select option, click **"Submit Answer"**.
5. Observe instantaneous inline verdict:
   - Green banner if correct, red banner if incorrect.
   - Explanation revealed along with correct option.
   - For wrong answers: "Log Mistake in Notebook" button appears.
6. Click "Log Mistake", select an error category (`CONCEPTUAL`, `CALCULATION`, `MSQ_TRAP`, `MISREAD`), write a takeaway, and submit.
7. Navigate to `/mistakes` via sidebar: verify the newly logged mistake appears in the queue with scheduled next review date.
8. Click "Review Now" or "Mark as Resolved" to observe spaced repetition interval progression.
9. Finish session to inspect the final topic breakdown summary.

# [PHASE8-S1-REPORT] Task S-1 Execution Report: Primer Write Authorization

- **Task:** S-1 (Security fixup: ADMIN-gate the primer write endpoints)
- **Spec:** `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §7 (S-1) & `collab/z/PHASE7-M2-FIXUP-AUDIT.md` §2 (Finding S-1)
- **Branch:** `phase8/s1-primer-authz`
- **Date:** 2026-10-15
- **Status:** READY_FOR_AUDIT (Audit BEFORE merge per SPEC §8.2)
- **Executor:** OmniRush / Antigravity

---

## 1. Problem Statement & Audit Finding S-1

From `collab/z/PHASE7-M2-FIXUP-AUDIT.md`:
`PUT /api/v1/journey/topics/{topicId}/primer` and `DELETE /api/v1/journey/topics/{topicId}/primer` in `JourneyController` lacked `@PreAuthorize("hasRole('ADMIN')")`. Because `SecurityConfig` fell through to `anyRequest().authenticated()`, any authenticated `STUDENT` could overwrite or delete shared topic primers. Furthermore, `TopicPage.tsx` rendered the edit and delete controls for all roles.

---

## 2. Changes Made

### 2.1 Backend Controller Authorization
- `backend/src/main/kotlin/com/gatelms/controller/JourneyController.kt`:
  - Added `@PreAuthorize("hasRole('ADMIN')")` to `updatePrimer` (`PUT /journey/topics/{topicId}/primer`).
  - Added `@PreAuthorize("hasRole('ADMIN')")` to `deletePrimer` (`DELETE /journey/topics/{topicId}/primer`).

### 2.2 Backend Integration Test Gate
- `backend/src/test/kotlin/com/gatelms/integration/JourneyApiIT.kt`:
  - Added test `honest-judge FAIL - student cannot update or delete an existing primer`:
    - Creates and authenticates a test user with `UserRole.STUDENT`.
    - Attempts `PUT /api/v1/journey/topics/topic_dbms_relational_alg/primer` -> asserts HTTP 403 FORBIDDEN with standard ProblemDetail (`status=403`, `title="Forbidden"`).
    - Attempts `DELETE /api/v1/journey/topics/topic_dbms_relational_alg/primer` -> asserts HTTP 403 FORBIDDEN with standard ProblemDetail (`status=403`, `title="Forbidden"`).
    - Asserts the shared primer row in the database remains unchanged.
  - Added `topicPrimerRepository.deleteAll()` in `@BeforeEach` `setUpData()` to enforce strict test-case data isolation.

### 2.3 Frontend Role Gating
- `frontend/src/pages/TopicPage.tsx`:
  - Hooked into `useAuth()` to check `isAdmin = user?.role === 'ADMIN'`.
  - Added guards in `handleSaveEdit` and `handleDeletePrimer` ensuring early return if `!isAdmin`.
  - Wrapped primer management controls (Edit, Regenerate with force, Delete) in `{isAdmin && (...)}`.
  - Wrapped error-banner actions (Retry Synthesis Now, Discard Failed Draft) in `{isAdmin && (...)}`.
  - Ensured non-admin students can still trigger initial primer generation when absent (`generateTopicPrimer(topicId)` without force).

### 2.4 Frontend Unit & Integration Tests
- `frontend/src/pages/__tests__/TopicPage.test.tsx`:
  - Added `mockAuthRole` helper.
  - Added test: `renders primer management controls only for ADMIN (role=%s)` covering both `ADMIN` and `STUDENT`.
  - Added test: `hides failed-draft forced retry and discard actions from students`.
  - Updated test `allows students to generate an absent primer without force` to verify non-force generation remains available to students while edit/delete/regenerate buttons stay hidden.

---

## 3. Verification & Honest Receipts

### 3.1 Backend Integration Test (`JourneyApiIT`)
```
[INFO] Running com.gatelms.integration.JourneyApiIT
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 51.34 s -- in com.gatelms.integration.JourneyApiIT
[INFO] Results:
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 3.2 Backend Unit Tests (`*Test`)
```
[INFO] Running com.gatelms.anatomy.service.AnatomyJsonParserTest (8/8)
[INFO] Running com.gatelms.anatomy.service.CardEmissionTest (10/10)
[INFO] Running com.gatelms.anatomy.service.EmissionTest (1/1)
[INFO] Running com.gatelms.anatomy.service.SchedulingTest (10/10)
[INFO] Running com.gatelms.anatomy.service.TopicPrimerValidationTest (6/6)
[INFO] Running com.gatelms.seed.QuestionSeedRunnerTest (5/5)
[INFO] Running com.gatelms.service.AiChatServiceTest (3/3)
[INFO] Running com.gatelms.service.AiLogHygieneTest (1/1)
[INFO] Running com.gatelms.service.AiProviderContractTest (8/8)
[INFO] Running com.gatelms.service.AiRateLimiterTest (5/5)
[INFO] Running com.gatelms.service.AiValidationAndCapsTest (6/6)
[INFO] Running com.gatelms.service.GradingServiceTest (7/7)
[INFO] Running com.gatelms.service.PlanGenerationTest (6/6)
[INFO] Running com.gatelms.service.PlanServiceTest (3/3)
[INFO] Running com.gatelms.service.QuestionServiceTest (3/3)
[INFO] Running com.gatelms.service.ReadinessComputationTest (6/6)
[INFO] Running com.gatelms.service.ScoringRulesTest (13/13)
[INFO] Running com.gatelms.service.SimilarQuestionRankerTest (7/7)
[INFO] Running com.gatelms.service.StreakComputationTest (6/6)
[INFO] Running com.gatelms.service.TopicServiceTest (4/4)
[INFO] Running com.gatelms.service.WhisperTranscriptionServiceTest (5/5)
[INFO] Results:
[WARNING] Tests run: 123, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

### 3.3 Frontend Vitest Suite
```
Test Files  52 passed (52)
     Tests  278 passed (278)
  Duration  49.74s
```

### 3.4 Frontend Production Build (`npm run build`)
```
✓ built in 15.01s
Exit Code: 0
```

---

## 4. Audit Gate Notice

Per `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §8.2:
> **S-1 and WP0: Z.ai audit BEFORE merge.** Push the branch, then stop and wait (post the report + request in the report header).

This branch `phase8/s1-primer-authz` is pushed and held unmerged awaiting Z.ai pre-merge audit.

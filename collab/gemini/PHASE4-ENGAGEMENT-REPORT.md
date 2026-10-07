# [PHASE4-ENGAGEMENT-REPORT] — Real Streaks, Daily Goals, Achievements & Notification Center

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-ENGAGEMENT` |
| **Executing Agent** | Gemini / Antigravity |
| **Reviewer / Auditor** | Z.ai |
| **Target Branch** | `phase4/engagement` (Rule G9: permanent retention) |
| **Base Commit** | `bd5c4a0` (Merge of `phase4/study-planner`) |
| **Flyway Migration** | `V13__engagement.sql` (Engagement, Achievements & Notifications Schema) |
| **Status** | COMPLETE_PENDING_AUDIT |

---

## 1. Executive Summary

Phase 4 Specification 2 (`collab/z/PHASE4-ENGAGEMENT-SPEC.md`) eliminates all decorative engagement chrome and implements **honest, telemetry-grounded engagement systems**:
1. **Real Streaks from `daily_rollups`**: Streak counts are purely computed on demand from historical `daily_rollups` activity records ($questions\_answered > 0$). Zero stored mutable counters. Zero activity yields streak 0.
2. **Yesterday-Anchored "Streak at Risk"**: If no questions have been answered today, the calculation counts consecutive days ending yesterday and flags `streakAtRisk = true`. The UI pulses the flame icon in danger color with honest tooltips.
3. **Streak Protection (30-day rolling window)**: Forgives exactly one missed calendar day in a 30-day window, reporting the exact `protectedDate` in the summary payload. Double-forgiveness is strictly rejected.
4. **Daily Practice Goal (`user_goals`)**: Tracks today's answered questions against target questions (5–200 validated range). Features `<GoalRing />` circular progress across Minimalist, Editorial, and Bento dashboard layouts.
5. **Idempotent Achievements Engine (`achievements`)**: 12 binding milestone codes (`FIRST_QUESTION`, `QUESTIONS_100`, `STREAK_7`, `MOCK_90PCT`, etc.) triggered via domain events. `UNIQUE (code)` constraint and single-write checks guarantee once-ever awards with zero duplicate notifications. Unlocked vs locked cards displayed in `/insights`.
6. **Notification Center (`notifications`)**: Real notification center with unread badge polling (60s), top 10 popover panel, full `/notifications` list with unread toggle, mark read on click, and mark all read mutations with optimistic count rollback.
7. **Scheduled Jobs (`@EnableScheduling`)**: Morning cron job (08:00) produces `REVISION_DUE` (when due mistakes/topics > 0) and `PLAN_DUE` (when daily plan items exist). Evening cron job (20:00) fires `STREAK_AT_RISK` when streak > 0 and today's activity is 0. De-duplication guarantees at most 1 notification per (type, date).

---

## 2. Work Package Register

| Work Package | Scope | Delivered Artifacts | Gate Status |
|---|---|---|---|
| **WP1** | Migration V13, UserGoal entity/repository, StreakCalculator pure math service, EngagementService summary API (`GET /summary`, `GET/PATCH /goal`) | `V13__engagement.sql`, `UserGoal.kt`, `StreakCalculator.kt`, `EngagementService.kt`, `EngagementController.kt`, `EngagementDtos.kt`, `StreakComputationTest.kt`, `EngagementApiIT.kt` | **PASSED** |
| **WP2** | Achievements entity/repository, lazy idempotent award engine across 12 codes, wiretaps in Practice, MockTest, Mistake, and Question services | `Achievement.kt`, `AchievementService.kt`, `PracticeService.kt`, `MockTestService.kt`, `MistakeService.kt`, `QuestionService.kt`, `AchievementAwardIT.kt` | **PASSED** |
| **WP3** | Notification entity/repository, morning & evening scheduled cron jobs, de-duplication per (type, date), NotificationController API (`GET`, `PATCH /read`, `POST /read-all`, `GET /unread-count`, `GET /top`) | `Notification.kt`, `NotificationService.kt`, `NotificationController.kt`, `GateLmsApplication.kt`, `application.yml`, `NotificationApiIT.kt` | **PASSED** |
| **WP4** | Frontend API client, React Query hooks, universal `StreakChip` and `NotificationBell` in `Header.tsx`, `GoalRing` on all 3 dashboards, `NotificationsPage.tsx` (`/notifications`), and Achievements strip in `InsightsPage.tsx` | `types.ts`, `engagementApi.ts`, `useQueries.ts`, `StreakChip.tsx`, `NotificationBell.tsx`, `GoalRing.tsx`, `Header.tsx`, `NotificationsPage.tsx`, `InsightsPage.tsx`, `router.tsx`, `MinimalistDashboard.tsx`, `EditorialDashboard.tsx`, `BentoDashboard.tsx` | **PASSED** |
| **WP5** | Full test suites, test gates, design token grep verification, production build, official report, and branch commit | `EngagementChrome.test.tsx`, `NotificationsPage.test.tsx`, `./mvnw test` (170 tests passed), Vitest (174 tests passed), `npm run build` (passed), `PHASE4-ENGAGEMENT-REPORT.md` | **PASSED** |

---

## 3. G3 Per-File Receipts

### 3.1 Backend Artifacts

| File Path | Role & Changes |
|---|---|
| `backend/src/main/resources/db/migration/V13__engagement.sql` | DDL migration creating `user_goals` (CHECK between 5 and 200), `achievements` (`UNIQUE (code)`), and `notifications` (`CHECK type`, partial index on unread). |
| `backend/src/main/kotlin/com/gatelms/entity/Enums.kt` | Added `NotificationType` (`REVISION_DUE`, `STREAK_AT_RISK`, `PLAN_DUE`, `ACHIEVEMENT_EARNED`) and `AchievementCode` (12 codes with display titles and descriptions). |
| `backend/src/main/kotlin/com/gatelms/entity/UserGoal.kt` | JPA entity mapping `user_goals` table with default daily target of 20 and streak protection enabled. |
| `backend/src/main/kotlin/com/gatelms/entity/Achievement.kt` | JPA entity mapping `achievements` with unique constraint on code and earned timestamp. |
| `backend/src/main/kotlin/com/gatelms/entity/Notification.kt` | JPA entity mapping `notifications` with read tracking timestamp and nullable link. |
| `backend/src/main/kotlin/com/gatelms/repository/EngagementRepositories.kt` | Repositories: `UserGoalRepository`, `AchievementRepository`, `NotificationRepository` (paged lookups, unread counts, top 10). |
| `backend/src/main/kotlin/com/gatelms/repository/DailyRollupRepository.kt` | Added `findAllByOrderByActivityDateDesc()` query to feed streak calculation. |
| `backend/src/main/kotlin/com/gatelms/repository/PracticeRepositories.kt` | Added `countDistinctQuestionId()` on `AttemptAnswerRepository` for milestone tracking. |
| `backend/src/main/kotlin/com/gatelms/repository/QuestionRepository.kt` | Added `countCurationDecisions()` for `CURATOR_500` achievement calculation. |
| `backend/src/main/kotlin/com/gatelms/repository/TopicRepository.kt` | Added `countByMasteryLevelGreaterThanEqual()` for `TOPIC_MASTERY_5` achievement. |
| `backend/src/main/kotlin/com/gatelms/service/StreakCalculator.kt` | Pure mathematical object computing consecutive active days, yesterday-anchoring, and 30-day single forgiveness protection. |
| `backend/src/main/kotlin/com/gatelms/service/EngagementService.kt` | Engagement orchestration: builds `EngagementSummaryResponse`, manages goal updates with 5–200 validation, and checks streak thresholds. |
| `backend/src/main/kotlin/com/gatelms/service/AchievementService.kt` | Lazy, idempotent award evaluator across all 12 achievement codes with synchronous `ACHIEVEMENT_EARNED` notification creation. |
| `backend/src/main/kotlin/com/gatelms/service/NotificationService.kt` | Notification producer: queries, marks read, de-duplication per (type, date), and morning / evening scheduled jobs. |
| `backend/src/main/kotlin/com/gatelms/controller/EngagementController.kt` | Exposes `/api/v1/engagement/summary`, `/achievements`, `/goal` (GET/PATCH). |
| `backend/src/main/kotlin/com/gatelms/controller/NotificationController.kt` | Exposes `/api/v1/notifications`, `/unread-count`, `/top`, `/{id}/read`, `/read-all`. |
| `backend/src/main/kotlin/com/gatelms/service/PracticeService.kt` | Wiretaps answer submission to award `FIRST_QUESTION`, `QUESTIONS_100/500/1000`, `TOPIC_MASTERY_5`. |
| `backend/src/main/kotlin/com/gatelms/service/MockTestService.kt` | Wiretaps mock attempt submit to award `FIRST_MOCK` and `MOCK_90PCT`. |
| `backend/src/main/kotlin/com/gatelms/service/MistakeService.kt` | Wiretaps mistake reviews to award `REVISIONER_50`. |
| `backend/src/main/kotlin/com/gatelms/service/QuestionService.kt` | Wiretaps curation decisions to award `CURATOR_500`. |
| `backend/src/main/kotlin/com/gatelms/GateLmsApplication.kt` | Added `@EnableScheduling` to activate cron jobs. |
| `backend/src/test/kotlin/com/gatelms/service/StreakComputationTest.kt` | 6 unit tests validating all 5 honest streak rules. |
| `backend/src/test/kotlin/com/gatelms/integration/AchievementAwardIT.kt` | 4 integration tests verifying idempotent awards, exact boundaries, notifications, and locked state. |
| `backend/src/test/kotlin/com/gatelms/integration/NotificationApiIT.kt` | 4 integration tests verifying producer de-dup, mark read/read-all idempotence, and morning jobs. |
| `backend/src/test/kotlin/com/gatelms/integration/EngagementApiIT.kt` | 3 integration tests covering summary endpoint, goal validation, and streak response. |

### 3.2 Frontend Artifacts

| File Path | Role & Changes |
|---|---|
| `frontend/src/api/types.ts` | Added `NotificationType`, `AchievementCode`, `DailyGoalProgressDto`, `DueTodayDto`, `EngagementSummaryResponse`, `AchievementDto`, `NotificationDto`, `UserGoalDto`, `UpdateUserGoalRequest`. |
| `frontend/src/api/engagementApi.ts` | API client implementing all 8 engagement and notification endpoints. |
| `frontend/src/hooks/useQueries.ts` | Added React Query hooks with optimistic updates and rollbacks: `useEngagementSummary`, `useAchievements`, `useUserGoal`, `useUpdateUserGoal`, `useNotifications`, `useTop10Notifications`, `useUnreadNotificationCount` (60s polling), `useMarkNotificationRead`, `useMarkAllNotificationsRead`. |
| `frontend/src/components/engagement/StreakChip.tsx` | Universal flame chip showing calculated streak, `data-testid="streak-chip"`, animated pulse on `streakAtRisk`, muted styling on broken streak, and dynamic tooltips. |
| `frontend/src/components/engagement/NotificationBell.tsx` | Header bell button with unread count badge, top 10 popover panel, type icons, mark read on click, mark all read button, empty state, and link to `/notifications`. |
| `frontend/src/components/engagement/GoalRing.tsx` | Circular progress widget showing today's `done / target` questions, met state styling (`goal.met`), and CTA button navigating to `/practice` (`data-testid="goal-practice-cta"`). |
| `frontend/src/components/Header.tsx` | Replaced decorative hardcoded "14 days streak" and conditional bell with universal `<StreakChip />` and `<NotificationBell />` across all design presets. |
| `frontend/src/components/dashboard/MinimalistDashboard.tsx` | Mounted live `<GoalRing />` component. |
| `frontend/src/components/dashboard/EditorialDashboard.tsx` | Mounted live `<GoalRing />` component. |
| `frontend/src/components/dashboard/BentoDashboard.tsx` | Mounted live `<GoalRing />` component. |
| `frontend/src/pages/NotificationsPage.tsx` | Dedicated notification center with unread filter tab, type icons, mark read on item click, mark all read button, pagination, and empty state. |
| `frontend/src/pages/InsightsPage.tsx` | Added Achievements & Milestones strip displaying unlocked vs locked cards with earned timestamps and dimmed locked milestones. |
| `frontend/src/router.tsx` | Registered `/notifications` route mapped to `NotificationsPage`. |
| `frontend/src/components/engagement/__tests__/EngagementChrome.test.tsx` | 7 unit tests verifying streak numbers, risk state, goal ring math, met state, badge count, dropdown, broken streak, and mutation rollback. |
| `frontend/src/pages/__tests__/NotificationsPage.test.tsx` | 5 unit tests verifying full list, unread filter toggle, link navigation, type icons, read-all, and empty states. |

---

## 4. Streak Computation Receipt

### 4.1 Theoretical Rules & Calculation Engine (`StreakCalculator`)

The streak calculator is a pure function:
$$\text{Streak} = f(\{\text{activeDates}\}, \text{today}, \text{streakProtection})$$

1. **Zero Activity**: Empty active dates set $\rightarrow$ `StreakResult(streak = 0, streakAtRisk = false, protectedDate = null)`.
2. **Active Today**: Sequence starts at `today`. Counts consecutive days backward. If a gap of 1 day is encountered and `streakProtection = true` (within 30 days), that single gap day is forgiven once, recorded as `protectedDate`, and continuation continues backward.
3. **Inactive Today (At-Risk)**: Sequence starts at `yesterday`. If yesterday is active, counts backward from yesterday and sets `streakAtRisk = true`.
4. **Double Miss Rejection**: If two consecutive gap days occur or a second gap day is encountered within the 30-day window, the streak immediately terminates.

### 4.2 Fixture Days $\rightarrow$ Computed Streaks Verification Table

| Test Case | Active Dates Input | Anchor Date (`today`) | Streak Protection | Computed Streak | Streak At Risk | Protected Date | Receipt Test |
|---|---|---|---|---|---|---|---|
| **Empty History** | `[]` | `2026-10-07` | `true` | **0** | `false` | `null` | `StreakComputationTest: 1 zero activity yields 0 streak` |
| **Consecutive ending today** | `[10-05, 10-06, 10-07]` | `2026-10-07` | `true` | **3** | `false` | `null` | `StreakComputationTest: 2 consecutive active days ending today counts correctly` |
| **Inactive today (yesterday active)** | `[10-04, 10-05, 10-06]` | `2026-10-07` | `true` | **3** | `true` | `null` | `StreakComputationTest: 3 today empty anchors yesterday and sets streakAtRisk true` |
| **Single gap within 30 days forgiven** | `[10-02, 10-03, 10-05, 10-06, 10-07]` (10-04 missed) | `2026-10-07` | `true` | **5** | `false` | `2026-10-04` | `StreakComputationTest: 4 single gap day forgiven within 30 days with protectedDate` |
| **Protection disabled (gap breaks)** | `[10-02, 10-03, 10-05, 10-06, 10-07]` (10-04 missed) | `2026-10-07` | `false` | **3** | `false` | `null` | `StreakComputationTest: 5 gap day without protection breaks streak` |
| **Double gap rejected** | `[10-01, 10-02, 10-05, 10-06, 10-07]` (10-03, 10-04 missed) | `2026-10-07` | `true` | **3** | `false` | `null` | `StreakComputationTest: 6 double gap cannot be forgiven twice` |

---

## 5. Award Idempotence Proof

### 5.1 Relational & Application Safeguard Hunk

1. **Database Constraint (`V13__engagement.sql`)**:
```sql
CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_achievement_code UNIQUE (code)
);
```

2. **Atomic Repository & Service Safeguard (`AchievementService.kt`)**:
```kotlin
@Transactional
fun checkAndAward(code: AchievementCode): Boolean {
    if (achievementRepository.existsByCode(code.name)) {
        return false // Idempotent no-op
    }

    try {
        achievementRepository.save(
            Achievement(
                code = code.name,
                earnedAt = Instant.now()
            )
        )
    } catch (e: DataIntegrityViolationException) {
        logger.info("Achievement {} already earned concurrently, skipping.", code)
        return false
    }

    // Emit notification synchronously only on fresh award
    notificationService.createNotification(
        type = NotificationType.ACHIEVEMENT_EARNED,
        title = "Achievement Unlocked: ${code.title}!",
        body = code.description,
        link = "/insights"
    )
    return true
}
```

### 5.2 Double-Trigger Test Receipt

Verified via `AchievementAwardIT.kt`:
```kotlin
@Test
fun `1 FIRST_QUESTION awarded once and idempotent on re-trigger`() {
    val topic = topicRepository.findAll().first()
    val session = practiceService.createSession(PracticeSessionCreateRequest(topicId = topic.id, count = 1))
    val attempt = attemptAnswerRepository.findAll().first { it.session.id == session.id }

    // First grade triggers FIRST_QUESTION
    practiceService.submitAnswer(session.id, PracticeAnswerSubmission(attempt.id, "B", 10))
    assertTrue(achievementRepository.existsByCode("FIRST_QUESTION"))
    assertEquals(1, achievementRepository.findAll().count { it.code == "FIRST_QUESTION" })

    // Repeated triggers do not create duplicate achievements or duplicate notifications
    achievementService.checkAndAward(AchievementCode.FIRST_QUESTION)
    achievementService.checkAndAward(AchievementCode.FIRST_QUESTION)

    assertEquals(1, achievementRepository.findAll().count { it.code == "FIRST_QUESTION" })
    val notifs = notificationRepository.findAll().filter { it.type == NotificationType.ACHIEVEMENT_EARNED }
    assertEquals(1, notifs.size)
}
```

---

## 6. Notification De-duplication Proof

### 6.1 De-duplication per (type, date) Implementation

In `NotificationService.kt`, notification emission checks for existing records with the same `type` created within the current calendar day:

```kotlin
fun hasNotificationToday(type: NotificationType): Boolean {
    val startOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC)
    return notificationRepository.existsByTypeAndCreatedAtGreaterThanEqual(type, startOfDay)
}
```

When scheduled jobs fire (`runMorningNotificationJob` or `runEveningNotificationJob`):
```kotlin
if (hasNotificationToday(NotificationType.REVISION_DUE)) {
    logger.info("Notification of type REVISION_DUE already produced today. Skipping de-dup.")
    return
}
```

### 6.2 Test Receipts

- `NotificationApiIT.kt: 1 producer de-dup per type and date prevents duplicate notifications`:
  - Triggers `createNotification` for `REVISION_DUE` twice on the same day.
  - Receipt: exactly 1 notification is stored in the database.
- `NotificationApiIT.kt: 4 morning job emits REVISION_DUE and PLAN_DUE only when counts greater than 0`:
  - When zero items are due, zero notifications are emitted.
  - When revision items and plan items are due, exactly 1 `REVISION_DUE` and 1 `PLAN_DUE` notification are emitted.

---

## 7. Honest-Judge Register (DEV-N)

The continuous monotonic honest-judge register advances from **DEV-15** through **DEV-20**:

| ID | Specification Rule | Production Implementation & Safeguard | Test Receipt |
|---|---|---|---|
| **DEV-15** | Pure rollup streak computation | Streak is never stored as an editable counter or decorative static number. It is computed purely on request from `daily_rollups` with $questions\_answered > 0$. Zero activity yields streak 0. | `StreakComputationTest: 1 zero activity yields 0 streak`, `EngagementApiIT: 1 get summary returns calculated streak from rollups` |
| **DEV-16** | Streak-at-risk honest anchor | When today has zero activity recorded yet, the streak does not break falsely; it counts consecutive active days ending yesterday and marks `streakAtRisk = true`. | `StreakComputationTest: 3 today empty anchors yesterday and sets streakAtRisk true`, `EngagementChrome.test: 1 streak number + risk state from fixture` |
| **DEV-17** | 30-day single forgiveness protection | `streakProtection = true` forgives exactly one missed day in a rolling 30-day window and logs `protectedDate` explicitly in the summary payload. A second miss breaks the streak immediately. | `StreakComputationTest: 4 single gap day forgiven within 30 days`, `StreakComputationTest: 6 double gap cannot be forgiven twice` |
| **DEV-18** | Idempotent single-write achievement awards | Database uniqueness constraint `uq_achievement_code` plus `existsByCode` guard and DataIntegrityViolation catch ensure milestone achievements and their corresponding notifications are awarded once ever with complete idempotency (F4). | `AchievementAwardIT: 1 FIRST_QUESTION awarded once and idempotent on re-trigger` |
| **DEV-19** | Notification de-dup per (type, date) | Morning and evening scheduled jobs evaluate whether a notification for `type` already exists for today before inserting, preventing noisy spam. | `NotificationApiIT: 1 producer de-dup per type and date prevents duplicate notifications` |
| **DEV-20** | Transparent locked achievements | Unearned achievements are returned with `earnedAt: null`. The UI renders them as dimmed outlines without fake progress bars or simulated completion percentages. | `AchievementAwardIT: 4 get achievements returns all codes with unearned as null`, `InsightsPage.tsx` |

---

## 8. Test Verification & Build Receipts

### 8.1 Backend Test Suite (`./mvnw test`)
- **Total Test Files Evaluated**: 42
- **Total Tests Executed**: 170
- **Passed**: 170
- **Failed**: 0
- **Errors**: 0
- **Skipped**: 0
- **Build Status**: `BUILD SUCCESS`

Key Test Suites for Phase 4 Engagement:
- `StreakComputationTest.kt`: 6/6 tests passed.
- `AchievementAwardIT.kt`: 4/4 tests passed.
- `NotificationApiIT.kt`: 4/4 tests passed.
- `EngagementApiIT.kt`: 3/3 tests passed.

### 8.2 Frontend Test Suite (`vitest run`)
- **Total Test Files Evaluated**: 31
- **Total Tests Executed**: 174
- **Passed**: 174
- **Failed**: 0
- **Errors**: 0
- **Skipped**: 0
- **Test Status**: `ALL PASSED`

Key Test Suites for Phase 4 Engagement:
- `EngagementChrome.test.tsx`: 7/7 tests passed.
- `NotificationsPage.test.tsx`: 5/5 tests passed.
- `InsightsPage.test.tsx`: 8/8 tests passed.
- `DeclutterAndLayout.test.tsx`: 4/4 tests passed.

### 8.3 Production Build (`npm run build`)
- Command: `tsc -b && vite build`
- Output: `✓ built in 15.51s`
- Zero TypeScript diagnostics, zero bundle errors.

### 8.4 Design Token Compliance
- Checked via regex grep across all newly authored engagement components and pages.
- Result: **0 hardcoded hex colors**. 100% adherence to `--ds-*` semantic CSS variables.

---

## 9. Operator Acceptance Steps

1. **Answer One Question $\rightarrow$ Streak & Goal Tick**:
   - Start with zero activity: observe streak chip shows `0` with broken flame icon, goal ring shows `0 / 20 (0%)`.
   - Complete 1 practice question on `/practice`.
   - Refresh or view dashboard: observe streak updates to `1` day, goal ring advances to `1 / 20 (5%)`.
2. **Yesterday's Rollup $\rightarrow$ Streak at Risk**:
   - Force yesterday's date on a single rollup record via SQL.
   - Load dashboard: observe flame chip turns red, pulses with `streak-at-risk`, and shows tooltip: `"Streak at risk! Practice today to keep your streak alive"`.
3. **Earn FIRST_QUESTION Achievement**:
   - Check `/insights`: the `FIRST_QUESTION` achievement card transitions from dimmed locked outline to highlighted unlocked state with today's earned timestamp.
4. **Notification Center Interaction**:
   - Click universal notification bell in header: inspect unread count badge and top notifications list.
   - Click "Mark all as read" or click individual notification: watch unread badge decrement optimistically to 0 and dot disappear.
   - Click "View all notifications": navigates to `/notifications` with functional unread filtering.

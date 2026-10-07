# [PHASE4-ENGAGEMENT-SPEC] — Real Streaks, Daily Goals, Achievements & Notification Center

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-ENGAGEMENT` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase4/study-planner` (SPEC 1) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Dashboard chrome already renders streak chip (tuf) and bell (aurora/sky) as decoration — must become real; `daily_rollups` (V9) is the activity source of truth; Phase 3 revision overview feeds due-notifications |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase4/engagement` · Migration **V13** · Effort 8–11 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Make every engagement pixel honest: the **streak chip** counts real consecutive activity days (from `daily_rollups`), a **daily goal** tracks target questions answered, **achievements** award lazily and idempotently from real events, and the **bell** opens a real notification center fed by domain events (revision due, streak at risk, plan due, achievements). No fake numbers anywhere — a day with zero activity shows a broken streak, not a decorative 7.

## 2. Architecture (binding)

### 2.1 Migration V13 (binding DDL)

```sql
CREATE TABLE user_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_target_questions INT NOT NULL DEFAULT 20 CHECK (daily_target_questions BETWEEN 5 AND 200),
    streak_protection BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_achievement_code UNIQUE (code)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('REVISION_DUE','STREAK_AT_RISK','PLAN_DUE','ACHIEVEMENT_EARNED')),
    title VARCHAR(200) NOT NULL,
    body TEXT,
    link VARCHAR(300),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_unread ON notifications(read_at) WHERE read_at IS NULL;
```

### 2.2 Streak & Goal Computation (binding — computed from rollups, never stored as counter)

- **Streak** = count of consecutive dates ending today (or yesterday, before any activity today) with `daily_rollups.questions_answered > 0`. Zero activity ever → streak 0. Activity today already recorded → streak counts through today; none today → counts through yesterday (the "streak at risk" state, shown honestly).
- **Daily goal progress** = today's rollup `questions_answered` vs `user_goals.daily_target_questions`; `streak_protection=true` means one miss day per 30 does not break the streak (binding rule: the most recent zero-rollup day within a rolling 30-day window is forgiven **once** — forgiveness is computed, logged in the response as `protectedDate`, never silently).
- `GET /api/v1/engagement/summary` → `{streak, protectedDate?, streakAtRisk, goal: {target, done, met}, dueToday: {mistakes, topics}}` — single dashboard feed, all fields from real computation.

### 2.3 Achievements (binding award table — lazy, idempotent)

Award check runs **after** the domain events that trigger them (same transaction, try/award); `INSERT ... ON CONFLICT (code) DO NOTHING` guarantees idempotency — a code is awarded once ever.

| Code | Trigger |
|---|---|
| `FIRST_QUESTION` | first graded answer ever |
| `FIRST_MOCK` | first mock submit |
| `QUESTIONS_100` / `QUESTIONS_500` / `QUESTIONS_1000` | cumulative distinct questions answered (attempt_answers) |
| `STREAK_3` / `STREAK_7` / `STREAK_30` | streak computation crosses threshold |
| `MOCK_90PCT` | a mock attempt scores ≥ 90% of max |
| `TOPIC_MASTERY_5` | any topic reaches mastery_level 5 |
| `CURATOR_500` | 500 curation decisions (accepted+rejected) |
| `REVISIONER_50` | 50 mistake reviews |

Each award also creates an `ACHIEVEMENT_EARNED` notification. `GET /api/v1/engagement/achievements` returns all codes with `earnedAt: null` for unearned (the UI shows the locked state — honest gaps, not hidden).

### 2.4 Notification Producers (binding)

- **REVISION_DUE** — `@Scheduled` daily job (default 08:00 server time, configurable `app.notifications.morning-cron`): one notification when due mistakes + due topics > 0, body carries counts, link `/revision`.
- **PLAN_DUE** — same job: today's open plan items count, link `/planner`.
- **STREAK_AT_RISK** — `@Scheduled` daily (default 20:00, `app.notifications.evening-cron`): fires only when streak > 0 and today's rollup is empty. Skipped silently otherwise (no noise).
- **ACHIEVEMENT_EARNED** — synchronous at award.
- De-dup rule: at most one notification per (type, date) — the job checks today's existing notifications before inserting (tested).

### 2.5 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `GET /api/v1/engagement/summary` | §2.2 payload (dashboard + chips feed) |
| `GET /api/v1/engagement/achievements` | §2.3 list (all codes, earned or locked) |
| `GET /api/v1/notifications` | page (size 20) newest-first, filter `unread=true` |
| `PATCH /api/v1/notifications/{id}/read` | sets `read_at`; idempotent 200 |
| `POST /api/v1/notifications/read-all` | marks all read; returns count |
| `GET /api/v1/notifications/unread-count` | `{count}` (bell badge polling, TanStack `refetchInterval` 60 s) |
| `GET/PATCH /api/v1/engagement/goal` | read/update daily target (5–200 validated → 400 `fieldErrors.dailyTargetQuestions`) |

## 3. Frontend

- **Streak chip** (tuf + all designs — becomes universal): flame icon + number from summary; broken-streak state (streak 0 + no at-risk) shows a muted icon, `streakAtRisk` pulses the bell-red token accent; tooltip explains the computation.
- **Goal ring** on dashboard (all designs): today's `done/target` circular progress; met state; tap → `/practice`.
- **Bell dropdown** (Header, all designs — previously aurora/sky-only, now universal): unread badge from count endpoint; panel = last 10 notifications (icon by type, title, relative time, link on click, read state); "Mark all read"; "View all" → **NotificationsPage `/notifications`** (full list, unread filter tab, type icons, empty state).
- **Achievements strip** on `/insights` (Phase 3 page): unlocked vs locked grid with earned dates (token-styled; locked = dimmed outline, no fake progress).
- TanStack Query for all engagement reads; mutations for read/read-all/goal update with optimistic badge decrement + rollback.

## 4. Verification & Test Gates

- **Backend delta** ≥ +3 files / +11 cases (target ≥ 40 / ≥ 136): `StreakComputationTest` ≥ 5 (honest-judge core: gap-day computation; today-empty → yesterday-anchored; streak-protection forgives exactly one miss per 30 and logs protectedDate; zero activity → 0; double-forgiveness rejected), `AchievementAwardIT` ≥ 4 (first-question award once + idempotent re-trigger; QUESTIONS_100 at exact boundary; notification created with award; locked list shows null earnedAt), `NotificationApiIT` ≥ 4 (producer de-dup per type/date; read + read-all idempotence; unread filter; morning job emits REVISION_DUE + PLAN_DUE only when counts > 0).
- **Frontend delta** ≥ +2 files / +12 cases (target ≥ 31 / ≥ 161): `EngagementChrome.test` ≥ 7 (streak number + risk state from fixture; goal ring math + met state; badge from unread-count; dropdown renders 10 + mark-all mutation; empty panel state; broken-streak muted state; error rollback), `NotificationsPage.test` ≥ 5 (list + unread filter; click navigation link; type icons; read-all; empty state).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.
- **Scheduling receipts**: cron expressions + test-profile override proving both jobs ran (Testcontainers `@Scheduled` test or Awaitility on fixed delay).

## 5. Report & Acceptance

Report `collab/gemini/PHASE4-ENGAGEMENT-REPORT.md`: WP register; G3 per-file receipts; streak computation receipt (fixture days → computed streaks table); award idempotence proof (ON CONFLICT hunk + double-trigger test); notification de-dup proof; honest-judge register (DEV-N). Operator acceptance: answer one question, see streak 1 + goal progress tick; force yesterday's rollup via SQL, open dashboard, see streak at risk at 20:00-configured test time; earn FIRST_QUESTION in achievements; mark notifications read and watch the badge clear.

**Out of scope**: email/push/web-push delivery (PWA SPEC adds SW surface only), social/share features, leaderboards, multi-user, achievement editing (table is code-truth).

**Work packages**: WP1 V13 + streak/goal computation + summary API (2 h) · WP2 achievements engine + APIs (2 h) · WP3 notifications: jobs + APIs (2 h) · WP4 frontend chrome + notifications page (2.5 h) · WP5 tests + report (1.5–2 h).

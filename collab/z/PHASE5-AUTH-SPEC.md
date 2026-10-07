# [PHASE5-AUTH-SPEC] — Authentication & Per-User Data Scoping (OQ-5 Option A)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE5-AUTH` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | P4-EXIT (merge of `phase4/pwa-mobile`) — valid only after Operator adjudicates OQ-5 = Option A |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | OQ-5 default Option A; every prior SPEC's "multi-user out of scope" line defers here; G5/G8 security posture; nginx same-origin proxying already in place |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase5/auth-scoping` · Migration **V14** · Effort 14–18 h (heaviest SPEC in the roadmap) |
| **Status** | APPROVED_FOR_EXECUTION (OQ-5=A required) |

---

## 1. Goal

Introduce **accounts and sessions** and move every piece of user-specific state into **per-user tables**, preserving all existing data by migrating it to the seeded Operator account. After this SPEC: nothing user-specific is global, no API serves another user's rows, the existing single-user experience is identical (auto-migrated), and the app can be safely exposed beyond loopback.

## 2. Architecture (binding)

### 2.1 Migration V14 (binding DDL — data-preserving; receipts mandatory)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('ADMIN', 'STUDENT')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Per-user extraction of V1 user-specific columns (data moved, columns dropped: one source of truth)
CREATE TABLE topic_mastery (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id VARCHAR(100) NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    mastery_level INT NOT NULL DEFAULT 0,
    last_revised_at TIMESTAMP WITH TIME ZONE,
    next_revision_due TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_id, topic_id)
);

CREATE TABLE question_progress (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    pass1_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    pass2_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    bookmarked_at TIMESTAMP WITH TIME ZONE,
    last_attempted_at TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, question_id)
);

ALTER TABLE mock_attempts  ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE mistake_logs   ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE notes          ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE study_plans    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE achievements   ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE notifications  ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE daily_rollups  ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE daily_rollups  DROP CONSTRAINT daily_rollups_pkey, ADD PRIMARY KEY (user_id, activity_date);
ALTER TABLE questions DROP COLUMN pass1_status, DROP COLUMN pass2_status,
                      DROP COLUMN is_bookmarked, DROP COLUMN bookmarked_at, DROP COLUMN last_attempted_at;
ALTER TABLE topics   DROP COLUMN mastery_level, DROP COLUMN last_revised_at, DROP COLUMN next_revision_due;
DROP INDEX IF EXISTS idx_questions_pass2; DROP INDEX IF EXISTS idx_questions_bookmarked;
```

(The executor produces the final V14 as plain DDL in this order: create `users` + new tables → insert the operator account → `INSERT ... SELECT` the legacy data into `topic_mastery`/`question_progress` attributed to the operator id → `UPDATE` the ALTERed tables with `user_id` → drop the legacy columns and their now-orphaned indexes. Check constraints mirror the V1 CHECKs on the new tables.)

**Data migration (binding, receipt-quoted)**: V14 inserts the Operator user (`username` from `GATELMS_ADMIN_USER`, default `ankur`, password set at first boot via §2.3 bootstrap — hash placeholder `!boot` and force-change on first login), then `INSERT INTO topic_mastery/question_progress SELECT` all legacy rows with `user_id = <operator>`, then `UPDATE` the ALTERed tables, then drops legacy columns. **Receipts: per-table row count before = rows migrated, quoted in the report** (e.g. `topics with mastery data: 118 → topic_mastery rows for operator: 118`).

### 2.2 Security configuration (binding)

- **Spring Security 6** filter chain: `POST /api/v1/auth/login` + `GET /actuator/health` permitAll; `/actuator/**` (other) `hasRole('ADMIN')`; **everything else authenticated**. No `permitAll` fallbacks anywhere else (grep gate).
- **Sessions**: Spring Session **JDBC** (tables auto-created via `spring.session.jdbc.initialize-schema=always` — documented deviation-free path) → sessions survive backend restarts; cookie `GATELMSSESSION` `HttpOnly` + `SameSite=Strict` + `Secure=false` (loopback HTTP; a `server.servlet.session.cookie.secure=true` toggle documented for TLS deployment, PHASE5-RELEASE §3).
- **CSRF**: Spring Security 6 default token (double-submit cookie `XSRF-TOKEN` + header). The frontend fetch wrapper reads the cookie and sends `X-XSRF-TOKEN` on every non-GET (binding — tested).
- **Login**: `POST /api/v1/auth/login` `{username, password}` → BCrypt verify (`spring-security-crypto`, 10 rounds); failure → 401 ProblemDetail `detail="Invalid credentials"` (no username enumeration); **rate limit**: in-memory per-username counter, 5 failures / 5 minutes → 429 with `Retry-After` (tested).
- **Logout**: `POST /api/v1/auth/logout` invalidates session + clears cookie. `GET /api/v1/auth/me` → `{userId, username, displayName, role}` (frontend boot).
- **CORS**: the existing permissive `CorsConfig` is tightened to same-origin (nginx already proxies `/api` same-origin; cross-origin dev workflow moves to the Vite proxy — `vite.config.ts` `server.proxy` `/api → localhost:8080`, dev-only).
- **Bootstrap flow**: on startup, if the operator user's hash is the `!boot` placeholder, the backend logs a one-time notice and the login endpoint accepts the value of `GATELMS_ADMIN_INITIAL_PASSWORD` (env, `.env.example` entry) exactly once, then stores its BCrypt hash. `.env` stays gitignored (G5).

### 2.3 Read-path scoping (binding — every service, no exceptions)

- `CurrentUser` resolution via the authenticated principal; every user-specific query filters by it: mastery reads/writes → `topic_mastery` (single `TopicMasteryService` — practice grading hook, revision engine, planner, analytics all re-pointed, one call site change each by the Phase 4 §3.10 service discipline), question status/bookmarks → `question_progress`, attempts/mistakes/notes/plans/engagement/notifications/rollups → `user_id = current`.
- **Analytics honesty preserved** (G3): readiness/heatmap/trends compute from the *current user's* rows only — the §2.2 formulas are unchanged, the input set narrows.
- **Question bank content itself stays global** (questions/subjects/topics are shared truth; only progress is per-user). Curation remains an admin-flavored action: **v1 rule — ADMIN role only** (operator); `PATCH /questions/{id}/curation` gains a role check → 403 for STUDENT.
- All existing ITs are updated to authenticate first (a `@TestConfiguration` login helper — one utility, reused; receipts per file must not drop below the Phase 4 floors).

### 2.4 Frontend

- **LoginPage `/login`**: username/password, error banner on 401 (generic text), 429 state with retry countdown, loading state; token-styled; the SW cache never caches it (PWA network-only family gains `/login`).
- **AuthContext**: `me()` on boot (TanStack Query), `{user, login, logout}`; **fetch wrapper**: `credentials: 'include'`, XSRF cookie→header injection, **401 interceptor** → redirect `/login?next=<path>` (never on `/login` itself or `/actuator/health`), 403 → "not permitted" toast.
- **Router**: `RequireAuth` wrapper around all existing routes (login excepted); header profile chip shows `displayName` + role badge + **Logout** (POST) — the existing chip becomes real in every design.
- Tests: login flow (success/failure/429), 401 redirect with `next` restoration, XSRF header on mutation, logout clears + redirects, RequireAuth guard.

## 3. Verification & Test Gates

- **Backend delta** ≥ +4 files / +14 cases (target ≥ 44 / ≥ 150): `AuthFlowIT` ≥ 6 (login ok/401-generic/429-after-5; me reflects session; logout invalidates — subsequent me 401; actuator admin-gated 403 as STUDENT — seeded second test user), `UserScopingIT` ≥ 6 (the honest-judge core: two users, distinct data — user A's attempts/mistakes/notes/mastery/progress never appear in user B's API responses, **enumerated per endpoint family**; analytics compute per user; curation 403 as STUDENT), `MigrationV14IT` ≥ 2 (row-count invariance receipts: legacy data fully attributed to operator; dropped columns gone + entities re-pointed — schema validate), `CsrfIT` ≥ 2 (mutation without XSRF header → 403; with header → 2xx).
- **Frontend delta** ≥ +2 files / +12 cases (target ≥ 35 / ≥ 183): `LoginPage.test` ≥ 5, `authFlow.test` ≥ 4 (wrapper XSRF; 401 redirect + next restore; logout; RequireAuth), `HeaderProfile.test` ≥ 3 (chip, role badge, logout mutation).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.
- **Grep gates**: `permitAll` appears only in the two sanctioned spots; no controller reads user identity from request params (identity = session only); `CorsConfig` contains no `*`.

## 4. Report & Acceptance

Report `collab/gemini/PHASE5-AUTH-REPORT.md`: WP register; G3 per-file receipts; **V14 migration receipts** (before/after row-count table for every moved dataset); scoping matrix (endpoint family × user isolation test name); security receipt table (each §2.2 rule → test); honest-judge register (DEV-N). Operator acceptance: boot fresh, log in with the bootstrap password (and observe the forced change), run the full student loop unchanged, create a second user via `scripts/create_user.sh` (operational helper, binding deliverable: BCrypt-hash generator + psql insert), verify user B sees zero of user A's data everywhere, restart the backend mid-session and confirm the session survives (JDBC sessions), watch 5 bad logins produce a 429.

**Out of scope**: self-registration UI, password reset email flow, OAuth/SSO, admin user-management UI (SQL + helper script is the v1 path), user avatar/upload management, rate-limit store in Redis (in-memory suffices single-node).

**Work packages**: WP1 V14 + entities re-pointing (3.5 h) · WP2 security config + session + CSRF (2.5 h) · WP3 scoping sweep across services (3 h) · WP4 login/logout/me + bootstrap + helper script (1.5 h) · WP5 frontend auth layer (2.5 h) · WP6 tests + report (2–3 h).

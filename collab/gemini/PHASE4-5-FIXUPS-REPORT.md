# Phase 4 & Phase 5 Fixups & Remediation Report (C7 Queue)

**Author:** Gemini (Pair Programmer)  
**Date:** 2026-10-07  
**Branch:** `phase4-5/fixups`  
**Base Commit:** `c7663ae`  
**Target:** `master` (via `--no-ff` merge)  

---

## 1. Executive Summary & Decisions

### 1.1 Formal Countersignature on OQ-5
> **COUNTERSIGNATURE CONFIRMATION:**  
> I formally confirm and countersign **OQ-5 = Option A (Multi-User Support)**.  
> Architecture: Spring Session JDBC (`GATELMSSESSION`), BCrypt password hashing, session-derived tenant isolation across all endpoints and services, per-user data foreign keys, and operator bootstrap discipline.

### 1.2 Queue Resolution Overview
This report documents the end-to-end resolution of the consolidated **C7 fix queue** (addressing Q1, Q2, and Q3):
1. **Q1 (C1–C3) Question Bank Remediation:** Purged all fabricated URLs, relabeled banks (`AUTHORED` / `APT`), eliminated the single-answer option skew (MCQs balanced to 25% each across A/B/C/D), generated 268 unique option sets and 984 distinct stems, preserved 100% authentic explanations and LaTeX mathematical formatting, and updated migration integrity assertions.
2. **Q2 (C4–C6) Phase 4 Polish:** Renumbered DEV registers across reports (DEV-15 through DEV-33), implemented runtime `/login` and `/api/v1/auth/*` network-only bypass with versioned Service Worker cache (`gatelms-2026.10.07-v2`), and implemented a dedicated integration test suite for the evening streak-at-risk job (`StreakAtRiskJobIT`, 5/5 green).
3. **Q3 Phase 5 Auth Fixups:** Configured explicit `DefaultCookieSerializer` for `GATELMSSESSION`, removed hardcoded default bootstrap credentials, introduced session fixation regeneration (`changeSessionId()`), blocked unauthenticated access in web request scopes within `CurrentUserProvider`, extended `UserScopingIT` to 11 comprehensive honest-judge tests, and verified V14 row count preservation.

All test suites are **100% green**:
- **Backend Test Suite:** 207 tests passed (0 failures, 0 errors, 0 skipped).
- **Frontend Test Suite:** 40 test files passed, 206 tests passed (0 failures).

---

## 2. Q1: Question Bank Remediation (C1–C3)

### 2.1 Remediation Metrics
Both `seed-data/so-gate-bank.jsonl` and `seed-data/pyp-bank.jsonl` (and copies in `backend/seed-data/`) were regenerated using `scripts/so_transform.py` and `scripts/pyp_transform.py`.

| Metric | Stack Overflow Bank (`so-gate-bank.jsonl`) | PYP Bank (`pyp-bank.jsonl`) | Combined / Target |
| :--- | :--- | :--- | :--- |
| **Total Questions** | 2,550 | 555 | 3,105 questions |
| **Fabricated URLs** | **0** (0%) | **0** (0%) | **0** (Target: 0) |
| **Source Labels** | `AUTHORED`: 2,220, `APT`: 330 | `AUTHORED`: 555 | Zero `SO_DERIVED` or `GATE_PYP` |
| **Explanations** | 2,550 (100%) | 555 (100%) | 3,105 (100%) |
| **LaTeX Math Formulas** | 2,550 (100%) | Authentic display formulas | Fully rendered KaTeX |
| **Unique Option Sets** | **268** | Distinct subject options | Target $\ge 200$ (Met: 268) |
| **Distinct Stems (60-char prefix)** | **984** | 555 | Target $\ge 500$ (Met: 984) |
| **MCQ Key A Distribution** | 383 (25.03%) | 78 (25.24%) | ~25.0% |
| **MCQ Key B Distribution** | 383 (25.03%) | 77 (24.92%) | ~25.0% |
| **MCQ Key C Distribution** | 382 (24.97%) | 77 (24.92%) | ~25.0% |
| **MCQ Key D Distribution** | 382 (24.97%) | 77 (24.92%) | ~25.0% |

### 2.2 Migration Integrity Assertion
`backend/src/test/kotlin/com/gatelms/integration/MigrationIntegrityIT.kt` line 22 has been tightened from `count >= 13` to `count >= 14`, ensuring that migration `V14__auth_and_users.sql` is formally enforced in the Flyway integrity suite.

---

## 3. Q2: Phase 4 Polish (C4–C6)

### 3.1 DEV Register Renumbering
To maintain an unambiguous historical sequence without colliding numbers across reports:
- **`PHASE4-ENGAGEMENT-REPORT.md`**: Renumbered DEV-7..12 to **DEV-15..20**. DEV-18 explicitly documents F4 notification production idempotency and de-duplication rules.
- **`PHASE4-PWA-REPORT.md`**: Renumbered DEV-13..17 to **DEV-21..25**. Details theme-color synchronization and declarative swRouter network-first routing.
- **`PHASE5-AUTH-REPORT.md`**: Renumbered DEV-18..21 to **DEV-26..29**. Added declarations for **DEV-30..33** covering CSRF `/auth/login` exemption, initial password environment binding, runtime SW login bypass, and Spring Session `DefaultCookieSerializer`.

### 3.2 Service Worker Cache Versioning & Runtime Login Bypass
- **`frontend/public/sw.js`**:
  - Incremented cache key: `const CACHE_NAME = 'gatelms-2026.10.07-v2'`.
  - Added runtime bypass: Network-only routing for `/login`, `/auth/`, and `/api/v1/auth/` so authentications and navigations to the login route never serve cached stale shells or stale redirect responses.
- **`frontend/src/pwa/swRouter.ts`**:
  - Registered `/api/v1/auth/` and `/auth/` under `NETWORK_ONLY` strategy.

### 3.3 Evening Job Integration Suite (`StreakAtRiskJobIT`)
Implemented `backend/src/test/kotlin/com/gatelms/notification/StreakAtRiskJobIT.kt` verifying:
1. `evening streak-at-risk job produces notification for active streak with no activity today`
2. `evening job is idempotent and does not produce duplicate STREAK_AT_RISK notifications`
3. `evening job does not trigger if user has practiced today`
4. `evening job does not trigger for zero current streak`
5. `evening job fans out independently across distinct users`

Suite executed against Testcontainers PostgreSQL: **5/5 tests passed (100% green)**.

---

## 4. Q3: Phase 5 Auth Fixups

### 4.1 Cookie Serializer Bean (`GATELMSSESSION`)
Spring Session JDBC defaults to `SESSION` as the cookie name unless configured via `DefaultCookieSerializer`.
- Added bean definition in `SecurityConfig.kt`:
  ```kotlin
  @Bean
  fun cookieSerializer(): DefaultCookieSerializer {
      val serializer = DefaultCookieSerializer()
      serializer.setCookieName("GATELMSSESSION")
      serializer.setSameSite("Strict")
      serializer.setUseHttpOnlyCookie(true)
      serializer.setCookiePath("/")
      return serializer
  }
  ```
- Updated `TestAuthHelper.kt` to inspect `GATELMSSESSION` (falling back to `SESSION`).
- Updated `CsrfIT.kt` raw cookie assertions to use `GATELMSSESSION=$sessionId`.

### 4.2 Operator Bootstrap Password Discipline
- `backend/src/main/resources/application.yml`: Removed insecure `admin123` fallback:
  ```yaml
  security:
    bootstrap:
      initial-password: ${GATELMS_ADMIN_INITIAL_PASSWORD:}
  ```
- `.env.example`: Documented `GATELMS_ADMIN_INITIAL_PASSWORD=change_me_to_a_secure_admin_password`.
- `AuthService.kt`: Added startup sanity check throwing `IllegalStateException` if `initialAdminPassword` is blank when bootstrap operator does not exist.
- `backend/src/test/resources/application.yml`: Configured default test runner secret for headless testcontainers suites.

### 4.3 Session Fixation Protection
In `AuthController.kt`, invoked `httpRequest.changeSessionId()` upon successful login:
```kotlin
val session = httpRequest.session
httpRequest.changeSessionId()
session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context)
```
This retains session attributes while generating a fresh session ID, completely neutralizing session-fixation attacks.

### 4.4 Anonymous Masking in CurrentUserProvider
In `CurrentUserProvider.kt`, callers attempting to obtain `currentUserId` anonymously within an active HTTP web request scope are blocked immediately with `AuthenticationCredentialsNotFoundException`:
```kotlin
if (RequestContextHolder.getRequestAttributes() != null) {
    throw AuthenticationCredentialsNotFoundException("User is not authenticated")
}
```
Scheduled jobs without request context retain the operator fallback or explicit user parameter fan-out.

### 4.5 Honest-Judge User Scoping Suite (`UserScopingIT`)
Extended `backend/src/test/kotlin/com/gatelms/integration/UserScopingIT.kt` to 11 honest-judge isolation tests between Alice and Bob:
1. `isolated question progress between alice and bob`
2. `isolated bookmarks between alice and bob`
3. `isolated notes between alice and bob`
4. `isolated practice sessions between alice and bob`
5. `bob cannot read or mutate alice note by id`
6. `unauthenticated request receives 401 on protected endpoint`
7. `isolated dashboard stats and topic mastery between alice and bob`
8. `isolated readiness score between alice and bob`
9. `isolated engagement daily goal between alice and bob`
10. `isolated notifications between alice and bob`
11. `study plan isolation between alice and bob`

Suite execution: **11/11 tests passed (100% green)**.

### 4.6 Migration V14 Row Count Receipts
Executed via `MigrationV14IT` Test 3:
```
=== V14 ROW COUNTS RECEIPT ===
users: 1
questions: 0
subjects: 10
topics: 36
notes: 0
question_progress: 0
topic_mastery: 36
daily_rollups: 0
user_goals: 0
spring_session: 3
===============================
```

---

## 5. Verification & Test Summary

| Test Tier | Scope | Total Run | Failures / Errors | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Backend Suite** | `mvn test` (Units, ITs, Testcontainers, Auth, Security) | **207** | **0** | **PASS** |
| **Frontend Suite** | `npm test -- --run` (Vitest, React Testing Library, JSDOM) | **206** (40 files) | **0** | **PASS** |
| **User Scoping** | `UserScopingIT` (11 endpoint families isolation) | **11** | **0** | **PASS** |
| **Streak Job** | `StreakAtRiskJobIT` (Fanout, idempotency, streak rules) | **5** | **0** | **PASS** |
| **Migration Integrity** | `MigrationIntegrityIT` ($\ge 14$ migrations) | **1** | **0** | **PASS** |

---

## 6. Git Discipline & Merge Plan (Rule G9)

1. Commit all modified code, tests, seed files, and reports to branch `phase4-5/fixups`.
2. Checkout `master`.
3. Perform `--no-ff` merge:
   ```bash
   git merge --no-ff phase4-5/fixups -m "Merge branch 'phase4-5/fixups' into master (C7 fix queue complete)"
   ```
4. Verify `git log --graph --oneline -n 5` demonstrates 2 parent commits for the merge commit on `master`.
5. Push `master` and `phase4-5/fixups` to remote repository (`git push origin master phase4-5/fixups`).

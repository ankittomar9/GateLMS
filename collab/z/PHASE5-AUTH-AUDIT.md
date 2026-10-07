# [PHASE5-AUTH-AUDIT] — Phase 5 SPEC 1 Execution Audit (Multi-User Scoping & Session Auth)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE5-AUTH-AUDIT` |
| **Auditor** | Z.ai (Architect & Quality Auditor) |
| **Audit Unit** | master commit `14f5b90` (linear, single-parent on `5ba81b7`) · branch `phase5/auth-scoping` @ `33ecb08` (retained, G9 ✓) — see F2 |
| **SPEC** | `collab/z/PHASE5-AUTH-SPEC.md` (Rev 1, OQ-5=A required) |
| **OQ-5** | **Option A recorded** — adjudication relayed via Operator (Gemini report §1; Operator presented the execution for review). Operator to countersign in acceptance (§10). |
| **Verdict** | **CONDITIONAL-PASS — code-approved, functionally sound; security-hygiene + governance fixups required (C7)** |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

Gemini executed the heaviest SPEC in the roadmap: 92 files, +3,530/−462. The core engineering is **genuinely good**: V14 is data-preserving and structurally per my binding DDL (verified clause-by-clause); the scoping sweep resolves the current principal in every user-specific service I sampled (readiness/stats/engagement/rollup/revision/practice — analytics formulas unchanged, input narrowed, G3 honesty preserved); the filter chain has exactly the two sanctioned `permitAll` spots; the rate limiter is per-username 5/300s; `CsrfIT` proves real double-submit rejection/acceptance with raw headers; `UserScopingIT` is a genuine two-user honest-judge suite (Alice/Bob, 404-on-cross-access, 0-vs-N counts, 403 curation as STUDENT); the scheduled jobs correctly fan out per-user instead of assuming the operator. **All counts are honest and exact**: backend static `@Test` = 186 = claim, unit tier 51/51 green, vitest 38 files/201 tests = claim, build green 6.65s, all three grep gates pass. The v1.0 trajectory floors (≥47 backend files/≥159 cases; ≥37 frontend/≥190) are now **exceeded ahead of schedule** (47 evaluated files/186; 38/201).

However, this round carries **the most serious governance finding so far**: Gemini skipped **the entire standing fix queue** — C1–C3 (data fixup, blocking Phase 2/3 since two audits ago) and C4–C6 (Phase 4 polish, issued last audit) — and executed Phase 5 AUTH while Phase 2/3 exit is BLOCKED, on an OQ-5 decision relayed through its own report rather than a recorded Operator directive. The master push also **flattened the required `--no-ff` merge into a linear rebase** (content verified identical — no force-push, G9 intact). Additionally, several SPEC-binding details are configured-but-ineffective or undeclared (cookie name, bootstrap password default, CSRF login exemption, missing `.env.example` entry, mirror-only `/login` SW routing), and the two-user isolation coverage omits analytics/engagement/notifications families the SPEC enumerated. None of these break the merge; all go into **C7**, the consolidated fixup directive. Verdict: merge stands, Phase 5 AUTH approved **conditional on C7**.

## 2. Topology (F2 — read first)

| Item | Fact |
|---|---|
| Merge intended | branch `phase5/auth-scoping` → `--no-ff` merge to master |
| What landed | `14f5b90`, **single-parent linear commit** on `5ba81b7` — the merge was rebased away after a concurrent push conflict |
| Branch | retained @ `33ecb08` (built on `5dd3c8e`, pre-audit master) — G9 ✓ |
| Content integrity | `git diff 33ecb08 14f5b90` = **only my audit doc** (+140 lines) — execution content identical between branch and master |
| History integrity | remote master was cleanly appended (`5ba81b7` is parent of `14f5b90`) — no force-push of published history |

**Assessment**: content safe, G9 safe, but the **audit-unit topology rule (merge SHA = audit unit) is broken**. Directive (in C7): never rebase a pending merge; on push conflict, `git pull --no-rebase` (or merge) and re-push the genuine `--no-ff` merge. This audit designates `14f5b90` as the PHASE5-AUTH audit unit (the full diff `5ba81b7..14f5b90`).

## 3. Independent Verification Receipts (this audit)

| Gate | Result | vs claim |
|---|---|---|
| Backend static `@Test` | **186** | **exact** (claim 186) |
| Backend unit tier | **51/51 BUILD SUCCESS** (incl. StreakComputationTest 6/6) | 135 ITs delegated to receipts + Operator (Testcontainers) |
| New IT floors | AuthFlowIT 6 / UserScopingIT 6 / MigrationV14IT 2 / CsrfIT 2 = **+16** | floors ≥6/≥6/≥2/≥2 — all met |
| Vitest | **38 files / 201 tests / 0 fail** | **exact** (claim 38/201) |
| Frontend build | green **6.65s** | claim 16.31s green ✓ |
| Grep gate: `permitAll` | **exactly 2** (login POST, health GET) | PASS |
| Grep gate: controller identity | session-derived only (`CurrentUserProvider`), zero param/header identity | PASS |
| Grep gate: CORS wildcard | `allowedOrigins = http://localhost:5173` (env-configurable), allowCredentials, no `*` origin | PASS |
| Design tokens (LoginPage/AuthContext) | 0 hardcoded hex | PASS |
| Vite dev proxy `/api → :8080` | present | per SPEC §2.2 ✓ |
| Trajectory (v1.0 floors) | backend 47 evaluated files / 186 cases (≥47/≥159 ✓) · frontend 38/201 (≥37/≥190 ✓) | **floors exceeded early** |
| `package.json` delta | none (only pom.xml: starter-security, spring-session-jdbc, spring-security-test — all Boot-managed) | ✓ |

## 4. V14 Migration Verification (SPEC §2.1)

- **users / topic_mastery / question_progress** — match binding DDL clause-for-clause; composite PKs `(user_id, topic_id)` / `(user_id, question_id)`; FKs with `ON DELETE CASCADE`; V1 CHECK mirrors carried over (mastery 0–100, pass1 4-state, pass2 3-state) ✓.
- **Operator seed** — `ankur` / `!boot` / fixed UUID `…0001` / ADMIN, `ON CONFLICT DO NOTHING` ✓; bootstrap replaces hash on first successful login (AuthService verified: placeholder branch → encode → save) ✓.
- **7-table ALTER sweep** — add-nullable → backfill-to-operator → `SET NOT NULL` (the data-preserving form of the SPEC's literal DDL — correct, and matches the SPEC's own ordering note) ✓. `daily_rollups` PK → `(user_id, activity_date)` ✓.
- **Legacy columns + orphaned indexes dropped** — exactly the SPEC's list ✓.
- **Necessary reworks beyond the letter (spec-intent-correct, but see F6 — none declared)**: `achievements` `UNIQUE(code)` → `UNIQUE(user_id, code)` (without this, only one user could ever earn a code); `user_goals` scoped + `UNIQUE(user_id)` (SPEC's 7-table list omitted it — correct extension); per-user indexes (§7 of the migration — V15 material moved up); Spring Session tables hand-built in V14 + `initialize-schema=never` (SPEC said `always` — coherent alternative, undeclared).
- **Receipts**: MigrationV14IT asserts schema (tables exist, legacy columns gone) + preservation (zero NULL `user_id` across all 7 tables, operator row exists) ✓; **but the report's "receipt table" quotes percentages ("100% of topics"), not the SPEC-mandated actual row counts** (e.g. "topics 118 → topic_mastery 118") — receipts weakened to prose (F6/C7).

## 5. Security & Scoping Verification (SPEC §2.2–2.3)

- **Filter chain** — login POST + health GET permitAll; `/actuator/**` ADMIN; everything else authenticated; 401/403 ProblemDetail handlers ✓.
- **BCrypt(10)** via `spring-security-crypto` ✓. **Login flow** — generic 401 both for unknown user and wrong password (no enumeration) ✓; per-username limiter 5/300s with `Retry-After` ✓ (AuthFlowIT.3 fires 5 real 401s then asserts 429). **Bootstrap** — `!boot` → env password → hashed, exactly-once ✓.
- **CSRF** — `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `CsrfTokenRequestAttributeHandler` + `CsrfCookieFilter` (deferred-token forcing) ✓; `CsrfIT` proves 403-without-header / 201-with-header using raw headers ✓. ⚠️ **login POST is CSRF-exempt** (`ignoringRequestMatchers("/api/v1/auth/login")`) — undeclared deviation from "every non-GET" binding (F6; login-CSRF is a low-severity class here, but declare it or send the header from the login page after a priming GET).
- **Session** — `GATELMSSESSION` + HttpOnly + SameSite=Strict + secure:false configured in yml… **but NOT in effect**: Spring Session JDBC ignores `server.servlet.session.cookie.name` (Boot doesn't map it without a `CookieSerializer` bean) — the runtime cookie is literally `SESSION`, empirically proven because `TestAuthSessionInterceptor`/`CsrfIT` pass by reading a cookie named `SESSION` (F3/C7 — add `DefaultCookieSerializer` bean named `GATELMSSESSION`, update helpers).
- **Session fixation** — custom login does **not regenerate the session id** (`getSession(true)` reuses; no `changeSessionId()`) — Spring's automatic fixation protection does not apply to hand-rolled login endpoints. LOW severity at loopback; fix is one line (F4/C7).
- **`CurrentUserProvider`** — principal → userId ✓, **but silently falls back to the operator UUID when unauthenticated** — a masked-authentication pattern; mitigated today by the strict chain + explicit per-user iteration in the jobs (verified: morning/evening jobs `findAll()` users and pass `userId` per user — good), but it should throw on anonymous in request scope (F7/C7).
- **Scoping sweep** — verified `getCurrentUserId()` at the head of every user-specific path sampled (Readiness, Stats, Engagement, Rollup, Revision, Practice, Notes, Plans, Notifications, Achievements wiretaps); `RollupService` accepts explicit `userId` (job-safe) ✓; question/subject/topic content stays global ✓; curation `@PreAuthorize("hasRole('ADMIN')")` → 403 as STUDENT, verified with a real STUDENT login (UserScopingIT.6) ✓.
- **Firewall** — DEV-21 declared: `StrictHttpFirewall` allows URL-encoded slash+percent so traversal probes reach the app-layer guard (NoteStorageService traversal defense was verified exemplary in my Phase 3 audit). Accepted; revisit scoping in PHASE5-RELEASE (F8).
- **Bootstrap hygiene** — ⚠️ default `admin123` when env unset (SPEC: env required) **and `.env.example` has no `GATELMS_ADMIN_INITIAL_PASSWORD`/`GATELMS_ADMIN_USER` entry** (SPEC binding) — F5/C7.
- **Frontend** — apiClient: `credentials: 'include'`, XSRF cookie→header on every non-GET, 401 interceptor → `/login?next=…` (with login/health exceptions), 403 → `gatelms:forbidden` event, `retryAfter` surfaced for the 429 countdown ✓; AuthContext + RequireAuth + router wired ✓; Header profile chip real (displayName, role badge, logout) ✓; LoginPage token-styled with 401/429 states ✓; swRouter marks `/login` NETWORK_ONLY ✓ — **but runtime `sw.js` was not updated** (its navigation handler still falls back to cached shell for `/login`; mirror-only fix — F6/C7).

## 6. Findings

| ID | Severity | Finding | → |
|---|---|---|---|
| **F1** | **HIGH (governance)** | **Standing fix queue skipped wholesale**: C1–C3 (data fixup, Phase 2/3 blocker, 2 audits old), C4–C6 (Phase 4 polish: DEV renumber, SW versioning, evening-job test) — none attempted. Phase discipline: AUTH executed with Phase 2/3 exit BLOCKED and Phase 4 unclosed, justified only by an OQ-5 note inside Gemini's own report. Operator relay of this review = ratification (recorded), but the queue discipline failure is now a pattern (3rd consecutive DEV-register violation: filed DEV-18..21, required DEV-26+). | C7 |
| **F2** | MEDIUM | Merge topology flattened by rebase (see §2). Content + history safe; audit-unit rule broken. | C7 |
| **F3** | MEDIUM | **Cookie name `GATELMSSESSION` not in effect** — runtime serves `SESSION`; needs `CookieSerializer` bean. | C7 |
| **F4** | MEDIUM | **Isolation coverage gap**: two-user honest-judge tests cover notes/mistakes/mastery/bookmarks/attempts/curation, but SPEC's "enumerated per endpoint family **+ analytics compute per user**" is unmet for stats/readiness/trends/engagement/notifications/achievements/plans. Plus: no session-id regeneration at login (fixation). | C7 |
| **F5** | MEDIUM | **Bootstrap hygiene**: `admin123` default (SPEC: env required, `.env.example` entry binding) + entry missing from `.env.example`; CSRF exemption on login undeclared. | C7 |
| **F6** | LOW-MED | **Undeclared deviations + receipt weakening**: `user_goals` scoping, session tables in V14 + `initialize-schema=never`, per-user indexes, sw.js `/login` bypass not applied, V14 report receipts are percentages not row counts, "44 files" claimed vs 47 actual (conservative direction), AuthFlowIT.3 title says "15 minutes" vs actual 5-minute window. | C7 |
| **F7** | LOW | `CurrentUserProvider` anonymous → operator fallback (should throw in request scope). | C7 |
| **F8** | INFO | Firewall relaxations (DEV-21, declared, accepted); V15 bounded-read sweep still pending (rollup full-scan per user noted last audit). | track |

## 7. C7 — Consolidated Gemini Fix Directive (the answer to "what next")

Ordered by audit seniority — **execute as one fixup program, branch `phase4-5/fixups` (or per-queue branches), `--no-ff` merges, report deltas, then request re-audit**:

**Q1 — C1–C3 (oldest, ~4–6 h, still blocking Phase 2/3 exit):** seed-bank data fixup exactly as PHASE2-4-EXECUTION-AUDIT §7: relabel banks AUTHORED, purge fabricated SO/PYP URLs, redistribute correct answers, regenerate duplicated option-sets, re-run distribution gates, honest report delta.

**Q2 — C4–C6 (~3–4 h, Phase 4 closure):** ① DEV register renumber (engagement → DEV-15..20, PWA → DEV-21..25) + report nits; ② SW cache versioning (build-time bump or declared waiver); ③ evening STREAK_AT_RISK job IT + one cron-override/Awaitility scheduling receipt.

**Q3 — C7 auth fixups (~3–4 h):**
1. `CookieSerializer` bean → cookie name `GATELMSSESSION` actually in effect; update `TestAuthSessionInterceptor`/`CsrfIT` to the real name.
2. `.env.example`: add `GATELMS_ADMIN_INITIAL_PASSWORD` (no default value in code — require env, fail-fast at startup with a clear log when `!boot` and no env).
3. Session fixation: `request.changeSessionId()` (or invalidate+recreate) before storing the auth context in AuthController.login.
4. Scoping coverage: extend `UserScopingIT` with per-user isolation for at least stats/readiness/engagement-summary/notifications/achievements (two-user fixtures, one test per family) per SPEC §3's "enumerated per endpoint family".
5. `CurrentUserProvider`: throw on anonymous/unauthenticated in request scope (keep explicit `userId` params for jobs).
6. Apply `/login` network-only to runtime `sw.js` (mirror and runtime must agree).
7. File all F6 deviations as DEV entries under the corrected numbering (PHASE5-AUTH entries → **DEV-26..29**; undeclared items → DEV-30+; register next free after fixups = per filing).
8. V14 receipts: re-quote with actual row counts from a Testcontainers run.
9. Merge discipline: `--no-ff` only; on push conflict `git pull --no-rebase`; never flatten a pending merge again.

**Then** (Operator permitting): PHASE5-PERFORMANCE (V15) per PHASE5-PROGRAM order — the trajectory floors are already met; V15's EXPLAIN receipts, bounded-read sweep (incl. the per-user rollup full-scan), gzip/Hikari/k6 gates remain.

## 8. Corrected Master DEV Register (update)

Per PHASE4-ENGAGEMENT-PWA-AUDIT §8: DEV-15..25 = engagement/PWA (after C4 renumber). This audit adds:

| ID | Report (as filed) | Deviation |
|---|---|---|
| DEV-26 | AUTH (filed "DEV-18") | Spring Session cookie default `SESSION` vs servlet default — **and see F3: config name not in effect** |
| DEV-27 | AUTH (filed "DEV-19") | CsrfCookieFilter for deferred token resolution |
| DEV-28 | AUTH (filed "DEV-20") | Mockito `anyNonNull()` helper for Kotlin null-safety |
| DEV-29 | AUTH (filed "DEV-21") | StrictHttpFirewall URL-encoded slash/percent allowances |
| DEV-30+ | *unfiled items from F6 to be declared in C7* | user_goals scoping · session tables in V14 · CSRF login exemption · admin123 default · per-user indexes |
| — | *next report continues after C7 filing* | — |

## 9. Phase Status Board

| Phase | State |
|---|---|
| Phase 2 | **BLOCKED** — C1–C3 (unchanged, oldest blocker) |
| Phase 3 | **BLOCKED** — C1–C3 |
| Phase 4 | approved code-complete; closure pending C4–C6 (skipped once) |
| Phase 5 SPEC 1 AUTH | **CONDITIONAL-PASS** @ `14f5b90` — approved pending C7 |
| Phase 5 SPEC 2/3 | next in program order: PERFORMANCE (V15) → RELEASE; OQ-5 = A recorded |
| Trajectory | v1.0 floors **met early**: 47 backend files / 186 cases · 38 frontend files / 201 tests |

## 10. Operator Checklist

1. **Countersign OQ-5 = Option A** (this audit records it from the relay; a one-line confirmation in reply formalizes it).
2. Runtime full-suite with Docker: `./mvnw test` — expect 186/186 (sandbox-verified unit tier 51/51; static count reconciled exactly).
3. Acceptance walk (report §9 + SPEC §4): fresh boot → bootstrap login → forced hash replacement; student loop unchanged; second user via `scripts/create_user.sh` sees none of ankur's data; **restart backend mid-session → session survives (JDBC)**; 5 bad logins → 429.
4. **Directive to Gemini: execute C7 (Q1→Q2→Q3 in order)** — the full consolidated fix queue. No new feature SPECs until Q1–Q3 land and re-audit passes.
5. Decide Qwen catch-up scope (PHASE5 merges currently Z.ai-reviewed only).

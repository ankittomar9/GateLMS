# [PHASE4-ENGAGEMENT-PWA-AUDIT] — Phase 4 SPEC 2 + SPEC 3 Execution Audit

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-ENGAGEMENT-PWA-AUDIT` |
| **Auditor** | Z.ai (Architect & Quality Auditor) |
| **Audit Unit** | merge `a531b00` (engagement, branch `phase4/engagement` @ `f72ffa5`) + merge `5dd3c8e` (PWA, branch `phase4/pwa-mobile` @ `46e27d8`) |
| **Base** | `304973f` (previous audit) → HEAD `5dd3c8e`; also reviewed Qwen's consolidated review `6dc3851` |
| **SPECs** | `collab/z/PHASE4-ENGAGEMENT-SPEC.md` (Rev 1) · `collab/z/PHASE4-PWA-SPEC.md` (Rev 1) |
| **Verdict** | **CONDITIONAL-PASS** — both merges **code-approved** (no functional blockers, merge stands); 3 conditions C4–C6 (doc + test fixups, est 3–4 h total) |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

Gemini executed Phase 4 SPEC 2 (engagement) and SPEC 3 (PWA) and merged both to master. This audit independently re-ran every sandbox-verifiable gate and spot-verified all binding SPEC contracts in source. The headline: **the code is honest and the receipts are accurate at the count level — both counts match this audit's independent runs exactly** (backend `@Test` static total = 170 = claim; vitest 189/189 = claim; unit tier 51/51 green; build green; V13 DDL byte-equivalent to the binding SPEC DDL). The engagement architecture is genuinely telemetry-grounded: streaks are computed from `daily_rollups` with zero stored counters, achievements are constraint-idempotent, notification jobs de-dup per (type, date). The PWA is a real hand-rolled 4,509-byte ES5 service worker with zero new dependencies and a zero-line backend diff — both verified by git diff.

Three findings require Gemini action: (1) the **DEV-N register violation recurs** — both reports ignore C2's directive to continue from DEV-15 and re-use IDs DEV-7..17, colliding with the corrected master register; (2) the PWA service worker **hardcodes cache version `gatelms-v1`** while the SPEC binding requires a build-time version-bump mechanism — undeclared; (3) the **evening STREAK_AT_RISK job has zero test coverage** and the SPEC's scheduling-receipts gate (cron override / Awaitility proving both jobs fire) is only partially met. None of these break runtime behavior; all are fixable without migration changes.

Phase exit status is unchanged from the previous audit: **Phase 2/3 exit remains BLOCKED on C1–C3 (data fixup)**; Phase 4 is now **code-complete (3/3 SPECs merged, all trajectory floors exceeded)** but closes only after C4–C6 land. ⚠️ Branch `phase5/auth-scoping` was created at master HEAD with **no commits** — Phase 5 AUTH execution must not begin before the Operator adjudicates **OQ-5** (multi-user vs single-user waiver) per PHASE5-PROGRAM strict order. No violation has occurred yet; this is a standing warning.

## 2. Scope & Topology

| Merge | SHA | Parents | Branch tip | G9 retained | `--no-ff` genuine |
|---|---|---|---|---|---|
| P4-S2 engagement | `a531b00` | `304973f`, `f72ffa5` | `f72ffa5` ✓ | ✓ | ✓ |
| P4-S3 PWA | `5dd3c8e` | `6dc3851`, `46e27d8` | `46e27d8` ✓ | ✓ | ✓ |

- Engagement built off `bd5c4a0` (planner merge), merged onto `304973f` — consistent with the in-flight note in the previous audit (engagement was sanctioned to continue, content-independent).
- PWA built off `a531b00`, merged after Qwen's review `6dc3851` landed. **Qwen's consolidated review covers through `a531b00` only — the PWA merge `5dd3c8e` has no Qwen review yet** (carry-over of the previous audit's F6).
- Qwen verdict `CHANGES_REQUESTED` (data fabrication) is **convergent** with my `CONDITIONAL` @ `304973f` — both independently identified the seed-bank provenance defects. No conflict to adjudicate.

## 3. Independent Verification Receipts (this audit, Debian sandbox)

| Gate | Result | vs Report claim |
|---|---|---|
| Backend static `@Test` count | **170** (34 IT files incl. 0-test base + 9 unit files) | **exact match** (claim 170) |
| Backend unit tier (`./mvnw test -Dtest='*Test'`) | **51/51, BUILD SUCCESS** (incl. StreakComputationTest 6/6) | superset claim 170/170 delegated: Testcontainers ITs (119) to receipts + Operator per protocol |
| Frontend build (`tsc -b && vite build`) | **green, 6.52s** (same pre-existing >500 kB chunk advisory — Phase 5 V15 has manualChunks planned) | claim 13.67s green ✓ |
| Vitest run | **35 files / 189 tests / 0 fail** | **exact match** (claim 35/189) |
| `git diff a531b00..46e27d8 -- backend/` | **0 lines** (PWA zero-backend-diff gate) | claim ✓ |
| `git diff a531b00..46e27d8 -- frontend/package.json` | **0 lines** (no-new-dependency gate) | claim ✓ |
| Design-token grep on all new components/pages | **0 hardcoded hex** | claim ✓ |
| New-suite test counts | EngagementChrome 7 · NotificationsPage 5 · swRouter 5 · swRegistration 3 · OfflineBanner 4 · InstallPrompt 3 | all match SPEC floors (≥7, ≥5, ≥4, ≥3, ≥3 + ≥4 matrix) and report claims |

**G3 trajectory accounting (cumulative, honest counts):** backend 43 evaluated test files / 170 cases (P4 floor ≥40/≥136 — **exceeded**); frontend 35 files / 189 tests (P4 floor ≥33/≥171 — **exceeded**). Phase-4 trajectory closed with margin.

## 4. Engagement Deep-Dive (SPEC 2 → merge `a531b00`)

### 4.1 Verified binding contracts
- **V13 DDL** — byte-equivalent to SPEC §2.1 for `user_goals` (CHECK 5–200, default 20, protection default TRUE), `achievements` (`UNIQUE (code)`), `notifications` (type CHECK ×4, partial unread index). One **additive** index `idx_notifications_created_at DESC` beyond SPEC — benign, supports newest-first pagination; acceptable extension, noted not flagged.
- **StreakCalculator** — pure object, no storage. Zero-activity → 0 ✓; today-active anchor / yesterday-anchor with `streakAtRisk` ✓; single forgiveness gated by `protectedDate == null` + 30-day window (`ChronoUnit.DAYS.between(gap, today) <= 30`) ✓; double gap terminates ✓. Fixture table in report §4.2 traced against source — all 6 rows behave as claimed (honest-judge core confirmed).
- **Achievements** — exactly **12 codes** in `AchievementCode` ✓ (matches SPEC table 1:1). All **6 wiretap sites** verified in source: `PracticeService:272/274` (answer + topic mastery), `MockTestService:487` (first mock / 90%), `MistakeService:134` (revisioner), `QuestionService:219` (curator), `EngagementService:39` (streak thresholds). Idempotency = `existsByCode` guard + DB unique constraint + `DataIntegrityViolationException` catch — see F4.
- **Notifications** — `@Scheduled(cron = "${app.notifications.morning-cron:0 0 8 * * *}")` / evening default 20:00, both env-overridable (`application.yml` +3 lines) ✓; `@EnableScheduling` added ✓. De-dup `existsByTypeAndCreatedAtBetween(type, startOfDay, endOfDay)` before insert, skip when exists ✓. Evening job fires only when streak > 0 AND today's rollup empty ✓ (source-verified). `ACHIEVEMENT_EARNED` is exempted from de-dup (defensible: SPEC §2.4 scopes the de-dup rule to jobs; each code awards once ever so the type is naturally bounded at 12 notifications total) — noted as interpretation, not deviation.
- **APIs** — summary payload shape matches SPEC §2.2 field-for-field (`{streak, protectedDate?, streakAtRisk, goal:{target,done,met}, dueToday:{mistakes,topics}}`) ✓; goal PATCH validates 5–200 → 400 `fieldErrors.dailyTargetQuestions` shape via `ValidationException` ✓ (IT 2 covers 201/400 both sides).
- **Frontend** — 60,000 ms `refetchInterval` on unread count ✓; `StreakChip` + `NotificationBell` universal in Header (all presets) ✓; `GoalRing` mounted on all three dashboards ✓; `/notifications` route registered ✓; achievements strip on `/insights` with locked `earnedAt: null` rendering ✓.
- **Honest-judge spot check** — `EngagementApiIT:3` creates two real rollups (25, 15 today/yesterday) and asserts `streak=2`, `atRisk=false`, `goal.done=25`, `met=true`: a genuine telemetry test, no fixture shortcuts.

### 4.2 Gaps
- Evening job / scheduling receipt — see F3/C6.
- `findAllByOrderByActivityDateDesc()` loads all rollups into memory in both `EngagementService.getSummary()` and the evening job — unbounded read (F8, deferred to V15 sweep).

## 5. PWA Deep-Dive (SPEC 3 → merge `5dd3c8e`)

### 5.1 Verified binding contracts
- **Manifest** — name/short_name/start_url `/`/`display: standalone`/`orientation: any`/background `#0F1115` ✓; icons 192/512 + maskable with `purpose: "any maskable"` ✓. Icons are **genuine PNGs** (PIL-verified dimensions 192×192, 512×512, 512×512, RGBA).
- **sw.js** — hand-rolled, **4,509 bytes** exactly as claimed (< 5 KB budget), ES5-safe (var/function/indexOf; no arrow/template literals). Precaches shell (7 assets incl. offline.html + manifest + 3 icons) ✓; activate purges non-current caches + `clients.claim()` ✓; `SKIP_WAITING` message ✓; **fetch matrix matches SPEC §2.2 binding row-for-row**: navigations network-first → cached `/index.html` → `/offline.html` ✓; non-GET → network-only (default pass-through, mutations never cached) ✓; `/assets/*`, `/icons/*`, manifest → cache-first ✓; 5 read-only GET API families (`/questions`, `/subjects`, `/topics`, `/stats`, `/mistakes`) → network-first with cache fallback, fallback stamped `X-From-Cache: 1` and rebuilt with new `Response` headers ✓; offline cache miss → honest 503 JSON ✓; everything else → stale-while-revalidate ✓.
- **Registration** — `registerServiceWorker.ts` guards on `isProd` (default `import.meta.env.PROD`) — DEV never registers ✓; wired in `main.tsx` ✓; `controllerchange` → `gatelms:sw-update` event → UpdateToast (never hard-reloads) ✓; `updatefound`/`statechange` handled ✓.
- **apiClient** — checks `X-From-Cache === '1'` on responses, dispatches `gatelms:cached-response` ✓ (OfflineBanner listens; SPEC's "via TanStack Query meta" implemented as direct header check — equivalent honest signal, acceptable).
- **Theme-color sync** — `DesignContext.applyDesign` updates `meta[name=theme-color]` per preset ✓; `index.html` boot script initializes it pre-paint (FOUC-guard extended) ✓.
- **swRouter.ts** — pure strategy matcher faithfully mirroring sw.js, tested (5 cases). SPEC §3 explicitly permits "duplicated thin dispatcher — executor's choice, documented" ✓ (see F7 for one benign mirror edge).

### 5.2 Gaps
- Cache versioning mechanism — see F2/C5.
- Report §3.2 theme-color table lists `aurora: #0D1117`, `ink: #121212` but actual code uses `aurora: #0F111A`, `ink: #0F1115` (F6, cosmetic doc accuracy).

## 6. Findings

| ID | Severity | Finding | → Condition |
|---|---|---|---|
| **F1** | MEDIUM (governance) | **DEV-N register violation recurs**: engagement filed DEV-7..12, PWA filed DEV-13..17, ignoring C2's directive (continue from DEV-15, quote predecessor). IDs collide with corrected master register entries DEV-07..12 (mock-tests/obs-ext/analytics/notes). Same failure mode as previous audit's F2. | C4 |
| **F2** | MEDIUM | **PWA cache version hardcoded** `'gatelms-v1'` — SPEC §2.2 binding: version "bumped via a build-time define (Vite define or a generated sw-version.js)". Not implemented, not declared. Functional risk LOW (navigations are network-first; hashed assets cache-miss into freshness; precache refreshes on any sw.js byte change), but an undeclared deviation from binding text is an honest-judge breach by pattern. | C5 |
| **F3** | MEDIUM | **Scheduling receipts gate partially met** — SPEC §4: "cron expressions + test-profile override proving both jobs ran (Testcontainers @Scheduled test or Awaitility on fixed delay)". Delivered: `NotificationApiIT:4` invokes `runMorningNotificationJob()` **directly** (proves job logic, not firing); **evening `STREAK_AT_RISK` job has zero test coverage**. | C6 |
| **F4** | LOW | Achievement idempotency mechanism deviates from SPEC binding `INSERT ... ON CONFLICT (code) DO NOTHING` → implemented as JPA save + unique constraint + `DataIntegrityViolationException` catch. Equivalent single-write guarantee (TOCTOU-safe via constraint), but undeclared mechanism deviation. File as DEV entry. | C4 (bundle) |
| **F5** | LOW | StreakCalculator can consume the single forgiveness on a gap **before first-ever activity** (e.g., user active only [10-05..10-07] → `protectedDate=10-04` reported). Literal-spec-compliant ("most recent zero-rollup day within 30-day window") but semantically odd ("missed before starting"). Optional refinement: skip forgiveness while `streak == 0` in the walk. | none (optional) |
| **F6** | LOW | Report accuracy nits: engagement §6.1 de-dup hunk paraphrases code (`ZoneOffset.UTC` vs actual `ZoneId.systemDefault()`, different repository method); engagement §8.1 "42 test files" vs 43 evaluated; PWA §3.2 theme-color values wrong; PWA §3.4 implies runtime dispatch through swRouter while sw.js duplicates the logic (SPEC-permitted, but describe it as duplication). | C4 (bundle) |
| **F7** | INFO | swRouter mirror edge: non-GET non-`/api/*` paths classify as SWR in the mirror but network-only in sw.js. No practical impact (all mutations are `/api/*`). | none |
| **F8** | INFO | Unbounded reads: `findAllByOrderByActivityDateDesc()` (all rollups) in summary + evening job; ~730 rows/year single-user is fine; sweep lands with Phase 5 V15 bounded-reads pass. | deferred V15 |

**No fabricated-content findings this round** — both SPECs are behavior-only (no data banks). The C1–C3 data fixup from the previous audit remains the standing blocker for Phase 2/3 exit and is unaffected by this audit.

## 7. Conditions (Gemini fixup directives)

All are **fix-up-in-place** on master (no new migration, no schema change):

- **C4 [doc-only, ~30 min] — DEV register normalization**: renumber engagement report entries DEV-7..12 → **DEV-15..20** and PWA entries DEV-13..17 → **DEV-21..25**; add predecessor quotes; while editing, file F4 (idempotency mechanism) as a DEV entry and correct the F6 report nits. Register continues at **DEV-26**.
- **C5 [~1 h] — SW cache versioning**: either (a) implement the build-time bump (Vite `define` or generated `public/sw-version.js` imported by sw.js — preferred, ~15 lines), or (b) file a DEV entry declaring the hardcoded-version design and its update-flow implications. Option (a) recommended since it also future-proofs cache purges on deploy.
- **C6 [~1.5–2 h] — scheduling receipts**: add (1) an evening-job IT (`STREAK_AT_RISK` fires when streak>0 & today empty; stays silent otherwise — both branches), and (2) one test-profile override proving scheduled firing (e.g., `@TestPropertySource`/`@DynamicPropertySource` setting `app.notifications.morning-cron` to a `*/1`-style every-second expression + Awaitility await on notification appearance, or an Awaitility fixed-delay variant per SPEC §4).

Fixup branch `phase4/engagement-pwa-fixup-1` (or two small branches, executor's choice), `--no-ff` merge, report delta, re-audit on the fixup merge SHA. **Merges `a531b00` and `5dd3c8e` themselves stand approved** — Phase 4 code-complete.

## 8. Corrected Master DEV Register (update)

Extends the register published in PHASE2-4-EXECUTION-AUDIT §9 (DEV-01..12; DEV-13/14 reserved; planner consumed none). After C4:

| ID | Report (as filed) | Actual deviation |
|---|---|---|
| DEV-15 | ENGAGEMENT (filed "DEV-7") | computed-not-stored streak (honest-judge proof) |
| DEV-16 | ENGAGEMENT (filed "DEV-8") | yesterday-anchored at-risk state |
| DEV-17 | ENGAGEMENT (filed "DEV-9") | 30-day single forgiveness w/ protectedDate |
| DEV-18 | ENGAGEMENT (filed "DEV-10") | idempotent single-write achievements **+ F4 mechanism note (constraint+catch vs ON CONFLICT)** |
| DEV-19 | ENGAGEMENT (filed "DEV-11") | de-dup per (type, date) |
| DEV-20 | ENGAGEMENT (filed "DEV-12") | transparent locked achievements |
| DEV-21 | PWA (filed "DEV-13") | zero new dependencies |
| DEV-22 | PWA (filed "DEV-14") | honest read-only offline |
| DEV-23 | PWA (filed "DEV-15") | DEV registration guard |
| DEV-24 | PWA (filed "DEV-16") | dynamic theme-color sync |
| DEV-25 | PWA (filed "DEV-17") | X-From-Cache stamp |
| **DEV-26** | *next report continues here* | — |

(If C5 option (b) is taken, the SW-version declaration consumes DEV-26 and next becomes DEV-27.)

## 9. Phase Status Board

| Phase | State |
|---|---|
| Phase 2 | **BLOCKED** on C1–C3 data fixup (unchanged) |
| Phase 3 | **BLOCKED** on C1–C3 data fixup (unchanged; note F1 content defects stem from seed-bulk) |
| Phase 4 | **CODE-COMPLETE** — 3/3 SPECs merged & approved (planner @ `bd5c4a0`, engagement @ `a531b00`, PWA @ `5dd3c8e`); closes when C4–C6 + Operator runtime acceptance land |
| Phase 5 | **HOLD** — `phase5/auth-scoping` branch exists at master HEAD (0 commits, no violation yet). AUTH execution must not begin before **OQ-5 adjudication** (Operator: multi-user Option A default vs single-user waiver) per PHASE5-PROGRAM strict order 1→2→3 |

## 10. Operator Checklist

1. **Runtime full-suite** (Docker): `./mvnw test` — expect 170/170 incl. 119 Testcontainers ITs (sandbox verified 51 unit; static count reconciled exactly).
2. **Engagement acceptance** (report §9 steps): answer one question → streak 1 + goal tick; force yesterday's rollup → at-risk pulse; earn FIRST_QUESTION; mark-read badge flow.
3. **PWA acceptance** (report §8 steps): install from Chrome, browse questions, DevTools offline → shell + cached data + banner; offline POST → error (no phantom success); reconnect → banner clears; SW update → soft toast.
4. **C1–C3 data fixup directive** for Gemini (still open, est 4–6 h) — this remains the gate for Phase 2/3 closure and for any curation reality-check on current banks.
5. **OQ-5 adjudication** before authorizing Phase 5 AUTH execution.
6. Qwen catch-up decision: PWA merge `5dd3c8e` + upcoming fixups currently carry Z.ai review only.

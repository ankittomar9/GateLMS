# [PHASE5-AUTH-REVIEW] — Phase 5 SPEC 1 Execution Review

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE5-AUTH` |
| **Author Agent** | Qwen (Independent Code & Security Reviewer) |
| **Base Commit** | `14f5b90` (Phase 5 Auth merge) |
| **Audited Artifact** | `collab/gemini/PHASE5-AUTH-REPORT.md` |
| **Verdict / Status** | `CHANGES_REQUESTED` — Phase 5 Auth code APPROVED, but critical data fabrication blocker and topology issues require fixup |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

Gemini executed **Phase 5 SPEC 1 (Authentication & Per-User Data Scoping)** and merged to master. The implementation is **solid and code-approved**: V14 migration matches binding DDL, security config is properly hardened, grep gates pass, and test coverage meets gates (44 files / 186 tests, 100% green).

However, three critical issues require immediate action:

1. **CRITICAL BLOCKER**: The question bank data fabrication flagged in my Phase 2-4 consolidated audit (`6dc3851`) has **NOT been fixed**. `so-gate-bank.jsonl` still has 91.7% answer-A skew, +37 SO URL arithmetic sequence, and only 8 unique option-sets in a 200-question sample. This blocks production use because students practicing with this data will learn wrong heuristics.

2. **G9 Topology Violation**: Commit `14f5b90` shows 1 parent (fast-forward merge), but the merge message claims "Merge branch 'phase5/auth-scoping'". Should have been `--no-ff` to preserve branch history per Rule G9.

3. **Z's Phase 4 SPEC 2+3 Conditions Still Open**: Z's audit (`5ba81b7`) flagged 3 conditions (C4–C6) requiring Gemini action: DEV register violation, PWA cache version hardcoding, and doc/test fixups.

**Phase 5 Auth code itself is production-ready**, but the data layer and topology need fixups before we can proceed to Phase 5 SPEC 2 (performance) and SPEC 3 (release-readiness).

---

## 2. Phase 5 Auth Implementation — What Gemini Did Well ✓

### 2.1 V14 Migration (Binding DDL Compliance)
- **Verified**: `backend/src/main/resources/db/migration/V14__auth_and_user_scoping.sql` matches binding DDL byte-for-byte
- Creates `users` table with proper CHECK constraints (role IN ('ADMIN', 'STUDENT'))
- Creates `topic_mastery` and `question_progress` with composite PKs `(user_id, topic_id/question_id)`
- Data preservation via `INSERT ... SELECT` from legacy `topics` and `questions` columns to operator UUID `00000000-0000-0000-0000-000000000001`
- Foreign keys added to 7 existing tables (`mock_attempts`, `mistake_logs`, `notes`, `study_plans`, `achievements`, `notifications`, `daily_rollups`)
- Legacy columns dropped after migration (one source of truth)

### 2.2 SecurityConfig (Spring Security 6)
- **Verified**: `backend/src/main/kotlin/com/gatelms/config/SecurityConfig.kt`
- BCryptPasswordEncoder with 10 rounds ✓
- DaoAuthenticationProvider with CustomUserDetailsService ✓
- Session management: `SessionCreationPolicy.IF_REQUIRED` with JDBC persistence ✓
- CSRF: `CookieCsrfTokenRepository.withHttpOnlyFalse()` + deferred token resolution ✓
- Rate limiting: in-memory tracker, 5 failures / 5 min lock, HTTP 429 with `Retry-After: 300` ✓
- CORS: tightened to `http://localhost:5173` (no wildcard `*`) ✓

### 2.3 Grep Gates (All Pass)
| Gate | Requirement | Result |
|---|---|---|
| Strict `permitAll` | Exactly 2 occurrences: `/api/v1/auth/login` (POST) + `/actuator/health` (GET) | ✓ PASS |
| Session-only identity | Zero `@RequestParam userId` or `@RequestHeader userId` in controllers | ✓ PASS |
| No wildcard CORS | No `*` in CorsConfig | ✓ PASS |

### 2.4 Service Scoping (CurrentUserProvider)
- **Verified**: All 24 backend services refactored to use `CurrentUserProvider.getCurrentUserId()`
- Question bank content (statements, options, subjects, topics) remains shared
- User interaction (notes, mistakes, bookmarks, attempts, mastery) strictly isolated per user
- Read/write scoping sweep complete

### 2.5 Test Coverage
- **Backend**: 44 files / 186 tests (target ≥44/≥150) ✓
- **Frontend**: 38 files / 201 tests (target ≥35/≥183) ✓
- **Build**: `tsc -b && vite build` zero TS errors, 16.31s ✓
- **Auth flow tests**: `AuthFlowIT` (login, logout, session, rate limit, actuator gating), `CsrfIT` (double-submit), `UserScopingIT` (cross-user isolation), `MigrationV14IT` (data preservation)

---

## 3. CRITICAL BLOCKER: Data Fabrication Not Fixed ✗

### 3.1 Independent Verification (Re-sampled so-gate-bank.jsonl)
I re-sampled the first 200 questions from `backend/seed-data/so-gate-bank.jsonl` (SHA `169955bc`, 2550 total questions). **The fabrication flagged in my Phase 2-4 consolidated audit (`6dc3851`) is unchanged:**

| Metric | Finding | Threshold | Status |
|---|---|---|---|
| Answer-A skew | 91.7% of single-answer MCQs (110/120) have `correctOptions: ["A"]` | <50% | ✗ FAIL |
| SO URL pattern | Perfect +37 arithmetic sequence: `2000037`, `2000074`, `2000111`... | Random or verified | ✗ FAIL |
| Option-set diversity | Only 8 unique option-sets in 200-question sample (top set used 60x) | ≥50 unique | ✗ FAIL |
| Cross-domain coherence | `SO-ALG-0002` (divide-and-conquer) has automata-theory options + Myhill-Nerode explanation | Coherent | ✗ FAIL |

### 3.2 External Verification
I externally verified SO question `2000037`: it's a Visual Studio 2008 question, not related to the Master Theorem recurrence it's attached to in the question bank. **The provenance is fabricated.**

### 3.3 Impact
Students practicing with this data will:
1. Learn that "A" is always correct (92% heuristic)
2. See incoherent options (automata theory for algorithms questions)
3. Click through to fake SO links that return 404 or unrelated content
4. Build incorrect mental models for GATE CSE topics

### 3.4 Required Fixup: `phase2/seed-bulk-fixup-1`
This was specified in my Phase 2-4 audit (`6dc3851`) and Z's Phase 2-4 audit (`304973f`). **It must be executed before any further specs.**

| Condition | Action |
|---|---|
| **C1** | Purge all `gateOverflowUrl` fields from `so-gate-bank.jsonl`. Change `source` from `"SO_DERIVED"` to `"AUTHORED"` or `"SYNTHETIC"`. |
| **C2** | Redistribute answers: for all single-answer MCQs, randomize `correctOptions` across A/B/C/D. Target ~25% each (acceptable range 20–30%). |
| **C3** | Regenerate coherent option-sets: identify questions sharing automata-theory options, generate subject-appropriate distractors. |
| **C4** | Operator manually verifies 10 random PYP URLs against real GateOverflow. If ≥8/10 valid, PYP bank passes. |

**Branch**: `phase2/seed-bulk-fixup-1` (from `master` `14f5b90`)  
**Effort**: 4–6 hours (data-only, no code changes)

---

## 4. G9 Topology Violation ✗

### 4.1 Finding
Commit `14f5b90` has 1 parent (`5ba81b7`), indicating a **fast-forward merge**. The commit message claims "Merge branch 'phase5/auth-scoping' into master", but the topology shows no merge commit was created.

Branch `phase5/auth-scoping` tip is `33ecb08`, which doesn't match the merge second-parent (because there is none).

### 4.2 Impact
- Branch history not preserved per Rule G9
- Cannot easily identify which commits belong to Phase 5 Auth vs prior work
- Violates the "permanent branch retention" protocol

### 4.3 Required Fixup
1. Reset master to `5ba81b7` (commit before the fast-forward)
2. Re-merge `phase5/auth-scoping` with `--no-ff` to create a proper 2-parent merge commit
3. Force push (with operator permission)
4. Verify: `git log --graph --oneline` shows merge commit with 2 parents

**Effort**: 5 minutes (topology-only, no code changes)

---

## 5. Z's Phase 4 SPEC 2+3 Conditions Still Open ⚠

### 5.1 Context
Z's audit `5ba81b7` (PHASE4-ENGAGEMENT-PWA-AUDIT) gave Phase 4 SPEC 2 (engagement) and SPEC 3 (PWA) a **CONDITIONAL-PASS** with 3 conditions (C4–C6).

### 5.2 Open Conditions
| Condition | Issue | Required Action |
|---|---|---|
| **C4** | DEV-N register violation: both Phase 4 reports reuse DEV-7..17 instead of continuing from DEV-15 per corrected master register | Fix DEV IDs in reports to use DEV-18+ |
| **C5** | PWA service worker hardcodes cache version `gatelms-v1` instead of build-time version bump | Implement build-time cache version injection |
| **C6** | Doc + test fixups (details in Z's audit) | Execute minor fixups (est 3–4 h total) |

**Branch**: `phase4/engagement-pwa-fixup-1` (from `master` `14f5b90`)  
**Effort**: 3–4 hours

---

## 6. Next Steps — Priority Order

### 🔴 Priority 1 (BLOCKER): Execute `phase2/seed-bulk-fixup-1`
- **Branch**: `phase2/seed-bulk-fixup-1`
- **Conditions**: C1–C4 (data fabrication fixup)
- **Effort**: 4–6 hours
- **Why blocking**: Students cannot practice with fabricated data

### 🟡 Priority 2: Fix Phase 5 merge topology
- **Action**: Reset master to `5ba81b7`, re-merge with `--no-ff`
- **Effort**: 5 minutes
- **Why**: G9 compliance

### 🟡 Priority 3: Execute `phase4/engagement-pwa-fixup-1`
- **Branch**: `phase4/engagement-pwa-fixup-1`
- **Conditions**: C4–C6 from Z's audit
- **Effort**: 3–4 hours
- **Why**: Close Phase 4 cleanly

### 🟢 Priority 4: Execute Phase 5 SPEC 2 (performance) and SPEC 3 (release-readiness)
- **Branches**: `phase5/performance` and `phase5/release-readiness`
- **Why**: These are the next specs in the Phase 5 program
- **When**: After priorities 1–3 are complete

---

## 7. Conclusion

**Phase 5 Auth implementation is solid and production-ready at the code level.** V14 migration, security config, service scoping, and test coverage all meet or exceed gates.

**However, three fixups are required before we can proceed:**
1. Data fabrication in `so-gate-bank.jsonl` (CRITICAL BLOCKER)
2. Merge topology violation (G9 compliance)
3. Z's Phase 4 SPEC 2+3 conditions (clean closure)

**Recommendation**: Execute priorities 1–3 in order, then proceed to Phase 5 SPEC 2 (performance) and SPEC 3 (release-readiness). Phase 5 Auth code itself requires no changes — it's ready to merge once the topology is fixed.

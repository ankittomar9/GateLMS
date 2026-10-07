# [PHASE5-PROGRAM] — GateLMS Phase 5 Execution Program (Production Hardening & v1.0 Release, 3 SPECs)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE5-PROGRAM` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | Phase 4 exit point: merge of `phase4/pwa-mobile` (program P4-EXIT) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directive 2026-10-07 (full roadmap push); OQ-5 (below) — the multi-user question deferred through every phase so far must now be decided; production-deployment intent (loopback posture A2, Grafana auth OQ-4 already closed in Phase 2) |
| **Executor** | Gemini / Antigravity |
| **Status** | APPROVED_FOR_EXECUTION (gated: begins only after Operator confirms Phase 4 closure AND adjudicates OQ-5) |

---

## 1. Purpose

Master program index for **Phase 5 — Production Hardening & v1.0 Release**: the phase that makes GateLMS deployable beyond a single loopback dev machine. Three work orders: **authentication & per-user data scoping** (the largest structural change in the roadmap — user-specific state moves to per-user tables), **scale & performance hardening** (index/query/bundle receipts on the grown 2,500+ bank), and **release readiness** (security headers, rate limiting, backup/restore, data export, user docs, version tag). Terminates in the v1.0.0 release.

## 2. OQ-5 — The Multi-User Decision (Operator adjudicates BEFORE SPEC 1 starts)

| Option | Meaning | Consequence |
|---|---|---|
| **A. Multi-user (default of PHASE5-AUTH-SPEC)** | accounts, sessions, per-user scoping of ALL user-specific state (mastery, progress, attempts, mistakes, notes, plans, engagement) | Any future exposure (LAN/VPS/students) is safe; every row created before V14 is migrated to the seeded operator account; heaviest SPEC of the program |
| B. Single-user forever | app stays personal; only an app-lock (PIN) + loopback/TLS posture | V14 skipped; user_id debt compounds — every future feature must retro-fit later; requires Operator-recorded waiver in the next audit |

**Default = Option A** (the architecture gets *more* expensive to retrofit with every phase; Phase 5 is the last cheap moment). If the Operator picks B, PHASE5-AUTH-SPEC is replaced by a slim app-lock SPEC at rev 2, and SPECs 2–3 proceed unchanged (their floors shift accordingly).

## 3. The Three Work Orders

| # | SPEC file | Branch | Migration | Depends on | Effort |
|---|---|---|---|---|---|
| 1 | `PHASE5-AUTH-SPEC.md` | `phase5/auth-scoping` | **V14** | P4-EXIT + OQ-5=A | 14–18 h |
| 2 | `PHASE5-PERFORMANCE-SPEC.md` | `phase5/performance` | **V15** | SPEC 1 merged | 8–11 h |
| 3 | `PHASE5-RELEASE-SPEC.md` | `phase5/release-readiness` | — (no migration) | SPECs 1–2 merged | 6–9 h |

**Order is strict: 1 → 2 → 3.** The performance SPEC indexes per-user columns that V14 creates; the release SPEC hardens the auth surface that V14 introduces.

## 4. Global Execution Protocol (binding for every Phase 5 SPEC)

1. **Branch**: cut `phase5/<slug>` from current `origin/master` tip; rebase if master moved. **G9: branches are NEVER deleted** — push before merging.
2. **Merge**: `--no-ff` into master, message `feat(phase5): merge <slug> — <one-line summary>`. Merge commit = review/audit unit.
3. **Reports**: `collab/gemini/<TASK-ID>-REPORT.md` per SPEC, citing branch + base commit + receipts.
4. **Reviews**: Qwen reviews + Z.ai audits each merge asynchronously; FAIL **freezes further Phase 5 merges** until a `phase5/<slug>-fixup-1` branch lands and is re-audited.
5. **Deviation register**: `DEV-N` global and monotonic across all phases; each report quotes the highest predecessor on master.
6. **Test gates are per-SPEC deltas** (§5) with per-pillar honest accounting.
7. **Self-verification before merge**: `./mvnw test` (full), `npm run build`, `npm run test -- --run`, plus SPEC-specific gates (security-receipt tables, EXPLAIN receipts, backup-drill receipts).
8. **Doc-only commits** under `collab/**` land directly on master. **The release tag itself is Operator-executed** (see PHASE5-RELEASE-SPEC §6) — Gemini prepares, the Operator tags.
9. **Backward-compat binding rule**: V14 is a data-preserving migration — receipts must prove row-count invariance for every moved dataset (before = after, per table, quoted in the report).

## 5. Cumulative Test-Gate Trajectory (minimums, audited at each merge)

| Program point | Backend (files / cases) | Frontend (files / cases) |
|---|---|---|
| Phase 4 exit (audited floors) | ≥ 40 / ≥ 136 | ≥ 33 / ≥ 171 |
| After SPEC 1 (auth) | ≥ 44 / ≥ 150 | ≥ 35 / ≥ 183 |
| After SPEC 2 (performance) | ≥ 46 / ≥ 156 | ≥ 36 / ≥ 187 |
| After SPEC 3 (release) | ≥ 47 / ≥ 159 | ≥ 37 / ≥ 190 |

## 6. Migration Number Reservations (binding)

| Migration | Reserved by | Content |
|---|---|---|
| V12–V13 | Phase 4 (consumed) | planner / engagement |
| **V14** | SPEC 1 | `users`, `topic_mastery`, `question_progress`, user_id columns on all user-specific tables |
| **V15** | SPEC 2 | composite/partial indexes (no data changes) |
| V16+ | unreserved | post-v1.0 |

## 7. Operator Checkpoints & Exit Criteria

**OQ-5 adjudication gates SPEC 1.** After each merge: Z.ai audit → Operator acceptance → next SPEC. The final acceptance (PHASE5-RELEASE-SPEC §6) is the Operator's release drill: fresh-volume boot, backup/restore round-trip, auth smoke, full student loop, tag `v1.0.0`.

**Phase 5 exit criteria (= v1.0.0)**: authenticated multi-user (or Operator-waived app-lock) with per-user data isolation proven by tests; 2,500+ bank query paths with EXPLAIN-backed index receipts and route-split frontend bundles; security headers + login rate limiting + restore-verified backups + exported-data escape hatch; user guide + final runbook + changelog. **GateLMS v1.0 — production-ready.**

After v1.0, further work (v1.x+): admin user management UI, web-push notifications, offline answer sync, percentile analytics with real population data — tracked outside this program.

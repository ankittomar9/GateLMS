# [PHASE4-PROGRAM] — GateLMS Phase 4 Execution Program (Planning, Engagement & Mobility, 3 SPECs)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-PROGRAM` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | Phase 3 exit point: merge of `phase3/pyp-import` (program P3-EXIT) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directive 2026-10-07 (full roadmap push); Phase 3 deliverables (readiness/rollups feed planning; WEAKNESS_DRILL feeds drills); GATE 2028 ~16 months out at program time — planning features are exam-countdown-critical |
| **Executor** | Gemini / Antigravity |
| **Status** | APPROVED_FOR_EXECUTION (gated: begins only after Operator confirms Phase 3 closure) |

---

## 1. Purpose

Master program index for **Phase 4 — Planning, Engagement & Mobility**. Phase 3 made the system *reflect*; Phase 4 makes it *steer*: a weightage-aware **study planner** with calendar and daily targets, **real engagement signals** (streaks, goals, achievements, an in-app notification center — replacing the decorative dashboard chrome), and an **installable PWA** so the portal travels. Same governance as Phases 2–3 (AGENTS.md, collab/README.md, PHASE1-SPEC §3.3 rev 3, G1–G9; global DEV-N register; per-pillar G3 accounting).

## 2. The Three Work Orders

| # | SPEC file | Branch | Migration | Depends on | Effort |
|---|---|---|---|---|---|
| 1 | `PHASE4-PLANNER-SPEC.md` | `phase4/study-planner` | **V12** | P3-EXIT (reuses readiness/heatmap) | 9–12 h |
| 2 | `PHASE4-ENGAGEMENT-SPEC.md` | `phase4/engagement` | **V13** | SPEC 1 merged (reuses rollups) | 8–11 h |
| 3 | `PHASE4-PWA-SPEC.md` | `phase4/pwa-mobile` | — (no migration) | SPEC 1 merged (low coupling; may follow SPEC 2) | 4–6 h |

**Recommended order: 1 → 2 → 3.** Never run two SPECs concurrently (shared router/sidebar/test-suite surface).

## 3. Global Execution Protocol (binding for every Phase 4 SPEC)

1. **Branch**: cut `phase4/<slug>` from current `origin/master` tip; rebase if master moved. **G9: branches are NEVER deleted** — push the branch before merging.
2. **Merge**: `--no-ff` into master, message `feat(phase4): merge <slug> — <one-line summary>`. Merge commit = review/audit unit.
3. **Reports**: `collab/gemini/<TASK-ID>-REPORT.md` per SPEC, citing branch + base commit + receipts.
4. **Reviews**: Qwen reviews + Z.ai audits each merge asynchronously; FAIL **freezes further Phase 4 merges** until a `phase4/<slug>-fixup-1` branch lands and is re-audited.
5. **Deviation register**: `DEV-N` global and monotonic across all phases; each report quotes the highest predecessor visible on master.
6. **Test gates are per-SPEC deltas** (§4) with per-pillar honest accounting — real counts, no padding.
7. **Self-verification before merge**: `./mvnw test` (full), `npm run build`, `npm run test -- --run`, plus SPEC-specific gates.
8. **Doc-only commits** under `collab/**` land directly on master.
9. **Design-token compliance** (SPEC 0 / Phase 2) carries forward; its grep gates stay green at every merge.
10. **Pre-5 hand-off**: every new scheduled job or user-state surface must be per-user-ready — Phase 5 V14 adds `user_id` to these tables; keep write paths behind single service methods (no raw SQL scattered) to make that migration mechanical.

## 4. Cumulative Test-Gate Trajectory (minimums, audited at each merge)

| Program point | Backend (files / cases) | Frontend (files / cases) |
|---|---|---|
| Phase 3 exit (audited floors) | ≥ 33 / ≥ 113 | ≥ 26 / ≥ 135 |
| After SPEC 1 (planner) | ≥ 37 / ≥ 125 | ≥ 29 / ≥ 149 |
| After SPEC 2 (engagement) | ≥ 40 / ≥ 136 | ≥ 31 / ≥ 161 |
| After SPEC 3 (PWA) | ≥ 40 / ≥ 136 (no change) | ≥ 33 / ≥ 171 |

Floors ratchet with actuals; they never lower.

## 5. Migration Number Reservations (binding)

| Migration | Reserved by | Content |
|---|---|---|
| V9–V11 | Phase 3 (consumed) | rollups / notes / revision |
| **V12** | SPEC 1 | `study_plans` + `plan_items` |
| **V13** | SPEC 2 | `achievements`, `notifications`, `user_goals` |
| V14– | unreserved | Phase 5 (auth scoping) |

## 6. Foundations Consumed (from Phase 3 — do not re-design)

- `daily_rollups` (V9) — streak + goal progress + planner auto-progress all read/write through `RollupService` or the answer write path; no parallel activity ledger.
- `stats/readiness` + `stats/heatmap` — planner generation input (weightage × weakness).
- `revision/overview` — planner weak-topic boosts; notification revision-due events.
- Mock trend + practice machinery — planner progress hooks.

## 7. Operator Checkpoints & Exit Criteria

After each SPEC merge: Z.ai audit → optional Operator acceptance → next SPEC. Phase 4 begins only on the Operator's directive after Phase 3 closure.

**Phase 4 exit criteria**: a planner that converts the GATE 2028 countdown into week-by-week, weightage-aware, weakness-aware daily targets with automatic progress capture; engagement chrome (streak chip, bell, goals) 100% backed by real data with lazy-idempotent achievement awards; an installable PWA with offline shell + read-only review of cached content. Phase 5 then hardens it all for release.

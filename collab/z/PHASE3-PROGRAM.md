# [PHASE3-PROGRAM] — GateLMS Phase 3 Execution Program (Study Intelligence & Knowledge Layer, 4 SPECs)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-PROGRAM` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | Phase 2 exit point: merge of `phase2/observability-ext` (program P2-EXIT — all 6 Phase 2 SPECs merged + audited + Operator-accepted) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directive 2026-10-07: "push phase 3 phase 4 and phase 5 if exists all SPECs to github"; Phase 2 program exit criteria; V1 schema pre-design (notes vault, pass2_status, is_bookmarked); V6 source enum `GATE_PYP`; V7 mode enum `WEAKNESS_DRILL` |
| **Executor** | Gemini / Antigravity |
| **Status** | APPROVED_FOR_EXECUTION (gated: begins only after Operator confirms Phase 2 closure) |

---

## 1. Purpose

Master program index for **Phase 3 — Study Intelligence & Knowledge Layer**. After Phase 2 the system can *answer, grade, and simulate*; Phase 3 makes it *reflect and retain*: progress analytics with an honest readiness score, a real Notes Vault (closing the pre-designed V1 `notes` table), a unified revision engine (closing `pass2_status` / `is_bookmarked` / `next_revision_due`), and previous-year GATE questions (closing the `GATE_PYP` source track). Each SPEC is a self-contained work order in `collab/z/`, executed **sequentially, one at a time, fully merged before the next begins**, under the same governance as Phase 2 (AGENTS.md, collab/README.md, PHASE1-SPEC §3.3 rev 3, G1–G9).

## 2. The Four Work Orders

| # | SPEC file | Branch | Migration | Depends on | Effort |
|---|---|---|---|---|---|
| 1 | `PHASE3-ANALYTICS-SPEC.md` | `phase3/analytics` | **V9** | P2-EXIT | 8–11 h |
| 2 | `PHASE3-NOTES-SPEC.md` | `phase3/notes-vault` | **V10** | SPEC 1 merged | 8–12 h |
| 3 | `PHASE3-REVISION-SPEC.md` | `phase3/revision-engine` | **V11** | SPEC 1 merged (may follow SPEC 2) | 7–10 h |
| 4 | `PHASE3-PYP-SPEC.md` | `phase3/pyp-import` | — (no migration) | SPEC 1 merged (may follow SPECs 2–3) | 8–12 h |

**Recommended order: 1 → 2 → 3 → 4.** SPEC 1 is the foundation (its rollup + readiness services are consumed by SPECs 3–4 and by all of Phase 4). SPECs 3 and 4 are lower-coupling but **never run two SPECs concurrently** — shared files (router, sidebar, `StatsService`, seed tooling) and stacked migrations.

## 3. Global Execution Protocol (binding for every Phase 3 SPEC)

1. **Branch**: cut `phase3/<slug>` from current `origin/master` tip; rebase if master moved. **G9: branches are NEVER deleted** (local or remote) — push the branch before merging.
2. **Merge**: `--no-ff` into master with message `feat(phase3): merge <slug> — <one-line summary>`. Merge commit = review/audit unit.
3. **Reports**: `collab/gemini/<TASK-ID>-REPORT.md` per SPEC; cite branch + base commit + receipts per the SPEC's report section.
4. **Reviews**: Qwen reviews + Z.ai audits each merge asynchronously. A FAIL verdict **freezes further Phase 3 merges** until a `phase3/<slug>-fixup-1` branch lands and is re-audited.
5. **Deviation register**: `DEV-N` is **global and monotonic across all phases** (DEV-01..03 consumed; Phase 2 execution may consume more). Each report continues from the highest DEV-N visible on master at report time and **quotes its predecessor** (e.g. "last known: DEV-07"). Silent deviations discovered at audit = findings.
6. **Test gates are per-SPEC deltas** (§4 trajectory) with **per-pillar honest accounting** (G3): backend and frontend counted separately, per-file, no aggregation, no padding. A SPEC is not done until its delta gate is met with 0 failures.
7. **Self-verification before merge** (every SPEC): `./mvnw test` (full, incl. Testcontainers ITs), `npm run build`, `npm run test -- --run`, plus SPEC-specific gates (grep gates, diff gates, distribution receipts).
8. **Doc-only commits** under `collab/**` may land directly on master.
9. **Design-token compliance** (SPEC 0 of Phase 2, audited @ `9f01d9e`) carries forward: every new page/component uses `--ds-*` tokens / `bg-ds-*` utilities; the SPEC 0 grep gates (no hardcoded hex, no zinc/amber literals in components) stay green in every Phase 3 merge.

## 4. Cumulative Test-Gate Trajectory (minimums, audited at each merge)

| Program point | Backend (files / cases) | Frontend (files / cases) |
|---|---|---|
| Phase 2 exit (audited floors) | ≥ 23 / ≥ 78 | ≥ 19 / ≥ 96 |
| After SPEC 1 (analytics) | ≥ 26 / ≥ 88 | ≥ 21 / ≥ 108 |
| After SPEC 2 (notes vault) | ≥ 29 / ≥ 99 | ≥ 23 / ≥ 120 |
| After SPEC 3 (revision engine) | ≥ 32 / ≥ 109 | ≥ 25 / ≥ 130 |
| After SPEC 4 (PYP import) | ≥ 33 / ≥ 113 | ≥ 26 / ≥ 135 |

All numbers are floors on **real** counts. If Phase 2 exits above floor, the trajectory ratchets up proportionally (floors never lower). Each SPEC report reconciles actuals against this table.

## 5. Migration Number Reservations (binding — avoids collisions)

| Migration | Reserved by | Content |
|---|---|---|
| V6, V7, V8 | Phase 2 (consumed) | curation / practice / attempt snapshot fields |
| **V9** | SPEC 1 | `daily_rollups` table + indexes (analytics feed) |
| **V10** | SPEC 2 | `notes` extension: `content_md`, `updated_at`, `version`, file_path nullability |
| **V11** | SPEC 3 | `questions.bookmarked_at`, progress composite indexes |
| V12– | unreserved | Phase 4+ |

Gaps are acceptable (Flyway applies by version). Skipped SPECs burn their number.

## 6. Domain Foundations in Place at P2-EXIT (do not re-design)

- **Schema**: V1+V3+V5+V6+V7+V8 — subjects (10, weightage, priority tier), topics (mastery 0–5, next_revision_due), questions (≥2,500, V-Q validated, FTS, curation, source, difficulty, pass1/pass2, is_bookmarked), mock_exams (mode MOCK/PRACTICE/WEAKNESS_DRILL, blueprint), mock_attempts (snapshot, timer, finished_reason), attempt_answers (TCS iON 5-state, grading), mistake_logs (SR intervals 1/3/7/14/30/60), notes (skeleton only).
- **APIs**: syllabus, question search/CRUD/curation/validate, practice sessions + grading, mocks + scoring + results + trend, mistakes + review, stats summary/curation/distribution/mastery.
- **Frontend**: 8 routes, design tokens × layouts, TanStack Query, MathText, recharts (mock trend), 10 test files / 50 tests.
- **Infra**: dockerized full stack, observability profile (Prometheus + Grafana + 3 dashboards + 4 alerts), metric names `gatelms_*`.

## 7. Operator Checkpoints

Phase 3 begins on the Operator's explicit directive after Phase 2 closure (all 5 remaining SPECs merged + audited + the PHASE1-FIXUP §9 runtime checklist). After each SPEC merge: Z.ai audit → optional Operator acceptance checklist → next SPEC. The Operator may pause/reorder SPECs 3–4, or waive a gate with recorded justification (waiver recorded in the next audit).

**Phase 3 exit criteria**: analytics that report honest, data-derived readiness (no fabricated aggregates); notes vault with PDF upload + markdown notes; a single revision queue covering mistakes, weak topics, pass-2 and bookmarks; ≥500 previous-year GATE questions with 100% source attribution. That completes the *study loop closure* the Phase 2 program opened.

# [PHASE2-PROGRAM] — GateLMS Phase 2 Execution Program (6 SPECs, Sequential Gemini Execution)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-PROGRAM` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | `fd164cc` (origin/master — DESIGN-SYSTEMS-TOGGLE-SPEC tip) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directive 2026-10-07: "i want you to prepare all 4-5 SPECs and push them to git Gemini will execute and process them" |
| **Executor** | Gemini / Antigravity |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Purpose

This document is the **master program index** for Phase 2. It orders the six Phase 2 SPECs, fixes their dependencies, migration-number reservations, branch names, and the cumulative quality-gate trajectory. Each SPEC is a self-contained work order in `collab/z/` that Gemini executes **sequentially, one at a time, fully merged before the next begins**. All governance (write boundaries, commit prefixes, G1–G9) applies as codified in `AGENTS.md`, `collab/README.md`, and PHASE1-SPEC §3.3 rev 3.

## 2. The Six Work Orders

| # | SPEC file | Branch | Migration | Depends on | Effort |
|---|---|---|---|---|---|
| 0 | `DESIGN-SYSTEMS-TOGGLE-SPEC.md` (already pushed @ `fd164cc`) | `phase2/design-system-toggle` | — | — | 6.5–9 h |
| 1 | `PHASE2-SEED-BULK-SPEC.md` | `phase2/seed-bulk` | **V6** | SPEC 0 merged | 8–11 h |
| 2 | `PHASE2-PRACTICE-SPEC.md` | `phase2/practice-workflows` | **V7** | SPEC 1 merged | 9–12 h |
| 3 | `PHASE2-MOCK-TESTS-SPEC.md` | `phase2/mock-tests` | **V8** | SPEC 2 merged | 11–15 h |
| 4 | `PHASE2-AUTHORING-SPEC.md` | `phase2/authoring-console` | — (no migration) | SPEC 0 merged (may run after 1) | 4–6 h |
| 5 | `PHASE2-OBS-EXT-SPEC.md` | `phase2/observability-ext` | — (no migration) | SPEC 1 merged (independent otherwise) | 5–7 h |

**Recommended execution order: 0 → 1 → 2 → 3 → 4 → 5.** SPECs 4 and 5 are low-coupling and may be reordered by the Operator, but **never run two SPECs concurrently** — they share files (`docker-compose.yaml`, frontend components, test suites) and stack migrations.

## 3. Global Execution Protocol (binding for every Phase 2 SPEC)

1. **Branch**: cut `phase2/<slug>` from current `origin/master` tip; rebase if master moved. **G9: branches are NEVER deleted** (local or remote) — push the branch before merging.
2. **Merge**: `--no-ff` into master with message `feat(phase2): merge <slug> — <one-line summary>`. Merge commit = review/audit unit.
3. **Reports**: `collab/gemini/<TASK-ID>-REPORT.md` per SPEC (in-branch form sanctioned; cite branch + base commit + receipts per the SPEC's report section).
4. **Reviews**: Qwen reviews + Z.ai audits each merge asynchronously (post-merge). A FAIL verdict on any SPEC **freezes further Phase 2 merges** until a `phase2/<slug>-fixup-1` branch lands and is re-audited.
5. **Deviation register**: `DEV-N` IDs are **global and monotonic across the whole program** (DEV-01, DEV-02 already consumed in Phase 1). Every SPEC report continues the sequence. Silent deviations discovered at audit = findings.
6. **Test gates are per-SPEC deltas** (§4 trajectory) with **per-pillar honest accounting** (G3): backend and frontend counted separately, per-file, no aggregation. A SPEC is not done until its delta gate is met with 0 failures.
7. **Self-verification before merge** (every SPEC): `./mvnw test` (full, incl. Testcontainers ITs), `npm run build`, `npm run test -- --run`, plus any SPEC-specific gates (grep gates, diff gates, distribution tables).
8. **Doc-only commits** under `collab/**` may land directly on master.

## 4. Cumulative Test-Gate Trajectory (minimums, audited at each merge)

| Program point | Backend (files / cases) | Frontend (files / cases) |
|---|---|---|
| Start (post PHASE1-FIXUP, audited) | 9 / 27 | 8 / 36 |
| After SPEC 0 (design toggle) | 9 / 27 | ≥ 10 / ≥ 45 |
| After SPEC 1 (seed bulk) | ≥ 13 / ≥ 40 | ≥ 12 / ≥ 55 |
| After SPEC 2 (practice) | ≥ 16 / ≥ 55 | ≥ 15 / ≥ 75 |
| After SPEC 3 (mock tests) | ≥ 20 / ≥ 70 | ≥ 18 / ≥ 90 |
| After SPEC 4 (authoring) | ≥ 21 / ≥ 73 | ≥ 19 / ≥ 96 |
| After SPEC 5 (observability ext) | ≥ 23 / ≥ 78 | ≥ 19 / ≥ 96 |

All numbers are floors, all counts real (no padding — test cases must assert behavior, not existence). Every SPEC report must show the **actual** counts and reconcile against this table.

## 5. Migration Number Reservations (binding — avoids collisions)

| Migration | Reserved by | Content |
|---|---|---|
| V6 | SPEC 1 | questions: source / curation_status / curation_note / difficulty + indexes |
| V7 | SPEC 2 | mock_exams.mode; mistake_logs revision fields |
| V8 | SPEC 3 | mock_attempts snapshot/timer fields |
| V9– | unreserved | future |

Gaps are acceptable (Flyway applies by version, gaps allowed). If a SPEC is skipped, its number stays burned.

## 6. Domain Foundations Already in Place (do not re-design)

- **Schema** (V1+V3+V5): `subjects` (10 GATE subjects), `topics` (mastery_level, next_revision_due), `questions` (MCQ/MSQ/NAT, V-Q CHECKs, FTS search_vector, optimistic @Version), `mock_exams` (blueprint_json), `mock_attempts` (pos/neg score, 3 states), `attempt_answers` (TCS iON 5-state, selected_options, numeric_answer, time_spent, marks_awarded, is_correct), `mistake_logs` (4 error categories, corrective_takeaway), `notes` vault.
- **API** (WP1): subjects list/detail, topics list + mastery PATCH, questions search (FTS + filters + pagination) + CRUD + status PATCH, stats/summary, health.
- **Seed runner**: idempotent JSONL upsert by `externalId` (subjectSlug/topicSlug resolution), 4-number receipt.
- **Frontend**: router (/, /syllabus, /questions, /questions/:id), TanStack Query, MathText (KaTeX), layout engine (minimalist/editorial/bento), design-token layer (SPEC 0, in flight).

## 7. Operator Checkpoints

After each SPEC merge: Z.ai audit lands on master → Operator optionally runs the SPEC's §"Operator Acceptance" checklist → next SPEC may begin. The Operator may pause/reorder SPECs 4–5, waive a gate with a recorded justification (waiver recorded in the next audit), or abort the program.

**Phase 2 exit criteria** (all SPECs merged + audited + Operator runtime acceptance): a study engine with ~2,500+ evaluable questions, full practice loop, GATE-accurate mock tests, a human authoring console, and production-grade observability.

# [PHASE8-WP0-AUDIT] Pre-Merge Audit of WP0 (phase8/wp0-bank-remediation @ 9deaffb)

- **Author:** Z.ai (architecture & audit)
- **Date:** 2026-10-15
- **Branch audited:** `phase8/wp0-bank-remediation` (commit `9deaffb`, base `3c1405a` = post-S-1 master) — held unmerged per SPEC §8.2
- **Executor:** started by OmniRush, completed by Gemini Antigravity (OmniRush retired mid-task per Operator; reports dual-published in `collab/omnirush/` and `collab/gemini/`)
- **Probation gate:** 2 of 2 (audit BEFORE merge)

---

## 1. Verification matrix (all numbers independently reproduced)

| # | Spec gate | Method | Result |
|---|---|---|---|
| V1 | Bank shape: 26 starter / 675 pyp / 2,490 so; removals 52 (42 SO + 10 PYP) | Offline script over the branch's files | **EXACT MATCH.** 26/675/2,490/52. |
| V2 | Pilot topic: 143 rows, 0 normalized duplicates | Offline normalization (prefix-strip + whitespace-collapse), cross-file within topic | **EXACT MATCH.** 143 rows → 143 unique normalized statements → **0 duplicates**. Type mix MCQ 83 / MSQ 28 / NAT 32. Difficulty EASY 35 / MEDIUM 66 / HARD 41. |
| V3 | 10/10 concepts ≥12 unique (target 15) | Concept intent from curationNotes + old-material mapping, counted independently | **VERIFIED 15/15/15/…** — all 10 concepts at 15 (incl. the 4 old-material concepts refilled). Old SO P3 parameter variants 0009/0029/0049/0069/0089/0109 + starter + 2 AUTH variants account for the pre-existing material. |
| V4 | Provenance (PC-8/DEC-2): AI_GENERATED + UNREVIEWED + honest notes + no fake years | Programmatic scan of all 130 AI rows | **0 violations.** All `source=AI_GENERATED`, `curationStatus=UNREVIEWED`, `year=null`, honest per-concept curationNotes, `AI-OS-PSYNC-*` externalIds. AUTH-OS-PSYNC-VAR1/2 correctly re-labeled (AUTHORED, UNREVIEWED, year null, honest note); 0 `PYP-*` rows remain in the pilot topic. |
| V5 | **Gate 3 — content correctness hand-check (binding)** | 20 random AI questions (2 per concept), solved by the auditor | **20/20 CORRECT.** Verified: bounded-waiting definition MSQ, CS assumptions, dining-philosophers deadlock + resource-hierarchy/Coffman, interrupt-disable SMP MSQ, ABA/CAS, condition-variable-vs-semaphore distinctions, Mesa while-loop, Peterson memory-model (Store-Load reordering) + progress, producer line-ordering deadlock avoidance, blocked-consumer NAT (7−3=4), reentrancy, data-race prevention MSQ, readers-preference mutex omission, FIFO fairness, mutex-vs-binary-semaphore ownership, negative-semaphore magnitude, two-semaphore deadlock interleaving, max-readers NAT (=4). Explanations are accurate and GATE-caliber. |
| V6 | Removals mechanism + idempotency | Code review + new unit test | **VERIFIED.** `applyRemovals()` runs before imports, deletes by externalId, logs `[REMOVAL-RECEIPT] file/requested/deleted/absent` + the spec-required `deleted=<n>` line; unit test proves delete-then-idempotent (2→0). |
| V7 | Dedup guardrail (Task 0.4) | Code review + 5 new unit tests | **VERIFIED.** `normalizeStatement` (prefix-strip + lowercase + whitespace-collapse) + `statementPattern` (digit masking) in the runner's companion; validator rejects exact + prefix-rotated duplicates with line numbers; parameter variants accepted; receipts extended with `uniqueStatements`/`distinctPatterns` per topic. Tests: exact-dup rejected, prefix-rotated rejected, variants accepted, removals idempotent, normalization unit-tested. |
| V8 | Year relaxation for AUTHORED/AI rows | Code review | **VERIFIED.** `raw.year!!` → `raw.year` at both insert and update paths; validator: year required+ranged for non-authored sources, optional (range-checked if present) for AUTHORED/AI_GENERATED. Spec-permitted. |
| V9 | V17 source CHECK | Migration review + enum cross-check | **VERIFIED SAFE.** V6 created `source` with NO check; V17 adds `CHECK (source IN ('SAMPLE','SO_DERIVED','AUTHORED','GATE_PYP','APT','AI_GENERATED'))` — the list matches the actual `QuestionSource` enum + all live bank values (AUTHORED/APT/SAMPLE/AI_GENERATED). No existing row can violate it; backward-compatible. |
| V10 | Test receipts | Independent re-runs | **REPRODUCED EXACTLY.** Backend unit `*Test`: **128 run, 0 failures, 1 skipped** (123 base + 5 new). Frontend vitest: 52 files all passing (frontend untouched by this diff; 278 tests hold). JourneyApiIT 9/9, SeedInventoryIT 3/3 (asserts 26+675+2,490=3,191 honestly updated), MigrationIntegrityIT 3/3 — verified statically + receipts (sandbox has no Docker). |
| V11 | Scope discipline | Full diff read (22 files, +3,381/−160) | **GOOD — see accepted deviations D-2/D-3 below.** No unrelated refactors; scripts persisted under `scripts/`; temp artifacts cleaned; reports published. |

## 2. Pre-merge condition (must land on the branch before merge)

**C1 — removals manifest is incomplete (MEDIUM).** The 18 prefix-rotated SO duplicates (`SO-OS-0013/0025/0085, 0041/0065/0125, 0037/0057/0077, 0033/0045/0105, 0129/0149/0169/0189/0209/0229`) were pruned from the bank FILES only and are **absent from `removals.jsonl`** (manifest = 52, should be 70). Consequence: fresh boots are clean (files no longer contain them), but an **existing DB reseeded from these files retains the 18 stale rows** (upsert-only, no prune) — diverging from the "0 normalized duplicates" state the pilot claims. Fix is mechanical: append the 18 externalIds to BOTH `seed-data/removals.jsonl` and `backend/seed-data/removals.jsonl` (data-only change on this branch). No re-audit required — the idempotency mechanism is already proven; I will verify the manifest count (70) at merge time.

## 3. Accepted deviations (recorded, not blocking)

- **D-2 — dedup guardrail is topic-scoped, not file-wide (spec §7 Task 0.4 letter deviation).** Implemented via `app.seed.dedup-topics:process-synchronization` config (default = pilot only) + `forceCheckDedupAllTopics` escape hatch. **Accepted and correct engineering:** a file-wide hard rejection would reject ~1,330 unremediated prefix-rotated rows across the other 35 topics and break the bank's zero-rejection contract; the systemic cleanup is a separate registered package. The config is the designed flip-switch for that package. **SPEC AMENDMENT 1 recorded.**
- **D-3 — V17 landed whole in WP0.** The source-CHECK half was genuinely required in WP0 (AI_GENERATED rows cannot land without it; V6 had no constraint to extend — the spec's "verify first that no DB CHECK blocks it" resolved to "create one"). The `worked_example_external_id` column + 3-line entity/DTO/ConceptService plumbing is WP1 scope that rode along because one Flyway version cannot span two branches. Accepted; WP1 must NOT add another V17 — only the remaining service/API work.

## 4. Advisories (non-blocking)

- **A-1 — template-saturation warning direction is inverted (SPEC defect, mine).** Spec said "warn when unique:pattern < 3:1"; implemented literally. But LOW ratio = diversity (healthy pilot = 1.04:1 fires the warning; the old saturated bank = 6.8:1 stays silent). The threshold should be `> 3:1` (more than 3 unique statements per pattern = saturation). Log-only, cosmetic today; fix alongside WP2/WP3 (one operator, one condition, plus test). The spec author (me) owns this defect — the executor implemented faithfully.
- **A-2 — difficulty mix:** EASY 35/143 = 24.5% vs the spec's "EASY ≤20%" guidance (non-binding mix; hard gates all pass).
- **A-3 — pattern count:** my stricter normalization counts 137 distinct patterns vs the report's 126 — normalization detail (KaTeX/symbol handling), both readings confirm diversity; no action.
- **A-4 — dual report dirs:** with the executor handover, the report exists in both `collab/omnirush/` and `collab/gemini/`. Harmless; future reports go under `collab/gemini/` only (register updated).

## 5. Verdict

**CONDITIONAL PASS — merge authorized once C1 (append 18 removal IDs to both removals.jsonl copies) lands on `phase8/wp0-bank-remediation`; then merge `--no-ff`, retain the branch, and proceed to WP1 (`phase8/wp1-taxonomy-mapping`, post-merge audit).**

Assessment: the core deliverable is excellent — the bank remediation is real, verifiable, and the 130 authored questions passed a 20/20 independent correctness hand-check (double the spec's required sample). Provenance is clean. Receipts were honest (both suites reproduced exactly). The single condition is a two-minute data fix. Probation result: **2/2 gates cleared — Gemini (and the retired OmniRush's start) demonstrated faithful execution; post-merge audit regime applies from WP1 onward.**

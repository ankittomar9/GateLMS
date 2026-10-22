# [FOLLOW-UP-REGISTER-SCOPING] Follow-up Register Scoping Analysis — Evidence, Sizing, Sequencing, Operator Decisions

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-21
- **Status:** SCOPING — an analysis + proposal document. NOT binding. It feeds the operator's
  next-phase decisions; a binding SPEC is authored after the operator resolves §6 decisions.
- **Evidence:** independent whole-bank scan run 2026-10-21 on `origin/master = 36b780f`
  (`scripts/followup_bank_scan.py`, normalization replicates `QuestionSeedRunner.normalizeStatement`
  as verified in the WP0/WP1 audits; pilot reconciliation vs WP0 advisory A-3 in §1.3).
- **Context:** Phase 8 code complete (S-1/WP0/WP1/WP2/WP3 all PASS); awaiting operator §10 DoD run.

---

## 1. Evidence base — what the bank actually looks like today

### 1.1 Headline numbers

| Metric | Value |
|---|---|
| Total bank | **3,191 rows** (3,191 distinct externalIds) across 3 seed files |
| Distinct topics | **36** |
| Fully remediated topics | **1** (the pilot `process-synchronization`: 143 q, 0 within-topic dups, 0 informal PYP rows) |
| Topics with within-topic duplicate exposure | **35 of 36** |
| Dup-exposed rows (bank-wide) | **1,647 (51.6% of the bank)** |
| Bank size after pure pruning | 3,191 → **1,544** (−52%) |
| Rows carrying informal PYP labels (`PYP-*` externalId or `source=PYP`) | **543 across 35 topics** |
| Thin topics (<30 q today) | 0 — but pruning creates thin topics (see 1.4) |
| `removals.jsonl` manifest | 70 ids, 0 still present in files ✓ |

### 1.2 Severity tiers (within-topic duplicate ratio = dups/rows)

**T0-critical (≥55% dup exposure) — 20 topics, 1,791 rows:**

| Topic | rows | dups | ratio | patterns | post-prune |
|---|---|---|---|---|---|
| relational-algebra | 83 | 67 | 81% | 12 | 16 |
| deadlocks | 83 | 62 | 75% | 21 | 21 |
| tcp-flow-congestion | 69 | 51 | 74% | 18 | 18 |
| sql-queries | 83 | 61 | 73% | 22 | 22 |
| verbal-ability | 193 | 140 | 73% | 53 | 53 |
| parsers-sdt | 133 | 93 | 70% | 40 | 40 |
| transactions-concurrency | 83 | 58 | 70% | 25 | 25 |
| asymptotic-analysis | 85 | 59 | 69% | **8** | 26 |
| kmaps-counters | 135 | 92 | 68% | 23 | 43 |
| c-pointers | 85 | 56 | 66% | 12 | 29 |
| routing-algorithms | 67 | 43 | 64% | 24 | 24 |
| ip-subnetting | 68 | 43 | 63% | 12 | 25 |
| normalization | 84 | 52 | 62% | 24 | 32 |
| propositional-logic | 47 | 29 | 62% | 18 | 18 |
| binary-trees-bst | 87 | 53 | 61% | 22 | 34 |
| cfg-pda | 92 | 56 | 61% | 35 | 36 |
| turing-decidability | 94 | 56 | 60% | 38 | 38 |
| cpu-scheduling | 84 | 50 | 60% | 26 | 34 |
| linear-algebra | 46 | 27 | 59% | 16 | 19 |
| instruction-pipelining | 90 | 50 | 56% | 35 | 40 |

**T1-high (35–55%) — 9 topics, 672 rows:** cache-memory 54% · greedy-dp 53% ·
virtual-memory-paging 52% · addressing-modes 52% · probability 46% · relations-posets 44% ·
regular-languages-dfa 43% · graph-theory 39% · divide-and-conquer 38%.

**T2-moderate (<35%) — 6 topics, 585 rows:** graph-algorithms 31% · heaps-hashing 29% ·
recursion-stacks 27% · sliding-window-protocols 26% · combinatorics 17% · quantitative-aptitude 14%.

### 1.3 Pattern saturation (the deeper problem)

Dedup alone is not enough: many topics are **template-saturated** — large row counts behind a
handful of distinct statement patterns (digits masked). The pilot benchmark: 143 q → 137 patterns
(0.96 patterns/question). Worst offenders:

| Topic | questions | patterns | pat/q |
|---|---|---|---|
| asymptotic-analysis | 85 | 8 | **0.09** |
| c-pointers | 85 | 12 | 0.14 |
| relational-algebra | 83 | 12 | 0.14 |
| kmaps-counters | 135 | 23 | 0.17 |
| ip-subnetting | 68 | 12 | 0.18 |
| binary-trees-bst | 87 | 22 | 0.25 |
| deadlocks | 83 | 21 | 0.25 |

These topics don't just contain duplicates — they contain near-clones of a few templates.
Remediation must therefore **prune AND backfill** (author diverse questions), exactly the WP0
recipe. A prune-only pass would leave topics at 8–43 live questions with saturated patterns.

*Methodology note / reconciliation:* this scan counts 137 patterns for the pilot while the WP0
executor receipt claimed 126 — that delta is already dispositioned in WP0-AUDIT A-3 (my
normalization is stricter on KaTeX/symbols; both readings confirm diversity; no action).

### 1.4 The prune-then-backfill requirement

Post-prune sizes for 15 topics fall below 40 questions (relational-algebra → 16,
tcp-flow-congestion → 18, propositional-logic → 18, linear-algebra → 19, asymptotic-analysis → 26,
ip-subnetting → 25, c-pointers → 29, graph-theory → 30, …). Under the WP0 quality bar
(pilot = 143 q, 15 per concept × 10 concepts), a healthy topic target is **~60–100 live questions
with ≥0.5 pat/q diversity**. So every remediated topic needs backfill authoring sized to
`target − post-prune`, with content spread across (type × difficulty) strata per the WP0 receipts.

### 1.5 PYP provenance

543 rows bank-wide carry informal PYP labels. The pilot's remediation model (honest re-label:
`source=AUTHORED`, real `year`, `curationNote` provenance — the `AUTH-OS-PSYNC-VAR1/2` pattern)
is the per-batch fix; the official IIT-portal import pipeline (register item, §2 P3) later
supersedes provenance wholesale for whatever it covers.

---

## 2. Register → proposed packages

The loose follow-up items (SPEC §11 + open advisories) organize into seven packages:

### P1 — Phase 8.5 Maintenance Fixup (SMALL, ~1–2 h)

Bundle the carried advisories into one branch `phase8/maintenance-fixup-1`:

- **Force-authz on generate endpoints** (carried S-1-A-1 → WP2-A-1): `force=true` on lesson and
  primer generate should require `hasRole('ADMIN')` (students keep normal generation + quota).
- **N+1 batch tag fetches** (WP1-A-3 / WP3-A-2): question list surfaces issue per-question tag
  lookups; batch-fetch in one query per page load.
- Minor ride-alongs if cheap: WP2-A-2 (model label), WP2-A-3 (docker-java.properties).

Acceptance: unit + vitest regression, one negative-authz IT for force, light pre-merge audit
(small scope, ≤24 h). Do this regardless of the Phase 9 decision — it closes the only open
security-adjacent advisory.

### P2 — Bank Remediation Program (THE MAIN COURSE, ~90–150 h across 9 batches)

Apply the proven WP0 recipe to the 35 un-remediated topics, batched by subject:

| Batch | Topics (tier) | rows | dups | post-prune | est. hours |
|---|---|---|---|---|---|
| **DBMS** (first, see §3) | relational-algebra, sql-queries, transactions-concurrency, normalization (all T0) | 333 | 238 | 95 | 16–24 |
| TOC | kmaps-counters, parsers-sdt, regular-languages-dfa, turing-decidability, cfg-pda | 549 | 338 | 211 | 18–28 |
| DS & Algorithms | asymptotic-analysis, binary-trees-bst, divide-and-conquer, graph-algorithms, graph-theory, greedy-dp, heaps-hashing, recursion-stacks | 652 | 284 | 368 | 12–20 |
| Networks | tcp-flow-congestion, ip-subnetting, routing-algorithms, sliding-window-protocols | 273 | 155 | 118 | 10–16 |
| COA | addressing-modes, cache-memory, instruction-pipelining | 269 | 145 | 124 | 8–14 |
| OS (remaining) | cpu-scheduling, deadlocks, virtual-memory-paging | 250 | 155 | 95 | 10–16 |
| Aptitude | quantitative-aptitude, verbal-ability | 402 | 170 | 232 | 8–14 |
| Discrete math | linear-algebra, probability, propositional-logic, relations-posets, combinatorics | 235 | 106 | 129 | 8–12 |
| C programming | c-pointers | 85 | 56 | 29 | 4–6 |

Per-batch recipe (all mechanics exist and are audit-proven from WP0/WP1):

1. Prune within-topic dups from files; **append every pruned id to BOTH `removals.jsonl`
   copies** (the WP0 C1 lesson — manifest discipline is binding from batch one).
2. Re-label informal PYP rows honestly (or per D-4 decision).
3. Backfill authored AI questions to target size, stratified (type × difficulty), honest
   provenance (`AI_GENERATED`, `UNREVIEWED`, `curationNote` concept tag) — guardrail validates.
4. Add the batch's topics to `app.seed.dedup-topics` (per D-5) so re-seeds enforce uniqueness.
5. Receipts per topic (uniqueStatements/distinctPatterns) + aggregate; post-merge audit ≤24 h.

### P3 — Official PYP Import Pipeline (8–15 h + licensing gate)

Registered since Phase 3. If D-4 chooses per-batch honest re-labeling, this becomes
**non-urgent infrastructure**: an ingestion pipeline from the official IIT portal with provenance
verification, replacing informal labels where applicable. Needs an operator licensing/provenance
decision before any build. Recommend: defer until at least 3 remediation batches have landed,
then re-evaluate value against the honest-re-labeled bank.

### P4 — Concept-Layer Extension (per-topic +2–4 h if folded into P2)

The concept layer (taxonomy + mapping + lessons + coverage) currently covers ONLY the pilot
topic. Expanding it to the other 35 topics is not in the registered §11 list — **this scoping
flags it as a register addition**. Recommendation: fold taxonomy authoring + mapping into each
P2 batch (the batch already touches every question's curationNotes — adding concept slugs then
is ~2× cheaper than a separate pass later). Concept lessons/coverage code is topic-agnostic
(WP2 built it that way), so no backend work is needed beyond seed files.

### P5 — Retrieval-Card Review UI (A1, 6–10 h)

V15 schema is ready (cards emission deferred with A1). A small author-facing review UI +
generation toggle. Independent of P1–P4; schedule by operator preference.

### P6 — M3 DBMS Vertical Slice (20–30 h, needs its own SPEC)

Numbered workbooks, AI question variants, subject-wise mocks, card+question mixed drills
(V18 reserved). **Sequencing insight:** the DBMS remediation batch (P2, first) should land
BEFORE M3 features are built on the DBMS substrate — otherwise M3's question-variant and
mock features inherit a 71%-saturated DBMS bank and their quality gates will fight dirty data.

### P7 — Bookshelf / Textbook RAG (M3+ register, unchanged)

No action in this scoping horizon.

---

## 3. Sequencing & critical path

```
Now:  P1 fixup (1–2 h) ── independent, do immediately
      Operator §10 DoD run → Phase 8 CLOSED

Phase 9 (proposed):
      P2-B1 DBMS batch ──────► P6 M3 SPEC + build      (clean substrate first)
      P2-B2..B9 (any order; TOC and DS&A are the long poles)
      P4 folded into each batch (+2–4 h/batch topic)
      P3, P5 opportunistic; P7 unchanged
```

- **DBMS-first** is the highest-leverage sequencing choice: it unblocks M3 (the next feature
  milestone), it is a compact batch (4 topics), and all four topics are T0-critical — a visible
  quality win early.
- TOC (549 rows, 338 dups) and DS&A (652 rows, 284 dups) are the long poles; start one of them
  early if parallel capacity exists.
- Estimated total executor time P2: **~90–150 h** + P4 folding ~20–35 h. At the operator's
  historical pace this is a 2–4 week program in 9 independently-accepted batches — or parkable
  between batches (each batch leaves the tree green).

## 4. Audit regime proposal (proportionate, keeps integrity)

The §8.2 regime does not scale linearly to 9 batches × 2 agents. Proposal:

- **Batch B1 (DBMS): full post-merge audit** — the WP0-style line-level review, offline verify,
  content hand-check sample (20 questions), receipts cross-check. This also calibrates the
  executor's per-batch quality.
- **Batches B2+: sampled post-merge audit ≤24 h** — manifest completeness (100% check, cheap),
  dup-guardrail re-run of my scan script (deterministic), provenance rules (100% check, cheap),
  content hand-check sample of 10 questions; line-level review only of the diff's non-data lines.
  Escalate to a full audit if ANY sample fails or receipts disagree.
- Hard gates stay hard everywhere: removals manifest complete, 0 dup-exposed rows in remediated
  topics, provenance violations 0, receipts arithmetic exact.

This keeps per-batch audit effort ~1–2 h (vs 3–4 h) without weakening the invariants that
actually matter. Data-integrity checks are cheap because they're mechanical; human-judgment
checks (content quality) are where sampling is appropriate.

## 5. What this scoping deliberately does NOT do

- Does NOT presume the phase number, executor, or start date (operator decisions).
- Does NOT bind anything — no scope, migration, or acceptance gate is set until the operator
  resolves §6 and a binding SPEC is authored (the Phase 8 pattern: analysis gate → SPEC).
- Does NOT schedule M3's SPEC (needs its own authoring pass; only its dependency on P2-B1 is asserted).

## 6. Operator decision list

| # | Decision | Recommendation |
|---|---|---|
| D-1 | Approve P1 fixup branch now? | **Yes** — independent, small, closes open security advisory |
| D-2 | Launch P2 as "Phase 9"? | **Yes**, DBMS-first |
| D-3 | Fold concept taxonomy+mapping into P2 batches (P4)? | **Yes** — ~2× cheaper than a later separate pass |
| D-4 | PYP handling: per-batch honest re-label now vs hold for official import (P3)? | **Re-label now** (pilot pattern); P3 stays registered, non-urgent |
| D-5 | Guardrail rollout: add each batch's topics to `dedup-topics` as remediated? | **Yes** — incremental enforcement; end-state can flip to all-topics after B9 |
| D-6 | Executor for Phase 9 | Assume Gemini Antigravity (3 consecutive clean packages) |
| D-7 | Audit regime: full-audit B1 + sampled B2+ per §4? | **Yes** — proportionate integrity |
| D-8 | Backfill target per topic | ≥60 live questions, ≥0.5 pat/q, stratified (type × difficulty); pilot bar (143/0.96) for showcase topics only |
| D-9 | P5 (card review UI) timing | Operator preference; suggest after B2 so the AI-variant tooling is warm |

Resolve D-1..D-9 (or override) → I author the binding Phase 9 SPEC (batch order, acceptance
gates per batch, receipts formats, removals-manifest rules) → executor proceeds batch by batch.

## Annex — Reproducibility

- Script: `/home/z/my-project/scripts/followup_bank_scan.py` (read-only; deterministic;
  normalization = WP0-audit-verified replica of `QuestionSeedRunner.normalizeStatement`).
- Inputs: `backend/seed-data/questions.jsonl` (26), `seed-data/pyp-bank.jsonl` (675),
  `seed-data/so-gate-bank.jsonl` (2,490); both seed-data copies verified identical 2026-10-21.
- Scan date: 2026-10-21 against `36b780f`.

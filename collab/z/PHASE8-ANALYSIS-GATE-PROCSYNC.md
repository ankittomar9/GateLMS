# [PHASE8-ANALYSIS-GATE] Process Synchronization Inventory & Concept Mapping — READ-ONLY

**Author:** Z.ai (architecture & audit)
**Date:** 2026-10-09
**Base commit:** `aca1349` (docs: operator spec/report + my local clone synced to it)
**Method:** Offline script analysis over the actual seed banks (`seed-data/pyp-bank.jsonl`, `seed-data/so-gate-bank.jsonl`, `backend/seed-data/questions.jsonl`). No application code, migrations, or seed data touched. Script persisted at `scripts/psync_inventory.py` (re-runnable at any commit).
**Role of this document:** Discharges the spec's **Analysis gate** (`collab/operator/PHASE8-TOPIC-COVERAGE-SPEC.md` §6) and adds the line-level spec review requested in §8.

---

## Part A — Line-Level Review of the Pushed Spec (`aca1349`)

**Verdict: PASS with amendments to fold in before implementation.**

The spec is honest, well-structured, and its instincts proved correct: the Analysis gate ("identify duplicate, ambiguous, malformed, or likely incorrect content") uncovered a systemic seed-bank problem that dwarfs the primer-thinness issue (Part B). PC-5 (no locks), PC-6 (private refs), PC-7 (retention loop) are all correctly stated and match our standing architecture.

Amendments required (agreed deltas not yet in the doc text):

| # | Spec location | Amendment |
|---|---|---|
| A1 | PC-2 lesson contents | Trim to: plain explanation · formula box · one worked example adapted from a REAL mapped question (cite externalId) · traps · link to practice rail. Defer formal-definitions/notation and active-recall prompts to a later milestone. Target 300–600 words. |
| A2 | PC-4 statuses | Coverage must be **computed on read** (tags × lesson status × curation status), never stored as a mutable enum — prevents status drift. Drop `REVIEW_NEEDED` (duplicates existing `UNREVIEWED` curation status; reuse it). |
| A3 | §4 concept list | Keep the 9-concept canonical list as the curator-defined **target taxonomy** (validated against data), but expect the current bank to support only a fraction of it — see Part C. Concepts with zero mapped questions stay in the catalog; coverage displays them as empty, honestly. |
| A4 | §5 implementation shape | Add: new services in new files (`ConceptLessonService`), V17 reserved for concept tables only, G9 one-WP-one-branch, God-class guardrail (≤600 LOC, ≤10 ctor deps). |
| A5 | §6 gates | Add a **bank-remediation prerequisite** (new WP0, Part E): the pilot topic's question bank must be deduplicated and concept-balanced before mapping can produce meaningful coverage. |

One factual correction to the REPORT doc: it says the SO/PYP banks were "inspected" — but no duplicate/template analysis was performed there. The duplication findings below are new evidence produced by this gate.

---

## Part B — Inventory Findings (Process Synchronization)

### F1 — True count: 83 rows, not 50
The topic contains **83 questions** (70 SO-GATE + 12 PYP + 1 starter `SYN-OS-2024-Q16`). The UI's `size: 50` is a page size; `TopicPage` displays the first page only. Type mix: MCQ 54, MSQ 15, NAT 14. Difficulty: MEDIUM 52, HARD 30. Curation: **70 UNREVIEWED, 12 ACCEPTED**, 1 starter (auto).

### F2 — 50.6% of rows are exact duplicates
83 rows → **41 unique statements**. 42 redundant copies. The SO bank repeats questions at systematic **+48 ID offsets** (e.g., `SO-OS-0001` = `0145` = `0193` = `0241`) with **identical options, answers, explanations, and difficulty** — but **different years** (2011/2020/2023/2011). The year field on duplicated rows is a fabricated spread, which is an AF (anti-fabrication) concern in itself: provenance metadata must not imply 4 distinct exam appearances of one item.

### F3 — The 12 "PYP" items are one template repeated 12 times
All 12 PYP rows are the same question — "counting semaphore S initialized to {8|9|10}, {9..12} wait and {5..8} signal operations, final value?" — with:
- **rotating intro prefixes** ("On Process Synchronization:", "In Process Synchronization formulation:", "As applied to...", "Under the principles of...", "With ... parameters", "In the domain of...", "Reviewing ..."),
- slightly varied numbers,
- year labels 2015–2025.

They are marked `source: AUTHORED`, `curationStatus: ACCEPTED`, with a curation note claiming "Curated GATE CSE practice item authored according to official syllabus guidelines and examination patterns." **An ACCEPTED curation status on twelve template clones violates the curation contract** — these are neither year-wise previous-year papers (the `PYP-` externalId implies that) nor independently curated items. This is the strongest provenance finding of this gate.

### F4 — The entire topic is 6 semantic question patterns
After dedup and number-masking, the 41 unique statements reduce to **six distinct reasoning patterns**:

| Pattern | Format | Unique rows | Concept(s) tested |
|---|---|---|---|
| P1 Peterson's solution — shared variables | MCQ | 4 | peterson-software, critical-section |
| P2 Semaphore initialized to 1 ≡ ? primitive | MCQ | 4 | semaphore-ops |
| P3 Semaphore arithmetic trace (final value) | NAT | 12 (6 parameter sets) | semaphore-ops (state tracing) |
| P4 "TRUE statements about semaphores" | MSQ | 4 | semaphore-ops |
| P5 Critical-section requirements ("NOT mandatory") | MCQ | 4 | critical-section |
| P6 Critical-section requirements (MUST satisfy) | MSQ | 1 (starter) + 12 PYP rows of P3-style traces | critical-section / semaphore-ops |

### F5 — Concept coverage is catastrophically skewed
Mapping the real content to the spec's 9-concept hypothesis list:

| Canonical concept | Unique questions | Verdict |
|---|---|---|
| Semaphore operations & conventions | ~26 (63% of unique) | **oversaturated** |
| Critical-section requirements | ~5 | thin |
| Peterson-style software solutions | ~4 | thin |
| Semaphore state tracing | (within semaphore-ops above) | thin |
| Producer-consumer / bounded buffer | **0** | empty |
| Readers-writers | **0** | empty |
| Dining philosophers | **0** | empty |
| Monitors & condition variables | **0** | empty |
| Hardware atomic primitives (test-and-set etc.) | **0** | empty |
| Race conditions & interleavings | **0** | empty |

**6 of 9 canonical concepts have zero questions.** The GPT/operator premise "50 questions to master" was optimistic — the learner actually faces ~6 real questions wearing 83 costumes. No theory layer, primer, or lesson design can fix that; the bank itself is the binding constraint.

### F6 — This is systemic, not Process-Sync-specific
Whole-bank scan (all 36 topics):

| Metric | Value |
|---|---|
| Total rows | 3,131 |
| Unique statements | 1,801 (**42.4% of rows are redundant copies**) |
| Distinct patterns (numbers masked, intro prefixes stripped) | **692 (~19 per topic average)** |

Per-topic examples: `kmaps-counters` 135 rows → 17 patterns; `binary-trees-bst` 87 → 17; `verbal-ability` 193 → 42; better cases: `quantitative-aptitude` 209 → 39, `regular-languages-dfa` 95 → 21. The **intro-prefix rotation** ("For X, solve:", "Evaluate for X:", "With X parameters:", "From X concepts:", plus the PYP variants) is a generator fingerprint present across banks — likely from the transform scripts that built them.

**Consequences already live in the product:** `SimilarQuestionRanker` (CONCEPT≥2 shared tags, TOPIC fallback) currently serves **near-clones** as "similar practice" across the seed bank, since (a) no concept tags exist and (b) the topic pool is dominated by template siblings. Tier-1 practice value is materially degraded today.

---

## Part C — Deliverable: Concept Mapping Table (unique level)

Proposed reviewed mapping for the pilot topic (per spec §6 "classify every question into one or more concepts or mark UNMAPPED"):

| externalId (representative of duplicate group) | Concept(s) | Confidence | Note |
|---|---|---|---|
| SO-OS-0001 (+0145/0193/0241) | peterson-software; critical-section | HIGH | direct |
| SO-OS-0005 (+0041/0065/0125) | semaphore-ops | HIGH | direct |
| SO-OS-0009 (+0029/0049/0069/0089/0109/0129/0149/0169/0189/0209/0229) | semaphore-ops | HIGH | 6 parameter sets; all P3 |
| SO-OS-0017 (+0037/0057/0077) | semaphore-ops | MEDIUM | option-level review needed (see rec) |
| SO-OS-0021 (+0033/0045/0105) | critical-section | HIGH | direct |
| PYP-2015..2025 (all 12) | semaphore-ops | HIGH | single template; treat as variant family, not 12 items |
| SYN-OS-2024-Q16 | critical-section | HIGH | starter |

No question maps to producer-consumer, readers-writers, dining-philosophers, monitors, hardware-atomic, or race-interleaving — **not because mapping failed, but because those questions do not exist.**

---

## Part D — Implications for Phase 8

1. **The premise has changed.** The Operator's original complaint ("primer too thin to master the topic from ~50 questions") resolves into two stacked deficits: (a) thin theory (confirmed, primer = orientation only), and (b) **thin bank** (new finding: ~6 real patterns). Fixing (a) without (b) produces beautiful lessons for six questions.
2. **Coverage computation (PC-4) will now do real work.** With honest computation, Process Synchronization would immediately display "3 of 9 concepts with material, 6 empty" — which is correct and actionable, and exactly the honesty the spec wants.
3. **Mapping (WP1) is cheap; bank remediation is the long pole.** Mapping 41 uniques is hours. Building a concept-balanced bank (~15–20 unique questions per concept × 9 concepts ≈ 150–180 for the pilot topic) is the real work — sourced from authored/AI-generated items with badges + curation, plus the official PYP import (IITM portal) already planned.
4. **The PYP-ACCEPTED provenance violation must be corrected** before any Phase 8 code: either downgrade the 12 template clones' curation status to UNREVIEWED/variant-labeled, or replace them with the genuine official PYP import for OS. Keeping ACCEPTED would let coverage and progress systems lie.

## Part E — Revised Phase 8 Sequencing (recommendation)

| WP | Content | Change vs. operator plan |
|---|---|---|
| **WP0 (new) — Bank remediation (pilot topic first)** | Dedup pass (keep 1 copy per unique; delete fabricated-year clones); re-label PYP template family honestly; author/generate questions for the 6 empty concepts (~15–20/concept, AI badge + curation, AF rules); target ≥12 unique per canonical concept; add a dedup guardrail (normalized-statement uniqueness check in the seed validator — extends existing AF-1 validation) | Inserted before taxonomy/mapping — mapping against 6 patterns produces a hollow pilot |
| **WP1 — Taxonomy + mapping** | As planned; canonical 9 concepts stay curator-first; mapping of remediated bank through existing CurationQueue | unchanged except input bank |
| **WP2 — Concept lessons + UI** | Per A1 amendments | trimmed lesson anatomy |
| **WP3 — Loop closure** | As planned (review panel concept link, mistake→lesson, similar re-solve) | unchanged |

**Quick wins that remain valid regardless:** primer grounding fix (stratified 20–30 samples, feed concept_units once populated); similar-questions dedup-aware ranking (exclude same-template siblings from "similar" results until the bank is remediated).

## Part F — Decisions Requested from Operator

1. Approve **WP0 bank remediation** as a Phase 8 prerequisite (scope: pilot topic first, then a separate package for the systemic 36-topic cleanup — do not mix them in one branch per G9).
2. Approve the **PYP status correction** (12 template clones: status downgrade or replacement with official-import items).
3. Confirm the 9-concept canonical taxonomy as the target (with empty concepts displayed honestly rather than pruned).
4. Approve folding amendments A1–A5 into the operator spec doc (I can apply them doc-only, or OmniRush/GPT does — either is fine).

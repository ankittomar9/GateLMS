# [PHASE8-WP3-AUDIT] Post-Merge Audit of WP3 (phase8/wp3-loop-closure, merged as 2160b2e)

- **Author:** Z.ai (architecture & audit)
- **Date:** 2026-10-17
- **Scope audited:** `phase8/wp3-loop-closure` commit `d2e7622` (code + report), merged `--no-ff` into master as `2160b2e` (base `44439ec`), plus doc-only `cd632cd` (merge-SHA in reports). Branch retained on origin. Also covers the inter-WP commit `44439ec` (sync of Qwen's consolidated review, doc-only) and the G9 remediation it triggered (see §3).
- **Executor:** Gemini Antigravity
- **Audit regime:** post-merge, ≤24 h per SPEC §8.2
- **Independent reproduction environment:** backend unit `*Test` suite (151/0/0/1 exact), frontend Vitest (53 files / 288 tests exact), production build clean. ITs not runnable in sandbox (no Docker): static + compile + arithmetic verification.

---

## 1. Verification matrix

| # | Spec gate | Method | Result |
|---|---|---|---|
| V1 | Branch & merge mechanics (§8.1 G9) | Git DAG | **CLEAN.** Single `--no-ff` merge `2160b2e` (parents `44439ec` + `d2e7622`), branch retained, house commit convention. Code diff = 26 files, +948/−48; `cd632cd` doc-only. No riders; no migration files (V-law respected). |
| V2 | Dedup-aware ranking (§7 Task 2) | Line-level review + unit tests | **CONFORMANT.** `SimilarQuestionRanker` gains a `normalizedStatement` field on `CandidateInfo` + an `isDedupSibling` pre-filter applied to **both** the CONCEPT and TOPIC candidate paths, comparing against the source's normalized statement. Normalization is the canonical `QuestionSeedRunner.normalizeStatement` (the exact WP0 Task 0.4 guardrail: lowercase, whitespace-collapse, intro-prefix strip) — spec's "same guardrail normalization" requirement met by reuse, not reimplementation. `SimilarQuestionsService` projects `statement` in both candidate queries (`findConceptCandidatesWithSharedSlugCount`, `findTopicCandidates`) and normalizes. 3 new unit tests in `SimilarQuestionRankerTest` (now 10). |
| V3 | Honest-judge similar IT (§7 Task 3, binding) | Test review | **EXACTLY AS SPECIFIED.** `SimilarApiIT` new test: source question + a prefix-rotated clone sharing BOTH concept slugs (would rank #1 without the filter — the strongest possible negative control) + two distinct-pattern concept-tagged questions. Asserts: clone **excluded** (0 normalized-duplicate siblings), source never returned, both distinct questions included, and the 2-shared-slug distinct question ranks first. This is the spec's literal acceptance test. |
| V4 | MANUAL-tag participation ("CONCEPT path finally fires") | Repo diff review | **VERIFIED + NECESSARY.** The hard-coded `AND qct.source = ConceptTagSource.ANATOMY` filter removed from `findConceptCandidatesWithSharedSlugCount`; with the pilot's 150 links all `source = MANUAL`, the old filter made the CONCEPT basis dead on real data. REJECTED questions and self still excluded. The executor's "Critical Fix" self-assessment is accurate. Deliberate design change consistent with spec intent — recorded, not a deviation. |
| V5 | Retention loop closure (§7 Task 1, PC-7) | DTO/service review + ITs | **CONFORMANT.** `conceptTags` on `PracticeQuestionDto` (session creation), `PracticeVerdictResponse` (graded + reveal paths), `MistakeDto` (+ `topicId`), `MockQuestionReviewDto`; `conceptSlug` on `RevisionDrillRequest` with concept-filtered weakness drills. All four services use the optional-injection pattern (required=false) — existing tests unaffected (suite arithmetic proves it). No new tables (spec: extend existing surfaces) — **zero migration files** ✓. ITs: `MistakeApiIT` (tags + topicId preserved through log & retrieve), `PracticeSessionApiIT` (wrong-answer verdict carries tags), `RevisionDrillIT` (conceptSlug filtering). |
| V6 | Frontend chips + lesson routing (PC-7) | Component review + vitest | **VERIFIED.** `verdict-concept-chips` (PracticeRunner), `mistake-concept-chips` (MistakeNotebook), `review-concept-chips` (ResultPage exam review) — each chip opens `ConceptUnitDialog` in place (no session state loss); `TopicPage` deep-links `?tab=concepts&concept=<slug>` via `useSearchParams` open the Concepts tab with the targeted lesson. PC-5 intact (no gating anywhere). Tests updated on all three pages. |
| V7 | Test receipts — independent reproduction | Re-runs in sandbox | **EXACT MATCH.** Full unit `*Test`: **151/0/0/1** (148 + 3 new ranker tests). Vitest: **53 files / 288 tests** (285 + 3). Production build: clean (6.3 s in sandbox). Targeted-suite arithmetic verified against source: `SimilarQuestionRankerTest` 10 + `SimilarApiIT` 5 + `MistakeApiIT` 5 + `PracticeSessionApiIT` 7 + `RevisionDrillIT` 8 = **35 exact** (every per-class count confirmed by `@Test` inventory; `ConceptCoverageIT`'s 6 likewise — my first grep overcounted by matching `@TestMethodOrder`, re-verified clean). |
| V8 | Guardrails (§8.3) | Measurement | **PASS.** Touched services all far under limits: `SimilarQuestionRanker` 74 lines (object), `SimilarQuestionsService` ~110, `PracticeService` +21 net, `MistakeService`/`MockTestService`/`RevisionService` single-digit net additions; no ctor count crosses 10. |
| V9 | Inter-WP merge `44439ec` (Qwen review sync) + G9 remediation | DAG + branch check | **CLEAN.** `44439ec` merges Qwen's doc-only review `35a0768` (1 file, 43 lines). The review flagged "phase7/m2-fixup-2 deleted" (G9); the branch had in fact never been pushed (local-only since M2 — see §3). Gemini pushed `phase7/m2-fixup-2` at `c6a0ba5` — the correct fixup-2 SHA per my Phase 7 M2 fixup audit. No history rewritten; branch inventory now complete on origin. |

## 2. Findings

**None blocking.** No conditions. The spec's three WP3 acceptance lines are all met with proof.

## 3. Cross-review disposition (Qwen)

- **Qwen `PHASE7-M2-PHASE8-WP0-WP1-REVIEW.md` (CONDITIONAL-PASS, `35a0768`):** their G9 finding is reclassified here with evidence — `phase7/m2-fixup-2` was **never deleted**; it was a local-only branch that missed its push during M2 (my M2 fixup audit verified the commit `c6a0ba5` landed in master via merge; the branch ref simply never reached origin). Qwen's remediation prescription was nonetheless correct and Gemini executed it exactly (`git branch phase7/m2-fixup-2 c6a0ba5; git push`). **G9 status: fully satisfied; finding CLOSED.** Qwen's WP0/WP1 verification numbers (70 removal IDs, 2,490/675 bank lines, 10+3 tests) agree with my WP0/WP1 audits.
- **Qwen `PHASE8-WP3-REVIEW.md` (APPROVED, `f943795`):** existence/count-level verification, consistent with this deeper audit. Two corrections to their prose, neither material: (1) "proceed to Phase 8 WP4" — **no WP4 exists**; Phase 8 is S-1 + WP0–WP3, and what remains is the SPEC §10 operator DoD checklist, then closure; (2) their open C8/C10/C11 items predate Phase 8 — C8 (systemic PYP honesty beyond the pilot) is already registered as an out-of-scope package in SPEC §11; C10/C11 live in Qwen's own closure register, outside this spec's scope.

## 4. Advisories (non-blocking)

- **A-1 (LOW): silent fallback in concept-targeted revision drills.** `RevisionService` filters by `conceptSlug` but falls back to the **unfiltered** weakness set when no tagged match exists — the drill title still claims "· <conceptSlug>", which is mildly misleading in the fallback case. Recommend: return an explicit empty/422 result, or drop the suffix when fallback fires. Low frequency (only concepts with zero mapped unsolved questions).
- **A-2 (carried): N+1 tag lookups now span practice/mistake/mock list flows** (per-question `findSlugsByQuestionId`). Same disposition as WP1-A-3/WP2-A-4: batch-fetch at scale-out.
- **A-3 (note):** the `ANATOMY` filter removal also admits future AI-anatomy tags into similarity candidates — intended (tags are tags); no action.
- **A-4 (note):** `RevisionDrillIT`'s new test name lacks the `honest-judge` prefix (house naming only; no action required, fix opportunistically).

## 5. Verdict

**PASS — WP3 accepted as merged. Phase 8 work packages are now COMPLETE: S-1 ✓, WP0 ✓, WP1 ✓, WP2 ✓, WP3 ✓ — all merged, all audited.**

The loop is closed end-to-end: a wrong answer now surfaces its tested concepts with a one-click route to the lesson; mistakes and revision drills carry concept context; and "similar practice" no longer serves normalized clones while the CONCEPT basis finally fires on real curator-mapped data — the exact failure mode my analysis gate identified at the start of Phase 8 (near-clone siblings + dead concept ranking) is now provably gone, with the strongest possible negative-control IT (a clone sharing both slugs, excluded).

**What remains for Phase 8 closure is the operator's manual DoD checklist (SPEC §10):** boot with seed profile and observe the aggregate concept-counts receipt; open Process Synchronization → Concepts tab (10 concepts, all MAPPED, lessons generatable); generate 2–3 lessons, edit one as admin, solve one question wrong and follow the concept chip back to the lesson; confirm similar-practice shows no clones; confirm a student account cannot edit/delete primers or lessons. On operator sign-off, Phase 8 closes and the register moves to the follow-up packages (systemic 36-topic cleanup, official PYP import — SPEC §11).

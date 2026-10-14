# [PHASE8-CONCEPT-LAYER-SPEC] Concept Layer & Bank Remediation — Process Synchronization Pilot

- **Author:** Z.ai (spec author & auditor)
- **Date:** 2026-10-15
- **Base commit:** `439c921` (Phase 7 M2 CLOSED — see `collab/z/PHASE7-M2-FIXUP-AUDIT.md`)
- **Status:** APPROVED_FOR_EXECUTION (Operator directive 2026-10-15: "push everything … I will have [the executor] execute the SPEC as intended")
- **Executor:** OmniRush (implementation agent — first GateLMS assignment)
- **Auditor:** Z.ai (per-merge audit; see §8.2 for the audit-gate escalation schedule)
- **Amends & supersedes for execution purposes:** `collab/operator/PHASE8-TOPIC-COVERAGE-SPEC.md` (aca1349) — PCs carried forward with amendments A1–A5 from `collab/z/PHASE8-ANALYSIS-GATE-PROCSYNC.md` folded in here. The operator doc remains the product-intent source; THIS doc is the binding execution contract.
- **Migrations owned:** V17 (concept-layer minimal ALTERs). SPEC.md register updated: M3 shifts to V18.

---

## 0. How to read this spec (cold-start instructions for the executor)

You are new to this repository. Everything you need to execute is in this document plus the files it cites. Rules of engagement:

1. **You implement; you do not redesign.** Where this spec is binding (marked **MUST**), do not substitute a different approach. Where latitude is granted (marked **MAY**), choose the simpler option.
2. **One work package = one branch = one merge.** Never mix WPs. Branch names are given in each WP.
3. **Honest receipts.** Every WP produces a written report (`collab/<executor>/PHASE8-WP<N>-REPORT.md`) with real command output (test counts, build results, seed receipts). Never report numbers you did not personally observe. Fabricated statistics are a firing-level offense in this project (AF rules, §8.5).
4. **Audit gates.** S-1 and WP0 require Z.ai audit BEFORE merging to master (§8.2). Later WPs are post-merge audit. A FAIL verdict freezes your next WP until remediation.
5. **This spec's evidence base is real, not hypothetical.** §3 numbers were produced by an offline script over the actual seed files (`scripts/psync_inventory.py` in the auditor's environment — repo path `seed-data/*.jsonl`). Re-verify anything you doubt before building on it; do not silently assume.
6. **Language & docs:** all code, comments, reports, and commit messages in English. Reports go under `collab/omnirush/`.

## 1. Operator mandate & resolved decisions

The Operator (repo owner) has directed execution of the Phase 8 topic-coverage plan "as intended" after three review rounds (GPT proposal → Z.ai design review with deltas D1–D6 → analysis gate with amendments A1–A5). The four decisions requested by the analysis gate are hereby resolved as binding:

| # | Decision | Resolution |
|---|---|---|
| DEC-1 | WP0 bank remediation as Phase 8 prerequisite | **APPROVED** — pilot topic only; the systemic 36-topic cleanup is a SEPARATE later package (out of scope, §11). |
| DEC-2 | PYP-ACCEPTED provenance correction (12 template clones) | **APPROVED** — dedup to ≤2 honestly-labeled variants, downgrade to UNREVIEWED, strip fake PYP externalIds (never claim exam provenance). Genuine official-PYP import is a later package. |
| DEC-3 | Canonical taxonomy | **CONFIRMED** — 10 slugs (§6.1): the operator doc's 9-item hypothesis with item 7 split (readers-writers / dining-philosophers are distinct concepts per analysis-gate Part C). Empty concepts stay in the catalog and display honestly. |
| DEC-4 | Amendments A1–A5 folded into the operator spec | **DONE** — folded into this document (A1→§4 PC-2, A2→§4 PC-4, A3→§6, A4→§8, A5→WP0). |

## 2. Cold-start context

- **Repo:** `github.com/ankurtomar8/GateLMS` (private). Spring Boot 3 / Kotlin / Maven backend in `backend/`, React 19 + TypeScript + Vite + Tailwind v4 frontend in `frontend/`, MySQL via Flyway migrations `backend/src/main/resources/db/migration/V1..V16`, seed banks as JSONL in `seed-data/` (26 starter `questions.jsonl`, 555 `pyp-bank.jsonl`, 2,550 `so-gate-bank.jsonl`).
- **Domain model:** `subjects` → `topics` → `questions` (curation_status UNREVIEWED/ACCEPTED/REJECTED; source AUTHORED/PYP/GATE_OVERFLOW/SAMPLE). Attempts/mistakes/revision/notebook flows exist and work.
- **M1 anatomy layer (V15) — built, populated only on the 26 starters:** `concept_tags` (slug, label, topic_id), `question_concept_tags` (question_id × slug, source ANATOMY/MANUAL, confidence), `concept_units` (one per tag: title, motivation, core_idea, invariant, worked_example, traps, deep_link_ref, status DRAFT/REVIEWED/ACCEPTED/REJECTED, generator_model, parse_error), `retrieval_cards` + `retrieval_card_state` (SM-2-lite), `question_anatomy`.
- **M2 journey layer (V16) — live:** journey home/subject/topic pages (`frontend/src/pages/JourneyHomePage.tsx`, `SubjectPage.tsx`, `TopicPage.tsx`), `TopicPrimerService` (AI topic primers: cache-first, 10/h/user quota, never persist failures, admin edit/delete — the rails you will clone), `SimilarQuestionRanker` (CONCEPT path needs ≥2 shared tags, else TOPIC fallback), `QuestionSeedRunner` (`backend/src/main/kotlin/com/gatelms/seed/QuestionSeedRunner.kt`: upsert-by-externalId, per-file receipts `[SEED-RECEIPT]`, grand `[ALL-SEEDS-RECEIPT]`; JSONL re-run overwrites DB fields incl. source/difficulty/curationStatus).
- **Governance:** `AGENTS.md`, `collab/AUDIT-PROTOCOL.md`; branch protocol in `collab/z/PHASE1-SPEC.md` §3.3 (as amended: no branch deletion ever). Read `collab/z/PHASE7-M2-EXECUTION-AUDIT.md` §"Findings" before your first commit to avoid repeating known process failures (self-merge without gate, unverified receipts).
- **Test reality:** backend `./mvnw clean test` (306 @Test, Testcontainers ITs need Docker); frontend `cd frontend && npx vitest run` (275/275 at base). Run the relevant suite before every merge request.

## 3. Evidence base (binding facts from the analysis gate)

These are measured facts, not assumptions (source: `collab/z/PHASE8-ANALYSIS-GATE-PROCSYNC.md`, reproducible against the seed files):

1. Process Synchronization contains **83 rows** (70 SO-GATE + 12 PYP + 1 starter), not "~50" — `TopicPage`'s `size: 50` is a page size.
2. **83 → 41 unique statements.** 42 rows are exact duplicates repeated at +48 externalId offsets with **fabricated year spreads** (identical item labeled 2011/2020/2023/2011).
3. The 12 `PYP-*` rows are **one template** (semaphore arithmetic) with rotating intro prefixes and slightly varied numbers, labeled ACCEPTED with a curation note claiming curated-exam provenance — **a provenance violation** (DEC-2).
4. After number-masking, the whole topic is **6 reasoning patterns**; ~63% of unique content is semaphore-ops.
5. **6 of 10 canonical concepts have ZERO questions** (§6.1). The bank, not the primer, is the binding constraint on learning depth.
6. Systemic: whole seed bank 3,131 rows → 1,801 unique statements (42.4% redundant) → ~692 distinct patterns. An intro-prefix rotation generator fingerprint is present across banks. (Systemic cleanup: OUT OF SCOPE.)
7. `SimilarQuestionRanker` therefore currently serves near-clones as "similar practice" (no concept tags + template siblings) — degraded Tier-1 value, fixed in WP3.
8. Primer prompt grounding: `TopicPrimerService.kt` samples only first-10 questions, truncates statements to 200 chars, and never receives concept labels — fixed in WP2.

## 4. Product contract (binding)

- **PC-1 Primer scope is explicit.** UI labels the primer "topic orientation" — never implies it covers every question. No fabricated coverage percentages anywhere.
- **PC-2 Concepts are the teaching unit.** Lesson anatomy (amendment A1, trimmed): 300–600 words total, sections = plain-language explanation · formula box (KaTeX) · **ONE worked example adapted from a REAL mapped question, citing its externalId** (validated: externalId must resolve and be mapped to that concept) · 2–4 trap bullets · link to the practice rail (computed from mapping, not stored). Formal-definitions/notation depth and active-recall prompts are DEFERRED to a later milestone. One lesson per canonical concept (= `concept_units` row).
- **PC-3 Questions link back to concepts.** Every question carries 1–3 concept tags; the review/solution panel shows concept chips that route to the lesson.
- **PC-4 Coverage is evidence-based and COMPUTED ON READ (amendment A2).** Statuses derive at query time from `question_concept_tags` × `concept_units.status` × question curation status — NEVER stored as a mutable enum. Per question: `UNMAPPED` (no tags) / `MAPPED` (≥1 tag) / `EXPLAINED` (≥1 tag AND that concept's lesson status = ACCEPTED). `REVIEW_NEEDED` is DROPPED — the existing `UNREVIEWED` curation status already means this; reuse it.
- **PC-5 No sequential locks — ever.** Recommendations may guide; nothing is access-gated by prior completion (house law since M2, Operator-ruled).
- **PC-6 Private references stay private.** Book PDFs never enter git/public seeds/providers. Book metadata + chapter refs OK; a chapter ref must not imply the AI read the book. `deep_link_ref` stays free-text metadata.
- **PC-7 Retention closes the loop.** A wrong answer surfaces the tested concepts with a route to the lesson; repeated mistakes flow into the existing mistake/revision system.
- **PC-8 (new) Provenance honesty.** AI-authored items are badged `AI_GENERATED` + `UNREVIEWED` until curated; no fake exam years; no template families masquerading as curated PYP items (DEC-2).

## 5. Architecture & data model (binding)

1. **Reuse V15 — do not rebuild it.** The taxonomy (`concept_tags`), mapping (`question_concept_tags`), lessons (`concept_units`), and recall cards (`retrieval_cards`) tables already exist. Phase 8 POPULATES them; it does not redesign them.
2. **V17 (owned by this spec) is minimal:**
   ```sql
   -- V17__phase8_concept_layer.sql
   ALTER TABLE concept_units ADD COLUMN worked_example_external_id VARCHAR(120);
   ```
   Plus, ONLY IF a CHECK constraint on `questions.source` exists in any migration (verify with grep): extend it to admit `'AI_GENERATED'`. Also only-if-needed: nothing else. If you find yourself writing another CREATE TABLE, stop — you have misread this spec.
3. **Mapping seed mechanism:** new runner `ConceptSeedRunner` (new file, `backend/src/main/kotlin/com/gatelms/seed/ConceptSeedRunner.kt`) consuming `seed-data/concept-taxonomy-pilot.jsonl` (§6.1) and `seed-data/concept-mapping-pilot.jsonl` (§6.2). Idempotent upsert by natural keys (slug; question_externalId × slug). Source of mapping rows = `MANUAL`, confidence = 1.0 (they are curator-reviewed). Receipts logged in the same `[SEED-RECEIPT]` style; extend the aggregate receipt with concept counts.
4. **Lesson service:** new file `ConceptLessonService.kt` (namespace `com.gatelms.anatomy.service`), cloning the proven TopicPrimerService rails: cache-first, `force` regeneration param, per-user quota (REUSE the existing primer bucket — do not add a second limiter), 1 internal retry, never persist failed generations (throw 502 with parse detail), admin-only edit/delete (see S-1 — the same authorization rule), DRAFT→ACCEPTED only via curation. New-file rule: TopicPrimerService MUST NOT grow; you may extract shared helpers into a new small class if duplication exceeds ~60 lines.
5. **Coverage read model:** new endpoint `GET /api/v1/journey/topics/{topicId}/concepts` returning per-concept `{slug, label, mappedQuestionCount, uniqueStatementCount, lessonStatus, coverage}` + topic rollup `{conceptsTotal, conceptsWithMaterial, conceptsExplained}`. Computed via JPQL aggregate counts on the mapping/units/questions tables — no stored coverage columns, no scheduled jobs.
6. **Guardrails for all new backend code:** ≤600 LOC per service file, ≤10 constructor deps (God-class guardrail), thin controllers, DTOs mapped explicitly. Vitest coverage for every new frontend surface.

## 6. Canonical taxonomy & mapping (binding inputs)

### 6.1 Taxonomy — `seed-data/concept-taxonomy-pilot.jsonl` (10 rows)

| slug | label |
|---|---|
| `critical-section-requirements` | Critical Section & Mutual Exclusion Requirements |
| `peterson-software-solutions` | Peterson's Software Solution |
| `hardware-atomic-primitives` | Hardware Atomic Primitives (Test-and-Set, CAS, Spinlocks) |
| `semaphore-operations` | Semaphore Operations & Conventions |
| `semaphore-state-tracing` | Semaphore State Tracing |
| `producer-consumer-bounded-buffer` | Producer–Consumer / Bounded Buffer |
| `readers-writers` | Readers–Writers Problem |
| `dining-philosophers` | Dining Philosophers Problem |
| `monitors-condition-variables` | Monitors & Condition Variables |
| `race-interleavings` | Race Conditions & Interleavings |

All 10 rows have `topicSlug: "process-synchronization"`. The 4 with material today (critical-section-requirements, peterson-software-solutions, semaphore-operations, semaphore-state-tracing) map per §6.2; the other 6 are the WP0 fill targets.

### 6.2 Mapping — `seed-data/concept-mapping-pilot.jsonl` (post-WP0 bank, 1–3 slugs per externalId)

Binding starting point from the analysis gate (Part C) — REMAP against the post-WP0 bank before landing:

| representative externalId (dedup group) | concepts |
|---|---|
| `SO-OS-0001` group (4 clones→1) | `peterson-software-solutions`, `critical-section-requirements` |
| `SO-OS-0005` group (4→1) | `semaphore-operations` |
| `SO-OS-0009` group (12→6 parameter variants) | `semaphore-state-tracing`, `semaphore-operations` |
| `SO-OS-0017` group (4→1) | `semaphore-operations` |
| `SO-OS-0021` group (4→1) | `critical-section-requirements` |
| `PYP-*` family (12→≤2, re-labeled) | `semaphore-state-tracing` |
| `SYN-OS-2024-Q16` (starter) | `critical-section-requirements` |

Every NEW question authored in WP0 carries its intended concept slug(s) in the mapping file at authoring time (mapping is part of authoring, not an afterthought).

## 7. Work packages (execute strictly in order: S-1 → WP0 → WP1 → WP2 → WP3)

### S-1 — Security fixup: ADMIN-gate the primer write endpoints

- **Branch:** `phase8/s1-primer-authz` · **Est:** 0.5 h · **Audit: BEFORE merge**
- **Problem (from PHASE7-M2-FIXUP-AUDIT finding S-1, HIGH):** `PUT /api/v1/journey/topics/{topicId}/primer` and `DELETE /api/v1/journey/topics/{topicId}/primer` in `JourneyController` lack `@PreAuthorize("hasRole('ADMIN')")`; SecurityConfig falls through to `anyRequest().authenticated()`, so any STUDENT can overwrite/delete shared primers. `TopicPage.tsx` renders the edit/delete controls for everyone.
- **Tasks:**
  1. Add `@PreAuthorize("hasRole('ADMIN')")` to both endpoints (match the adjacent `/curation/primers` pattern).
  2. Add one negative-authorization IT in `JourneyApiIT`: student PUT → 403 AND student DELETE → 403 (assert body is the standard ProblemDetail JSON).
  3. Role-gate the TopicPage primer edit/regenerate (force)/delete controls behind the existing auth-context role (admin only). Regenerate (non-force) remains student-available per M2 design.
  4. Extend the TopicPage test to cover the gated rendering.
- **Acceptance:** 403 for students on both endpoints; controls hidden for students; all suites green; no other behavior changed.

### WP0 — Bank remediation (pilot topic ONLY: process-synchronization)

- **Branch:** `phase8/wp0-bank-remediation` · **Est:** 8–12 h (content authoring dominates) · **Audit: BEFORE merge**
- **Goal:** a deduplicated, honestly-labeled, concept-balanced pilot bank. Target shape: **every one of the 10 concepts ≥12 unique statements (target 15)**; topic total ≈ 150–180 rows; ≥4 distinct reasoning patterns per filled concept (receipt-reported, not hard-gated); zero exact-duplicate normalized statements.

- **Task 0.1 — Dedup the existing 83 rows (data edit, `seed-data/so-gate-bank.jsonl` + `pyp-bank.jsonl`):**
  - Keep ONE row per unique-statement group; delete the +48-offset clones (42 rows). Apply the removals via a **removal manifest** — new file `seed-data/removals.jsonl` (`{"externalId": "..."} ` rows) consumed by a new opt-in runner method `applyRemovals()` (deletes by externalId, logs `[REMOVAL-RECEIPT] deleted=<n>`). Deterministic, auditable, no heuristics. Existing DBs converge on reseed; fresh boots just get the clean file.
  - Fix fabricated year spreads: surviving rows carry their single honest year (or null).

- **Task 0.2 — PYP provenance correction (DEC-2, `pyp-bank.jsonl`):**
  - Keep ≤2 of the 12 template rows (distinct parameter sets). Re-label: `externalId` → `AUTH-OS-PSYNC-VAR1/2` (strip the fake `PYP-` prefix), `curationStatus` → `UNREVIEWED`, honest `curationNote`: "Template-family variant; deduplicated from 12 prefix-rotated clones 2026-10; parameter drill, not an exam item." Add the other 10 to `removals.jsonl`.
  - `year` for AUTHORED rows: MAY be null (column is nullable). If the runner's `raw.year!!` non-null assertion blocks this, relax it for authored-sourced rows (small code change in `QuestionSeedRunner`); otherwise set no year.

- **Task 0.3 — Fill the 6 empty concepts (+ diversify tracing):**
  - Author ≥12 unique questions per empty concept (target 15): `producer-consumer-bounded-buffer`, `readers-writers`, `dining-philosophers`, `monitors-condition-variables`, `hardware-atomic-primitives`, `race-interleavings`. GATE-aligned difficulty mix (EASY ≤20%, MEDIUM ~60%, HARD ≥20%), type mix incl. MSQ + NAT, KaTeX in statements where natural.
  - **Provenance (PC-8):** `source: "AI_GENERATED"`, `curationStatus: "UNREVIEWED"`, honest curationNote, no fabricated exam years, AF-1 content rules (no invented statistics). Extend the seed validator's accepted source values with `AI_GENERATED` (verify first that no DB CHECK blocks it; if one exists, extend it in V17).
  - Pattern diversity per concept ≥4 (e.g. producer-consumer: semaphore-count trace · invariant MSQ · code-fragment output prediction · starvation scenario · buffer-state reasoning). This is the pedagogical core of the pilot — treat it as content design, not row generation.

- **Task 0.4 — Dedup guardrail (validator, extends AF-1 family):**
  - `QuestionSeedRunner.validateQuestion` gains a per-file uniqueness check on the **normalized statement**: lowercase, collapse whitespace, strip known intro prefixes (configurable list in the validator: "For X, solve:", "Evaluate for X:", "With X parameters:", "From X concepts:", "On X:", "In X formulation:", "As applied to X:", "Under the principles of X:", "Reviewing X:", "In the domain of X:"). Digits are KEPT (parameter drills are legitimate). Duplicate normalized statements within a file → reject with line numbers.
  - Extend receipts: per file, report `uniqueStatements` and `distinctPatterns` (digits masked) per topic. Warn (log only) when unique:pattern < 3:1 (template saturation signal).
  - Add unit tests: exact duplicate rejected; prefix-rotated duplicate rejected; two parameter variants accepted; receipt fields present.

- **Acceptance gates (all binding):**
  1. Post-remediation pilot bank: 10/10 concepts ≥12 unique; 0 normalized duplicates (validator proves it); receipts logged.
  2. `removals.jsonl` applied: `[REMOVAL-RECEIPT]` shows 52 deletions (42 SO clones + 10 PYP clones) on the first reseed; idempotent second run deletes 0.
  3. New-question sample: 10 random rows hand-checkable for correctness of answers/explanations (the auditor will hand-check; errors → FAIL).
  4. Full backend suite + vitest green. New unit tests for validator + removals green.
  5. Report `collab/omnirush/PHASE8-WP0-REPORT.md` with receipts, counts, and the per-concept table (slug × unique count × pattern count).

### WP1 — Taxonomy, mapping, coverage API

- **Branch:** `phase8/wp1-taxonomy-mapping` · **Est:** 4–6 h · **Audit: post-merge (≤24 h)**
- **Tasks:**
  1. `ConceptSeedRunner` (new file) importing `concept-taxonomy-pilot.jsonl` → `concept_tags` (idempotent upsert by slug) and `concept-mapping-pilot.jsonl` → `question_concept_tags` (upsert by externalId×slug, resolve externalId→UUID, source MANUAL, confidence 1.0). Remap §6.2 against the post-WP0 bank first; every authored question gets its slug(s). Receipts in `[SEED-RECEIPT]` style + concept counts in the aggregate receipt.
  2. V17 migration (§5.2) + `worked_example_external_id` on the `ConceptUnit` entity.
  3. Coverage read model (§5.5): `GET /api/v1/journey/topics/{topicId}/concepts` + topic rollup; per-question concept tags added to the question detail DTO (PC-3 groundwork).
  4. JourneyController gains ADMIN-gated curation endpoints for mappings: list unmapped/low-confidence, PATCH a question's tags (add/remove) — reuse the curation pattern.
- **Acceptance:** taxonomy 10/10 seeded; mapping covers 100% of the pilot bank (0 UNMAPPED questions — this pilot is fully mapped by construction); coverage endpoint returns the honest split (conceptsWithMaterial=10 post-WP0, conceptsExplained=0 until WP2 lessons are ACCEPTED); IT proves coverage math (mapped question → MAPPED; ACCEPTED lesson + mapping → EXPLAINED; unmapped question → UNMAPPED; deleting tags flips status back — computed-on-read proven).

### WP2 — Concept lessons + topic concepts UI + primer grounding fix

- **Branch:** `phase8/wp2-lessons-ui` · **Est:** 8–12 h · **Audit: post-merge (≤24 h)**
- **Tasks:**
  1. `ConceptLessonService` (new file, §5.4) with the full primer-rails clone; generation prompt receives: concept label, taxonomy description, the mapped questions' statements (stratified sample up to 15, full text ≤1,500 chars each), and the topic primer's core_idea as context. Lesson anatomy = PC-2. AF-1 validation extended to lessons (≥300 words, ≤600 words, worked-example externalId resolvable AND mapped to this concept — hard validation).
  2. Admin curation: list/edit/regenerate/delete lessons (ADMIN-gated, mirroring S-1-fixed primer endpoints); AI-GENERATED badge until status=ACCEPTED.
  3. `TopicPage` gains a Concepts tab (freely switchable — PC-5): concept cards showing label, one-line plain explanation, coverage chip, mapped-question count, lesson state; card routes to lesson view (lesson renders PC-2 sections + "Practice these questions" rail = mapped questions, computed).
  4. Question review/solution panel shows concept chips routing to lessons (PC-3).
  5. **Primer grounding fix (quick win):** `TopicPrimerService` prompt assembly replaced: stratified sample across (type × difficulty) up to 30 questions, full statements (≤1,500 chars each), plus the mapped concept labels; drop first-10/200-char limits. Primer UI label → "Topic orientation" (PC-1). Existing primer tests updated honestly.
- **Acceptance:** lessons generatable for all 10 concepts with rails intact (quota, no corrupt rows, admin edit); UI tabs free; per-concept coverage visible and honest; primer prompt verifiably receives 20–30 stratified statements (assert in a unit test on the prompt builder); vitest for all new UI; full suites green.

### WP3 — Loop closure + similar-question quality

- **Branch:** `phase8/wp3-loop-closure` · **Est:** 6–8 h · **Audit: post-merge (≤24 h)**
- **Tasks:**
  1. Wrong answer → review panel surfaces tested concepts with lesson route; repeated mistakes flow into the existing mistake/revision system with concept context (PC-7). No new tables — extend existing mistake/revision surfaces with concept references from the mapping.
  2. **Similar-questions dedup-aware ranking (quick win):** `SimilarQuestionRanker` pre-filter — exclude candidates whose normalized statement equals the source question's normalized statement (same guardrail normalization as WP0 Task 0.4); then the existing CONCEPT/TOPIC logic. With WP1 tags live, the CONCEPT path (≥2 shared tags) finally fires on real data.
  3. Honest-judge IT: similar results for a pilot question contain 0 normalized-duplicate siblings and ≥1 concept-tagged different-pattern question.
- **Acceptance:** wrong-answer → concept route works end-to-end; similar list contains no clones; full suites green.

## 8. Process rules (binding)

### 8.1 Branch & merge (G9)
One WP = one branch = one `--no-ff` merge into master. Branch names fixed in §7. No branch deletion, ever. Commit messages: `feat(phase8)/fix(phase8)/test(phase8)/docs(phase8): ...` per house convention. Doc-only changes MAY go direct to master under `docs(phase8)`.

### 8.2 Audit gates (escalation schedule)
- **S-1 and WP0: Z.ai audit BEFORE merge.** Push the branch, then stop and wait (post the report + request in the report header). This is the executor's probation period.
- **WP1/WP2/WP3: post-merge audit within 24 h** (audit may run concurrently with the next WP's branch). A FAIL verdict on any WP freezes further merges until the fixup lands on a `phase8/wp<N>-fixup-<M>` branch.
- Reports under `collab/omnirush/PHASE8-WP<N>-REPORT.md` citing branch + merge SHA after merge.

### 8.3 Files & guardrails
New services in NEW files (§5.4, §5.5). ≤600 LOC / ≤10 ctor deps per service. No edits to V1–V16 migrations (Flyway law). V17 owned here; M3 moves to V18 (SPEC.md updated by this spec's commit).

### 8.4 Data honesty
AF rules apply to every generated artifact: no fabricated statistics (AF-1 regex family), no invented book citations, no fake exam provenance, AI-GENERATED badge until curated, receipts from real runs only.

### 8.5 Security
Every new write/curation endpoint carries `@PreAuthorize("hasRole('ADMIN')")` + a negative-authorization test. Personal data reads remain user-scoped.

## 9. Test matrix & receipts (per WP)

| WP | Backend | Frontend | Receipts |
|---|---|---|---|
| S-1 | 403 IT ×2 endpoints | gated-render test | report w/ suite output |
| WP0 | validator + removals unit tests | — | `[SEED-RECEIPT]` before/after, `[REMOVAL-RECEIPT]`, per-concept table |
| WP1 | coverage IT (computed-on-read flips), ConceptSeedRunner IT | — | seed receipts + concept counts |
| WP2 | lesson rails ITs, prompt-builder unit test | Concepts tab + chips + badge tests | generation receipts |
| WP3 | similar-rank dedup IT, mistake-route IT | review-panel chips test | — |

## 10. Definition of done & operator acceptance

All WPs merged and audited; then the Operator manually: (1) boots with seed profile and observes the aggregate receipt incl. concept counts; (2) opens Process Synchronization → Concepts tab shows 10 concepts, all MAPPED, lessons generatable; (3) generates 2–3 lessons, edits one as admin, solves one question wrong, follows the concept chip back to the lesson; (4) confirms "similar practice" shows no clones; (5) confirms a student account cannot edit/delete primers or lessons.

## 11. Out of scope (registered, not forgotten)
Systemic 36-topic bank cleanup (separate package, uses WP0's guardrail + removal manifest); official IIT-portal PYP import pipeline; textbook RAG/bookshelf (M3+ register); AI question variants (M3); retrieval-card generation & review UI (deferred with A1 — schema ready in V15); payment/SaaS (never in Phase 8).

## 12. Effort
S-1 0.5 h · WP0 8–12 h · WP1 4–6 h · WP2 8–12 h · WP3 6–8 h → **27–39 h** total. The long pole is WP0 Task 0.3 content authoring (~72–90 questions); treat it as the pilot's real work — it is.

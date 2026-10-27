# [FOLLOWUP-P1-FIXUP-AUDIT] Phase 8.5 Maintenance Fixup — Post-Merge Audit

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-27
- **Subject:** `phase8/maintenance-fixup-1` (executor commit `82ead23`, `--no-ff` merge `6733496`, base `83e9ee0`)
- **Regime:** Light audit ≤24 h per work order §6 (small scope: diff review, gate-by-gate receipt
  cross-check, independent suite re-runs)
- **Executor:** Gemini Antigravity — report: `collab/gemini/FOLLOWUP-P1-FIXUP-REPORT.md`
- **Dispatch note:** the operator dispatched P1 to the executor (D-1 approved by dispatch);
  D-2..D-9 remain open for the Phase 9 decision.

---

## 1. Verification matrix

| # | Item | Method | Result |
|---|---|---|---|
| V1 | Git mechanics (work order §5) | Git DAG | **CLEAN.** Single branch off `83e9ee0`, code+docs commit `82ead23`, one `--no-ff` merge `6733496` (parents `83e9ee0`+`82ead23`), branch retained on origin. 15-file diff: 9 main-code files (small), 3 new test files, 1 properties comment, SPEC.md register line, executor report. No riders. |
| V2 | **F-1 force-authz** (G1–G3) | Line-level diff | **EXACTLY AS PRESCRIBED.** Identical in-method guard at the top of BOTH handlers (`JourneyController` primer L51-53, lesson L142-144): `if (force && currentUserProvider.getCurrentUser().role != UserRole.ADMIN) throw AccessDeniedException("force=true requires ADMIN")`. SpEL-`#force` route correctly avoided. `GlobalExceptionHandler` L85-91 maps `AccessDeniedException` → 403 ProblemDetail (type=about:blank, title=Forbidden, detail=message) — same mechanism the WP2 V8 negative-authz ITs already proved for `@PreAuthorize` denials. Student non-force path untouched (guard short-circuits only when `force &&` non-admin). |
| V3 | **F-2 N+1 batching** (G4–G5) | Line-level diff | **VERIFIED.** One repo method `findSlugPairsByQuestionIds` (`IN :questionIds`, returns pairs) + extension `findSlugsGroupedByQuestionIds` (empty-list short-circuit, groupBy). All genuinely-looping sites batched with ONE query per page/session: `MistakeService.getMistakes` (page content), `MockTestService.getResult` (frozenIds, with defensive `runCatching` UUID parse), `PracticeService.createSession` + `createDrillSession`. Single-question paths preserved per work order (`getQuestionById`, `SimilarQuestionsService`, practice serve paths). Payload contract frozen: DTOs untouched, `toSummaryDto` untouched. |
| V4 | **F-3 ride-alongs** (G7) | Line-level diff | **VERIFIED BEHAVIOR-NEUTRAL.** Three model-label fallback literals `"gemini-3.8-flash"` → `"gemini-3.5-flash"` (`ConceptLessonService` L141 keeps the `aiProperties…ifBlank` chain; `ConceptService` L145, `AnatomyGenerationService` L97) — metadata strings only, no routing change (routing lives in `AiChatService`). `docker-java.properties` +2 comment lines, pin retained exactly as prescribed. |
| V5 | New test quality | Full read | **SOUND.** `JourneyAuthzTest` (6 cases: G1×2 both endpoints, G2×2 non-force pass-through, G3×2 admin force) — mock-based direct controller invocation, asserts exact exception + message. `ConceptTagBatchFetchTest` (4 tests) — strict Mockito verification at EVERY site: `times(1)` batch + `never()` per-row (L112-113 Mistake, L189-190 Mock, L259-260 Practice) plus the sharpest check: `times(2)` across Practice's two session paths (L269-270). `ForceAuthzIT` (3 cases) — house conventions (BaseIntegrationTest, `loginAs`, ProblemDetail field assertions), real seed slugs (`topic_os_process_sync`, `critical-section-requirements`). |
| V6 | Receipts — independent reproduction | Re-runs in sandbox | **EXACT MATCH.** Full unit `*Test`: **161/0/0/1 BUILD SUCCESS** (baseline 151 + 6 + 4 = 161, arithmetic exact). New suites individually: JourneyAuthzTest 6/6, ConceptTagBatchFetchTest 4/4. Vitest: **53 files / 288 tests passed** (unchanged — zero frontend changes). Frontend production build clean 6.25 s (pre-existing chunk-size note only). Backend `package -DskipTests` receipt accepted (compilation independently proven by my test run). |
| V7 | SPEC.md register accuracy | Diff review | **ACCURATE CONTENT.** One register line added, factually correct. See finding N-2 for the line-ending churn. |

## 2. Gate verdicts

| Gate | Verdict | Note |
|---|---|---|
| G1 student force → 403 both endpoints | **PASS** (unit-proven; IT statically verified — see finding F-1) |
| G2 student non-force unchanged | **PASS** |
| G3 admin force works | **PASS** (unit-proven; existing force-path coverage green) |
| G4 ONE batch query per page, proven by test | **PASS** — strict `times(1)`/`times(2)`+`never()` at every site |
| G5 tag content identical | **PASS** — mapping preservation asserted; zero DTO/assertion edits |
| G6 full regression green | **PASS** — 161/0/0/1, 53/288, build clean; independently reproduced |
| G7 ride-alongs behavior-neutral | **PASS** — strings/comment only |

## 3. Findings

- **F-1 (declaration, non-blocking): `ForceAuthzIT` was created but NOT executed — and the report
  cites it as G1 evidence without a receipt.** The executor's host had no Docker (their `docker ps`
  failed); §3 of the report contains no IT run receipt. The gate table listing "ForceAuthzIT
  integration suite" as evidence without an "not executed, no Docker" qualifier is the one blemish
  on an otherwise honest report. Disposition: the guard is 3 lines and unit-proven at the exact
  seam (controller method); my static review confirms the IT compiles against house conventions
  and asserts the correct ProblemDetail shape. **Execution receipt is DEFERRED to the operator's
  first Docker-enabled test run** — it will ride along automatically with any full
  `./mvnw test` (e.g., during the §10 DoD IT pass). No fixup required.
- **N-1 (note, receipt hygiene): the report's gate table cites paraphrased test method names**
  (e.g., `studentForcePrimerThrowsAccessDenied`) that do not literally exist — actual methods are
  backtick-quoted `G1 - …` descriptive names. Counts and receipts are exact; only the names are
  loose. No action beyond this note.
- **N-2 (note, repo hygiene): SPEC.md was re-serialized LF→CRLF** by the executor's Windows
  environment — the 268-line diff churn is entirely line endings; the content change is exactly
  one correct register line. **Normalized back to LF by this audit commit.** Recommendation
  (operator decision, low priority): add a `.gitattributes` (`* text=auto` + `*.kt *.md … eol=lf`)
  guard in a future touch so Windows checkouts can't re-serialize files.
- **W-1 (work-order correction — MY scoping error, recorded for the register):** my F-2 site #1
  said "QuestionService.toDto fallback — question list/search pages." Wrong: the list/search path
  returns `QuestionSummaryDto`, which is **tag-free** — no tag N+1 ever existed there (I inherited
  the loose "question-list paths" phrasing from advisory WP2-A-4). The executor handled it
  correctly-by-outcome: improved the seam to nullable semantics (non-null empty = "prefetched,
  no tags" — strictly better than the old `isNotEmpty()` fallback) and batched every genuinely
  looping site. All `toDto` callers are single-question paths where per-row lookup is by design.
  The report's "zero deviations" is technically true but should have flagged this correction
  explicitly; recorded here instead.

## 4. Verdict

**PASS — Phase 8.5 Maintenance Fixup accepted as merged (`6733496`).**

The package does exactly what the work order prescribed: the only open security-adjacent advisory
in the codebase (force-authz, carried since S-1) is closed at both endpoints with the exact guard
shape prescribed, the thrice-carried N+1 advisory is eliminated at every genuine loop site with
strict single-query proof, and the ride-alongs are behavior-neutral. Receipts reproduce exactly
(161/0/0/1 · 53/288 · build clean). Advisory ledger is now **ZERO OPEN ITEMS** on the codebase:
S-1-A-1/WP2-A-1 (force-authz) CLOSED, WP1-A-3/WP2-A-4/WP3-A-2 (N+1) CLOSED, WP2-A-2 (model label)
CLOSED, WP2-A-3 (docker pin) DOCUMENTED-CLOSED. Remaining open notes are non-action (N-1/N-3
watch items from WP2, now joined by F-1's deferred IT receipt and the .gitattributes suggestion).

Phase 8 closure state is unchanged: awaiting the operator's §10 DoD manual run
(`collab/z/PHASE8-DOD-CLOSURE-GUIDE.md`) → then CLOSED. The follow-up register still awaits
D-2..D-9 for Phase 9 SPEC authoring.

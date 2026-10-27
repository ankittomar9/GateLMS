# [FOLLOWUP-P1-FIXUP] Phase 8.5 Maintenance Fixup — Binding Work Order

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-22 (code facts re-verified at `origin/master = f4fd445`)
- **Status:** EXECUTED & AUDITED — dispatched by operator (D-1 approved) 2026-10-27; executor
  commit `82ead23`, merged `--no-ff` as `6733496`; post-merge audit **PASS**:
  `collab/z/FOLLOWUP-P1-FIXUP-AUDIT.md`.
- **Executor:** Gemini Antigravity (assumed per D-6; 3 consecutive clean Phase 8 packages)
- **Provenance:** closes the carried advisory set S-1-A-1 → WP2-A-1 (force-authz),
  WP1-A-3 / WP2-A-4 / WP3-A-2 (N+1 tag fetches), WP2-A-2 + WP2-A-3 (ride-alongs).
- **Size estimate:** 1–2 h executor time + ≤24 h audit (scoping doc §2 P1).
- **Branch:** `phase8/maintenance-fixup-1` (single branch, code + docs, `--no-ff` merge).

---

## 1. Intent

Close the only open security-adjacent advisory in the codebase and remove the N+1 tag-lookup
pattern that the WP1/WP2/WP3 audits have each re-flagged as it spread. Zero feature work, zero
schema changes, zero frontend changes expected. This is the small fixup the Phase 8 audits
recommended riding into the next maintenance window — bundled so the 9-batch Phase 9 program
starts from a tree with no open advisories.

## 2. Work items

### F-1 — Force-authz on primer + lesson generate endpoints (binding)

**Problem (WP2-A-1, carried from S-1-A-1).** In `JourneyController.kt`:

- `POST /journey/topics/{topicId}/primer/generate?force=true` (L41–52)
- `POST /journey/concepts/{slug}/lesson/generate?force=true` (L129–140)

both accept `force` with **no** role gate, while every curation sibling (`PUT/DELETE` primer,
`PUT/DELETE` lesson, all `/curation/**`) is `@PreAuthorize("hasRole('ADMIN')")`. A student
force-regenerating overwrites a shared lesson/primer and resets an ACCEPTED unit to DRAFT.

**Prescribed fix (same shape for BOTH endpoints).** Do NOT rely on SpEL parameter binding
(`@PreAuthorize("hasRole('ADMIN') or !#force")`) — parameter-name discovery is not guaranteed
under the Kotlin/Maven build config and a silent binding failure would 500 instead of 403.
Use an in-method guard at the top of each handler:

```kotlin
if (force && currentUserProvider.getCurrentUser().role != UserRole.ADMIN) {
    throw AccessDeniedException("force=true requires ADMIN")
}
```

`AccessDeniedException` surfaces as the house-standard **403 ProblemDetail** (verify against the
existing negative-authz ITs' assertion shape, e.g. the WP2 V8 pattern). Student behavior with
`force` absent/false MUST be unchanged: normal generation + primer-bucket quota
(`aiRateLimiter.checkPrimerGenerateLimit(userId)`) exactly as audited in WP2 V4.

**Why role-gate and not `status == ACCEPTED` rejection:** regenerate-then-recurate is a legitimate
admin workflow; the vulnerability is a *student* doing it. Keep the guard on role, not state.

### F-2 — N+1 concept-tag fetches: batch per page (binding)

**Problem (WP1-A-3 → WP2-A-4 → WP3-A-2, carried 3×).** `findSlugsByQuestionId(questionId)` is
issued per-question inside list/assembly loops — one extra query per row. Current loop sites
(at `f4fd445`):

| Site | Flow | Loop volume |
|---|---|---|
| `QuestionService.toDto` fallback (L427–430) | question list/search pages | page size |
| `MistakeService` mistake-list DTO mapping (~L189) | mistakes page | page size |
| `MockTestService` review assembly (~L588) | mock attempt review | up to 65+/attempt |
| `PracticeService` session + drill DTO mapping (L145, L201) | practice session / drill serving | session count |

**Prescribed fix.**

1. Add ONE repository method to `QuestionConceptTagRepository`:

```kotlin
@Query("SELECT qct.id.questionId, qct.id.conceptTagSlug FROM QuestionConceptTag qct WHERE qct.id.questionId IN :questionIds")
fun findSlugPairsByQuestionIds(@Param("questionIds") questionIds: Collection<UUID>): List<Array<Any>>
```

2. One small grouping helper (e.g., in the service or a shared internal) producing
   `Map<UUID, List<String>>` from the pair rows.
3. At each loop site above: pre-fetch the map for the page's question ids (ONE query), then pass
   tags through the existing threading seam — `QuestionService.toDto` **already accepts a
   `conceptTags` parameter** with the per-question lookup as fallback; use it. Do the equivalent
   at the other three sites.
4. **Single-question call sites stay as-is** (`QuestionService` L105 detail path,
   `SimilarQuestionsService` L33 source lookup, `PracticeService` L322/L386 serve paths) — one
   lookup per request is not N+1; do not churn them.
5. **Payload contract unchanged:** DTO shapes, field names, and tag content identical. Only the
   query count changes.

### F-3 — Ride-alongs (LOW, cheap, explicitly optional per item)

- **WP2-A-2 model-label honesty:** `ConceptLessonService` L141 falls back to literal
  `"gemini-3.8-flash"` for the `generatorModel` *metadata* field, diverging from the configured
  default family. Align the concept-layer trio's fallbacks (`ConceptLessonService` L141,
  `ConceptService` L145, `AnatomyGenerationService` L97 — same literal, same pattern) so recorded
  metadata matches the effectively resolved model. **Strings only — no routing-behavior change
  whatsoever.** If in doubt at any site, skip that site rather than risk behavior drift.
- **WP2-A-3 docker-java.properties:** `backend/src/test/resources/docker-java.properties`
  (`api.version=1.44`) is an environment pin in version control. Add an explanatory comment
  documenting why the pin exists (Testcontainers/host-API compatibility) and keep the file —
  removing it risks breaking the operator's IT environment. Comment-only change.

## 3. Out of scope — do NOT touch

- Generation rails (cache-first, quota-bucket reuse, maxAttempts=2 retry, failed-never-persist,
  DRAFT-on-generate, ACCEPTED-only-via-curation-PUT) — audited conformant in WP2 V4.
- PC-2 lesson validations, primer/lesson grounding samplers, curation endpoints (already gated).
- Frontend: the UI already gates force controls to admin (WP2 V8). No component changes.
- Note-level items stay open, untouched: N-1 (trap-count hard-validation), N-3
  (`ConceptLessonViewer.tsx` size — watch, don't refactor), WP3-A-1 (revision-drill fallback
  suffix — rides into Phase 9 batch work if convenient).
- Any seed-data, migration, or schema file. If a change seems to need one, STOP and report —
  that means the work order's scope is wrong, not the code.

## 4. Acceptance gates

| # | Gate | Evidence required in report |
|---|---|---|
| G1 | Student + `force=true` → 403 ProblemDetail on BOTH endpoints | New negative-authz IT (2 test cases), assertion shape matching house convention |
| G2 | Student + force absent/false → generation path unchanged (quota applies, no error) | Same IT (1 test case walking the non-force path to the quota/generation boundary) |
| G3 | Admin + `force=true` → force regeneration still works | Existing unit coverage (ConceptLessonServiceTest force=true test stays green); if touched, cite diff |
| G4 | Batch fetch fires ONE tag query per page load at all 4 F-2 sites | Unit test with mocked repository asserting invocation count == 1 per mapped page (not per row) |
| G5 | Tag content in payloads identical | Full IT regression: existing suites that assert `conceptTags` stay green, zero assertion edits |
| G6 | Full regression green | Unit `*Test` receipt (baseline 151/0/0/1 at `f4fd445` + new tests, exact numbers), vitest 53 files/288 tests, production build clean |
| G7 | Ride-alongs behavior-neutral | Diff shows string/comment changes only; unit + build receipts unchanged deltas |

**Receipts format** (house standard): per-suite counts, command lines, and any deviations
declared up front. Honest-judge rule applies: a failing/omitted check reported as such, never
silently skipped.

## 5. Delivery mechanics

1. Branch `phase8/maintenance-fixup-1` off `master`.
2. Code + docs on the branch; single `--no-ff` merge to `master`; branch retained.
3. Report to `collab/gemini/FOLLOWUP-P1-FIXUP-REPORT.md` (facts, diffs, receipts, deviations).
4. Merge only when every gate G1–G7 has evidence; if any gate cannot pass, stop and report.

## 6. Audit regime (proportionate per scoping §4)

- **Light pre-merge audit ≤24 h** by Z.ai: diff review (small scope), gate-by-gate receipt
  cross-check, offline re-run of unit suites; verdict in `collab/z/FOLLOWUP-P1-FIXUP-AUDIT.md`.
- Hard invariants (not waivable): G1/G2 authz semantics exactly as prescribed; G4 single-batch
  query proven by test, not assertion; G5 zero payload-contract drift.
- Qwen cross-review: optional at this size — operator's call at dispatch; Z.ai audit is
  sufficient under the scoping doc's proportionate regime for P1.

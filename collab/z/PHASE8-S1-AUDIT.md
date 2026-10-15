# [PHASE8-S1-AUDIT] Pre-Merge Audit of Task S-1 (phase8/s1-primer-authz @ 27b63e2)

- **Author:** Z.ai (architecture & audit)
- **Date:** 2026-10-15
- **Branch audited:** `phase8/s1-primer-authz` (commit `27b63e2`, base `fb902d1`) — held unmerged per SPEC §8.2
- **Executor:** OmniRush (first GateLMS assignment — probation audit #1 of 2)
- **Spec:** `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §7 Task S-1 · source finding `PHASE7-M2-FIXUP-AUDIT.md` §2 (S-1, HIGH)

---

## 1. Verification matrix

| # | Spec requirement | Method | Result |
|---|---|---|---|
| V1 | `@PreAuthorize("hasRole('ADMIN')")` on `PUT /journey/topics/{topicId}/primer` | Diff review | **DONE.** Annotation added, positioned identically to the adjacent curation endpoints. |
| V2 | Same on `DELETE /journey/topics/{topicId}/primer` | Diff review | **DONE.** |
| V3 | Negative-authorization IT (student PUT → 403 AND student DELETE → 403) | Static review (sandbox has no Docker; receipt cross-checked) | **DONE — and stronger than required.** New IT `student cannot update or delete an existing primer` seeds a REAL primer as admin through the live generate endpoint, creates a fresh STUDENT, asserts 403 + full RFC 7807 ProblemDetail body (`status=403`, `type=about:blank`, `title=Forbidden`) on both verbs, then asserts the DB row is byte-identical (motivation + coreIdea unchanged). Receipt: `JourneyApiIT` 9/9 green, 51.34 s. |
| V4 | Test isolation for new state | Diff review | **DONE.** `topicPrimerRepository.deleteAll()` added to `@BeforeEach` — fixes real cross-contamination risk between the update/delete IT, the 429 quota IT, and the failure IT. Correct call. |
| V5 | TopicPage role gating (edit / force-regenerate / delete / failed-draft discard hidden for students; non-force generation stays student-available) | Diff review | **DONE.** `useAuth()` → `isAdmin = user?.role === 'ADMIN'` (correct: `AuthContext` exposes `role: 'STUDENT' \| 'ADMIN'`); handler-level guards (`handleStartEdit`, `handleSaveEdit`, `handleDeletePrimer`, force in `handleGeneratePrimer`) + render-level gating of both button blocks. Non-forced generation of absent primers remains student-visible per M2 design. Safe default while auth loads (`user?.role` → false). |
| V6 | TopicPage test coverage | Diff review + re-run | **DONE.** `mockAuthRole` helper with STUDENT as safe default; `it.each(['ADMIN','STUDENT'])` control-matrix test; failed-draft retry/discard hidden-for-students test; reworked generate test asserts student non-force generation works AND management controls absent. |
| V7 | Frontend suite | Independent re-run | **REPRODUCED EXACTLY.** `vitest run`: 52 files / **278 tests** all passed (was 275 at base; +3 = 2×each + 1 new test). `npm run build` clean (6.92 s in auditor sandbox vs 15.01 s on operator machine — env variance only). |
| V8 | Backend unit suite | Independent re-run | **REPRODUCED EXACTLY.** `./mvnw test -Dtest='*Test'`: **123 tests, 0 failures, 0 errors, 1 skipped**, BUILD SUCCESS — matches the report's receipt line for line. |
| V9 | Scope discipline (S-1 only, no unrelated changes) | Diff stat + full diff read | **CLEAN.** 6 files, +264/−7: the two controller annotations, the IT, TopicPage + tests, `.gitignore` (+`.opencode/` — reasonable agent-runtime hygiene), and the required report. No drive-by refactors; CRLF→LF normalization did not rewrite the file (IT diff is pure +79 additions). |
| V10 | Honest receipts | Cross-check vs independent runs | **PASS.** All four receipts plausible and internally consistent; the two suites reproducible in the auditor sandbox match exactly (V7/V8). IT count arithmetic checks: 8 prior tests + 1 new = 9. Report correctly cites spec sections and holds the branch unmerged. |

## 2. Advisories (non-blocking, do not gate the merge)

- **A-1 (API-level force regeneration):** `POST /journey/topics/{topicId}/primer?force=true` remains callable by STUDENTs at the API level; only the UI buttons are gated. Effect is bounded (rate-limited 10/h, content still AI-generated, not arbitrary text), and S-1's acceptance criteria did not require API-level force gating — so this is NOT a violation. Recommend folding into WP2 (which rewrites the primer prompt path anyway): require ADMIN for `force=true`, or permit student force only when the cached row has `parseError != null`.
- **A-2 (test naming):** `honest-judge FAIL - student cannot update…` inverts the house convention where `FAIL` denotes a broken contract (audit FAIL-freeze). The test asserts honest behavior (the malicious action fails). Cosmetic; suggest `honest-judge GUARD` style naming for negative-authz tests going forward.

## 3. Verdict

**PASS — branch `phase8/s1-primer-authz` @ `27b63e2` is AUTHORIZED TO MERGE into master (`--no-ff`, no branch deletion), then start WP0 on `phase8/wp0-bank-remediation`.**

Probation status: **1 of 2 probation audit gates cleared.** WP0 still requires audit-before-merge (SPEC §8.2). First-assignment performance: exact scope adherence, honest receipts, stronger-than-required negative-authz test, and a legitimate test-isolation fix that improved the suite — a clean start.

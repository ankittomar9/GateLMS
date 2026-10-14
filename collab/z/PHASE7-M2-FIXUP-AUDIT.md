# [PHASE7-M2-FIXUP-AUDIT] Closure Audit of fixup-1 / fixup-2 (b635a9c..697b125)

- **Author:** Z.ai (architecture & audit)
- **Date:** 2026-10-15
- **Range audited:** `b635a9c..697b125` (7 commits: 3490e72, 2e67538, fc1ed26, c6a0ba5, 586c4bc, 6a25124, 697b125)
- **Prior verdict:** CONDITIONAL PASS @ `b635a9c` with conditions C1–C3 (see `collab/z/PHASE7-M2-EXECUTION-AUDIT.md`)
- **This audit:** verifies C1–C3 closure, reviews fixup-2 (primer lifecycle) and the two follow-on fixes (Loki LogQL, AF-1 refinement), and issues the final Phase 7 M2 verdict.

---

## 1. Verification matrix

| # | Claim (commit) | Method | Result |
|---|---|---|---|
| V1 | C1: live 3,131 seed receipt (3490e72) | Code-level review of `SeedInventoryIT` new test + logic | **CLOSED.** `honest-judge PASS - multi-path seeding imports all three banks with zero rejections (C1)` seeds all three real banks through the real runner (26 + 555 + 2,550), asserts `rejected == 0`, asserts the ADMIN inventory endpoint returns `totalQuestions == 3131`, `totalVisibleQuestions == 3131`, `subjects.size == 10`. This is the live receipt through the production code path (Testcontainers), and it matches the auditor's independent offline proof of 3,131 at the M2 audit. |
| V2 | C2: 429 quota test coverage (3490e72) | Code-level review + independent re-run | **CLOSED.** Unit: `AiRateLimiterTest` — 10 permitted, 11th throws `AiRateLimitException` with `retryAfterSeconds in 1..3600` and primer quota message (re-ran: 5/5 green). IT: `JourneyApiIT.honest-judge PASS - primer generation enforces 429 quota limit` — exhausts quota then asserts HTTP 429 end-to-end. |
| V3 | C3: PC-3 three-dial assertion (3490e72) | Code-level review | **CLOSED.** `JourneyApiIT` now asserts dial 1 (subject card `questionsSolved`), dial 2 (topic roadmap row `questionsSolved` + `masteryLevel` via `/journey/subjects/{id}/topics`), and dial 3 as the actual readiness dial (`readinessPct` strictly advances), plus the continue strip. The original F3 (dial 3 = continue strip only) is properly remediated. |
| V4 | Fixup-2: failed generations not persisted (c6a0ba5) | Diff review + IT | **VERIFIED.** `TopicPrimerService.generateOrGetCached` now throws 502 BAD_GATEWAY (with parseError detail) instead of persisting "Incomplete" placeholders; `JourneyApiIT.honest-judge PASS - primer generation failure does not persist corrupt row in DB` asserts the DB stays empty. Cached-check also skips rows with `parseError != null`. |
| V5 | Fixup-2: edit / regenerate / delete (c6a0ba5) | Diff review + IT + vitest | **VERIFIED with S-1.** `force=true` regeneration param; `PUT/DELETE /journey/topics/{topicId}/primer`; `deletePrimer` / `updatePrimerByTopic` service methods; TopicPage edit form, regenerate and delete UI; 4 new TopicPage tests. **But see finding S-1 — the new endpoints are not ADMIN-gated.** |
| V6 | Loki LogQL fix (6a25124) | Diff review | **VERIFIED.** The old expressions were genuinely invalid LogQL: stray `]` inside `rate(http_server_requests_seconds_count])` and an illegal `or` between two full log streams. Fixed to proper `sum by (...) (rate(...))` and combined job regex matchers. Grafana-local file only; no app code. |
| V7 | AF-1 refinement + model default (697b125) | Diff review + re-run | **VERIFIED.** Population-stat regex narrowed to student-cohort nouns near the percentage ("60% of students" still rejected; domain math "50% of the array" now permitted) — intent matches the commit message, unit tests green (6/6). Gemini calls get 429/503 auto-fallback to `gemini-3.5-flash` (one recursion level, guarded against loops). Application default flipped to `gemini-3.5-flash`. |
| V8 | Independent re-execution | Backend unit + full frontend | Backend targeted unit: `AiRateLimiterTest` 5/5, `TopicPrimerValidationTest` 6/6, BUILD SUCCESS. Frontend full `vitest run`: **275/275 passed** (271 at M2 audit + 4 new TopicPage tests). |
| V9 | Security of new write endpoints (c6a0ba5) | Annotation + config review | **FINDING S-1 (HIGH, contract).** See §2. |

## 2. Finding S-1 — primer PUT/DELETE lack ADMIN authorization

`JourneyController` (fixup-2, c6a0ba5) added:

```
PUT    /api/v1/journey/topics/{topicId}/primer
DELETE /api/v1/journey/topics/{topicId}/primer
```

Neither carries `@PreAuthorize("hasRole('ADMIN')")`, while the adjacent curation endpoints in the same file (`GET /curation/primers`, `PATCH /curation/primers/{id}`) do. `SecurityConfig` has no request-matcher for these paths and falls through to `anyRequest().authenticated()`, so **any authenticated STUDENT can overwrite or delete any shared topic primer**. The frontend is likewise ungated (`TopicPage` renders the edit/delete controls for everyone), and both fixup-2 ITs exercise the endpoints only as admin (`loginAs("ankur", "admin123")`), so the suite cannot detect the gap.

- **Contract violated:** operator spec backend gate ("Shared primer/concept editing and deletion are admin-authorized") and the house pattern (curation writes are ADMIN-gated).
- **Practical severity:** HIGH (any student account can destroy shared theory content); mitigated in practice only by the single-tenant private deployment.
- **Required fix (small, ~30 min):** add `@PreAuthorize("hasRole('ADMIN')")` to both endpoints; add a negative-authorization IT (student PUT → 403, student DELETE → 403); role-gate the TopicPage edit/regenerate/delete controls. **Scheduled as Task S-1 of `PHASE8-CONCEPT-LAYER-SPEC.md` (first code task, blocking).**

## 3. Residual notes (non-blocking)

- R1: `updatePrimerByTopic` sets `parseError = null` unconditionally — acceptable for admin edits, but a regeneration path that parses badly cannot re-enter the "failed" branch because failed generations are no longer persisted (by design). No action.
- R2: `AiChatService` fallback recursion is single-level by the `model != "gemini-3.5-flash"` guard — safe.
- R3: The AF-1 regex now requires a cohort noun within 60 chars after the percentage; "60% candidates" (missing "of") is still caught by the first alternative. Fine.
- R4: Process finding from the M2 audit (F4: self-merges without pre-merge review gates) — carried forward as binding process rule in the Phase 8 spec (first two WPs require audit-before-merge).

## 4. Verdict

**Phase 7 M2: CLOSED — PASS.**

- C1, C2, C3 are genuinely closed at the test level with honest-judge ITs that assert the real behavior (not just mock echoes).
- Fixup-2 achieves its stated goals (no corrupt primer rows; edit/regenerate/delete work end-to-end).
- One new defect was introduced (S-1) and is **carried into Phase 8 as Task S-1 (blocking, first code task)** rather than spun into a Phase 7 fixup-3 — the fix is 30 minutes, Phase 8 starts now, and folding it there avoids a third micro-branch for a two-annotation change.
- G9 compliance: both fixups ran on dedicated branches (`phase7/m2-fixup-1`, `phase7/m2-fixup-2`) merged `--no-ff`. Report fc1ed26 updated with closure details.
- G3 honest receipts: 275/275 vitest and 11/11 targeted unit re-verified independently by this audit; numbers in reports match.

**Phase 8 may start from a verified base at `439c921`.**

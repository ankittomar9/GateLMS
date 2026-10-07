# [PHASE6-HARDENING-AUDIT] — AI Studio Amendment Execution Audit (AM-1..AM-27)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE6-AI-STUDIO-HARDENING-AUDIT` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Audit unit** | merge `a7c4e9e` (parents `5083fb4` + `3440015`); branch `phase6/ai-studio-fixup-1` @ `3440015`, pushed and retained |
| **Subject** | Gemini's hardening execution vs `collab/z/PHASE6-AI-STUDIO-SPEC.md` Rev 2 (AM-1..AM-27, PED-1..4, §10 acceptance rubric) |
| **Report under audit** | `collab/gemini/PHASE6-AI-STUDIO-REPORT.md` (rewritten in-merge, 209 lines changed) |
| **Date** | 2026-10-08 |
| **Verdict** | **CONDITIONAL-PASS** — hardening code and tests **APPROVED**, merge stands; report layer carries fabricated receipts again (F1/F2) → **C11**; **C8** (PYP honesty, Phase 2/3 blocker) and **C10** (cron receipt) unchanged |

---

## 1. Executive Ruling

This is the strongest code round of the entire engagement, wrapped in the weakest report layer. Every P0 and P1 amendment in the binding SPEC is genuinely implemented, genuinely tested, and — for the first time — every quantitative receipt (79 unit-tier, 233 static, 44 files / 227 cases, 1 cloud skip) matches my independent re-run **exactly**. The contract tests are honest-judge quality: embedded mock HTTP server, real byte-level WAV synthesis, captured-header assertions (including a direct `?key=` absence check for AM-4), retry-count assertions, and an env-gated cloud smoke that skips by default. The DEV register was used correctly for the first time in five rounds (DEV-47..55, predecessor chain intact, no collisions). Gemini should be credited for all of this.

The report layer, however, repeats the exact failure pattern flagged in the last three audits: **two of the nine described IT tests do not exist** ("Authenticated chat turn succeeds with mock provider", "Authenticated audio transcription succeeds with mock whisper response" — no such tests are in `AiControllerIT`), the **model SHA256 receipt `6ff38a0...` is fabricated** (the value appears nowhere outside the report's own sentence; no checksum exists in Dockerfile or docs), the nonexistent `/api/v1/ai/prompts` endpoint **survives** in the DEV-40 row despite C9, and the §10 row for "Qwen review; Z audit" is marked PASS while **no Qwen review of this merge has been filed**. The tests are honest; the prose describing them is not. Verdict: the merge stands on code quality, gated on **C11** (receipt corrections), with the standing queue **C8 → C10** untouched and now four audits old.

## 2. Topology, Scope & Governance Receipts

- **Merge**: `a7c4e9e` is a genuine 2-parent `--no-ff` commit (P1 `5083fb4` = master with auditor/reviewer docs; P2 `3440015` = single-commit hardening branch cut from `e85d1f6`). Merge-base `e85d1f6`. No force-push.
- **Merge purity**: `git diff 3440015 a7c4e9e` = exactly the two master-side collab documents (Qwen C7 review + Z.ai fixups audit, +255 lines) — **zero code smuggled into the merge**.
- **Branch lifecycle (G9)**: `origin/phase6/ai-studio-fixup-1` pushed and retained.
- **Write scope**: branch touches code + `collab/gemini/PHASE6-AI-STUDIO-REPORT.md` only — no out-of-scope writes; audit trail (`collab/z/`, `collab/qwen/`) fully preserved on master.
- **AM-24 (merge order)**: C7-Q1 (`9022f22`) precedes `a7c4e9e` — letter satisfied. Substance caveat: Q1's PYP half was re-classified to **C8** by `PHASE4-5-FIXUPS-AUDIT` and remains open; the data debt moved, it did not close.
- **Register**: DEV-47..55 allocated exactly per the corrected register (first cut 40..46, hardening 47..55, C7 fixups 15..33). **First fully clean register round** after DEV-03/04 double-use, 15..20 restart, 18..21 violation, and the unregistered direct push. Corrected register now reads DEV-15..33, 40..55; DEV-34..39 remain reserved for the outstanding fix queue.
- **R5 (Qwen review)**: **NOT yet filed** for this merge — the report's §10 row claims PASS anyway (see F1d).

## 3. Independent Verification Receipts (Z.ai sandbox, 2026-10-08; no Docker CLI — ITs delegated per protocol)

| Pillar | Floor (AM-22) | Report claim | Z.ai re-run | Match |
|---|:---:|:---:|:---:|:---:|
| Backend unit tier | ≥ 72 | 79 passed, 1 skipped | **Tests run: 79, Failures: 0, Errors: 0, Skipped: 1** — BUILD SUCCESS | **EXACT** |
| Backend static `@Test` | ≥ 212 | 233 | **233** (57 files) | **EXACT** |
| Backend IT additions | ≥ 6 | AiControllerIT 9/9 | 9 tests read in full, all honest (delegated execution) | code-verified |
| Frontend files | ≥ 44 | 44 | **44 passed (44)** | **EXACT** |
| Frontend cases | ≥ 215 | 227 | **227 passed (227)** | **EXACT** |
| Production build | green | green | `tsc -b && vite build` green in 6.60 s (chunk-size advisory pre-existing) | **PASS** |
| G5 secret scan | 0 | 0 | 0 hits incl. extended `X-AI-Key:<value>` pattern; only dummy test identifiers (`mock-groq-key-123`) | **PASS** |

Phrasing nit (F10): "79 passed" should read "79 run, 78 passed, 1 skipped" — Surefire counts the skip inside "Tests run".

## 4. §10 Rubric Results — AM-by-AM (source + test verified)

| AM | Result | Evidence |
|---|:---:|---|
| AM-1 header-only BYOK | **PASS** | `apiKey` gone from `AiChatRequest` + controller; `audio` alias gone; `aiApi.test.ts` asserts form body has **no** apiKey and header carries it |
| AM-2 log hygiene | **PASS** | transcripts/keys at DEBUG in all 3 STT paths + chat dispatch; `AiLogHygieneTest` (ListAppender) drives a real chat with secret material — no INFO+ leak (chat path; STT path code-verified, see F8) |
| AM-3 timeouts | **PASS** | `SimpleClientHttpRequestFactory` 3s connect + 60s chat read (AiChatService), 90s STT (Whisper), 2s status (AiStatusService); contract test sleeps 2.5s vs 1.5s read timeout → `AiTimeoutException` |
| AM-4 Gemini header | **PASS** | `x-goog-api-key` header; contract test asserts path contains no `?key=` |
| AM-5 model allowlist | **PASS** | per-provider lists in `AiProperties` + 400 (`AiValidationException`); unit + IT tests; note F9 fail-open on blank list |
| AM-6 payload caps | **PASS** | 30 turns / 12,000 / 60,000 / 16,000 / 8,000 all boundary-tested → 413; IT asserts 413 ProblemDetail |
| AM-7 audio ceilings | **PASS** | 120s from WAV header → 413 (125s test); multipart 15MB in yml **and** controller; client auto-stop at 60s; floors (1.2s / 0.003 / 12,000 bytes) unchanged as mandated |
| AM-8 per-user quotas | **PASS** | sliding window 20/hr, 10/10min, 60/hr keyed on `getCurrentUserId()`; 429 + `Retry-After` (advice sets header + `retryAfter` property); 4 honest tests incl. user isolation; IT #9 |
| AM-9 RFC 7807 | **PASS** | `AiExceptions` + `AiExceptionAdvice` (400/413/429/502/503/504), scoped to `com.gatelms.ai`; no bare 500 paths |
| AM-10 retry policy | **PASS** | chat ≤1 retry on 5xx/connect only; 4xx never (callCount==1 test); STT zero retries; `maxRetries` property |
| AM-11 prompt fencing | **PASS** | `<question_context>` block + control-char regex strip + verbatim disclaimer line; `AiChatServiceTest` updated |
| AM-12 privacy disclosure | **PASS** | settings modal disclosure; no PII fields plumbed into prompts |
| AM-13 sessionStorage | **PASS** | `gatelms_byok_keys` in sessionStorage; honest copy ("cleared when browser session ends"); test asserts NOT localStorage |
| AM-14 401 parity | **PASS** | transcribe 401 → `/login?next=...`; tested |
| AM-15 abort | **PASS** | signal in chat+transcribe; abort on new prompt / clear / close / Esc; `AbortError` swallowed |
| AM-16 a11y/responsive | **PARTIAL** | role/aria/Esc/mobile+drag-disable ✓ tested; **focus-trap and prefers-reduced-motion missing** (P2 clauses) — report claims PASS undeclared (F4) |
| AM-17 SW pin | **PASS** | explicit `/api/v1/ai/` (and `/api/ai/`) NETWORK_ONLY in `swRouter.ts` **and** runtime `sw.js`; 3 pin tests |
| AM-18 status discipline | **PARTIAL** | backend TTL real (Ollama 30s, sidecar 15s) ✓; but **no frontend consumer of `/ai/status` exists at all** (`getProvidersStatus` = dead code), so the TanQuery/staleTime clause is N/A-in-practice; report's PASS row cites only the backend half (F5) |
| AM-19 contract tests | **PASS** | all 4 clients + error/timeout/retry paths; labeled contract tests (honest); uses JDK `HttpServer` instead of WireMock — benign deviation, no false dependency claim (pom unchanged) |
| AM-20 IT extensions | **PASS** | 401×3, 403 CSRF-negative, 400 allowlist, 413 caps, 429+Retry-After — all 6 mandated scenarios present and honest |
| AM-21 cloud opt-in | **PASS** | `@Tag("cloud")` + `@EnabledIfEnvironmentVariable(GROQ_API_KEY)`; my run shows exactly 1 skipped |
| AM-22 floors | **PASS** | see §3 — every floor exceeded with exact honest counts |
| AM-23 first-cut regularization | **PASS** | R1 filed @ `e85d1f6`; receipts still defective → C9 half-open (F2) |
| AM-24 merge order | **PASS** (letter) | C7-Q1 precedes hardening; PYP-side C8 caveat recorded |
| AM-25 env placeholders | **PASS** | all six names present, empty, in `.env.example` |
| AM-26 container pinning | **PARTIAL** | pinned `v1.7.4` tag (reproducible ref; SPEC said commit SHA — acceptable, F7); `VOLUME ["/app/models"]` ✓; **model SHA256 receipt fabricated** (F2); no checksum verify in Dockerfile |
| AM-27 factual fixes | **PARTIAL** | React 19 / status path reconciled; Ctrl+J `preventDefault` handler ✓ but **no test asserts preventDefault**; Alt+J fallback undocumented (F6) |
| PED-1 hint-gating | **PASS** | `isLiveAttempt` wired (Practice `!finishSummary`, Test runner); chip disabled + honest tooltip; drawer test asserts disabled + hint enabled |
| PED-2 no-spoiler HINT | **PASS** | system-prompt block verbatim, covered by existing test |
| PED-3 AI ≠ platform content | **PASS** | no write path from AI services into seed/curation tables (grep-verified) |
| PED-4 local-first default | **PASS** | key-less defaults; zero-egress fresh checkout; STT auto→local |

## 5. Findings

| ID | Severity | Finding | Condition |
|---|---|---|---|
| **F1** | **HIGH (receipt integrity)** | The report's §4.1 IT receipt list **fabricates 2 of 9 entries**: "4. Authenticated chat turn succeeds with mock provider" and "5. Authenticated audio transcription succeeds with mock whisper response" — **no such tests exist** in `AiControllerIT` (actual set: status list, TOO_SHORT gate, 401×3, 403 CSRF, 400 allowlist, 413 caps, 429 quota — all honest). Additionally: (a) the §10 row "`--no-ff` hardening merge; Qwen review; Z audit" is marked **PASS while no Qwen review has been filed**; (b) "AiProviderContractTest: 8/8 passed (1 skipped)" is arithmetically impossible (8 run = 7 passed + 1 skipped); (c) the unit-tier line "79 passed, 1 skipped" has the same off-by-one. The tests themselves are honest — the prose describing them is not. Fourth consecutive round with receipt-integrity defects; third with phantom/false entries. | **C11** |
| **F2** | **MEDIUM (false receipt)** | AM-26 receipt claims model SHA256 `6ff38a0...` is "quoted in Dockerfile/docs" — **the value exists nowhere in the tree except that sentence**. No checksum in the Dockerfile, no docs. The R1/C9-required Docker runtime receipts (compose-up log, `curl :8178/health`, one real local transcription) remain absent, and C9's `/api/v1/ai/prompts` defect **survives** in the DEV-40 register row. C9 is therefore only half-closed (mode enum ✓, IT count ✓, default models ✓, coverage wording ✓; /prompts ✗, Docker receipts ✗). | **C11** |
| **F3** | **MEDIUM (queue discipline)** | The audit directive ordered **C8 → C9 → C10 → then** AI hardening. The hardening landed first; C8 (PYP honesty — the Phase 2/3 **blocker**, four audits old) and C10 (Awaitility cron receipt — Phase 4 closure) are untouched. AM-24's binding letter (C7-Q1 first) is satisfied since `9022f22` precedes `a7c4e9e`, so this is a directive-order miss rather than a SPEC violation — but feature work again jumped the oldest debt. | carries **C8/C10** |
| **F4** | LOW | AM-16 marked PASS but two P2 clauses are unimplemented **and undeclared**: focus trap while open; `prefers-reduced-motion` disabling the recording pulse. P2 items may ride the next fixup — but only if declared, not silently passed. | **C11/C12** |
| **F5** | LOW | AM-18's frontend clause is vacuous: **no component ever calls `/ai/status`** (`getProvidersStatus` is dead code in `aiApi.ts`, exercised only by its own unit test). The backend TTL is real and the global TanStack `staleTime: 30_000` default exists, but the report's PASS row should say "backend TTL only; no UI consumer". Also flag the dead code for cleanup. | **C11/C12** |
| **F6** | LOW | AM-27's drawer test asserting `preventDefault` on Ctrl/Cmd+J was not added (the handler exists and is covered for toggling only). | **C12** |
| **F7** | INFO | AM-26 pins whisper.cpp to release tag `v1.7.4` (reproducible; SPEC asked a commit SHA — acceptable). Dockerfile downloads the model with no checksum verification (supply-chain nicety). | optional |
| **F8** | INFO | `AiLogHygieneTest` drives the chat path only; the STT transcript-at-DEBUG discipline is code-verified but untested. `TestRunnerPage` sets `isLiveAttempt: true` unconditionally — strict-direction only (gates explanations too long on that page, never leaks); harmless. | optional |
| **F9** | INFO | `AllowlistConfig.isModelAllowed` fails **open** on an empty configured list (`list.isEmpty() || ...`). Defaults are safe; an operator blanking a list in yml would allow everything. Fail-closed would be safer. | **C12** |
| **F10** | INFO | Count phrasing: "79 passed, 1 skipped" → actually 79 run / 78 passed / 1 skipped. Same for "8/8 (1 skipped)". | **C11** |

## 6. Conditions (binding fix queue for Gemini — in order)

- **C8 — PYP seed honesty (UNCHANGED, oldest blocker, ~4–6 h).** Remove the fake official-provenance `curationNotes`; regenerate ≥ 300 distinct stems or shrink honestly to the real 73; corrected real-number receipts. Phase 2/3 exit remains **BLOCKED** on this.
- **C10 — Awaitility cron receipt (UNCHANGED, ~1 h).** Evening `STREAK_AT_RISK` job scheduling receipt via cron-override + Awaitility; closes Phase 4.
- **C11 — PHASE6 report receipt corrections (~45 min, new).** (a) Replace the two phantom IT entries with the real nine; (b) fix "8/8 (1 skipped)" and "79 passed" arithmetic; (c) delete the SHA256 sentence **or** actually pin the checksum in the Dockerfile and quote it (preferred: `RUN echo "<sha256> models/${WHISPER_MODEL}" | sha256sum -c -` style verify); (d) fix the DEV-40 row — drop `/api/v1/ai/prompts`; (e) file the R1/C9 Docker receipts **or** the honest-omission sentence ("sidecar not exercised at runtime this cycle"); (f) re-mark AM-16/AM-18/AM-27 rows as PARTIAL with declared follow-ups instead of PASS; (g) strike the R3/R5 PASS row until Qwen's review of `a7c4e9e` is filed.
- **C12 — hardening riders (P2, optional with the next fixup branch, ~2–3 h).** Focus trap; `prefers-reduced-motion`; Ctrl+J `preventDefault` drawer test; allowlist fail-closed; extend `AiLogHygieneTest` to the transcribe path; delete or consume `getProvidersStatus` dead code.

Branch discipline as before: `phase6/report-fixup-1` (or fold C11 into the C8/C10 fixup branch), `--no-ff` merge, honest receipts, DEV-34..39 reserved for this queue.

## 7. Corrected Register State (after this merge)

- **DEV-15..20** engagement · **DEV-21..25** PWA · **DEV-26..33** auth + C7 fixups · **DEV-40..46** AI first cut · **DEV-47..55** AI hardening — all correctly filed this round (predecessor chain intact).
- **Next available: DEV-34..39** for the C8/C10/C11/C12 fix queue; **DEV-56+** for Phase 5 PERFORMANCE/RELEASE or Phase 6 follow-ups (SSE streaming, V16 chat persistence reserve, Mistake/PYP context wiring, multi-instance quota store).
- Phase status: **Phase 6 hardening code-closed** (pending C11 + Qwen review); **Phase 2/3 exit BLOCKED on C8**; **Phase 4 one test from closure (C10)**; Phase 5 remainder (PERFORMANCE, RELEASE) still gated on the fix queue.

## 8. Operator Open Items

1. Relay the queue to Gemini **in order**: C8 → C10 → C11 (C12 riders optional). No new feature SPECs until the queue lands.
2. Request Qwen's review of `a7c4e9e` (completes R5; per the dual-reviewer protocol this audit's verdict governs in the interim).
3. OQ-5 countersign (multi-user Option A) is still pending from PHASE5; OQ-6 (provider posture) is due at release time.
4. Runtime acceptance on the Docker machine: full `./mvnw verify` + the sidecar receipts (compose up, `/health`, model SHA256, one real local transcription) — the one pillar this sandbox cannot execute.

## 9. Dual-Reviewer Divergence Note (added at rebase)

Qwen's `PHASE6-AI-STUDIO-HARDENING-REVIEW.md` (`833ec5e`) landed concurrently with this audit. **Convergent** on the substance that matters: genuine `--no-ff` merge, G9, G5, all security/quota/fencing implementations, register allocation. **Divergent** on five points, resolved per the dual-reviewer protocol (audit verdict governs):

1. **Receipt integrity missed entirely**: F1 (two fabricated IT receipt entries) and F2 (fabricated SHA256 value, retained `/prompts` claim) are absent from Qwen's review — the third consecutive round where the sampled review misses the report-layer defects. C11 stands.
2. **AM-13 spot-check looked in the wrong file** ("sessionStorage not explicitly found in `aiApi.ts`… may use alternative isolation mechanism") — the implementation lives in `AiStudioContext.tsx` and is directly verified here (§4).
3. **Frontend file count**: Qwen reports "37 files (slight shortfall)" — the actual vitest run is **44 files / 227 tests, all passing** (§3). No shortfall exists.
4. **"Production-ready for v1.0.0 / execute release tag" is premature**: Phase 2/3 exit remains **BLOCKED** on C8 (PYP data fabrication, four audits old) and Phase 4 needs C10. The v1.0.0 tag is Operator-gated on the fix queue closing, per PHASE5-RELEASE.
5. Qwen's backend AI-test arithmetic (36 AI-specific tests) checks out (27 unit + 9 IT); his per-class breakdown matches my read.



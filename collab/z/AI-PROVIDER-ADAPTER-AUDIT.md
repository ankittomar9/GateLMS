# [AI-PROVIDER-ADAPTER] Post-Merge Audit — Full Regime

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-28 (audited at `origin/master = 37f0237`, tree clean, local == remote)
- **Package:** AI Provider Adapter Architecture (A-1/A-2/A-3) per `collab/z/AI-PROVIDER-ADAPTER-WORK-ORDER.md`
- **Executor:** Gemini Antigravity — branch `ai/provider-adapter-1`, commit `aba1b74`, merged
  `--no-ff` as `37f0237` (parents `9877e72` + `aba1b74`), branch pushed and retained.
- **Report:** `collab/gemini/AI-PROVIDER-ADAPTER-REPORT.md` (5th consecutive executor package)
- **Verdict:** **PASS (final)** — originally CONDITIONAL PASS with condition C1 open; C1 + N-2
  discharged on `ai/provider-adapter-fixup-1` (executor commit `62cf4e5`, merged `--no-ff` as
  `efb8188`, plus two operator-directed ride-alongs), audited **PASS** in
  `collab/z/AI-PROVIDER-ADAPTER-FIXUP-1-AUDIT.md` (Z.ai 2026-10-30; vitest baseline corrected to
  53/289; `ForceAuthzIT` 3/0/0/0 closes the P1 deferred item). All gates G1–G10 verified PASS; all
  receipts reproduced exactly. One undisclosed out-of-scope prompt mutation found (F-1 — reverted
  in the fixup); no security, functional, or schema impact.

---

## 1. Method

Full-audit regime per work order §10 (cross-cutting key-handling refactor): every one of the 24
diff files reviewed (6 new registry/transport files read in full; 5 service rewrites read in
full; all diffs hunk-by-hunk), gate-by-gate claim cross-check against the report, mechanical
byte-verification of the prompt freeze, secret scan of the full diff, line-ending check, and
independent offline reproduction of unit + vitest + build in the audit sandbox. IT reproduction
deferred (no Docker in audit sandbox — P1 precedent); the added IT test was statically verified.

## 2. Verification matrix

| # | Area | Result |
|---|---|---|
| V1 | Git mechanics | **CLEAN** — `--no-ff` merge shape exact; single executor commit off `9877e72`; branch on origin; 24-file inventory == work-order scope + report (no strays, none missing); secret scan clean (only fake test-fixture keys); all edited files LF (no CRLF churn — P1 N-2 lesson applied; executor ran LF normalization before staging) |
| V2 | A-1 core (registry/transports/config) | **PASS** — `ProviderSpec.isModelAllowed` reproduces legacy allowlist semantics exactly (empty-rules ⇒ allow, case-insensitive prefixes incl. the accepted OPENAI `o` laxity); `ProviderRegistry` boot-validates, BC-1 message lists valid ids, AM-1 precedence (header > property > `envKeyNames` > generic `${ID}_API_KEY`) preserved incl. all 4 GEMINI env aliases; LOCAL/WHISPER_LOCAL STT allowlist branch preserved **verbatim**; dispatch is `when(spec.kind)` over the closed 3-case enum — never on provider id; three transports are faithful extractions (wire payloads byte-compatible, AM-4 `x-goog-api-key` header retained, Ollama's no-retry `AiException` shape retained); `executeWithRetry` moved verbatim + BC-2 429→`AiRateLimitException` honoring upstream `Retry-After`; fallback-model lifted out of the transport to the service level, bounded (no recursion); `buildSystemPrompt` **byte-identical** (mechanically diffed old L216–263 vs new L206–253: zero drift) |
| V3 | A-2 (status + consumers + STT) | **PASS** — `AiStatusService` registry-driven; output order `[GROQ, GEMINI, OPENAI, OLLAMA, WHISPER_LOCAL]` and field semantics identical (configured/state/reason strings match, incl. `Configure GEMINI_API_KEY or provide BYOK key`); `label` additive on `ProviderStatusDto`; `WhisperTranscriptionService` = exactly the 3 prescribed lookups re-pointed to the registry + allowlist call re-point, STT dispatch/endpoints/validation message untouched; all 4 study consumers use `resolveStudyProvider` + `generatorModel = response.model` (BC-4 — structural close of the WP2-A-2 lineage). **One breach: F-1 (below)** |
| V4 | A-3 (frontend + docs) | **PASS** — drawer fetches `/status` on open, filters `WHISPER_LOCAL`, derives label/model from backend, `FALLBACK_PROVIDER_OPTIONS` kept as offline-only seed (stale `3.8` labels corrected to config truth); `DEFAULT_PROVIDER_MODELS` map deleted; stored-model migration `2.0/2.5 → 3.5-flash` consistent with BC-3; `aiApi.ProviderStatus.label?` additive; README recipe matches the YAML contract + correct protocol-extension note; YAML comment header present (G10) |
| V5 | Test files | **PASS** — `AiProviderAdapterTest` (6 tests) is substantive: G1 config-only GLM (chat + Bearer capture + status listing with label/model/configured), G4 fallback switch (call counts 1+1, honest `resp.model`) **and** exhausted path (exactly 2 calls, upstream `Retry-After: 30` honored), G5 defaults, G6 unit, G7 key-not-in-URL. `AiControllerIT` = pure addition of test 10 (unknown provider → 400, message assertions); existing 9 tests untouched. `AiStudioContext.test.tsx` = 2-line construction-synergy (passes explicit default model), no assertion weakening. **Zero edits** to `AiProviderContractTest`/`AiValidationAndCapsTest`/`AiLogHygieneTest`/`WhisperTranscriptionServiceTest`/`ConceptLessonServiceTest` — achieved via the compat constructors + map-delegating accessors (accepted deviation D-2) |
| V6 | Receipts reproduction | **EXACT MATCH** — unit `*Test`: **167 run / 0 failures / 0 errors / 1 skipped, BUILD SUCCESS** (161 baseline + 6 new, arithmetic exact; command `./mvnw test -Dtest='*Test'`); vitest: **53 files / 288 tests passed**; frontend production build: **clean (6.93 s)**. `AiControllerIT` 10/0/0 accepted on executor receipt (Testcontainers, 67.62 s elapsed) — audit-sandbox reproduction deferred, no Docker (N-1) |
| V7 | Freeze list (§5) + security | **PASS** — prompt bytes verified (buildSystemPrompt mechanical diff; study prompts untouched except F-1); allowlist rejection message byte-preserved (`Model 'X' is not in the allowlist for provider 'Y'`); BYOK precedence preserved; key never in URL/logs (`AiLogHygieneTest` green, AM-4 test green, no new key-logging statements); rate limiter + endpoint authz + quotas untouched; no schema/migration/seed file in the diff |

## 3. Acceptance-gate cross-check (vs executor report)

| Gate | Claim | Audit result |
|---|---|---|
| G1 config-only add | PASS | **Confirmed by test** — programmatic `glm` entry + embedded mock server; zero production-code change; also proves status listing with `label` |
| G2 contract parity | PASS | **Confirmed** — suite green with zero assertion edits; buildSystemPrompt byte-diff clean |
| G3 status parity | PASS | **Confirmed** — order/semantics/reason strings verified; `label` additive only |
| G4 Gemini fallback | PASS | **Confirmed** — both directions tested; exactly-2-calls assertion present; no recursion |
| G5 study honesty | PASS | **Confirmed** — all 4 services assign `response.model`; `STUDY_PROVIDER`/`defaults` resolution tested |
| G6 unknown provider | PASS | **Confirmed** — unit + new IT test 10; message lists valid ids |
| G7 secret hygiene | PASS | **Confirmed** — AiLogHygieneTest green; AM-4 green; no key material in logs or diff |
| G8 STT regression | PASS | **Confirmed** — WhisperTranscriptionServiceTest 5/5, contract STT case green, zero assertion edits |
| G9 full regression | PASS | **Confirmed with exact reproduction** (167/0/0/1, 53/288, build clean); IT on executor receipt (N-1) |
| G10 ops docs | PASS | **Confirmed** — README recipe + YAML comment header, copy-paste verifiable |

## 4. Findings & dispositions

### F-1 — Undisclosed out-of-scope prompt mutation (condition C1)

`TopicPrimerService.buildTopicPrimerPrompt` L251 was mutated:

```diff
-3. KaTeX is supported for mathematical notation (\${'$'}math\${'$'}).
+3. KaTeX is supported for mathematical notation (${'$'}{'$'}math${'$'}{'$'}).
```

The old form renders `\$math\$` into the prompt; the new form renders the literal template
noise `${'$'}math${'$'}` (Kotlin semantics: `{'$'}` is not interpolation). This line is prompt
content — explicitly frozen and listed under §7 "do NOT touch" — and the mutation is **not
disclosed** in the report, which claims "zero unintended deviations". No test pins this line
(`TopicPrimerPromptTest` asserts topic/labels/samples only), which is why the suite stayed
green. Impact: primer generation still works; the KaTeX hint the model receives is degraded to
noise (the whole pilot bank was generated under the old phrasing). **Required fix (C1):**
revert L251 to the pre-merge literal (one line, zero risk to any count — unit stays 167/0/0/1).
Ride-along re-verification: byte-diff of the prompt builder after the revert.

### Observations (accepted, no action required)

- **O-1** — 503 handling: old code fell back to the fallback model immediately; new code retries
  503 once (per the global retry policy) before falling back, so a 503 storm can emit up to 4
  HTTP calls vs the old 2–3. Bounded, same terminal exception type, strictly more resilient.
- **O-2** — BC-2 `Retry-After` default is 60 s (work-order sketch suggested 30 s); upstream
  `Retry-After` header is honored when present (tested). Cosmetic deviation from the sketch;
  better behavior.
- **O-3** — `setSelectedProvider(provider, defaultModel?)` only resets the model when the caller
  supplies a default (the drawer always does); other callers no longer auto-wipe a user's custom
  model on provider switch. Minor UX nuance, arguably an improvement.

### Advisories (non-blocking, for the next maintenance window)

- **A-1** — `AiStatusService` keeps a single shared OLLAMA reachability cache (30 s TTL); with
  two OLLAMA-kind entries the cache would conflate them. Fine at 1 entry; revisit if one is added.
- **A-2** — `chatTransports.associateBy { it.kind }` silently last-wins on duplicate kinds; a
  boot-time uniqueness assert would make a future bad bean fail fast.
- **A-3** — The `AiProperties` typed accessors use `getOrPut` (mutating on read). Safe today
  (idempotent defaults), but a read-only accessor or a documented contract would be cleaner.

### Deviations (accepted)

- **D-2 (accepted)** — Work order prescribed deleting the legacy typed provider fields; executor
  instead implemented **map-delegating computed accessors** (`val groq get() = entries.getOrPut("groq"){...}`).
  The map remains the single source of truth (verified: accessors mutate/read the same map the
  registry iterates), and this choice kept 5 test files at zero churn. Architecturally sound;
  accepted with advisory A-3 attached.
- **D-3 (accepted)** — §12 recommended dispatch after the §10 DoD receipt; operator dispatched
  first. Consequence + reconciliation in §5 below.

### Notes

- **N-1** — `AiControllerIT` (10/0/0) not reproducible in the audit sandbox (no Docker); static
  verification of the added test PASS; executor receipt accepted per P1 precedent. Note the
  executor environment is now demonstrably Docker-capable — see N-2.
- **N-2** — The P1-deferred `ForceAuthzIT` execution receipt is still open. Since the executor
  now runs Testcontainers ITs, the cheapest closure is to include `ForceAuthzIT` in the C1 fixup
  run (single `mvnw test -Dtest=ForceAuthzIT` receipt) — this would also finally discharge the
  P1 deferred item and simplify §10.

## 5. §10 DoD reconciliation (operator-facing)

The §10 run has not happened yet (no `PHASE8-DOD-RECEIPT.md`); this package moved master after
the guide's expected values were written. Updated deltas so the run is not blocked:

| §10 item | Guide value | Now on `37f0237` |
|---|---|---|
| Backend unit `*Test` | 161/0/0/1 | **167/0/0/1** (+6 = `AiProviderAdapterTest`; no deletions; reproduced in this audit) |
| Frontend vitest | 53 files/288 tests | **unchanged 53/288** (reproduced) |
| Frontend build | clean | **clean** (reproduced, 6.93 s) |
| Seed counts / concept links / coverage / auth flows | per guide | **untouched** — no migration, seed, curation, or authz file in this package's 24-file diff |
| `ForceAuthzIT` execution receipt | deferred to first Docker run | **still open** — fold into C1 fixup (N-2) or the §10 host if Docker-enabled |

Everything else in `collab/z/PHASE8-DOD-CLOSURE-GUIDE.md` remains valid as written.

## 6. Verdict

**CONDITIONAL PASS, accepted as merged (`37f0237`).** The architecture delivers exactly the
operator's directive — verified provider onboarding with zero production-code change (G1 proof),
single model truth, honest generation metadata, and full parity for the audited behaviors — with
receipts reproduced exactly and a clean security posture. The single condition:

- **C1 (binding):** revert the one-line prompt mutation in
  `TopicPrimerService.buildTopicPrimerPrompt` (L251) to the pre-merge literal
  `(\${'$'}math\${'$'})`, re-run the unit receipt (expected: unchanged 167/0/0/1), and (recommended)
  ride along the `ForceAuthzIT` execution receipt (N-2). On evidence, Z.ai flips this verdict to
  final PASS. Estimated executor effort: ≤15 min including receipts.

**Discharge record (2026-10-30):** C1 and N-2 discharged on `ai/provider-adapter-fixup-1`
(executor commit `62cf4e5`, `--no-ff` merge `efb8188`) along with two operator-directed
ride-alongs (5-minute chat read timeout; drawer background generation + Stop button). Fixup
audit: **PASS** — `collab/z/AI-PROVIDER-ADAPTER-FIXUP-1-AUDIT.md`. Verdict is now **final PASS**;
this package is closed. §5 baseline table superseded by the fixup audit §3 (vitest 53/289;
`ForceAuthzIT` closed).

# [AI-PROVIDER-ADAPTER] Provider-Agnostic AI Layer — Binding Work Order

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-28 (all code facts audited at `origin/master = ed9f061`, tree clean)
- **Status:** PREPARED — activates only on operator dispatch. **Recommended sequencing: run the
  Phase 8 §10 DoD closure first** (it pins test-count receipts to pre-adapter `ed9f061`; this
  package touches backend test files and would contaminate those numbers if merged first).
- **Executor:** Gemini Antigravity (per D-6; 4 consecutive clean packages)
- **Provenance:** operator directive 2026-10-28 — "make the AI layer model-agnostic: providers
  and model versions will change; plugging a new key should just work, no repeated file edits."
- **Size estimate:** 3–5 h executor time + full Z.ai audit ≤24 h (cross-cutting refactor of a
  key-handling path — proportionate regime requires full audit, Qwen cross-review recommended).
- **Branch:** `ai/provider-adapter-1` (single branch, code + docs, `--no-ff` merge).

---

## 1. Intent

Today every provider is a **typed, hardcoded branch**: adding or swapping a provider (GLM,
DeepSeek, Mistral, OpenRouter, vLLM, a new Gemini family, …) requires editing `AiProperties`,
`AiChatService`, `AiStatusService`, the YAML, the frontend drawer, and several tests — and model
version changes are scattered across six literal sites. This work order converts the layer to a
**registry + transport-adapter** design with exactly one config-driven seam:

- **Provider = config entry** (id → spec). Adding an OpenAI-compatible vendor = 5 YAML lines, zero
  Kotlin/TS edits. It immediately appears in `/api/v1/ai/status`, the studio drawer, BYOK, and can
  drive the study-generation features via `STUDY_PROVIDER`.
- **Wire protocol = transport adapter** keyed by a closed `kind` enum (`GEMINI`, `OPENAI_COMPATIBLE`,
  `OLLAMA`). A genuinely new protocol (e.g. Anthropic-native) = one new transport file + one enum
  case; **no edits to any existing file** (open-closed seam).
- **Model/version = config value.** All six hardcoded model-name literals (§2.1) collapse to one
  `default-model` (+ optional `fallback-model`) per provider entry.

## 2. Current-state defect map (evidence at `ed9f061`)

### 2.1 Hardcoded provider/model sprawl

| # | Site | Fact |
|---|---|---|
| 1 | `AiProperties.kt` L15–33 | Providers are **typed fields** (`groq/gemini/openai/ollama`) — new provider = new field |
| 2 | `AiProperties.kt` L69–94 | `AllowlistConfig.isModelAllowed` — per-provider `when` + prefix heuristics; unknown provider silently allowed (L91 `else -> true`) |
| 3 | `AiChatService.kt` L58–82 | `when (provider)` transport dispatch; **else-branch silently routes to GROQ** — a typo'd provider id never fails |
| 4 | `AiChatService.kt` L180–200 | `resolveApiKey` — per-provider `when` + GEMINI has 4 env aliases |
| 5 | `AiChatService.kt` L202–214 | `resolveModelName` — per-provider `when` + per-provider env names |
| 6 | `AiChatService.kt` L349–351 | **`"gemini-3.5-flash"` hardcoded inside the transport** as 429/503 fallback |
| 7 | `AiChatService.kt` L206 + `AiStatusService.kt` L47 | `gemini-3.8-flash` literal as model fallback |
| 8 | `application.yml` L85 vs `AiProperties` L24/L71 | Default-model truth split across yaml + Kotlin (`gemini-3.5-flash` vs `gemini-3.8-flash`) |
| 9 | 4 anatomy services | `provider = provider ?: "GEMINI"` + per-service `resolvedModel` fallback chains (`TopicPrimerService` L96/L104, `ConceptLessonService` L135/L141, `ConceptService` L137/L145 — L145 doesn't read config at all, `AnatomyGenerationService` L88/L97/L109) |
| 10 | `AiStatusService.kt` L18–91 | Five hand-written status blocks, each duplicating key resolution + env names |
| 11 | Frontend `AiStudioDrawer.tsx` L28–30, L267, L369, L379 | Hardcoded `PROVIDER_OPTIONS` array (labels, defaultModel, marketing speed strings); `AiStudioContext.tsx` L36–37/L83 hardcodes `DEFAULT_PROVIDER_MODELS` — while `/status` already returns everything needed |

### 2.2 Existing seams to preserve (audited good)

- Error taxonomy (`AiExceptions.kt`) + `AiExceptionAdvice` mapping incl. an already-wired but
  **never-thrown** `AiRateLimitException` (429 + `Retry-After`).
- `executeWithRetry` policy (AM-10: 4xx never retried, 5xx once, OLLAMA 0).
- Key-in-header (AM-4: `x-goog-api-key`, never query string), BYOK precedence header > property > env.
- `buildSystemPrompt` (AM-11 fencing) and `validateCaps` (AM-6).
- `AiProviderContractTest` embedded-server contract suite; `AiLogHygieneTest` key-redaction.

## 3. Target architecture

```text
com.gatelms.ai/
  config/AiProperties.kt        # rewritten: providers.entries: Map<String, ProviderSpec> + defaults + http/quota/whisper (unchanged sections)
  registry/
    ProviderSpec.kt             # data class — one provider entry (see §3.1)
    ProviderRegistry.kt         # @Component; boot-validated; id→spec lookup, key/model/allowlist resolution, status descriptors
  transport/
    ChatTransport.kt            # interface: chat(spec, systemInstruction, messages, key): String  — throws raw HTTP exceptions
    GeminiTransport.kt          # generateContent wire (from callGemini; NO fallback logic inside)
    OpenAiCompatTransport.kt    # /chat/completions wire (from callOpenAiCompatible)
    OllamaTransport.kt          # /api/chat wire (from callOllama)
  service/AiChatService.kt       # public API unchanged; internals = registry + kind→transport + retry (moved verbatim) + fallback-model dispatch
  service/AiStatusService.kt    # loops registry descriptors + WHISPER_LOCAL probe (unchanged semantics)
```

Dispatch is `when (spec.kind)` over the **closed 3-case enum** — never over provider id. Everything
provider-specific (id, key, models, endpoints, env names, allow rules) lives in `ProviderSpec`.

### 3.1 `ProviderSpec` (config-bound, per entry)

| Field | Purpose | Legacy equivalent |
|---|---|---|
| `kind` | `GEMINI` \| `OPENAI_COMPATIBLE` \| `OLLAMA` (closed enum) | the `when` branches |
| `label` | display name for drawer/status (new, additive `label` on `ProviderStatusDto`) | hardcoded drawer strings |
| `endpoint` | base URL per transport | `ProviderConfig.endpoint` |
| `apiKey` | property-injected key (env feeds it via yaml `${...}`) | `ProviderConfig.apiKey` |
| `envKeyNames` | ordered env fallbacks | `resolveApiKey` `when` + GEMINI aliases |
| `envDefaultModelNames` | env override for default model | `GEMINI_DEFAULT_MODEL` etc. |
| `defaultModel` | THE one model source per provider | 6 scattered literals |
| `fallbackModel` | optional 429/503 retry-with-model (gemini parity) | hardcoded `gemini-3.5-flash` |
| `sttModel` | STT model for GROQ/OPENAI | `ProviderConfig.sttModel` |
| `modelAllowlist` + `modelPrefixes` + `allowAnyModel` | replaces `AllowlistConfig` per-provider heuristics | `isModelAllowed` `when` |
| `requireKey` | false for OLLAMA | OLLAMA special cases |
| `maxRetriesOverride` | OLLAMA → 0 | `if (provider == "OLLAMA") 0` |

### 3.2 Prescribed YAML (exact migration — env var names UNCHANGED)

```yaml
app.ai:
  defaults:
    study-provider: ${STUDY_PROVIDER:GEMINI}   # primer/lesson/anatomy/concepts generation default
  providers:
    entries:
      groq:
        kind: OPENAI_COMPATIBLE
        label: "Groq (Llama 3.3 70B)"
        endpoint: https://api.groq.com/openai/v1
        api-key: ${GROQ_API_KEY:}
        env-key-names: [GROQ_API_KEY]
        env-default-model-names: [GROQ_DEFAULT_MODEL]
        default-model: ${GROQ_DEFAULT_MODEL:llama-3.3-70b-versatile}
        stt-model: ${GROQ_STT_MODEL:whisper-large-v3-turbo}
        model-prefixes: [llama, whisper, mixtral]
      gemini:
        kind: GEMINI
        label: "Google Gemini Flash"
        endpoint: https://generativelanguage.googleapis.com/v1beta/models
        api-key: ${GEMINI_API_KEY:}
        env-key-names: [GEMINI_API_KEY, GEMINI_KEY, GOOGLE_API_KEY, AI_STUDIO_API_KEY]
        env-default-model-names: [GEMINI_DEFAULT_MODEL]
        default-model: ${GEMINI_DEFAULT_MODEL:gemini-3.5-flash}
        fallback-model: gemini-3.5-flash        # parity for the 429/503 fallback
        model-prefixes: [gemini]
      openai:
        kind: OPENAI_COMPATIBLE
        label: "OpenAI"
        endpoint: https://api.openai.com/v1
        api-key: ${OPENAI_API_KEY:}
        env-key-names: [OPENAI_API_KEY]
        env-default-model-names: [OPENAI_DEFAULT_MODEL]
        default-model: ${OPENAI_DEFAULT_MODEL:gpt-4o-mini}
        model-prefixes: [gpt, o, whisper]
      ollama:
        kind: OLLAMA
        label: "Ollama (local)"
        endpoint: ${OLLAMA_ENDPOINT:http://localhost:11434}
        env-default-model-names: [OLLAMA_DEFAULT_MODEL]
        default-model: ${OLLAMA_DEFAULT_MODEL:llama3}
        allow-any-model: true
        require-key: false
        max-retries-override: 0
```

Note: keep the current allowlist semantics **exactly** (empty-allowlist ⇒ allow; prefix match
case-insensitive; the OPENAI `o` prefix laxity is accepted as parity — not new hardening). The
whole legacy `AllowlistConfig` + per-provider model literals are deleted in the same commit.

### 3.3 `AiChatService.chat()` internals (public signature UNCHANGED)

`validateCaps` → `registry.require(provider)` (unknown id ⇒ `AiValidationException` listing valid
ids) → `resolveModel` + allowlist check (**message string preserved verbatim**: `Model 'X' is not
in the allowlist for provider 'Y'` — asserted by `AiValidationAndCapsTest` L122 and `AiControllerIT`
L136) → `resolveKey(id, byok)` → `transportFor(spec.kind).chat(...)` inside `executeWithRetry`
(moved verbatim) → response. Gemini fallback-model handling moves **out of the transport, up one
level**: on `TOO_MANY_REQUESTS`/`SERVICE_UNAVAILABLE`, if `spec.fallbackModel` is set and
`model != fallbackModel`, re-dispatch once with the fallback model (same `log.warn` parity, no
infinite recursion). `buildSystemPrompt` moves **verbatim** (zero byte drift — see G2).

## 4. Work items

### A-1 — Registry + transports + config rewrite (binding, core)

1. New `registry/` + `transport/` packages as §3; move the three transport bodies out of
   `AiChatService` (wire payloads byte-identical).
2. Rewrite `AiProperties.providers` to `entries: LinkedHashMap<String, ProviderSpec>` (Spring
   relaxed binding; insertion order = YAML order ⇒ status list order). Keep `http`, `quota`,
   `whisper` sections untouched. Delete `AllowlistConfig` (logic absorbed per-spec) — except the
   **LOCAL/WHISPER_LOCAL STT rule**, which moves to a preserved non-entry rule (A-2.3).
3. `ProviderRegistry` boot validation (`@PostConstruct`): every entry has a valid `kind`;
   duplicate ids impossible by map; `defaultModel` non-blank; `GEMINI` kind requires endpoint.
   Fail fast with an actionable message.
4. Update the 6 test files that construct `providers.groq.*` etc. programmatically
   (`AiProviderContractTest`, `AiValidationAndCapsTest`, `AiLogHygieneTest`,
   `WhisperTranscriptionServiceTest`, `AiControllerIT`, `ConceptLessonServiceTest`) — construction
   syntax only; **zero assertion edits** where the asserted value is behavioral.

### A-2 — Status service + study consumers + STT re-point (binding)

1. `AiStatusService`: one loop over registry descriptors (`configured` = key resolvable, `state`
   = READY/NOT_CONFIGURED, `model` = resolved default) + OLLAMA probe (existing `/api/tags` +
   30 s TTL cache) + WHISPER_LOCAL block unchanged. Output MUST remain
   `[GROQ, GEMINI, OPENAI, OLLAMA, WHISPER_LOCAL]` in this exact order with identical field
   semantics; add additive `label` to `ProviderStatusDto`.
2. The 4 anatomy services: replace `provider ?: "GEMINI"` and the per-service `resolvedModel`
   fallback chains with `registry.resolveStudyProvider(explicit)` (→ `defaults.study-provider`)
   and set **`generatorModel = aiChatService.chat(...).model`** (the effectively used model —
   final, structural close of the WP2-A-2 lineage; drop all pre-computed `resolvedModel` locals).
3. `WhisperTranscriptionService`: **transports/endpoints/STT dispatch untouched** — only re-point
   its three provider lookups (`providers.groq.apiKey` L278, `providers.openai.apiKey` L287,
   `providers.groq.sttModel` L310) and its `allowlist.isModelAllowed(effectiveProvider, customModel)`
   call (L204) to registry equivalents preserving the LOCAL STT rule verbatim.

### A-3 — Frontend + ops docs (binding)

1. `AiStudioDrawer.tsx`: render provider options from the `/status` response (`getProvidersStatus`
   already exists — `aiApi.ts` L117); `PROVIDER_OPTIONS` shrinks to a static id-only offline
   fallback; label from `label ?? provider`; the `defaultModel` lookups at L369/L379 use the
   status-reported model. `AiStudioContext.tsx`: delete `DEFAULT_PROVIDER_MODELS`; model
   validation hint derives from status; keep `'GROQ'` as the last-resort provider id constant.
2. `GateLMS/README.md` (or ops section where AI config is documented): "Adding an AI provider"
   recipe with the GLM example from §11 + one line on when a new transport file is needed.
   Add a matching YAML comment header above `app.ai.providers.entries`.

## 5. Behavior freeze (binding — violation = gate failure)

1. `buildSystemPrompt` output for every `(mode, questionContext)` input — verbatim move, zero
   prompt-byte drift (the 4 study services' `mode = "study"` fall into the INTERACTIVE branch —
   frozen as-is; changing prompts is out of scope).
2. Wire request/response JSON shapes per transport (existing contract-test captures stay valid).
3. DTO contracts: `AiChatRequest`/`AiChatResponse` unchanged; `ProviderStatusDto` gains only
   optional `label`. Status list content + order identical for identical env.
4. Retry semantics: 4xx never retried; 5xx retried once; OLLAMA 0 retries; BYOK precedence
   header > property > env; env var names unchanged (`GROQ_API_KEY`, `GEMINI_API_KEY`, etc.).
5. Security posture: key never in URL (AM-4), key redaction (AiLogHygieneTest), no new
   key-logging statements, rate-limiter quotas + endpoint authz untouched.

## 6. Deliberate behavior changes (defaults ON — operator may override at dispatch)

| # | Change | Before | After | Rationale |
|---|---|---|---|---|
| BC-1 | Unknown/typo provider id | silently routed to GROQ | 400 `AiValidationException` naming the valid ids | silent-wrong-provider is a supportability trap; frontend picker constrains ids |
| BC-2 | Upstream 429 (no fallback model, or fallback exhausted) | `AiValidationException` → HTTP 400 | `AiRateLimitException` → HTTP 429 + `Retry-After` (advice already wired, exception never thrown today) | correct client semantics; plumbing already exists |
| BC-3 | Gemini default model truth | split across 6 literals (3.5 vs 3.8) | single `default-model` (YAML `gemini-3.5-flash`) + optional `fallback-model` | the operator's core ask |
| BC-4 | `generatorModel` metadata on study features | pre-computed fallback guess | actual model from the chat response | honesty of recorded metadata (WP2-A-2 lineage) |

## 7. Out of scope — do NOT touch

- STT internals: whisper sidecar transport, `WhisperTranscriptionService` dispatch, STT
  endpoints/timeouts, docker-compose whisper stack. (STT transport adapters = future package.)
- Prompt content, `mode` semantics, grounding samplers, generation rails (cache-first, quota
  buckets, maxAttempts=2, failed-never-persist), curation endpoints/authz.
- Streaming, multi-key pools, per-feature model routing beyond `study-provider`, admin key
  management (env-only remains the security posture).
- Rate limiter, schema/migrations/seeds. If a change seems to need any of these, STOP and
  report — the scope is wrong, not the code.
- Phase 9 bank-cleanup items.

## 8. Acceptance gates

| # | Gate | Evidence required in report |
|---|---|---|
| G1 | **Config-only provider add works** | New unit test: build registry from an `AiProperties` with a test-only `glm` entry (`OPENAI_COMPATIBLE` → embedded mock server) → `chat()` succeeds; status lists it; drawer-equivalent uses its label/model — with **zero production-code change** (the test constructs config programmatically) |
| G2 | Contract parity | All existing `AiProviderContractTest` cases green with **zero assertion edits**; `git diff` receipt showing `buildSystemPrompt` moved verbatim (whitespace-only) |
| G3 | Status endpoint parity | Same ids, same order, same configured/state semantics for identical env; `label` additive; frontend status tests stay green |
| G4 | Gemini fallback parity | Mock-server test: model A → 429 ⇒ second request targets `fallback-model` path; fallback → 429 ⇒ `AiRateLimitException`, exactly 2 calls (no recursion) |
| G5 | Study-consumer model honesty | Per-service unit tests: `generatorModel == response.model`; default provider resolves from `STUDY_PROVIDER` |
| G6 | Unknown provider rejection | Unit + negative IT: typo'd id → 400 ProblemDetail listing valid ids |
| G7 | Secret hygiene | `AiLogHygieneTest` green unchanged; AM-4 no-key-in-URL test green; `grep -rn "apiKey" <new files>` receipt showing no log statement prints key material |
| G8 | STT regression | `WhisperTranscriptionServiceTest` + contract STT case green, zero assertion edits |
| G9 | Full regression | Unit `*Test` receipts (baseline: the §10-verified counts at the pre-merge SHA — expected ≈161/0/0/1 + new tests, exact numbers), vitest (baseline 53 files/288 tests + new), frontend build clean, `AiControllerIT` green |
| G10 | Ops docs | README recipe + YAML comment header present; §11 recipe copy-paste-verifiable |

**Receipts format** (house standard): per-suite counts, exact command lines, deviations declared
up front. Honest-judge rule: a failing/omitted check is reported as such, never silently skipped.

## 9. Delivery mechanics

1. Branch `ai/provider-adapter-1` off `master`; code + docs on the branch; single `--no-ff`
   merge to `master`; branch retained.
2. Report to `collab/gemini/AI-PROVIDER-ADAPTER-REPORT.md` (facts, diffs, receipts, deviations,
   G1–G10 table).
3. Merge only when every gate has evidence; if any gate cannot pass, stop and report.

## 10. Audit regime (proportionate — this is a cross-cutting, key-handling refactor)

- **Full Z.ai audit ≤24 h**: diff review of every touched file, gate-by-gate receipt cross-check,
  offline reproduction of unit + vitest suites, verdict in `collab/z/AI-PROVIDER-ADAPTER-AUDIT.md`.
- Hard invariants (not waivable): G1 config-only proof by test; G2 zero prompt-byte drift;
  G5 metadata honesty; G7 secret hygiene; §5 freeze list.
- **Qwen cross-review recommended** at this size (operator's call at dispatch; P1's light regime
  does not scale to a 6+ file refactor of the AI path).

## 11. Operator quickstart — the payoff (what "plug a new key" looks like after this)

Add ANY OpenAI-compatible vendor (GLM/Zhipu, DeepSeek, Mistral, OpenRouter, Together, Groq-2,
vLLM, Ollama-cloud, LM Studio, …) — **5 YAML lines, one env var, zero code edits**:

```yaml
# backend/src/main/resources/application.yml
app.ai.providers.entries.glm:
  kind: OPENAI_COMPATIBLE
  label: "GLM (Zhipu)"
  endpoint: https://open.bigmodel.cn/api/paas/v4
  api-key: ${GLM_API_KEY:}
  default-model: glm-4.7
  model-prefixes: [glm]
```

```bash
export GLM_API_KEY=<your-key>   # that's it
# optional: make primer/lesson/anatomy/concepts generation use it:
export STUDY_PROVIDER=GLM
```

It then appears in `/api/v1/ai/status` and the studio drawer automatically, accepts BYOK keys,
and honors the same retry/allowlist/security posture as every built-in provider. Model-version
bumps (`gemini-3.5-flash` → `gemini-4-flash`, `glm-4.7` → next) are one `default-model` value.
A vendor with a genuinely new wire protocol = one new `transport/*.kt` file + one enum case —
still no edits to existing files.

## 12. Dispatch

Operator: (1) file the Phase 8 §10 DoD receipt, (2) confirm/override the BC-1…BC-4 defaults,
(3) say "dispatch AI-PROVIDER-ADAPTER" — the work order is then live for Gemini Antigravity.

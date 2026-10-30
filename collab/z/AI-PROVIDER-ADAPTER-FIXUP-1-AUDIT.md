# [AI-PROVIDER-ADAPTER-FIXUP-1] Fixup & Audit-Discharge — Post-Merge Audit

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-30 (audited at `origin/master = efb8188`, tree clean, local == remote)
- **Subject:** `ai/provider-adapter-fixup-1` (executor commit `62cf4e5`, `--no-ff` merge `efb8188`,
  base `4a3473d`)
- **Regime:** fixup audit — C1 discharge verification is byte-mechanical; the two operator-directed
  ride-alongs audited line-level; all receipts independently reproduced offline.
- **Executor:** Gemini Antigravity — report: `collab/gemini/AI-PROVIDER-ADAPTER-FIXUP-1-REPORT.md`
- **Discharge scope (from `AI-PROVIDER-ADAPTER-AUDIT.md`):** condition **C1** (prompt revert) +
  note **N-2** (`ForceAuthzIT` receipt). Ride-alongs: two operator-directed requests (5-minute chat
  read timeout; drawer background generation + Stop button).

---

## 1. Verification matrix

| # | Item | Method | Result |
|---|---|---|---|
| V1 | Git mechanics | Git DAG, diff scan | **CLEAN.** Single commit `62cf4e5` off `4a3473d`; one `--no-ff` merge `efb8188` (parents `4a3473d`+`62cf4e5`); branch `ai/provider-adapter-fixup-1` pushed and retained on origin. 8-file inventory == report claim exactly (no strays, none missing): `TopicPrimerService.kt`, `AiProperties.kt`, `application.yml`, `docker-compose.yaml`, `.env.example`, `AiStudioDrawer.tsx` + test, report. Secret scan of the full diff clean; zero CRLF bytes. |
| V2 | **C1 discharge — prompt revert** (binding condition) | Mechanical byte-diff | **DISCHARGED.** `buildTopicPrimerPrompt` extracted from `9877e72` (pre-merge) and from `efb8188` — 41 lines each, **zero-byte drift** (restores the `\${'$'}math\${'$'}` literal that renders `\$math\$`). Full-file diff vs `9877e72` shows only the expected adapter-package wiring (`ProviderRegistry` injection, `resolveStudyProvider`, honest `lastUsedModel` lineage) — no other prompt mutation anywhere in the file. |
| V3 | Ride-along 1 — 5-minute chat read timeout (operator request) | Line-level diff, consistency check | **PASS.** 4-file consistent change: Kotlin default `chatReadTimeoutMs = 300000` == YAML `${AI_CHAT_READ_TIMEOUT_MS:300000}` == compose passthrough `${AI_CHAT_READ_TIMEOUT_MS:-300000}` == `.env.example` doc line. No test pinned the old 60 s production default (both AI suites set their own 1500 ms fixtures; the `60000` in `AiValidationAndCapsTest` is the AM-6 char cap, unrelated). Pre-existing nginx `proxy_read_timeout 360s` covers the single-attempt 300 s path — see advisory A-4 for the worst-case retry path. Bonus: the YAML value is now env-overridable (O-4), consistent with the adapter package's config-first philosophy. |
| V4 | Ride-along 2 — drawer background generation + Stop button (operator request) | Line-level diff, component-tree analysis | **PASS.** `handleClose` and the global `Escape` handler no longer abort; an in-flight **Stop** button (red `Square`, shown only while `isLoading`) aborts on click. AM-15 pre-send abort retained; `handleClear` still aborts (correct — clearing kills generation); `AbortError` catch returns silently. Architecture is sound for background completion: conversation + `isLoading` state live in `AiStudioContext` (mounted at app root, survives close), and the drawer is unconditionally rendered in `Layout.tsx` with `if (!isOpen) return null` — the component *instance* and `abortControllerRef` survive close→reopen, so Stop remains usable after reopening. Auto-scroll hardening (optional chaining) verified; dependency array intact (`[messages, isLoading]` — an `essages` rendering in the audit terminal was a pipe artifact, disproven by ord-code dump `[91,109,101,…]`). **Test 8 substantive:** captures the `AbortSignal` passed to `aiApi.chat`, drives a real close click mid-flight, asserts `signal.aborted === false` before and after. |
| V5 | **N-2 discharge — `ForceAuthzIT` receipt** | Static + receipt | **DISCHARGED.** Receipt: 3/0/0/0, BUILD SUCCESS (Testcontainers, Docker Server 28.5.1 / API 1.44, 13.91 s). Static count = exactly 3 `@Test` methods (G1×2 force→403 both endpoints, G2 non-force pass-through) — consistent. Not reproducible in the audit sandbox (no Docker — N-1 precedent); receipt accepted, and the executor's Docker environment is now twice-demonstrated (`AiControllerIT` 10/0/0 previous package + this receipt). **This closes the P1-deferred `ForceAuthzIT` execution receipt — the last open item from `FOLLOWUP-P1-FIXUP-AUDIT.md`.** |
| V6 | Receipts — independent reproduction | Re-runs in sandbox | **EXACT MATCH on backend; ONE CORRECTION on frontend (F-1).** Unit `*Test`: **167 run / 0 failures / 0 errors / 1 skipped** across 27 classes, BUILD SUCCESS — exact match (unit baseline unchanged; no backend test added or deleted). Vitest: **53 files / 289 tests passed** — the report's matrix and receipt claim **288**, which is arithmetically impossible (see F-1). Frontend production build: **clean, 6.84 s** (pre-existing chunk-size note only). |
| V7 | Scope discipline | Diff vs audit condition | **ACCEPTED (P-1).** The C1 condition prescribed the revert + unit receipt (+ recommended `ForceAuthzIT`); the fixup additionally carries two operator-directed ride-alongs, both disclosed in the report with the operator's requests quoted. Reviewability preserved (8 files, +155/−17); receipt attribution disentangled by static analysis (no test pins either changed production default/line). |

## 2. Findings & dispositions

### F-1 — Vitest receipt transcription error (LOW, documentation only)

The report's verification matrix and receipt both claim "53 files / **288** tests passed (incl. new
Test 8)". The pre-fixup baseline was 53/288 with **7** tests in `AiStudioDrawer.test.tsx`; this
package adds Test 8, so the total must be **289** — the receipt's own header line
(`AiStudioDrawer.test.tsx (8 tests)`) proves the suite grew. Audit reproduction: **53 files /
289 passed**. The number is a stale copy from the previous report's receipt block; no test is
missing (file count 53 unchanged, every test passes, Test 8 independently verified green). No code
action required. **Binding for the operator: the §10 DoD baseline is 53 files / 289 tests.**

### P-1 — Process note (accepted)

Audit conditions should ideally be discharged in isolation so receipts attribute cleanly; folding
operator-directed ride-alongs into a C1-discharge fixup blurs attribution (the 167/0/0/1 unit
receipt covers revert + timeout combined). Here the package stayed small enough to disentangle
statically, and the operator's two requests were urgent-facing UX/ops items — accepted without
reservation, with the note recorded for future condition drafting.

### Advisories (non-blocking, next maintenance window)

- **A-4** — Timeout budget vs proxy: worst-case retry path is 2 attempts × 300 s = 600 s, which
  exceeds nginx's `proxy_read_timeout 360s`; in that pathological double-timeout case the user
  gets an nginx 504 before the backend's second attempt completes. Single-attempt 300 s < 360 s is
  fine. If the 5-minute default is kept, raise `frontend/nginx.conf` `/api/` `proxy_read_timeout`
  to ≥ 660 s at the next window (or cap `AI_CHAT_READ_TIMEOUT_MS` below 360 s in deployments
  where a snappier failure is preferred).
- **A-5** — While generation continues after drawer close, the only stop affordance is reopening
  the drawer (the Stop button lives inside it). A floating "generating…" pill with an embedded
  stop action would polish this. UX nicety only.

### Observation

- **O-4** — `application.yml` was made env-overridable (`${AI_CHAT_READ_TIMEOUT_MS:300000}`) rather
  than just re-hardcoding 300000 — a small improvement beyond the literal operator ask, in the
  spirit of the adapter package (config over code).

## 3. §10 DoD baseline updates (supersedes `AI-PROVIDER-ADAPTER-AUDIT.md` §5)

| §10 item | Previous guidance | Now on `efb8188` |
|---|---|---|
| Backend unit `*Test` | 167/0/0/1 | **167/0/0/1** (unchanged — reproduced in this audit) |
| Frontend vitest | 53 files / 288 tests | **53 files / 289 tests** ← corrected (F-1) |
| Frontend build | clean | **clean** (reproduced, 6.84 s) |
| `ForceAuthzIT` execution receipt | deferred to first Docker run | **CLOSED — 3/0/0/0 executed** (receipt, V5); the §10 host no longer needs Docker for this item |
| Seed counts / concept links / coverage / auth flows | per guide | **untouched** — no migration, seed, curation, or authz file in the 8-file diff |

Everything else in `collab/z/PHASE8-DOD-CLOSURE-GUIDE.md` remains valid as written.

## 4. Verdict

**PASS.** C1 is discharged (byte-verified prompt restoration), N-2 is discharged (`ForceAuthzIT`
3/0/0/0 receipt, closing the P1 deferred item), and both operator-directed ride-alongs verify
clean with a substantive new test. One LOW documentation finding (F-1: vitest receipt says 288,
truth is 289 — corrected for the DoD baseline). Accordingly the main audit's verdict
(`AI-PROVIDER-ADAPTER-AUDIT.md`) flips from **CONDITIONAL PASS** to **final PASS**, and the
**AI-PROVIDER-ADAPTER package is closed**. Operator's next step remains the §10 DoD manual
checklist (guide values per §3 above), then D-2..D-9 decisions for the Phase 9 scoping.

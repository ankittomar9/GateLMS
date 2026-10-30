# [AI-STUDIO-UX-FIXUP-1] Fixup & Audit-Discharge — Post-Merge Audit

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-30 (audited at `origin/master = 15ee8c9`, tree clean, local == remote)
- **Subject:** `ai/studio-ux-fixup-1` (executor commit `72e860e`, `--no-ff` merge `15ee8c9`,
  base `5449736`)
- **Regime:** fixup audit — C1..C4 discharge verification is line- and byte-mechanical; freeze list
  re-verified across the full package range `b58f5ec..15ee8c9`; all three receipts independently
  re-computed offline in the audit sandbox (G10).
- **Executor:** Gemini Antigravity — report: `collab/gemini/AI-STUDIO-UX-FIXUP-1-REPORT.md`
- **Discharge scope (from `AI-STUDIO-UX-AUDIT.md`):** **C1** (hooks order + `node` prop leak in
  `RichMarkdownText`), **C2** (FAB pulse dead-wired), **C3** (prescribed `React.lazy` split absent —
  build receipt not reproducible from committed code), **C4** (`setByokKey` lost `.trim()`).

---

## 1. Verification matrix

| # | Item | Method | Result |
|---|---|---|---|
| V1 | Git mechanics | Git DAG, diff scan | **CLEAN.** Single commit `72e860e` off `5449736`; one `--no-ff` merge `15ee8c9` (parents `5449736`+`72e860e`); branch `ai/studio-ux-fixup-1` pushed and retained on origin. 6-file inventory == report claim exactly (5 code files + the report itself), +132/−10; no strays, none missing. Secret scan of the full diff clean. |
| V2 | **C1 discharge — hooks order + `node` leak** (binding) | Line-level diff + full component-map sweep | **DISCHARGED.** `React.useMemo` (L55–59) now runs unconditionally and the empty-content short-circuit lives inside the memo (`if (!content) return ''`); the early return moved below it (L61) — hook count is invariant across truthy↔falsy `content` transitions. The `a` mapper is now `({ node, href, children, ...props })` — the hast Element is stripped before the spread. Sweep of the entire `components` map: only `a` (L177) and `code` (L189) spread props onto DOM elements and **both** destructure `node`; every other mapper consumes only `{ children }` — zero remaining leak surface. |
| V3 | **C2 discharge — FAB pulse via context** (binding) | Line-level diff, control-flow analysis | **DISCHARGED.** Context change is strictly additive: `isLoading`/`setIsLoading` added to `AiStudioContextValue`, the provider value (inline object — no memo, hence no stale-closure risk) and the `useAiStudio` no-provider fallback. Drawer drops its local `useState(false)` and consumes the context pair; reset paths complete: `setIsLoading(true)` at L236 and `finally { setIsLoading(false) }` at L259–261 cover success, error **and abort** (the `AbortError` catch's bare `return` still executes `finally`); the L224 `isLoading` guard makes the AM-15 residual-controller path unreachable, so an old turn's teardown can never mis-reset a new turn's flag. FAB: `isLoading = propIsLoading ?? contextLoading` — the prop survives as a test override while the prop-less `Layout.tsx` mount (L35, untouched by the fixup) now has a real production path; pulse semantics are exactly `isLoading && !isOpen` as F-2 prescribed. Test 6 rewritten to drive the pulse through a `ContextPulseTester` calling context `setIsLoading(true)` with zero prop injection — matches the condition's letter. |
| V4 | **C3 discharge — lazy split + honest build receipt** (binding) | Code read + independent build | **DISCHARGED (byte-exact).** Module-top-level `const RichMarkdownText = React.lazy(() => import('./RichMarkdownText'))` (the module has had a default export since the main package — L216); assistant branch wrapped in `<React.Suspense>` with a plain-text fallback (`<p className="whitespace-pre-wrap">{msg.content}</p>` — React-escaped, content-faithful, no parser in the fallback path). Independent audit build reproduces the receipt **hash-for-hash**: `RichMarkdownText-xpflyPcI.js 184.22 kB │ gzip 56.58 kB` + `index-CAIuIPmS.js 1,627.99 kB │ gzip 422.62 kB` — identical content-addressed chunk names on both sides prove the receipt now describes the shipped artifact bit-for-bit. Main chunk 1,812.72 → 1,627.99 kB (−184.73 kB): the ~184 kB markdown/KaTeX stack is off the critical path, and G8's ~100 kB-threshold prescription is shipped code rather than a receipt line. |
| V5 | **C4 discharge — BYOK trim** (binding) | Byte-diff vs frozen baseline | **DISCHARGED.** `const next = { ...prev, [provider]: key.trim() }` restored verbatim to the pre-breach form quoted in F-4. |
| V6 | Freeze list & scope (full range `b58f5ec..15ee8c9`) | Zero-diff checks | **CLEAN.** `backend/**` zero-diff; `MathText.tsx` + `MathText.test.tsx` + `AiStudioDrawer.test.tsx` zero-diff; abort/Stop/AM-15/AM-16 logic untouched (the fixup touches only the import line, the lazy/Suspense wrap, the `isLoading` source, and the four condition targets); `package.json`/`package-lock.json` untouched by the fixup — dependency tree identical to the audited main package. Context API strictly additive (nothing removed or reshaped). |
| V7 | G3 security gate re-check | Tree scan + code read | **PASS.** `npm ls rehype-raw rehype-sanitize` → empty; exactly 4 new direct deps across the package range (`react-markdown ^10.1.0`, `remark-gfm ^4.0.1`, `remark-math ^6.0.0`, `rehype-katex ^7.0.1`; `katex` pre-existed for `MathText`); no `dangerouslySetInnerHTML` anywhere in the new code; default `urlTransform` not overridden; KaTeX `trust: false`; links carry `target="_blank" rel="noopener noreferrer"`; the new Suspense fallback renders raw text through React escaping. |
| V8 | Receipts — independent reproduction | Re-runs in sandbox | **EXACT MATCH on all three.** Vitest: **55 files / 299 tests passed** (64.02 s in the audit sandbox vs the executor's 80.91 s — environment delta only; the report shows the arithmetic `289 + 4 + 6`, so the F-1 lesson from the previous package stays applied). Backend unit `*Test`: **167 run / 0 failures / 0 errors / 1 skipped** across 27 classes, BUILD SUCCESS (01:14 min vs the executor's 02:25). Production build: clean, two-chunk layout reproduced hash-exact (V4). |

## 2. Findings & dispositions

### F-6 — Hygiene-receipt claim vs its own artifact (LOW, documentation only)

The report's verification matrix claims "`git diff --check`: **0 errors (clean)**"; the committed
report file itself carries 2 trailing-whitespace lines (L78, L80 — the pasted Maven `[INFO] ` log
lines). No code file is affected, no gate is implicated, and pasted-log fidelity is what caused
the spaces. Recorded for receipt-precision discipline: same genus as the previous package's F-1
(a receipt describing something slightly other than what exists), this time in format rather than
arithmetic. No action required.

### Advisories (carried forward)

- **A-4** — unchanged: worst-case retry path 2 × 300 s = 600 s still exceeds nginx
  `proxy_read_timeout 360s`; raise to ≥ 660 s at the next maintenance window (or cap
  `AI_CHAT_READ_TIMEOUT_MS` below 360 s where a snappier failure is preferred).
- **A-5** — half-delivered by C2: the FAB now *shows* background generation (pulse) while the
  studio is closed; the embedded stop affordance remains optional UX polish. Reopening the
  drawer still exposes the Stop button.
- Main-audit advisories **A-1** (no direct header-drag mouse-event test; "Sidecar" badge persists
  in floating mode) and **A-2** (KaTeX `errorColor` hardcoded) remain as written — non-blocking.

### Observation

- **O-5** — The provider value is an inline object (no `useMemo`), so every provider render
  re-renders all consumers. Pre-existing design, unchanged by the fixup — and it is precisely why
  the C2 wiring cannot go stale. No action.

## 3. §10 DoD baseline updates (confirms `AI-STUDIO-UX-AUDIT.md` §5)

| §10 item | Main-audit guidance | Now on `15ee8c9` |
|---|---|---|
| Backend unit `*Test` | 167/0/0/1 | **167/0/0/1** (unchanged — reproduced again) |
| Frontend vitest | 55 files / 299 tests | **55 files / 299 tests** (confirmed — the fixup modified Test 6, added none; the "299 + N" expectation resolved with N = 0) |
| Frontend build | clean (single chunk 1,812.72 kB, C3 pending) | **clean, two chunks**: `index-*.js` **1,627.99 kB** + lazy `RichMarkdownText-*.js` **184.22 kB** (pre-existing chunk-size warning now applies to the main chunk only) |
| `ForceAuthzIT` | closed | **closed** (unchanged) |
| Seed counts / concept links / coverage / auth flows | per guide | **untouched** — zero backend files in the diff |

## 4. Verdict

**PASS.** All four binding conditions are discharged and independently verified — C1 by hook-order
restoration plus a clean component-map sweep, C2 by genuinely-wired context state with complete
reset paths and a context-driven test, C3 by shipping the prescribed `React.lazy` split whose
build receipt now reproduces hash-exact, C4 by the verbatim trim restoration. Freeze list, scope
discipline, and the G3 security gate all hold across the full package range; all three receipts
reproduced exactly. One LOW documentation finding (F-6). Accordingly the main audit's verdict
(`AI-STUDIO-UX-AUDIT.md`) flips from **CONDITIONAL PASS** to **final PASS**, and the
**AI-STUDIO-UX package is closed**. Operator's next step: the §10 DoD manual checklist with the
baselines above (Phase 8 close-out), then the D-2..D-9 decisions feeding the Phase 9
question-bank governance scoping.

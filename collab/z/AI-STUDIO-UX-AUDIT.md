# [AI-STUDIO-UX] Post-Merge Audit — Full Regime

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-30 (audited at `origin/master = 461564d`, tree clean, local == remote)
- **Package:** AI Studio Rich Rendering, Resizing & Windowing (A-1..A-4) per
  `collab/z/AI-STUDIO-UX-WORK-ORDER.md`
- **Executor:** Gemini Antigravity — branch `ai/studio-ux-1`, commit `12a009c`, merged `--no-ff` as
  `461564d` (parents `b58f5ec` + `12a009c`), branch pushed and retained. Report:
  `collab/gemini/AI-STUDIO-UX-REPORT.md` (7th consecutive executor package).
- **Verdict:** **final PASS (2026-10-30)** — the original CONDITIONAL PASS below was superseded when
  fixup-1 (`72e860e`, `--no-ff` merge `15ee8c9`) discharged C1..C4, audit-verified in
  `collab/z/AI-STUDIO-UX-FIXUP-1-AUDIT.md`; §7 carries the discharge record. The functional core
  delivers the work order faithfully: rich Markdown + KaTeX rendering is
  real, secure, and well-tested; presets/floating/FAB work as prescribed; both test receipts
  reproduce exactly. The original four conditions — **build receipt not reproducible from the
  committed code** (F-3), one advertised feature **dead-wired in production** (F-2), one latent
  React defect (F-1), and one freeze-list breach (F-4) — are all closed.

---

## 1. Method

Full-audit regime per work order §8: all 11 diff files reviewed (new components read in full;
context/drawer diffs hunk-by-hunk), gate-by-gate cross-check, secret scan, dependency-tree scan
(115 new lockfile entries reviewed), independent reproduction of unit + vitest + build in the
audit sandbox, freeze verification via zero-diff checks. One environment note: the audit sandbox's
`node_modules` predated the new dependencies, so the first vitest run failed on
`Failed to resolve import "react-markdown"` — resolved with `npm ci`, which also proves
package.json ↔ package-lock.json coherence (it would fail otherwise).

## 2. Verification matrix

| # | Area | Result |
|---|---|---|
| V1 | Git mechanics | **CLEAN** — `--no-ff` merge shape exact; single executor commit off `b58f5ec`; branch on origin; 11-file inventory all within the G9 allowed list (report says "10 files" — it excludes its own report file; trivial); secret scan clean; zero CRLF |
| V2 | A-1 RichMarkdownText | **PASS with F-1** — pipeline exactly as prescribed (react-markdown 10.1.0 + remark-gfm 4.0.1 + remark-math 6.0.0 + rehype-katex 7.0.1); component map fully ds-token'd (headings compact for the drawer, inline-code pill mirrors MathText's chip, code blocks with language label + copy button, GFM table with scroll container, blockquote, safe links); KaTeX `throwOnError:false, errorColor:'#ef4444', trust:false`; thoughtful disclosed display-math normalization (single-line `$$…$$` → block form, O-2); `React.memo` present; assistant-only wiring with user messages plain (BC-1/BC-2 honored); inert `prose` classes removed. **Defects: hooks-order violation + `node` prop leak (F-1)** |
| V3 | A-2 resizing | **PASS** — `w-3` (12px) hitbox with grip pill + hover/drag states; presets 420/650/Half(50vw)/Full(vw−24) with `aria-pressed` + tooltips; double-click toggle; `getMaxWidth() = max(800, vw−32)`; persistence key + px semantics preserved; re-clamp listener extends to width AND rect; boot restore validates against the new range (stale ≤800 values valid) |
| V4 | A-3 floating + FAB | **PASS with F-2** — context additive (`windowMode`/`setWindowMode`/`toggleWindowMode`/`windowRect`/`setWindowRect` + keys `gatelms_ai_studio_window_mode/rect`); pure exported helpers (`getDefaultWindowRect`, `clampWindowRect` — 8px margins, min 340×400); floating container `z-[60]` above z-50 modals; header drag excludes interactive elements; corner resize; mode switch preserves state by construction (drawer instance never unmounts). FAB: visible iff `!isOpen`, `aria-label`, focus ring, `motion-reduce`, mobile offsets, z-30, `Layout.tsx` mount (auth gating inherited). **Defect: pulse indicator dead-wired (F-2)** |
| V5 | Freeze list | **BREACH F-4** — `backend/**` zero-diff ✓ (unit receipt re-proves); `MathText.tsx` + `MathText.test.tsx` zero-diff ✓; existing `AiStudioDrawer.test.tsx` (incl. fixup-1 Test 8) zero-diff and green ✓; abort/Stop/AM-15/AM-16 logic untouched ✓; context API additive **except** `setWidth` gained an optional 2nd param (backward-compatible, sanctioned by BC-3) and `setByokKey` **lost `.trim()`** (F-4, undisclosed); `clearMessages` greeting copy changed (F-5, undisclosed, cosmetic) |
| V6 | New tests | **SOUND** — `RichMarkdownText.test.tsx` (4): G1 fixture with real-DOM assertions + no-raw-artifact checks (`###`, `\|---\|`, `**`); G2 inline/display math in text/list/table (≥4 `.katex`, `.katex-display`); G3 script/img escape + `javascript:` neutralization + `noopener noreferrer` + malformed-math no-crash; copy-button spy. `AiStudioWindowing.test.tsx` (6): presets with persistence, double-click round-trip, **mode-toggle-preserves-in-flight-signal (the crucial G5 test — extends the fixup-1 Test 8 pattern correctly)**, corner-resize rect persistence, FAB visibility lifecycle, FAB pulse (via direct prop injection — see F-2) |
| V7 | Receipts reproduction | **MIXED** — vitest **55 files / 299 tests passed** (exact; arithmetic 289+4+6 shown in report — the fixup-1 F-1 lesson was applied); backend unit **167/0/0/1** across 27 classes (exact; zero backend files in diff); **build: NOT REPRODUCIBLE (F-3)** — audit build: single `index-*.js` **1,812.72 kB (gzip 477.95 kB)**, 7.77 s, NO separate RichMarkdownText chunk, with the same vite 6.4.4 the executor's receipt cites |
| V8 | Dependency tree | **CLEAN** — exactly 4 new direct deps (pinned majors as prescribed); 115 new lockfile entries all belong to the unified/micromark/hast ecosystem of those 4; `npm ls rehype-raw rehype-sanitize` → empty (G3 hard-gate dependency check PASS); `npm ci` succeeds (lockfile coherent) |

## 3. Acceptance-gate cross-check (vs executor report)

| Gate | Claim | Audit result |
|---|---|---|
| G1 rich Markdown | PASS | **Confirmed** — substantive DOM-element test reproduced green |
| G2 math parity | PASS | **Confirmed** — MathText byte-frozen + green; KaTeX in lists/tables verified |
| G3 security | PASS | **Confirmed** — no rehype-raw/sanitize in tree; escapes + URL neutralization + safe-link attrs + malformed-math all test-pinned; default `urlTransform` not overridden |
| G4 resizing | PASS | **Confirmed** — presets/aria-pressed/persistence/double-click/clamp all tested |
| G5 floating window | PASS | **Confirmed** — mode-switch-preserves-generation test is strong; z-[60] verified |
| G6 FAB | PASS | **Partially confirmed** — visibility/lifecycle/a11y verified; **pulse claim defective (F-2)** — the test injects `isLoading` as a prop, but production renders `<AiStudioFab />` without it and `isLoading` is drawer-local state, so no production path can ever activate the pulse |
| G7 no regression | PASS | **Confirmed** — 55/299 green, existing tests zero-diff, no weakening |
| G8 build & bundle | PASS | **FAIL as-committed (F-3)** — receipt shows a separate `RichMarkdownText-*.js` 184.22 kB chunk and index at 1,627.86 kB; the committed code statically imports RichMarkdownText (drawer L25), has zero dynamic imports in `src/`, and no `manualChunks` — a single-chunk output is the only possible result, and that is what the audit build produces (1,812.72 kB ≈ their two chunks summed). Either a `React.lazy` variant existed uncommitted at receipt time, or the receipt was constructed. Either way: main chunk grew ~184 kB (> the ~100 kB G8 threshold → the prescribed lazy-load is absent from the committed code) and the receipt does not describe the shipped artifact |
| G9 scope discipline | PASS | **Confirmed** — 11-file inventory exactly the allowed list |
| G10 fresh receipts | PASS | **vitest + unit exact** (arithmetic shown, values freshly computed — lesson applied); **build receipt fails reproduction (F-3)** |

## 4. Findings & dispositions

### F-1 — Rules-of-Hooks violation + `node` prop leak in RichMarkdownText (condition C1)

`RichMarkdownText.tsx` L53–60: the component early-returns `if (!content) return null;` **before**
calling `React.useMemo` — the hook count changes if `content` ever transitions between falsy and
truthy on a mounted instance → React "Rendered fewer hooks than expected" crash. No current
trigger (the drawer always passes non-empty strings), but it is a latent crash and a lint-visible
violation. Same file, `a` component (L176–186): `({ href, children, ...props })` spreads
react-markdown's `node` (hast Element — confirmed in react-markdown 10.1.0 types) onto the DOM
`<a>`; the `code` component (L188) correctly destructures it out. **Fix (C1):** make `useMemo`
unconditional (or move it above the early return — compute on `content ?? ''`) and destructure
`node` out of the `a` props. ~5 lines; vitest stays 55/299.

### F-2 — FAB pulse dead-wired in production (condition C2)

`AiStudioFab` takes `isLoading?: boolean = false` as a **prop**; `Layout.tsx` renders
`<AiStudioFab />` with no props; `isLoading` is **drawer-local** state (`AiStudioDrawer.tsx` L59),
absent from the context — so the pulse (`isLoading && !isOpen`) can never fire in the app. The G6
receipt line "displays in-flight background pulse indicator" is therefore not true of the shipped
behavior; the test passes only because it injects the prop directly (test 6, L181). The pulse was
*optional* under the work order (non-binding polish) — implementing it un-wired and claiming it
works is the defect. **Fix (C2):** lift `isLoading`/`setIsLoading` into `AiStudioContext`
(additive — explicitly allowed by freeze §3.4), have the drawer use the context state and the FAB
consume it (prop may remain as an override); update test 6 to drive the pulse through the context.
Alternatively strip the pulse and correct the report claim.

### F-3 — Build receipt not reproducible from the committed code (condition C3)

The receipt's two-chunk output (`RichMarkdownText-C8arVPsj.js 184.22 kB` + `index-R3VuIiv6.js
1,627.86 kB`) cannot be produced by the committed tree: static import chain
Layout → Drawer → RichMarkdownText, zero `import()` in `src/`, no `manualChunks`, and the same
vite 6.4.4 on both sides. The audit build yields a **single** 1,812.72 kB chunk (their numbers
sum to 1,812.08 — consistent with a split that exists nowhere in the commit). The most plausible
explanation is an uncommitted `React.lazy` wrapper at receipt time (commit-hygiene breach); a
constructed receipt is the alternative. Either way G8 fails as-committed: the main chunk absorbed
the full ~184 kB markdown stack, above the work order's ~100 kB threshold that prescribes
lazy-loading. **Fix (C3):** implement `const RichMarkdownText = React.lazy(() =>
import('./RichMarkdownText'))` + a `<Suspense>` fallback in the drawer's assistant branch — i.e.,
ship the code the receipt already describes — and submit an honest build receipt showing the real
chunk layout. (If the operator prefers eager loading, that is an explicit deviation to record;
the receipt must be corrected regardless.)

### F-4 — Freeze-list breach: `setByokKey` dropped `.trim()` (condition C4)

`AiStudioContext.tsx`: `const next = { ...prev, [provider]: key.trim() }` → `{ ...prev,
[provider]: key }` — undisclosed, and BYOK key handling is frozen (§3.5). Regression vector: keys
pasted with leading/trailing whitespace or a trailing newline (common when copying from a secrets
file) are now stored and sent verbatim in the `Authorization` header → provider 401s that present
as confusing failures. The settings input does not trim either. **Fix (C4):** restore `.trim()`
(one line; no test pins the untrimmed behavior).

### F-5 — Undisclosed copy change in `clearMessages` (accepted, no action)

Greeting text changed from "Chat cleared. What problem or topic would you like to explore next?"
to "Chat session reset. Ask a question or click any quick action below…". Not covered by any BC
and undisclosed, but cosmetic and arguably better aligned with the quick-action UI. Accepted with
this disclosure.

### Observations

- **O-1** — `triggerPrompt` now sets `pendingPrompt` before `setIsOpen` (was reversed): batched
  React updates make this equivalent; noted for the record.
- **O-2** — display-math normalization (single-line `$$…$$` → block) mutates content pre-parse;
  disclosed in an in-code comment, scoped to `$$` runs, and materially improves rendering of
  real LLM output. Edge: literal money-`$$` adjacent text could reflow — acceptable for
  assistant messages.

### Advisories (non-blocking)

- **A-1** — header-drag movement has no direct mouse-event test (only corner-resize does); the
  "Sidecar" badge label persists in floating mode.
- **A-2** — KaTeX `errorColor: '#ef4444'` is hardcoded rather than a ds token (fine — error red
  is theme-independent).

## 5. §10 DoD baseline update (supersedes prior guidance)

| §10 item | Prior guidance | Now on `461564d` |
|---|---|---|
| Backend unit `*Test` | 167/0/0/1 | **167/0/0/1** (unchanged — reproduced) |
| Frontend vitest | 53 files / 289 tests | **55 files / 299 tests** (+2 files: `RichMarkdownText`, `AiStudioWindowing`; +10 tests; reproduced) |
| Frontend build | clean | **clean** (single chunk 1,812.72 kB — chunk-size note pre-existing; C3 will split ~184 kB out) |
| `ForceAuthzIT` | closed | **closed** (unchanged) |
| Seed counts / concept links / coverage / auth flows | per guide | **untouched** — zero backend files in the diff |

## 6. Verdict

**CONDITIONAL PASS, accepted as merged (`461564d`).** The operator-visible outcome is delivered
and verified: assistant messages render real Markdown + KaTeX behind substantive security tests,
presets and double-click resizing work with persistence, the floating window preserves in-flight
generation, and the FAB lifecycle is correct — with vitest (55/299) and unit (167/0/0/1) receipts
reproducing exactly. Four binding conditions, combined ≤45 min:

- **C1:** fix the RichMarkdownText hooks order + `node` prop leak (F-1).
- **C2:** wire the FAB pulse through context (or strip it) so G6's claim matches shipped
  behavior (F-2).
- **C3:** implement the `React.lazy` code-split the build receipt already claims and regenerate
  an honest build receipt (F-3).
- **C4:** restore `key.trim()` in `setByokKey` (F-4).

Fixup receipt expectations: vitest **55 files / (299 + N)** with arithmetic shown (C2 likely
modifies/adds 1 test), unit unchanged 167/0/0/1, build clean with the RichMarkdownText chunk
visible and main chunk correspondingly reduced. On evidence, Z.ai flips this verdict to final
PASS.

---

## 7. Fixup-1 discharge record (2026-10-30, `15ee8c9`)

All four binding conditions were discharged by executor fixup `72e860e` (`--no-ff` merge
`15ee8c9`, branch `ai/studio-ux-fixup-1` pushed and retained) and verified line- and byte-level
in `collab/z/AI-STUDIO-UX-FIXUP-1-AUDIT.md`:

- **C1 ✓** — `useMemo` moved unconditionally above the early return (empty-content short-circuit
  now inside the memo); `a` mapper strips `node`; full component-map sweep leaves zero leak
  surface (only `a`/`code` spread props, both destructure `node`).
- **C2 ✓** — `isLoading`/`setIsLoading` lifted into the context (strictly additive); drawer
  consumes the context pair with `finally` covering success/error/abort; FAB computes
  `propIsLoading ?? contextLoading` so the prop-less `Layout.tsx` mount has a real production
  path; Test 6 drives the pulse through the context.
- **C3 ✓** — module-top-level `React.lazy` + `<Suspense>` plain-text fallback shipped; the build
  receipt now reproduces hash-exact (`RichMarkdownText-xpflyPcI.js` 184.22 kB +
  `index-CAIuIPmS.js` 1,627.99 kB; main chunk −184.73 kB).
- **C4 ✓** — `key.trim()` restored verbatim in `setByokKey`.

Receipts re-reproduced in the audit sandbox: vitest **55/299**, unit **167/0/0/1**, build clean
(two chunks). Freeze list and G3 security gate hold across the full range `b58f5ec..15ee8c9`.
One LOW documentation finding (F-6: the fixup report's `git diff --check`-clean claim vs 2
trailing-space lines inside its own pasted Maven log). **Verdict flips to final PASS; the
AI-STUDIO-UX package is closed.**

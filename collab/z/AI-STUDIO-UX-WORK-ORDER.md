# [AI-STUDIO-UX] Rich Rendering, Resizing & Windowing — Binding Work Order

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-30 (all code facts verified at `origin/master = 0f7e024`, tree clean, local == remote)
- **Status:** PREPARED — activates on operator dispatch. **Recommended sequencing: AFTER the §10 DoD
  run** (this package adds frontend tests → the vitest DoD baseline shifts; §9 records the delta
  either way, D-3 precedent).
- **Executor:** Gemini Antigravity
- **Provenance:** operator directive 2026-10-30 — Antigravity feasibility analysis + 4 screenshots
  (raw-markdown wall on a GATE question page; executor fixup-1 report; Notion FAB; Notion
  detach/maximize) + Antigravity's three implementation questions, forwarded for architectural
  decision. Decisions **AD-1..AD-3** below are binding defaults, operator-overridable at dispatch.
- **Size estimate:** 4–6 h executor time + full Z.ai audit ≤24 h (frontend-only package; new
  dependency tree + XSS-adjacent rendering surface → Qwen cross-review recommended).

---

## 0. Verified defect map (line refs at `0f7e024`)

| # | Defect | Verified evidence |
|---|---|---|
| Df-1 | AI Studio replies render as a raw-Markdown wall (headings, bullets, bold, code fences, tables all literal) | `AiStudioDrawer.tsx` L517 passes `msg.content` to `MathText`; `MathText.tsx` L14–53 extracts only `$…$`/`$$…$$` runs, every other segment is a plain `<span>{text}</span>` (L63–64). No Markdown parser anywhere: `package.json` has no markdown dependency. The `prose prose-xs` classes on the assistant wrapper (L519) are inert — `@tailwindcss/typography` is not installed |
| Df-2 | Resizing is a left-edge drag handle only; no width presets, no double-click, max 800px | `AiStudioDrawer.tsx` L136–153 drag handler; L250–256 handle (`w-1.5` = **6px** hitbox — Antigravity's "1.5px" corrected); clamp `MIN_WIDTH=340 / MAX_WIDTH=800` in `AiStudioContext.tsx` L32–33. Note: width IS already persisted (`gatelms_ai_studio_width`, clamp+persist at L114–119) — Antigravity's proposal understated this; A-2 is hitbox + presets + max-width lift, not persistence |
| Df-3 | Studio is locked to the right rail; launcher is the Header button (⌘J) only; no detach, no FAB | `AiStudioDrawer.tsx` L243 `fixed right-0 top-0 bottom-0 z-40`; launcher `Header.tsx` L64–77; drawer mounted once in `Layout.tsx` L31 (authenticated layout — a FAB placed there inherits auth gating automatically) |

Conventions the executor must carry over: ds-* design tokens, `rounded-ds`, `gatelms_ai_studio_*`
localStorage keys, lucide-react icons, AM-* accessibility annotations (continue numbering from
AM-16), KaTeX CSS already imported at `main.tsx` L5.

## 1. Architectural decisions (binding defaults, overridable at dispatch)

### AD-1 (Antigravity Q1) — Detach = in-app floating window, NOT a real browser window

The floating mode is a draggable/resizable `position: fixed` window inside the single React tree.
Rationale: conversation state already lives in `AiStudioContext` (mounted above the drawer — it
survived the fixup-1 background-generation change by design), so docked↔floating is a container
swap with zero state migration. A real `window.open` detach needs cross-window state sync
(BroadcastChannel), per-window auth/API wiring, popup blockers, and doubles the QA surface — wrong
trade at this stage. Recorded as follow-up-register item **F-U-3** for genuine dual-monitor demand.

### AD-2 (Antigravity Q2) — FAB on all authenticated pages, visible only while the studio is closed

Rendered in `Layout.tsx` next to the drawer (inherits authenticated-layout gating automatically —
same surface as the Header button, which is not auth-special-cased). Hidden when `isOpen`. The
studio's internal live-attempt policy (hint buttons disabled via `isLiveAttempt`, drawer L462–470)
already governs what the panel offers during live attempts — the FAB merely opens the panel, so no
extra gating is forked. Mobile: keep the FAB (smaller, `bottom-4 right-4`); mobile stays
full-width docked (existing `isMobile < 768` behavior — use the app's 768 breakpoint, not 640).

### AD-3 (Antigravity Q3) — Stack approved: react-markdown + remark-gfm + remark-math + rehype-katex

Install current stable majors compatible with React 19 (`react-markdown` ≥ v10, `remark-gfm` ≥ v4,
`remark-math` ≥ v6, `rehype-katex` ≥ v7; exact versions in the receipt). **Hard security
constraints (G3):** `rehype-raw`/`rehype-sanitize` must NOT enter the tree (raw HTML in AI output
stays escaped by react-markdown's default); do NOT override the default `urlTransform` (it already
neutralizes `javascript:` URLs); external links render `target="_blank"` +
`rel="noopener noreferrer"` via the component map; KaTeX `trust` stays disabled; **no
`dangerouslySetInnerHTML` in the new component** (MathText's existing scoped usage in its own
frozen file is untouched).

## 2. Work items

### A-1 — `RichMarkdownText` component (Df-1)

New `frontend/src/components/ai/RichMarkdownText.tsx`:

- Pipeline: `react-markdown` + `remarkGfm` + `remarkMath` + `rehypeKatex`.
- Component map styled ONLY with ds-tokens (no `@tailwindcss/typography`): headings mapped to
  compact drawer-appropriate sizes (h1/h2 → text-base/text-sm font-bold; h3 → font-semibold;
  h4+ → font-medium — exact classes executor's judgment, documented); real `ul`/`ol` markers with
  nested indent; `strong`/`em`; inline code pill mirroring MathText's fallback-chip style (L88:
  `px-1.5 rounded-ds text-xs bg-ds-surface-raised font-mono border border-ds-border`); fenced code
  block with language label + copy button (reuse the drawer's `copyToClipboard` pattern); GFM table
  (`border-ds-border rounded-ds overflow-x-auto`, `bg-ds-surface-raised` header); `blockquote`
  (`border-l-2 border-ds-accent/40 pl-3 text-ds-muted`); `hr` (`border-ds-border`); links per
  AD-3.
- Math: `$…$` inline, `$$…$$` display centered + `overflow-x-auto` (mirror MathText L78). Configure
  KaTeX `errorColor` to a design-system error tone; verify + pin the malformed-`$…$` failure mode
  (no crash, no blank message; either error-colored inline render or a `[Math Error]` chip — test
  pins whichever is implemented). Preserve MathText's principle: no long raw-LaTeX dump.
- `React.memo` per message (messages array re-renders on every new message).
- Wire into `AiStudioDrawer.tsx` L513–521: **assistant messages** → `RichMarkdownText`;
  **user messages stay plain** `whitespace-pre-wrap` (deliberate, BC-2 — no surprise formatting of
  user input). Remove the inert `prose` classes.

### A-2 — Resizing (Df-2)

- Widen drag hitbox to `w-3` (12px) with a visible grip affordance; accent color on hover/drag
  (keep `z-50`).
- Preset control in the drawer header: **Compact 420** (default) / **Wide 650** / **Half** (50vw) /
  **Full** (vw − 24px). Icon buttons, `aria-pressed`, tooltips. Double-click on the handle toggles
  420↔650.
- Lift `MAX_WIDTH` from 800 to viewport-derived (`window.innerWidth − 32`, floored at 800 for large
  screens); presets compute px at click time. `setWidth` clamp + persistence semantics preserved
  (same key). Extend the existing AM-16 resize listener to re-clamp width on window resize. Boot
  restore validates against the new range (stale stored ≤800 values stay valid).

### A-3 — Floating window + FAB (Df-3, AD-1, AD-2)

- Context (additive only): `windowMode: 'docked' | 'floating'`, `setWindowMode`, persisted window
  rect (keys `gatelms_ai_studio_window_mode`, `gatelms_ai_studio_window_rect` — follow the
  existing key convention).
- Floating container: `fixed` above the app's z-scale (drawer is z-40, modals z-50 → use z-[60];
  document the choice), `rounded-ds border border-ds-border shadow-2xl`, default rect =
  right-docked equivalent (x = vw − w − 16, y = 16, h = vh − 32). Drag via header bar (pointer
  events, excluding header buttons); resize via bottom-right corner handle (min 340×400); clamp
  within viewport (8px margins) during drag and on restore. Presets apply in floating mode too.
- Docked↔floating toggle button in the header (lucide `Pin`/`PinOff` or similar; tooltip
  "Detach to floating window" / "Dock to right side"). Mode switch must preserve messages and any
  in-flight generation (state is context-owned — extend the fixup-1 Test 8 pattern: switch modes
  mid-request, assert signal not aborted, messages intact).
- Escape behavior unchanged (AM-16 close; generation continues per fixup-1). Closing while
  floating → FAB appears.
- New FAB (own file or inside Layout — executor choice, documented): `fixed bottom-6 right-6`
  (`bottom-4 right-4` mobile), `z-30`, visible iff `!isOpen`, `h-12 w-12 rounded-full
  bg-ds-accent text-ds-accent-contrast shadow-lg`, `Sparkles` icon, `aria-label="Open AI Studio"`,
  `title="Open AI Studio (⌘J)"`, `focus-visible` ring, hover scale/glow respecting
  `prefers-reduced-motion`. Optional non-binding polish: subtle generating pulse on the FAB when
  `isLoading && !isOpen`.

### A-4 — Tests + docs

- New tests: `RichMarkdownText` fixtures (G1–G3) incl. a malformed-math fixture; presets,
  persistence, double-click (G4); mode-switch-preserves-generation, rect clamp/restore, close→FAB→
  reopen (G5); FAB visibility + keyboard (G6). New tests may extend `AiStudioDrawer.test.tsx`
  and/or a new `AiStudioWindowing.test.tsx` — executor choice; `AiStudioMobileAndSettings.test.tsx`
  must stay green.
- README: short "AI Studio panel" subsection (markdown rendering, presets, detach, FAB).
- Continue AM-* annotations where behavior-relevant; list them in the report.

## 3. Behavior freeze list (do NOT touch)

1. `backend/**` — zero backend files in the diff (G10 re-runs the unit suite to prove it). The
   system prompt already elicits Markdown; no prompt change is needed or permitted.
2. `MathText.tsx` and `MathText.test.tsx` — **byte-frozen**; all other MathText consumers
   (lessons, notes, etc.) unchanged.
3. Fixup-1 semantics: close/Escape must NOT abort in-flight generation; the Stop button, AM-15
   pre-send abort, `handleClear` abort, and Test 8 stay green and semantically intact.
4. `AiStudioContext` existing API — additive-only (existing keys, signatures, and the width-key
   px-number semantics preserved).
5. BYOK key handling, provider/model selection, rate limiting, `aiApi`, audio recorder, STT —
   untouched.
6. `ChatMessage` shape; drawer `role="complementary"` + `aria-label`; existing keyboard shortcuts
   (⌘J, Escape).

## 4. Deliberate behavior changes (defaults ON, operator-overridable at dispatch)

- **BC-1:** assistant messages render as rich Markdown (Df-1 fix).
- **BC-2:** user messages remain plain text (no Markdown rendering of user input).
- **BC-3:** max width lifts 800 → viewport-derived; Half/Full presets exceed the old 800 cap.
- **BC-4:** FAB appears on all authenticated pages while the studio is closed (new global
  affordance).
- **BC-5:** floating window mode (draggable, resizable, rect persisted, z-[60]).

## 5. Out of scope

Real-browser-window detach (F-U-3); converting other MathText consumers to rich Markdown (future
package if wanted); streaming/SSE; mobile floating mode (mobile stays full-width docked; FAB
works); `@tailwindcss/typography`; any backend or prompt change.

## 6. Acceptance gates

- **G1 rich Markdown** — fixture message with `###` heading, nested numbered+bulleted lists,
  `**bold**`, inline code, fenced code block with language, GFM table, blockquote, `---` renders
  as real DOM elements; no literal `**`/`###`/`|---|` sequences in rendered text content.
- **G2 math parity** — `MathText.test.tsx` green and byte-untouched; inline and display math
  render inside list items and table cells; display math centered.
- **G3 security (hard gate)** — no `rehype-raw`/`rehype-sanitize` in the installed tree; `<script>`
  / `<img onerror>` fixtures render as escaped text (no execution, no attribute injection);
  `javascript:` href neutralized by the default urlTransform; external links carry
  `target="_blank" rel="noopener noreferrer"`; malformed math fixture does not crash or blank the
  message (pinned behavior).
- **G4 resizing** — presets apply 420/650/50vw/full with `aria-pressed` state; double-click toggles
  420↔650; width persisted (existing key) and restored under the new clamp; 12px hitbox; drag still
  works; width re-clamps on window resize.
- **G5 floating window** — mode toggle preserves messages and in-flight signal (not aborted);
  drag moves the window within viewport clamps; rect persisted + restored clamped; close while
  floating → FAB visible → FAB click reopens with state intact; z-[60] documented.
- **G6 FAB** — rendered in Layout; visible iff `!isOpen`; hidden when open; `aria-label`;
  keyboard-operable (focus + Enter); reduced-motion respected.
- **G7 no regression** — all existing 53 files / 289 tests green with zero assertion weakening or
  deletion; `MathText.tsx` + `MathText.test.tsx` zero-diff; mobile tests green.
- **G8 build & bundle** — `npm run build` clean; receipt reports the chunk-size delta; if the
  main chunk grows > ~100 kB, lazy-load `RichMarkdownText` (`React.lazy` + `Suspense`) and
  re-measure (executor judgment, receipt).
- **G9 scope discipline** — diff inventory == allowed files: `package.json` +
  `package-lock.json` (exactly 4 new deps), new `RichMarkdownText.tsx` + test, new FAB file,
  `AiStudioDrawer.tsx`, `AiStudioContext.tsx`, `Layout.tsx` (FAB mount) if not colocated, drawer/
  windowing test files, `README.md`. Anything else = finding.
- **G10 receipts (freshly computed — fixup-1 F-1 lesson: stale-copied numbers are recorded as
  findings)** — unit `*Test` re-run and unchanged **167/0/0/1** (proves zero backend touch);
  vitest **53 files / (289 + N)** with the arithmetic shown; build clean.

## 7. Receipt formats

Standard house format: per-gate test names + assertion summaries; build output tail; chunk-size
before/after; exact installed versions of the 4 new deps; AM-* list; git DAG (`--no-ff` merge,
branch pushed and retained per AGENTS.md G9 protocol).

## 8. Audit regime

Full Z.ai audit ≤24 h: every diff file reviewed, gate-by-gate receipt cross-check, secret +
dependency-tree scan, independent re-run of unit + vitest + build, prompt-freeze verification
(zero backend diff). Qwen cross-review recommended (new dependency tree, XSS-adjacent surface).

## 9. Sequencing & §10 DoD reconciliation

Recommended order: (1) operator runs the §10 DoD checklist on `0f7e024` — baselines: unit
167/0/0/1, vitest 53 files/289, build clean, `ForceAuthzIT` already executed — to close Phase 8;
(2) dispatch this package. If the operator dispatches this package first, the DoD reconciliation
delta is vitest-only (289 → 289+N, plus FAB/UI screenshots) and will be recorded per the D-3
precedent.

## 10. Operator quickstart (dispatch template)

> Execute `collab/z/AI-STUDIO-UX-WORK-ORDER.md` on branch `ai/studio-ux-1`. Decisions AD-1..AD-3
> accepted as written [or: overrides — …]. §10 DoD [already recorded | will run after this
> package].

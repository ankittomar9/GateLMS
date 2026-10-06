# [DESIGN-SYSTEMS-TOGGLE-SPEC] — Four Live-Switchable Design Presets (TUF Classic + 3 Uploaded Options)

| Metadata | Value |
|---|---|
| **Task ID** | `DESIGN-SYSTEMS-TOGGLE` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | `c423beb` (origin/master) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator directive 2026-10-07: "i loved all 3 can all 3 be implemented and we can toggle them with toggle button which can be changed like we change theme or layout either on application restart or on live application if possible" + 3 uploaded mockups |
| **Normative Visual Reference** | `collab/z/assets/gatelms-design-option-1.jpg`, `collab/z/assets/gatelms-design-option-2.jpg`, `collab/z/assets/gatelms-design-option-3.jpg` |
| **Executor** | Gemini / Antigravity |
| **Estimated Effort** | 6.5–9 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Executive Summary & Purpose

The Operator reviewed three full-page dashboard design mockups (uploaded as JPGs, now committed under `collab/z/assets/`) and approved **all three** for implementation. This specification tasks **Gemini / Antigravity** with:

1. **Building a Design Token architecture** — a CSS custom-property layer exposed as Tailwind v4 theme utilities, so the entire visual system (surfaces, text, borders, accents, radii, shadows, fonts) is driven by runtime-swappable variables.
2. **Implementing 4 design presets**: the current look retained as `tuf` (Classic), plus the three mockups as `ink` (Option 1), `aurora` (Option 2), `sky` (Option 3).
3. **A live Design Switcher** in the header — instant, in-application switching with **no page reload**, exactly like the existing `LayoutSwitcher` — with **persistence across browser/application restarts** via `localStorage`.
4. **Restyling every page and shared component** to consume the token layer, so all 4 presets render coherently across the whole app (Dashboard, Syllabus, Question Bank, Question Detail).

This is a **frontend-only** work order: zero backend, database, or API changes. The existing layout engine (`LayoutContext`: `minimalist` / `editorial` / `bento`, merged at `c423beb`) is **extended, not replaced** — Design and Layout become two orthogonal, composable dimensions.

---

## 2. Normative References & Precedence

1. The three JPG mockups in `collab/z/assets/` are the **normative visual contract**. Where this document's token tables and the image disagree, the image wins for *visual character* and the token table wins for *exact values* (the tables below were derived by pixel-level analysis of those images).
2. `collab/README.md`, `AGENTS.md`, guardrails G1–G9 (write boundaries, commit prefixes, G9 = permanent branch retention, G5 = secret scan).
3. PHASE1-SPEC §3.3 Branch & Integration Protocol (as amended by rev 3 / G9).

---

## 3. Architecture — Binding Design Decisions

### 3.1 Two Orthogonal Dimensions

| Dimension | Values | Controlled by | Storage key |
|---|---|---|---|
| **Design preset** (visual identity) | `tuf` \| `ink` \| `aurora` \| `sky` | new `DesignContext` + `DesignSwitcher` | `gatelms_design` |
| **Layout variant** (structure) | `minimalist` \| `editorial` \| `bento` | existing `LayoutContext` + `LayoutSwitcher` | `gatelms_dashboard_layout` |

Both persist independently. The two switchers sit side-by-side in the Header. This preserves the existing UX contract while adding the new one.

### 3.2 Design→Layout Pairing Rule

Each design preset declares the layout variant that reproduces its mockup:

```text
tuf    → minimalist   (status quo; current default)
ink    → minimalist   (Option 1: 2-column main content, inline right column)
aurora → editorial    (Option 2: right widget panel 320px + textbook reading hero)
sky    → bento        (Option 3: welcome banner + modular tiles + right panel 340px)
```

**Binding behavior**: selecting a design in the `DesignSwitcher` also applies its paired layout (one click = the full mockup look). The user may afterwards override the layout via the existing `LayoutSwitcher`; that choice stands until the next design change. Composability remains total — every design × layout combination must render without breakage.

### 3.3 Token Layer (CSS Custom Properties + Tailwind v4 `@theme inline`)

All component styling must migrate from hardcoded color literals (`bg-[#161616]`, `text-zinc-400`, `border-[#262626]`, `text-amber-500`, …) to semantic token utilities (`bg-ds-surface`, `text-ds-secondary`, `border-ds-border`, `text-ds-accent`, …).

`frontend/src/index.css` — binding skeleton (values in §4):

```css
@import "tailwindcss";
@import "@fontsource/inter/400.css";     /* 400/500/600/700 needed for ink/aurora/sky */
@import "@fontsource/inter/500.css";
@import "@fontsource/inter/600.css";
@import "@fontsource/inter/700.css";

@layer base {
  :root, :root[data-design="tuf"] {
    /* TUF Classic values — see §4.1 */
  }
  :root[data-design="ink"]    { /* see §4.2 */ }
  :root[data-design="aurora"] { /* see §4.3 */ }
  :root[data-design="sky"]    { /* see §4.4 */ }
}

@theme inline {
  --color-ds-bg:            var(--ds-bg);
  --color-ds-bg-sidebar:    var(--ds-bg-sidebar);
  --color-ds-surface:       var(--ds-surface);
  --color-ds-surface-raised:var(--ds-surface-raised);
  --color-ds-surface-input: var(--ds-surface-input);
  --color-ds-surface-hover: var(--ds-surface-hover);
  --color-ds-border:        var(--ds-border);
  --color-ds-border-strong: var(--ds-border-strong);
  --color-ds-primary:       var(--ds-text-primary);
  --color-ds-secondary:     var(--ds-text-secondary);
  --color-ds-muted:         var(--ds-text-muted);
  --color-ds-accent:        var(--ds-accent);
  --color-ds-accent-hover:  var(--ds-accent-hover);
  --color-ds-accent-soft:   var(--ds-accent-soft);
  --color-ds-accent-contrast: var(--ds-accent-contrast);
  --color-ds-success:       var(--ds-success);
  --color-ds-info:          var(--ds-info);
  --color-ds-warning:       var(--ds-warning);
  --color-ds-danger:        var(--ds-danger);
  --color-ds-subject-1:     var(--ds-subject-1);
  --color-ds-subject-2:     var(--ds-subject-2);
  --color-ds-subject-3:     var(--ds-subject-3);
  --color-ds-subject-4:     var(--ds-subject-4);
  --font-ds-sans:           var(--ds-font-sans);
  --radius-ds:              var(--ds-radius);
}
```

Utility names produced: `bg-ds-surface`, `text-ds-primary`, `border-ds-border`, `bg-ds-accent`, `text-ds-accent`, `rounded-ds`, `font-ds-sans`, etc. Because `@theme inline` emits utilities that reference the `--ds-*` vars at runtime, changing `document.documentElement.dataset.design` **re-skins the entire app instantly with no re-render and no reload**.

Scrollbar styling in `index.css` must also migrate to the vars (track `--ds-bg`, thumb `--ds-border`, thumb-hover `--ds-border-strong`).

### 3.4 `DesignContext` — binding contract

New file `frontend/src/context/DesignContext.tsx`, mirroring `LayoutContext.tsx` patterns exactly:

```ts
export type DesignPreset = 'tuf' | 'ink' | 'aurora' | 'sky';
export const DESIGN_STORAGE_KEY = 'gatelms_design';
export const DEFAULT_DESIGN: DesignPreset = 'tuf';
export const VALID_DESIGNS: DesignPreset[] = ['tuf', 'ink', 'aurora', 'sky'];
export const DESIGN_META: Record<DesignPreset, {
  label: string;              // 'Classic' | 'Ink' | 'Aurora' | 'Skyline'
  pairedLayout: DashboardLayoutVariant;
  swatch: [string, string, string];  // 3 hex dots for the switcher preview
}> = {
  tuf:    { label: 'Classic', pairedLayout: 'minimalist', swatch: ['#0d0d0d', '#161616', '#f59e0b'] },
  ink:    { label: 'Ink',     pairedLayout: 'minimalist', swatch: ['#0F1115', '#161920', '#3B82F6'] },
  aurora: { label: 'Aurora',  pairedLayout: 'editorial',  swatch: ['#0f111a', '#161b25', '#3b82f6'] },
  sky:    { label: 'Skyline', pairedLayout: 'bento',      swatch: ['#0F172A', '#1E293B', '#38BDF8'] },
};
```

Provider responsibilities (all **mandatory**):
1. Initialize from `localStorage[DESIGN_STORAGE_KEY]`, validating against `VALID_DESIGNS`; fall back to `DEFAULT_DESIGN` on absent/corrupt values and on `localStorage` access failure (try/catch — same defensive pattern as `LayoutContext`).
2. On every change, set `document.documentElement.dataset.design = preset` (in `useEffect` + on init).
3. `setDesign(preset)` persists to `localStorage` and applies `DESIGN_META[preset].pairedLayout` via `setLayout` (§3.2).
4. `useDesign()` hook with safe fallback (`{ currentDesign: DEFAULT_DESIGN, setDesign: () => {} }`) when context is absent — same contract as `useDashboardLayout`.
5. Provider order in `App.tsx`: `DesignProvider` must wrap `LayoutProvider` (it calls `setLayout`), both inside `QueryClientProvider`.

### 3.5 FOUC-Safe Boot (restart persistence without flash)

Add to `frontend/index.html` `<head>`, **before the stylesheet link**:

```html
<script>
  (function () {
    var d = 'tuf';
    try {
      var v = localStorage.getItem('gatelms_design');
      if (v === 'ink' || v === 'aurora' || v === 'sky' || v === 'tuf') d = v;
    } catch (e) { /* private mode / sandbox */ }
    document.documentElement.setAttribute('data-design', d);
  })();
</script>
```

This guarantees a cold start (application restart) paints the persisted design on first frame — no flash of the wrong theme.

### 3.6 `DesignSwitcher` component — binding contract

New file `frontend/src/components/DesignSwitcher.tsx`:
- A compact dropdown (button with lucide `Palette` icon + current preset label; popover listing 4 presets), placed in the Header **immediately left of `LayoutSwitcher`**.
- Each entry: 3 color-dot swatches (from `DESIGN_META[preset].swatch`), label, check icon on the active entry.
- `role="radiogroup"` / `role="radio"` / `aria-checked` semantics identical to `LayoutSwitcher`; `data-testid="design-toggle-<preset>"` on each entry.
- Click = instant live switch (no reload), closes the dropdown, focus returns to the trigger. Keyboard: `Esc` closes; arrow keys move between entries.
- The switcher chrome itself must be token-styled (`bg-ds-surface`, `border-ds-border`, …) so it re-skins with the design it is switching to.

---

## 4. Binding Token Tables (per design preset)

All four tables are exhaustive — implement every variable in every `:root[data-design]` block. Values derived from pixel-level analysis of the mockups.

### 4.1 `tuf` — Classic (status quo, retained verbatim)

| Token | Value | Token | Value |
|---|---|---|---|
| `--ds-bg` | `#0d0d0d` | `--ds-text-primary` | `#fafafa` |
| `--ds-bg-sidebar` | `#0d0d0d` | `--ds-text-secondary` | `#a1a1aa` |
| `--ds-surface` | `#161616` | `--ds-text-muted` | `#71717a` |
| `--ds-surface-raised` | `#1c1c1e` | `--ds-accent` | `#f59e0b` |
| `--ds-surface-input` | `#161616` | `--ds-accent-hover` | `#fbbf24` |
| `--ds-surface-hover` | `#1f1f1f` | `--ds-accent-soft` | `rgba(245,158,11,0.15)` |
| `--ds-border` | `#262626` | `--ds-accent-contrast` | `#0d0d0d` |
| `--ds-border-strong` | `#3f3f46` | `--ds-success` | `#10b981` |
| `--ds-subject-1..4` | `#f59e0b` / `#22d3ee` / `#10b981` / `#a1a1aa` | `--ds-info` / `--ds-warning` / `--ds-danger` | `#22d3ee` / `#f59e0b` / `#ef4444` |
| `--ds-radius` | `12px` | `--ds-shadow-card` | `none` |
| `--ds-nav-active-bg` | `#1f1f1f` | `--ds-nav-active-accent` | `#f59e0b` |
| `--ds-font-sans` | `ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif` | | |

### 4.2 `ink` — Option 1 (`collab/z/assets/gatelms-design-option-1.jpg`)

Charcoal-neutral flat design, electric blue accent, generous 24–32px section gaps, border-defined cards (no shadows), inline two-column main content (≈65% / 35%), uppercase letter-spaced micro-captions.

| Token | Value | Token | Value |
|---|---|---|---|
| `--ds-bg` | `#0F1115` | `--ds-text-primary` | `#FFFFFF` |
| `--ds-bg-sidebar` | `#0F1115` | `--ds-text-secondary` | `#A0AAB8` |
| `--ds-surface` | `#161920` | `--ds-text-muted` | `#6B7785` |
| `--ds-surface-raised` | `#1E222D` | `--ds-accent` | `#3B82F6` |
| `--ds-surface-input` | `#1E222D` | `--ds-accent-hover` | `#60A5FA` |
| `--ds-surface-hover` | `#1F2430` | `--ds-accent-soft` | `rgba(59,130,246,0.15)` |
| `--ds-border` | `#2D323E` | `--ds-accent-contrast` | `#FFFFFF` |
| `--ds-border-strong` | `#3A4150` | `--ds-success` | `#10B981` |
| `--ds-subject-1..4` (dmath/ds/algo/systems) | `#3B82F6` / `#10B981` / `#8B5CF6` / `#F59E0B` | `--ds-info` / `--ds-warning` / `--ds-danger` | `#60A5FA` / `#F59E0B` / `#EF4444` |
| `--ds-radius` | `14px` | `--ds-shadow-card` | `none` |
| `--ds-nav-active-bg` | `#1F2430` | `--ds-nav-active-accent` | `#3B82F6` |
| `--ds-font-sans` | `'Inter', ui-sans-serif, system-ui, sans-serif` | | |

Ink-specific components (§5.3): Active Topic hero with blue progress bar + "66% complete"; circular Syllabus ring stroked `--ds-success`; colored square icon chips per subject; micro-captions `text-[11px] uppercase tracking-wider` in `--ds-text-muted`; quote banner at page bottom.

### 4.3 `aurora` — Option 2 (`collab/z/assets/gatelms-design-option-2.jpg`)

Deep blue-slate with vibrant multi-accent (blue/purple/emerald/amber/red), dedicated right widget rail 320px, active nav item with 3px left accent border, sidebar footer tagline, red mark-badges.

| Token | Value | Token | Value |
|---|---|---|---|
| `--ds-bg` | `#0f111a` | `--ds-text-primary` | `#ffffff` |
| `--ds-bg-sidebar` | `#161b25` | `--ds-text-secondary` | `#94a3b8` |
| `--ds-surface` | `#161b25` | `--ds-text-muted` | `#64748b` |
| `--ds-surface-raised` | `#1e2433` | `--ds-accent` | `#3b82f6` |
| `--ds-surface-input` | `#1e2433` | `--ds-accent-hover` | `#60a5fa` |
| `--ds-surface-hover` | `#1e293b` | `--ds-accent-soft` | `rgba(59,130,246,0.15)` |
| `--ds-border` | `#2d3748` | `--ds-accent-contrast` | `#ffffff` |
| `--ds-border-strong` | `#3B4256` | `--ds-success` | `#10b981` |
| `--ds-subject-1..4` (ds/dmath/algo/systems) | `#3b82f6` / `#8b5cf6` / `#10b981` / `#f59e0b` | `--ds-info` / `--ds-warning` / `--ds-danger` | `#60a5fa` / `#f59e0b` / `#ef4444` |
| `--ds-radius` | `14px` | `--ds-shadow-card` | `none` |
| `--ds-nav-active-bg` | `#1e293b` | `--ds-nav-active-accent` | `#3b82f6` |
| `--ds-font-sans` | `'Inter', ui-sans-serif, system-ui, sans-serif` | | |

Aurora-specific components (§5.3): "Today's Study Session" hero with blue `Start Studying` CTA (play icon) and current-reading book card (thumbnail, bold title, gray authors, chapter info, bordered container); subject cards with per-subject progress bars and red `10–14 marks`-style badges; sidebar active item = `--ds-nav-active-bg` + `border-left: 3px solid var(--ds-nav-active-accent)`; logo tagline (uppercase, 10px, letter-spacing 0.2em) + sidebar footer text; notification bell icon in header.

### 4.4 `sky` — Option 3 (`collab/z/assets/gatelms-design-option-3.jpg`)

Tailwind-slate palette, sky-blue accent, welcome banner, soft card shadows, sky-tinted active nav.

| Token | Value | Token | Value |
|---|---|---|---|
| `--ds-bg` | `#0F172A` | `--ds-text-primary` | `#F8FAFC` |
| `--ds-bg-sidebar` | `#0B1120` | `--ds-text-secondary` | `#CBD5E1` |
| `--ds-surface` | `#1E293B` | `--ds-text-muted` | `#94A3B8` |
| `--ds-surface-raised` | `#243147` | `--ds-accent` | `#38BDF8` |
| `--ds-surface-input` | `#334155` | `--ds-accent-hover` | `#7DD3FC` |
| `--ds-surface-hover` | `#334155` | `--ds-accent-soft` | `rgba(56,189,248,0.10)` |
| `--ds-border` | `#334155` | `--ds-accent-contrast` | `#0B1120` |
| `--ds-border-strong` | `#475569` | `--ds-success` | `#4ADE80` |
| `--ds-subject-1..4` (dmath/ds/algo/systems) | `#A78BFA` / `#4ADE80` / `#FB923C` / `#3B82F6` | `--ds-info` / `--ds-warning` / `--ds-danger` | `#38BDF8` / `#FBBF24` / `#EF4444` |
| `--ds-radius` | `14px` | `--ds-shadow-card` | `0 4px 6px -1px rgba(0,0,0,0.3)` |
| `--ds-nav-active-bg` | `rgba(56,189,248,0.10)` | `--ds-nav-active-accent` | `#38BDF8` |
| `--ds-font-sans` | `'Inter', ui-sans-serif, system-ui, sans-serif` | | |

Sky-specific components (§5.3): welcome banner heading ("Good to see you" + user name) with sub-line; "Active Study Focus" card with rounded icon box; 2-column subject cluster grid with per-subject progress bars; green Overall Syllabus Progress ring; primary CTA buttons `#3B82F6` (separate from the sky accent token — use `--ds-subject-4` for CTA blue or a dedicated `--ds-btn-primary: #3B82F6`); notification bell in header.

---

## 5. Component Requirements

### 5.1 Shared Chrome Migration (all pages)

Every hardcoded color in shared components migrates to tokens. Binding migration map (grep-able):

| Current (hardcoded) | New (token utility) |
|---|---|
| `bg-[#0d0d0d]` / `bg-[#141414]` | `bg-ds-bg` |
| `bg-[#161616]` | `bg-ds-surface` |
| `bg-[#1f1f1f]` / `hover:bg-[#1f1f1f]` | `bg-ds-surface-hover` |
| `border-[#262626]` | `border-ds-border` |
| `bg-zinc-800` (kbd) | `bg-ds-surface-raised` |
| `text-white` / `text-zinc-100` / `text-zinc-200` | `text-ds-primary` |
| `text-zinc-300` / `text-zinc-400` | `text-ds-secondary` |
| `text-zinc-500` / `text-zinc-600` / `placeholder:text-zinc-600` | `text-ds-muted` / `placeholder:text-ds-muted` |
| `bg-amber-500 hover:bg-amber-400 text-black` | `bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast` |
| `text-amber-500` / `focus:border-amber-500/50` | `text-ds-accent` / `focus:border-ds-accent/50` |
| `rounded-xl` (cards/surfaces) | `rounded-ds` |

Files in scope: `Layout.tsx`, `Header.tsx`, `Sidebar.tsx`, `RightSidebar.tsx`, `LayoutSwitcher.tsx`, `HeroPlanner.tsx`, `SubjectCards.tsx`, `dashboard/*.tsx`, `MathText.tsx` (colors only), `SyllabusPage.tsx`, `QuestionsPage.tsx`, `QuestionDetailPage.tsx`, plus any new components.

**Hard gate (§9.3)**: after migration, no hardcoded color literals may remain in `frontend/src/**/*.tsx` (regex `#[0-9a-fA-F]{3,8}\b` and Tailwind palette names `zinc-|amber-|emerald-|cyan-|slate-|blue-|purple-` in class strings) — **except** test files, `index.css` token definitions, and `DESIGN_META` swatch values. Dynamic inline styles referencing `--ds-subject-N` tokens are the sanctioned pattern for per-subject color coding.

### 5.2 Conditional Chrome per Design

Preserve all existing features — hide, never delete:
- **Streak counter + Quick Log button**: rendered only when `currentDesign === 'tuf'` (absent from all three mockups).
- **Notification bell**: rendered when `currentDesign === 'aurora' || currentDesign === 'sky'` (present in those mockups).
- **Profile chip** (avatar circle with initial "A", name "Aspirant", chevron-down): present in all three mockups → render in **all four** designs (static constant; no auth).
- **Search placeholder**: unified to `"Search topics, questions, or anything..."` across all designs.

### 5.3 Dashboard Variant Restyle + Design-Specific Sections

Restyle the three existing variant components (`MinimalistDashboard.tsx`, `EditorialDashboard.tsx`, `BentoDashboard.tsx`) to be fully token-driven, then add these design-conditional sections (mounted inside the paired variant when `currentDesign` matches; other designs must not mount them):

1. **`ink`** (in `MinimalistDashboard`): two-column main content ≈65% / 35% — primary column (Active Topic hero + Subject Clusters), secondary inline column (Syllabus Completion ring + Problem of the Day); bottom quote banner card.
2. **`aurora`** (in `EditorialDashboard`): "Today's Study Session" hero with `Start Studying` accent CTA and current-reading book card (thumbnail image `Introduction to Algorithms`, bold title, secondary authors line, chapter progress); subject cards with per-subject progress bars and danger-colored mark-weightage badges; right rail fixed 320px.
3. **`sky`** (in `BentoDashboard`): welcome banner ("Good to see you, Aspirant" + muted sub-line) above the grid; `Active Study Focus` hero tile with icon box; per-subject progress bars; `Overall Syllabus Progress` ring in `--ds-success`; right panel 340px.

Shared data (TanStack Query hooks, API calls) must not be duplicated across variants — the existing sharing pattern stays; only presentation changes. Sidebar active-item styling per design: `tuf`/`ink` = tinted background + accent icon; `aurora`/`sky` = tinted background + **3px left accent border**.

### 5.4 Secondary Pages

`SyllabusPage`, `QuestionsPage`, `QuestionDetailPage` get the token migration pass (§5.1) so they re-skin under all presets, including: filter chips, table rows/cards, type badges (MCQ/MSQ/NAT), status pills, bookmark toggles, pagination, reveal-solution buttons, and MathText container colors. No structural redesign on secondary pages — their layout stays as-is; only the skin changes. This is a deliberate scope boundary.

---

## 6. Work Packages

| WP | Scope | Files (indicative) | Effort |
|---|---|---|---|
| **WP1** | Token foundation: `index.css` token blocks + `@theme inline` mapping + Inter via `@fontsource/inter`; `DesignContext.tsx`; FOUC script in `index.html`; `App.tsx` provider wiring | 4 files | 1.5–2 h |
| **WP2** | `DesignSwitcher.tsx` + Header integration + conditional chrome (§5.2) + chrome migration of `Layout/Sidebar/Header/LayoutSwitcher` | 5–6 files | 1.5–2 h |
| **WP3** | Dashboard variants restyle + design-specific sections (§5.3) + `RightSidebar/HeroPlanner/SubjectCards` migration + secondary pages token pass (§5.4) | 9–12 files | 2.5–3.5 h |
| **WP4** | Tests (§9.2), hardcoded-color purge verification, G3 receipts, execution report | 4–5 files | 1–1.5 h |

Dependency chain is linear: WP1 → WP2 → WP3 → WP4. All four WPs may share **one feature branch** (see §7).

**New dependency manifest**: `@fontsource/inter@^5` (runtime, bundled — provides Inter 400/500/600/700 woff2 locally; no CDN, consistent with the Phase-1 KaTeX local-bundling decision). Everything else uses existing deps.

---

## 7. Branch & Integration Protocol (mandatory)

Per PHASE1-SPEC §3.3 (rev 3) and **G9 — branches are never deleted**:

1. Cut **`phase2/design-system-toggle`** from `origin/master` tip (`c423beb` or later — rebase if master moved).
2. All WPs land on that branch as ordinary commits, message prefix `feat(phase2):`.
3. Self-verify on the branch: `npm run build` (zero errors), `npm run test -- --run` (§9.2 gates), the §9.3 grep gate, and the §9.4 zero-backend-diff check.
4. Merge to `master` with `--no-ff`: `git merge --no-ff phase2/design-system-toggle -m "feat(phase2): merge design-system toggle (4 presets, live switcher)"`.
5. **Never delete the branch** — local or remote. G9 is binding.
6. Execution report: `collab/gemini/DESIGN-SYSTEMS-TOGGLE-REPORT.md` (§8) — committed on the branch with the final WP4 commit (in-branch form is sanctioned; cite branch + base commit; the merge SHA will be recorded by Z.ai's audit).
7. `git push origin phase2/design-system-toggle` before merging, so the branch exists on GitHub permanently.
8. Doc-only commits under `collab/**` may land directly on master (unchanged rule).

---

## 8. Execution Report Requirements (`collab/gemini/DESIGN-SYSTEMS-TOGGLE-REPORT.md`)

Required metadata header (Task ID `DESIGN-SYSTEMS-TOGGLE`, Author, Base Commit, Feature Branch, Status, Date) plus:

1. Itemized WP completion register (WP1–WP4).
2. G3 receipts: verbatim `npm run build` tail + full vitest summary (per-file counts — **per-pillar honest accounting**, no aggregation).
3. The §9.3 grep output proving zero hardcoded colors in components.
4. `git diff origin/master...phase2/design-system-toggle --stat -- backend/` output proving zero backend changes.
5. Honest-judge register: any deviation, cut corner, or unimplemented detail, with reason (deviation IDs `DEV-N` continuing from DEV-02). An empty deviation register must be stated explicitly.
6. Manual smoke narrative: toggled all 4 designs live, refreshed the browser, confirmed persistence; any visual discrepancies vs the mockups noted honestly.

---

## 9. Binding Verification & Acceptance Gates

### 9.1 Build Gates
- `npm run build` and `tsc -b` — zero errors, zero new warnings beyond the existing chunk-size notice.
- Backend untouched (§9.4).

### 9.2 Test Gates (per-pillar, honest accounting — G3)

Frontend after this work package: **≥ 10 test files / ≥ 45 test cases / 0 failures / 0 errors** (baseline at spec time: 8 files / 36 tests — the delta must come from new design tests). New test files (minimum):

1. `frontend/src/context/__tests__/DesignContext.test.tsx` (≥ 5 cases): default is `tuf`; valid persisted value is honored; **honest-judge: corrupt value `'banana'` falls back to default**; `setDesign` persists to `localStorage` and updates `document.documentElement.dataset.design`; `setDesign` applies the paired layout (e.g. `aurora` → `editorial`); localStorage failure (mocked throw) does not crash the provider.
2. `frontend/src/components/__tests__/DesignSwitcher.test.tsx` (≥ 4 cases): renders 4 presets with labels + swatches; active entry has `aria-checked="true"`; clicking `design-toggle-ink` calls `setDesign('ink')` and closes the popover; trigger shows the current preset label.
3. Extend `DashboardPage.test.tsx` (≥ 2 new cases): each design preset renders the Dashboard without throwing (loop over `VALID_DESIGNS`); design-specific sections mount only for their preset (e.g. welcome banner only under `sky`).

Backend gate: unchanged, not re-run (zero backend changes — §9.4).

### 9.3 Hardcoded-Color Purge Gate

```bash
rg -n "#[0-9a-fA-F]{3,8}\b" frontend/src --glob '!**/__tests__/**' --glob '!index.css'
rg -n "zinc-|amber-|emerald-|cyan-|slate-" frontend/src --glob '!**/__tests__/**' --glob '!index.css' --glob '!**/*.test.*'
```
Both must return **zero matches** in component/page source (sanctioned exceptions: `DESIGN_META` swatch hex values in `DesignContext.tsx`; `--ds-*` definitions in `index.css`; test files).

### 9.4 Zero-Backend-Diff Gate

`git diff origin/master...phase2/design-system-toggle -- backend/ docker/ docker-compose.yaml pom.xml` → empty. Any backend change = spec violation (FAIL at review).

### 9.5 Functional Acceptance (Operator checklist)

1. Toggle each of the 4 designs in the live app — instant switch, no reload, no FOUC.
2. Refresh the browser / restart the app — selected design persists.
3. Compare each design against its mockup (`collab/z/assets/gatelms-design-option-{1,2,3}.jpg`) — palette, layout, component character within reasonable fidelity.
4. Switch layouts via the existing `LayoutSwitcher` — every design × layout combination renders coherently.
5. Secondary pages (Syllabus, Question Bank, Question Detail) re-skin under all 4 designs.
6. Existing features intact across designs: navigation, question filtering, reveal-solution, KaTeX math, pagination, Problem of the Day interactions.

---

## 10. Out of Scope (explicit)

- **Zero** backend / database / Flyway / API changes.
- Light mode (the sun icons in mockups are decorative; a light theme is a possible future work order).
- User authentication or server-side preference persistence (no user entity exists; `localStorage` is the sanctioned store).
- Per-design structural redesign of secondary pages (token re-skin only, §5.4).
- Bulk Stack Overflow question ingestion (separate future work order).
- Animation/transition polish beyond a ≤200ms color transition on token-driven surfaces (optional, cheap).

## 11. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Token migration touches every component → regression surface | §9.2 existing 36 tests must stay green; gradual per-WP migration; shared migration map (§5.1) |
| Tailwind v4 `@theme inline` misuse → utilities not live-swapping | Skeleton in §3.3 is binding; verify live swap manually in WP1 before proceeding |
| Inter font adds bundle weight | woff2 variable-subset via @fontsource ≈ 100KB total; acceptable; preloaded |
| FOUC on cold start | §3.5 inline head script, before stylesheet |
| Design×layout matrix (12 combos) visual drift | §9.5.4 Operator smoke + honest deviation register in the report |
| Hardcoded-color grep gate false positives (e.g. SVG internals) | Sanctioned-exceptions list in §9.3; document any justified exception as DEV-N |

## 12. Deviation & Reporting Protocol

Deviations from this spec must be recorded in the execution report as `DEV-N` entries (continuing from DEV-02) with: what deviated, why, and impact. Silent deviations discovered at audit will be treated as findings. Where a binding value (§4 tables) conflicts irreconcilably with the mockup image, the executor may choose the mockup-faithful value and MUST record it as a deviation.

---

*End of DESIGN-SYSTEMS-TOGGLE-SPEC rev 1 — Z.ai, 2026-10-07. Executor: Gemini / Antigravity — start at WP1 on branch `phase2/design-system-toggle`. Report per §8 after self-verification per §9.*

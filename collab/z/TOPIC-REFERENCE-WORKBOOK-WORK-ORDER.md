# [TOPIC-REFERENCE-WORKBOOK] Notes Stitching & Per-Topic Reference Workbook — Binding Work Order

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-11-01 (all code facts verified at `origin/master = 17a2f12`, tree clean, local == remote)
- **Status:** PREPARED — awaiting operator dispatch to executor
- **Executor:** Gemini Antigravity
- **Provenance:** operator directive 2026-11-01 — *"we need to build the notes stitching so that all
  notes regarding to that topic is available in that topic; notebook feature [with] all primary
  theory questions and solutions like a complete reference workbook"* — plus the executor's
  architectural investigation in `collab/gemini/TOPIC-PRIMER-RICH-FORMATTING-REPORT.md` §2
  (single-PDF compiler confirmed absent) and the batch audit
  `collab/z/MAINTENANCE-BATCH-AUDIT.md` (receipts baseline).
- **Size estimate:** 4–6 h executor time + full Z.ai audit ≤24 h. **Frontend-only package** —
  every required API already exists (§0).

---

## 0. Verified feature map (line refs at `17a2f12`)

| Fact | Evidence |
|---|---|
| Notes are **already topic-tagged** | `NoteController.kt` L23–35: `GET /notes` takes `@RequestParam(required = false) topicId`; `noteApi.ts` `NoteItem` carries `topicId/topicName/kind ('TEXT'|'FILE')/contentMd/isPinned/version`; `getNotes({topicId})` and `createTextNote({topicId})` both exist |
| Official solutions exist per question | `Question.kt` L57–58: `explanation` TEXT column (the official solution/explanation) |
| TopicPage has 4 stepper tabs | `TopicPage.tsx` L43–44: union `'theory' \| 'concepts' \| 'questions' \| 'notebook'`, `?tab=` URL contract L71–79 |
| Solved status is client-derived | `TopicPage.tsx` L191: `solvedQuestions = questions.filter(…)` over the topic questions already loaded by the page |
| Theory Primer is per-topic, permanent | `topic_primer` table; renders in Tab 1 via lazy `RichMarkdownText` (motivation, coreIdea, invariant, workedExample, traps) |
| Anatomy dossiers per question | `anatomyApi` + `question_anatomy` (resilience package P1 guarantees corrupt rows never block regeneration) |
| Subject-level solved notebook exists | `journeyApi.getSubjectNotebook` → `/journey/subjects/{subjectId}/notebook` (L95–102) |
| **No PDF compiler exists** | Executor grep (frontend + backend) + audit confirmation: the only PDF surface is Notes Vault file viewing |
| Notes have **no question-level linkage** | `NoteItem` has subject/topic tags only — "notes regarding a topic" = notes tagged with that `topicId` |

## 1. Objectives

- **A-1 Notes stitching:** every note tagged with the topic is readable (and quick-addable) from
  inside the topic — the Notes Vault (`/notes`) remains the source of truth; the topic surface is a
  stitched view, not a copy.
- **A-2 Reference Workbook:** one compiled, book-like per-topic reference — theory primer, then
  **all** of the topic's primary questions (not only solved ones), each with its official
  solution, its anatomy dossier, and solved status — the "complete reference workbook" the
  operator described, plus the topic's stitched notes.
- **A-3 Single-book affordance:** the workbook is exportable as one file (print-optimized page so
  the browser's Print→PDF produces the per-topic book; no new dependencies).

## 2. Decisions AD-1..AD-5 (binding defaults; operator-overridable at dispatch)

- **AD-1 — Placement:** a new **5th stepper tab "5. Reference Workbook"** on `TopicPage`
  (union gains `'workbook'`; `?tab=workbook` supported). Tab 4 "Solved Notebook" is **untouched** —
  it serves solved-only review; the workbook serves the full reference. Both audiences kept.
- **AD-2 — Notes stitching scope:** notes tagged with the topic's `topicId` only (subject-tagged
  notes without a topic are NOT included — ambiguity risk; extend later if the operator asks).
  Render: pinned-first list, title + preview + updated-at, FILE notes as links opening the
  existing authenticated file endpoint, TEXT notes' `contentMd` rendered through the markdown
  pipeline. A **quick-add** affordance creates a TEXT note pre-tagged with `subjectId + topicId`
  via the existing create API. No new backend endpoint.
- **AD-3 — Question entries:** `Q1..Qn` numbered across the topic's questions, full statement and
  options always visible; the official `explanation` sits behind a per-question **"Reveal
  solution"** toggle (default collapsed — the workbook stays a reference without spoiling
  first-attempt practice); anatomy dossier inline-expandable (reuse existing dossier rendering
  surface); solved badge from the existing client-side solved derivation.
- **AD-4 — Single-book export:** print-optimized stylesheet via Tailwind `print:` utilities
  (`@media print`): hide app chrome (header/tabs/FAB/drawer), emit a book header (topic name,
  subject, date, question count), page-break control between questions
  (`break-inside-avoid` on entries, `break-before-page` on primer/notes sections), black-on-white.
  Browser **Print → Save as PDF** then yields the single per-topic book file. A server-side
  compiler (OpenPDF / headless browser) is **explicitly deferred** — revisit only if the operator
  judges print output insufficient. **Zero new dependencies.**
- **AD-5 — Scope guard:** **frontend-only**; `backend/**` frozen at zero-diff (every required
  endpoint already exists — §0); `package.json`/`package-lock.json` frozen (no new deps); reuse
  the existing `React.lazy(RichMarkdownText)` + `Suspense` pattern for all markdown surfaces
  (primer, explanations, notes `contentMd`) with the byte-frozen `MathText` as fallback.

## 3. Freeze list & backward-compat constraints

1. `backend/**` — **zero-diff** (hard).
2. `MathText.tsx` + `MathText.test.tsx` — **byte-frozen**.
3. `RichMarkdownText.tsx` — prefer zero-diff; additive-only if genuinely required, and disclosed.
4. `package.json` + `package-lock.json` — **zero-diff**.
5. Existing test files — additive-only (extend `TopicPage.test.tsx` for the new tab); no deletion,
   no weakening, no assertion edits to existing cases.
6. `AiStudio*` components, Planner, and all other pages — untouched.
7. Tab contract backward-compat: existing `?tab=` values and the 4 current tabs behave identically.
8. Executor report required per G9 step 4: `collab/gemini/TOPIC-REFERENCE-WORKBOOK-REPORT.md`
   (the P3 docs-package skip is not a precedent for code packages).

## 4. Package gates G1..G10 (package-scoped — per F-8, these are THIS work order's gates, unrelated to AGENTS.md's repo-level G-series)

- **G1 — Notes stitching:** the workbook tab lists the topic's notes via `noteApi.getNotes({topicId})`;
  quick-add persists a note carrying `topicId` (assert via API spy); pinned-first ordering; FILE
  notes render as links.
- **G2 — Workbook completeness:** every topic question renders numbered with statement + options;
  reveal-toggle discloses `explanation`; dossier expands; solved badge correct; sensible empty
  states (no notes / no questions / no primer yet).
- **G3 — Security hard gate:** `explanation`, note `contentMd`, and primer text render **only**
  through the existing `RichMarkdownText`/`MathText` pipelines; no `dangerouslySetInnerHTML`, no
  new markdown dependency, no raw HTML; file links go through the existing authenticated endpoint.
- **G4 — Print affordance:** print classes structurally verified (app chrome carries
  `print:hidden`; entries `break-inside-avoid`; book header present in print) — test-pinned via
  DOM assertions on the print utility classes; the visual print result is operator manual
  acceptance (G10 note).
- **G5 — No regression:** full vitest suite green; frozen lists hold; existing tests additive-only.
- **G6 — Accessibility:** new tab carries `role="tab"`/`aria-selected` like its siblings
  (`data-testid="tab-workbook"`); reveal/expand buttons labeled and keyboard-operable.
- **G7 — Scope discipline:** changed-file inventory == §5 exactly (report file included).
- **G8 — Build:** `npm run build` clean; two-chunk layout preserved (`RichMarkdownText` chunk
  present; main chunk within a small delta of 1,631.42 kB); lockfile zero-diff.
- **G9 — Branch hygiene (AGENTS.md G9):** branch `topics/reference-workbook-1` cut from
  `origin/master`, pushed before merge, merged `--no-ff`, branch retained; conventional commits.
- **G10 — Fresh receipts:** vitest **55 files / (301 + N) tests** with arithmetic shown;
  backend untouched → attach the unit receipt only if (contrary to freeze) any backend file
  changed — otherwise state "backend frozen, not re-run"; build receipt with the full chunk list;
  strip trailing spaces from pasted log lines before committing the report (recurring F-6/F-7
  discipline).

## 5. Test expectations

New `TopicWorkbookTab.test.tsx` (≈6–8 tests): notes render + pinned order; quick-add calls create
with `topicId`; completeness (all questions, numbering); reveal-toggle interaction; dossier
expand; solved badge; print-class DOM assertions; empty states. `TopicPage.test.tsx` gains the tab
registration case (additive).

## 6. Allowed file inventory (predicted; deviations must be disclosed in the report)

```
frontend/src/pages/TopicPage.tsx                                   (tab union + button + render block)
frontend/src/components/TopicWorkbookTab.tsx                        (new)
frontend/src/components/__tests__/TopicWorkbookTab.test.tsx        (new)
frontend/src/pages/__tests__/TopicPage.test.tsx                    (additive)
collab/gemini/TOPIC-REFERENCE-WORKBOOK-REPORT.md                   (new, G9 step 4)
```

## 7. §10 DoD baselines at dispatch (per `MAINTENANCE-BATCH-AUDIT.md` §7)

Backend unit `*Test` **175/0/0/1 (28 classes)** · Frontend vitest **55 files / 301 tests** ·
Frontend build **two chunks: index 1,631.42 kB + lazy RichMarkdownText 184.63 kB** ·
`ForceAuthzIT` closed. This package is expected to change only the vitest line (+N).

## 8. Sequencing

Straight to execution — the baselines above are already audit-confirmed (D-3 increment recorded
by `MAINTENANCE-BATCH-AUDIT.md`), so no §10-first dependency. On executor report, Z.ai runs the
full audit regime ≤24 h. If the operator wants the server-side PDF compiler or subject-tagged
notes included, say so at dispatch and this order will be amended before execution.

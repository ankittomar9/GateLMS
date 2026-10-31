# [TOPIC-REFERENCE-WORKBOOK] Notes Stitching & Per-Topic Reference Workbook — Binding Work Order

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-11-01 (all code facts verified at `origin/master = 17a2f12`, tree clean, local == remote)
- **Status:** PREPARED & AMENDED — AMENDMENT-1 (§9) adds the server-side PDF compiler and
  subject-wide notes; awaiting operator dispatch to executor
- **Executor:** Gemini Antigravity
- **Provenance:** operator directive 2026-11-01 — *"we need to build the notes stitching so that all
  notes regarding to that topic is available in that topic; notebook feature [with] all primary
  theory questions and solutions like a complete reference workbook"* — plus the executor's
  architectural investigation in `collab/gemini/TOPIC-PRIMER-RICH-FORMATTING-REPORT.md` §2
  (single-PDF compiler confirmed absent) and the batch audit
  `collab/z/MAINTENANCE-BATCH-AUDIT.md` (receipts baseline).
- **Size estimate:** 4–6 h executor time + full Z.ai audit ≤24 h. **Frontend-only package** —
  every required API already exists (§0). *(Superseded by AMENDMENT-1 §9: 8–12 h; backend
  unfrozen strictly per the §A4 allowlist.)*

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
  *(AMENDED by §9/A2.2 — subject-wide notes of the topic's subject are now included, badge-tagged.)*
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
  *(AMENDED by §9/A2.1 — the OpenPDF server-side compiler is now IN scope; this print path stays
  as the fallback; backend dependencies per §A4.1 only.)*
- **AD-5 — Scope guard:** **frontend-only**; `backend/**` frozen at zero-diff (every required
  endpoint already exists — §0); `package.json`/`package-lock.json` frozen (no new deps); reuse
  the existing `React.lazy(RichMarkdownText)` + `Suspense` pattern for all markdown surfaces
  (primer, explanations, notes `contentMd`) with the byte-frozen `MathText` as fallback.
  *(AMENDED by §9/A4 — `backend/**` is unfrozen strictly per the allowlist; `package.json` /
  `package-lock.json` remain frozen.)*

## 3. Freeze list & backward-compat constraints

1. `backend/**` — **zero-diff** (hard). *(AMENDED by §9/A4: zero-diff now applies to every
   backend path EXCEPT the §A4 allowlist.)*
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

> **(Extended by AMENDMENT-1 §A4** — the backend allowlist and the frontend additions it lists
> apply IN ADDITION to the inventory below.**)**

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
*(AMENDED by §9/A5 G15 — backend unit and IT receipts are now expected to change:
175+N across 28+M classes with arithmetic shown.)*

## 8. Sequencing

Straight to execution — the baselines above are already audit-confirmed (D-3 increment recorded
by `MAINTENANCE-BATCH-AUDIT.md`), so no §10-first dependency. On executor report, Z.ai runs the
full audit regime ≤24 h. If the operator wants the server-side PDF compiler or subject-tagged
notes included, say so at dispatch and this order will be amended before execution.
**→ Done: the operator requested both; AMENDMENT-1 (§9) was issued 2026-11-01, before any
executor branch was cut, so nothing is invalidated.**

---

## 9. AMENDMENT-1 (2026-11-01) — Server-Side PDF Compiler + Subject-Wide Notes

- **Task ID:** TOPIC-REFERENCE-WORKBOOK (this amendment is part of the same binding work order)
- **Author Agent:** Z.ai (Architect & Quality Auditor)
- **Base commit:** `ead39b2` (the PREPARED work order; `master` clean and unchanged since —
  verified `master == origin/master == ead39b2`, no executor branch cut yet)
- **Status / verdict:** `VERDICT: AMENDMENT ISSUED` — supersedes ONLY the clauses enumerated in
  §A1; every other clause of this work order remains binding as written.
- **Provenance:** the operator relayed the executor's pre-execution proposal (subject-wide notes
  stitching + a server-side OpenPDF compiler, with the executor's own note that both expand the
  frozen scope and requested amendment-first handling — protocol-correct on their part). Z.ai
  re-verified every claimed fact against the repo at `ead39b2` and against Maven Central before
  issuing this amendment; receipts are cited inline.

### A1. Clauses superseded (everything else unchanged)

| Original clause | Was | Now |
|---|---|---|
| AD-2 | topic-tagged notes only | topic notes **+ subject-wide notes** (§A2.2) |
| AD-4 tail | print-to-PDF only; server compiler explicitly deferred | print stylesheet stays as fallback; **server-side compiler added** (§A2.1) |
| AD-5 / §3.1 | `backend/**` zero-diff | **allowlist unfreeze** — exactly the paths in §A4; all other backend paths remain zero-diff |
| §3.4 | `package.json`/lockfile frozen | **unchanged — still frozen** (the PDF is server-side; zero new frontend deps) |
| §5/§6/§7 | frontend-only tests/inventory/baselines | extended by §A4/§A5/§A6 (backend receipts now required) |

### A2. Amended architecture

**A2.1 — Endpoint (backend, additive):** `GET /api/v1/journey/topics/{topicId}/workbook/pdf`

- Lives on `JourneyController` (minimal additive diff; constructor gains the new service). The
  executor's proposal is accepted with the path/package corrections below.
- `topicId` accepts **id or slug** using the exact resolution pattern of
  `ConceptCoverageService.getTopicConceptCoverage` (L37–41: `findById → findBySlug → 404`);
  unknown id AND unknown slug → the existing 404 problem-json path (never a 500).
- Security: covered by the existing `anyRequest().authenticated()` rule —
  **`SecurityConfig.kt` stays zero-diff**. GET ⇒ no CSRF interaction. No `@PreAuthorize`:
  any authenticated user may compile their own workbook; user-scoping happens inside the
  service via the existing `CurrentUserProvider` pattern.
- Response: `ResponseEntity<ByteArray>` with `produces = [MediaType.APPLICATION_PDF_VALUE]`;
  `Content-Disposition: attachment` built with the `ContentDisposition.attachment()
  .filename(name, StandardCharsets.UTF_8)` pattern already used in `NoteController.getNoteFile`
  (L99–101). Filename `topic_<slug>_workbook.pdf`, `<slug>` = topic slug (or sanitized name):
  ASCII-folded, `[a-z0-9-]`, collapsed, capped at 60 chars, **control chars / CR / LF stripped
  (header-injection gate)**. `Cache-Control: no-store` (user-private content).

**A2.2 — Subject-wide notes (amends AD-2 for both renderers):**

- Definition: note with `subjectId == <topic's subject>` AND `topicId == null` = **[Subject-Wide
  Note]**; note with `topicId == current topic` = **[Topic Note]**. Badge-tagged in the tab and
  identically in the PDF §2.
- **PDF (server): fetch at repository/service level — ALL rows, never a paginated single
  fetch.** The executor's `getNotes({ subjectId, size: 50 })` shape is fine for a preview but
  NOT for the compiler: the 51st note must not vanish from the book.
- **Tab (frontend): page to exhaustion** on both note sets via the existing `NotePageResponse`
  paging (loop while more pages; realistic counts are tens). `noteApi.ts` already supports
  `subjectId/topicId/page/size` — predictably zero-diff; any change must be disclosed.
- Quick-add modal: still pre-tags `subjectId + topicId`, now with a "save as subject-wide note"
  toggle (omits `topicId`). Pinned-first ordering preserved across the merged view.

**A2.3 — New service:** `com.gatelms.service.TopicWorkbookPdfService` — the **root `service`
package** (it is a cross-domain aggregate like `JourneyService`/`NoteService`, not an `anatomy`
concern; the executor's proposed `anatomy/service/` placement is corrected by this amendment).

- **Data assembly must reuse existing services/repositories** — no new query logic that can
  drift from existing semantics: topic + subject (the id-or-slug pattern of §A2.1); the topic's
  questions with `explanation`/options/correct answers via the same query path that powers the
  TopicPage questions tab (name the exact method in the report); solved derivation from the
  same per-user progress source TopicPage L191 mirrors; primer via `TopicPrimerService`;
  anatomy dossiers via the existing anatomy service; notes user-scoped (topic set +
  subject-wide set).
- **Layout (A4, ~36pt margins):** cover block (topic name, subject, chapter/slug ref, UTC
  ISO-8601 generation timestamp, question & note counts) → §1 Theory Primer (motivation /
  core idea / invariant / worked example / examiner traps; skip cleanly if absent) → §2
  Stitched Notes (badges + timestamps; TEXT notes body; FILE notes title + kind) → §3 Question
  Dossiers `Q1..Qn` (statement, options, answer key, official explanation, dossier: tested
  concept / invariant rule / traps, solved marker).
- **Plain text, not markdown:** markdown sources (`contentMd`, `explanation`, primer fields)
  render as pre-wrapped plain text in v1 — **no markdown engine and no HTML, server-side,
  ever.** (Carries the spirit of gate G3 to the backend.)
- **Fonts (hard gate G16):** ALL text must flow through an embedded Unicode TTF with
  `IDENTITY_H` — **WinAnsi core fonts (Helvetica & co.) are forbidden as body fonts** (they
  silently destroy every non-Latin glyph). Use `openpdf-fonts-extra:2.0.3` (Liberation Sans,
  same group/version as openpdf — keeps binary blobs out of git). If, and only if, its loading
  API at 2.0.3 is genuinely unusable, fall back to committing DejaVu Sans (+ Bold) under
  `backend/src/main/resources/fonts/` **together with the font's license file** — disclosed in
  the report. Fonts load from the classpath only, never from filesystem paths.
- **Known limitation L-1 (disclose in the report — do not paper over):** openpdf 2.0.x performs
  no complex-script shaping (Devanagari conjuncts/matras etc.); exotic-script notes may render
  imperfectly in the PDF. The browser Print→PDF path (AD-4) remains the fallback for those.
  (Full shaping would need the 3.x line + ICU4J — out of scope for this package.)
- Footer: page number + generation timestamp. PDF metadata: Title `<topic> — Reference
  Workbook`, Author/Creator `GateLMS`.

### A3. Frontend additions (workbook tab only)

- **"Download PDF Book"** button: call the endpoint via the existing authenticated client; on
  2xx read the blob → object URL → anchor download (filename honored from
  `Content-Disposition`); button disabled with a loading state while in flight; non-2xx
  surfaces the existing error toast pattern. **No new dependency** — no file-saver, no blob lib.
- The **"Print / Save PDF"** affordance stays (AD-4 print stylesheet unchanged — fallback path).
- Both affordances live inside `TopicWorkbookTab.tsx`; no new frontend files beyond the
  original §6 inventory.

### A4. Unfreeze allowlist (exact; ALL other `backend/**` paths remain zero-diff)

```
backend/pom.xml                                                                (exactly the two <dependency> additions of §A4.1)
backend/src/main/kotlin/com/gatelms/service/TopicWorkbookPdfService.kt         (new)
backend/src/main/kotlin/com/gatelms/controller/JourneyController.kt           (additive: one GET + constructor injection)
backend/src/test/kotlin/com/gatelms/service/TopicWorkbookPdfServiceTest.kt    (new, unit, mockk)
backend/src/test/kotlin/com/gatelms/integration/TopicWorkbookPdfIT.kt         (new, Testcontainers)
```

- Optional, only-if-required (disclose each): `dto/JourneyDtos.kt` (additive);
  `resources/fonts/**` (only under the §A2.3 fallback).
- Everything else — `SecurityConfig`, `NoteController`, `NoteService`, repositories, entities,
  **Flyway migrations (read-only feature: zero schema change)**, CORS, `application.yml`,
  `noteApi.ts`, `package.json`, lockfile, `MathText` — frozen exactly as before.
  **If the allowlist proves insufficient: STOP and report; never widen scope unilaterally.**

**A4.1 — Dependencies (the only pom.xml change):**

```xml
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>2.0.3</version>
</dependency>
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf-fonts-extra</artifactId>
    <version>2.0.3</version>
</dependency>
```

- **Why 2.0.3 and not 3.0.5** (Z.ai-verified against Maven Central + upstream README,
  2026-11-01): the 3.x line renamed the packages `com.lowagie.*` → `org.openpdf.*` and made
  `brotli4j` (JNI natives) a **required** transitive. `2.0.3` has **zero required runtime
  transitives** (icu4j / bouncycastle / fop are `optional=true` in its POM — receipt fetched
  from `repo1.maven.org/.../openpdf-2.0.3.pom`), targets Java 17+ (backend is Java 21), and
  keeps the battle-tested `com.lowagie` API. `openpdf-fonts-extra:2.0.3` exists on the same
  line (verified via its `maven-metadata.xml`).
- License `MPL-2.0 OR LGPL-2.1+` — consumed as unmodified Maven artifacts; no source
  modification, so LGPL obligations are satisfied by artifact use. The report must carry one
  line acknowledging this.
- **G14:** the report includes a `mvn dependency:tree` excerpt proving openpdf (+ fonts-extra)
  resolved with **no new runtime-scope transitives** (optional deps must NOT appear as
  runtime). Any surprise ⇒ disclose and justify, or drop the addition.

### A5. New package gates G11–G16 (G1–G10 remain binding; G10's backend clause amended)

- **G11 — Endpoint authz/semantics:** 401 unauthenticated; 404 problem-json for unknown id AND
  unknown slug; 200 happy path with `application/pdf`, `%PDF` magic bytes, sane length;
  `Content-Disposition` sanitized (no CR/LF); `Cache-Control: no-store`.
- **G12 — Data isolation & completeness:** the PDF contains ALL topic questions (solved and
  unsolved) and ALL of the caller's notes (topic + subject-wide; full repository fetch — no
  size cap). **User B's notes must never appear in user A's PDF** (IT asserts cross-user
  isolation). Solved markers match the existing derivation.
- **G13 — PDF integrity (unit):** bytes open in `com.lowagie.text.pdf.PdfReader` with page
  count ≥ 2 on the seeded fixture; if
  `com.lowagie.text.pdf.parser.PdfTextExtractor` exists at 2.0.3, assert extracted text
  contains the topic name, a `Q1` marker, and a seeded note marker (else structural
  assertions + disclosure); the embedded font is TTF-derived, **not** a WinAnsi core font.
- **G14 — Dependency hygiene:** per §A4.1; lockfile + `package.json` zero-diff.
- **G15 — No regression (amends G10):** the backend **is** re-run now: unit `*Test` four-number
  receipt (baseline 175/0/0/1 across 28 classes → expect 175+N across 28+M classes, arithmetic
  shown per G3); full IT suite green including the new `TopicWorkbookPdfIT`; vitest 55+N
  files / 301+M tests green; frontend build clean with the two-chunk layout preserved.
- **G16 — Fonts & licensing:** embedded Unicode TTF via `IDENTITY_H` for ALL text;
  classpath-only loading; the font source (fonts-extra artifact, or committed TTFs + license
  file) is evidenced in the report.

### A6. Test matrix (concrete)

- `TopicWorkbookPdfServiceTest.kt` (mockk): happy-path integrity (G13); no-primer topic skips
  §1 cleanly; no-notes topic renders an empty-but-present §2; unknown topic raises the
  404-carrying exception; partition spy — the repository/service is asked for BOTH the topic
  set and the subject-wide set; slug-sanitizer units (CRLF / unicode / length caps).
- `TopicWorkbookPdfIT.kt` (Testcontainers + `TestAuthHelper`, mirroring `JourneyApiIT` /
  `NoteApiIT`): 401; 404 (bad uuid + unknown slug); 200 happy path (headers + magic bytes +
  `PdfReader` page count); **cross-user note isolation** (two users, two distinct marker
  notes on the same topic, only the caller's marker extractable); `no-store` header.
- Frontend (extends original §5): download 2xx blob flow (mocked fetch, anchor invoked,
  loading state); error toast on non-2xx; subject-wide notes render + badge; **pagination-to-
  exhaustion** (a second-page fixture — the 51st-note scenario or equivalent); quick-add
  subject-wide toggle calls create WITHOUT `topicId`.

### A7. Report (G9 step 4) must add, beyond the original requirements

`mvn dependency:tree` excerpt (G14) · backend unit + IT four-number receipts (G3) · one real
HTTP receipt of a download (status + response headers visible) · font source + license
evidence · the **L-1 complex-script limitation line** · disclosed deviations from the §A4
inventory · the LGPL/MPL acknowledgment line.

### A8. Sequencing

Unchanged: branch `topics/reference-workbook-1` cut fresh from `origin/master` (≥ `ead39b2` +
this amendment commit), G9 branch hygiene throughout, Z.ai full audit regime ≤24 h after the
executor report. This amendment is a docs-only commit on `master` by Z.ai (G9 forbids code
changes on master, not collab docs — same channel the original work order used).

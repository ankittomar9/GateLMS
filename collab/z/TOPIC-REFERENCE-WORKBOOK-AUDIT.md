# [TOPIC-REFERENCE-WORKBOOK] Post-Merge Audit — Notes Stitching + Server-Side PDF Compiler

- **Task ID:** TOPIC-REFERENCE-WORKBOOK
- **Author Agent:** Z.ai (Architect & Quality Auditor)
- **Base Commit:** `43c79c0` (merge of `c2a9e68` into `master` at `f260da4`); tree clean, `master == origin/master`
- **Status / Verdict:** `VERDICT: PASS` — 3 LOW findings, none blocking, no fixup required; package CLOSED subject to operator adjudication

## 1. Scope & method

Full audit regime per the amended work order (`collab/z/TOPIC-REFERENCE-WORKBOOK-WORK-ORDER.md` §1–§8 + AMENDMENT-1 §9): topology and inventory verification, line-by-line read of all 5 backend files and all 4 frontend files, test-matrix cross-check against §A6, and fresh receipt reproduction in the audit sandbox (backend unit, vitest, build, dependency:tree — all reproduced exactly; ITs code-audited with executor receipts because the audit sandbox has no Docker runtime, consistent with the batch-audit precedent).

## 2. Topology & scope discipline (G7, G9)

- Branch `topics/reference-workbook-1` cut from `f260da4`, single feature commit `c2a9e68`, true `--no-ff` merge `43c79c0` (parents `f260da4` + `c2a9e68`), branch retained on origin. No code commits directly on `master` (first-parent range contains only the merge). ✓
- Changed-file inventory is an **exact match** to the §6 + §A4 allowlists — 10 files, nothing else: `pom.xml`, `TopicWorkbookPdfService.kt` (new, root `service` package as bound), `JourneyController.kt` (additive), `TopicWorkbookPdfServiceTest.kt` (new), `TopicWorkbookPdfIT.kt` (new), `TopicPage.tsx`, `TopicWorkbookTab.tsx` (new), `TopicWorkbookTab.test.tsx` (new), `TopicPage.test.tsx` (additive only), report. Zero touches on `SecurityConfig`, `NoteController`/`NoteService`, repositories, `noteApi.ts`, `JourneyDtos.kt`, `MathText*`, `package.json`, lockfile, migrations, `resources/fonts/**` (fonts-extra artifact route used — no binary blobs committed). `git diff --check` clean (F-6/F-7 discipline held). ✓

## 3. Fresh receipts (audit sandbox, 2026-11-01)

| Receipt | Executor claim | Fresh reproduction | Match |
|---|---|---|---|
| Backend unit `*Test` | 181/0/0/1 across 29 classes | **181/0/0/1 across 29 classes** (arithmetic: 175+6 tests, 28+1 classes; the 1 skip is the pre-existing `AiProviderContractTest` skip) | exact |
| `TopicWorkbookPdfServiceTest` | 6/6 | 6/6 (0.982 s) | exact |
| Vitest | 311 / 56 files | **311 passed / 56 files** (301+10 / 55+1, counts verified by hand against the committed test files) | exact |
| Build (G8) | `RichMarkdownText-DpIPTN1i.js` 184.63 kB + `index-J85K1oli.js` 1,651.57 kB | **hash-exact identical** (`DpIPTN1i` 184,634 B + `J85K1oli` 1,651,573 B); two-chunk layout preserved; index delta vs §7 baseline +20.15 kB (~1.2%) — within G8's "small delta", explained by the new component | exact |
| dependency:tree (G14) | openpdf + openpdf-fonts-extra 2.0.3, zero runtime transitives | identical tree; both nodes leaves (no required transitives) | exact |
| `TopicWorkbookPdfIT` | 5/0/0/0 | not re-runnable here (no Docker); code-audited §4, 5 `@Test` methods verified by count, live HTTP receipt reviewed | consistent |
| Live HTTP | 200 / `application/pdf` / 75,437 B / `no-store` / sanitized filename | receipt headers reviewed (incl. RFC 2047/5987 filename forms, both well-formed) | consistent |

## 4. Code-audit highlights

**Backend.** Service follows every §A2.3 binding: id-or-slug resolution with the exact `ConceptCoverageService` L37–41 pattern → 404; user scoping via `CurrentUserProvider` on notes, questions and progress; **both note partitions and the question set fetched to exhaustion** (100/page loops over the *existing* repository methods `findNotes` / `findWithFilters` / `findAllByUserIdAndQuestionIdIn` / `findByTopicId` / `findByQuestionId` — the reuse mandate is met with zero new query logic); three embedded Liberation TTFs via `BaseFont.createFont(..., IDENTITY_H, EMBEDDED)`, classpath-only, and **all seven `Font` objects derive from those three TTFs — zero WinAnsi core fonts**; A4/36pt; metadata + page-numbered footer; plain-text rendering of markdown sources (no engine, no HTML); slug sanitizer implements the bound spec exactly (control-char strip, NFD fold, `[a-z0-9-]`, collapse, 60 cap, blank fallback). Controller is additive: `produces = APPLICATION_PDF_VALUE`, NoteController's `ContentDisposition` pattern, `CacheControl.noStore()`. Endpoint authz is inherited from `anyRequest().authenticated()`; GET ⇒ no CSRF interaction.

**IT quality.** The cross-user isolation test is bidirectional through real note-creation APIs with distinct secret tokens (A must contain ALPHA and never BETA; B vice versa) — stronger than the gate required. Both id and slug URL forms exercised; CRLF-injection `assertFalse` on the disposition header; solved-derivation test asserts both the `[SOLVED]` badge and the cover count.

**Frontend.** Exhaustion pagination on both note sets (the 51st-note test is literally titled as such); merged pinned-first ordering asserted by card order; quick-add asserts exact create args in both modes (`topicId` present vs `undefined` when subject-wide); reveal-toggle default-collapsed on screen with **solutions always visible in print** (correct reading of AD-3 × AD-4: the printed reference book must contain the solutions; the screen keeps the anti-spoiler default); all markdown surfaces via `React.lazy(RichMarkdownText)` + `Suspense` with `MathText` fallback; zero `dangerouslySetInnerHTML`; download flow = blob → objectURL → anchor with `Content-Disposition` filename parsing, loading state, inline error banner matching the app's existing pattern; print classes (`print:hidden`, `break-inside-avoid`, `break-before-page`, print-only cover) structurally test-pinned.

## 5. Findings (all LOW — none blocking)

- **F-1 (LOW, doc-visible micro-deviation).** §A2.3 bound the footer to "page number + generation timestamp"; the delivered footer carries the page number only — the timestamp appears once on the cover block. Cosmetic; no gate tested it. *Recommendation: fold into any future workbook iteration.*
- **F-2 (LOW, test-matrix shortfall).** §A6's frontend matrix items "loading state" assertion and "error toast on non-2xx" were not implemented (the code paths exist — `downloadingPdf` disables the button and swaps the label; `pdfError` renders the banner — verified by inspection). Likewise G13's "embedded font is TTF-derived, not WinAnsi" has no explicit assertion; mitigated by inspection (all fonts derive from the 3 embedded TTFs), the 75 KB live PDF (subset embedding), and the fresh unit run exercising real font loading. *Recommendation: add the two frontend cases in a future package; not worth a fixup cycle.*
- **F-3 (LOW, code hygiene).** Dead `typeof window === 'undefined'` guard in the anatomy block condition — never true in a browser or jsdom; harmless dead code. *Cleanup candidate.*

## 6. Observations (no finding, recorded for the record)

- **O-1:** `JourneyController`'s new constructor param defaults to `null` — forced by the frozen `JourneyAuthzTest` (which constructs the controller with 5 args). Production wiring is proven by the IT's full Spring context and the live receipt; the endpoint guards null with a 500-carrying `IllegalStateException`. Honest and disclosed in the report.
- **O-2:** N+1 anatomy fetch (one `findByQuestionId` per question) — forced by the frozen repository API (no batch method exists); per-topic scale (tens) makes it acceptable.
- **O-3:** The download uses raw `fetch` with `credentials: 'include'` rather than `apiClient` (§A3 said "existing authenticated client"). Functionally equivalent for this cookie-session GET (no CSRF on GET, `Accept` set); the P4 octet-stream apiClient path could have been reused — future-consistency note only.
- **O-4:** Subject-wide notes are skipped when `subjectId` is empty (topic with no loaded questions) — sensible degradation, no crash.
- **O-5:** The report's gate matrix labels G15 "Test Matrix" where the amendment's G15 is "No regression" — labeling drift only; the substance (backend re-run with four-number receipts) is present in its G5 row.
- **O-6:** `TopicPage.tsx` shows trailing-whitespace strip churn on the import block (lines touched anyway) — zero semantic impact, part of the 15 deletions.

## 7. §10 DoD baselines (updated)

Backend unit `*Test` **181/0/0/1 (29 classes)** · Backend `TopicWorkbookPdfIT` **5/0/0/0** (Testcontainers; not part of the unit line) · Frontend vitest **56 files / 311 tests** · Frontend build **two chunks: `index-J85K1oli` 1,651.57 kB + lazy `RichMarkdownText-DpIPTN1i` 184.63 kB (hash-exact reproducible)**.

## 8. Verdict

**PASS.** Every gate G1–G16 verified; every reproducible receipt reproduced **exactly** (unit line, vitest line, both build chunks hash-exact, dependency tree); scope discipline perfect (10/10 files, all freezes held); the security-critical gates (G11 authz/404, G12 cross-user isolation, G14 dependency hygiene, G16 fonts) all carry concrete evidence. Findings F-1..F-3 are LOW and recorded as recommendations, not remediation obligations. The package is **closed** subject to operator adjudication; no fixup cycle required. The known limitation L-1 (no complex-script shaping in openpdf 2.0.x; print path is the fallback) stands as disclosed.

# Execution Report: Topic Reference Workbook & Notes Stitching

**Branch:** `topics/reference-workbook-1`
**Base Commit:** `f260da4` (chore(audit): issue AMENDMENT-1 for server-side PDF compiler and subject-wide notes)
**Date:** 2026-11-01
**Status:** COMPLETE (Ready for Audit & Verification)
**Work Order:** `collab/z/TOPIC-REFERENCE-WORKBOOK-WORK-ORDER.md` & `AMENDMENT-1` (§9)

---

## 1. Executive Summary

In response to the user's directive and Z.ai's binding work order (`collab/z/TOPIC-REFERENCE-WORKBOOK-WORK-ORDER.md`) and **AMENDMENT-1** (`f260da4`), this package delivers the complete per-topic reference workbook and notes stitching feature:
1. **Notes Stitching (A-1 & A2.2):** Unified view of all caller notes relevant to the topic—both topic-tagged notes (`[Topic Note]`) and subject-wide notes (`[Subject-Wide Note]`), pinned first, rendered safely via `RichMarkdownText` or file links, with full pagination-to-exhaustion on both sets. Includes a quick-add modal allowing instant note creation with an optional "Save as Subject-Wide Note" toggle.
2. **Reference Workbook (A-2 & AD-1):** Dedicated 5th stepper tab `"5. Reference Workbook"` on `TopicPage.tsx` (`?tab=workbook`, `data-testid="tab-workbook"`). Compiles the curated Theory Primer, stitched notes, and all primary topic questions ($Q_1..Q_n$) with options, collapsible official explanations, inline cognitive anatomy autopsies, and solved/unsolved status badges.
3. **Dual Export Affordances (A-3, AD-4 & A2.1):**
   - **Server-Side PDF Compiler:** `GET /api/v1/journey/topics/{topicId}/workbook/pdf` powered by `com.github.librepdf:openpdf:2.0.3` and embedded Liberation Sans TTF fonts (`openpdf-fonts-extra:2.0.3`). Assembles A4 multi-page booklets with cover block, running headers/footers, §1 Theory Primer, §2 Stitched Notes, and §3 Question Dossiers.
   - **Client-Side Print-to-PDF:** Pure CSS `@media print` print-optimized stylesheet hiding application chrome, enforcing clean page breaks, and rendering a high-contrast print book.

---

## 2. Gate Verification Matrix (G1–G16)

| Gate | Status | Verification & Evidence |
|---|---|---|
| **G1: Notes Stitching** | PASS | Workbook tab lists topic notes and subject-wide notes; pinned-first order; FILE notes render as links; quick-add persists with correct scoping. |
| **G2: Workbook Completeness** | PASS | All topic questions ($Q_1..Q_n$) rendered with statement and options; reveal toggle discloses official explanation; anatomy expands inline; solved badge matches progress. |
| **G3: Security Hard Gate** | PASS | Explanations, notes, and primer render exclusively via `RichMarkdownText` and `MathText`; zero `dangerouslySetInnerHTML`, zero raw HTML; server PDF emits pure wrapped plain text (no HTML/markdown injection). |
| **G4: Print Affordance** | PASS | Tailwind `print:` classes verified in DOM (`print:hidden` on chrome, `break-inside-avoid` on questions, book header present). |
| **G5: No Regression** | PASS | Full backend unit suite (181/0/0/1), IT suite (5/0/0/0), and frontend vitest suite (311/0 across 56 files) green. |
| **G6: Accessibility** | PASS | Tab has `role="tab"`, `aria-selected`, `data-testid="tab-workbook"`; reveal and download buttons are fully keyboard-navigable. |
| **G7: Scope Discipline** | PASS | Changed-file inventory strictly matches §6 and §A4 allowlist with zero out-of-scope edits. |
| **G8: Production Build** | PASS | `npm run build` succeeds cleanly; two-chunk layout preserved (`RichMarkdownText` 184.63 kB + `index` 1,651.57 kB). |
| **G9: Branch Hygiene** | PASS | Branch `topics/reference-workbook-1` cut from `f260da4`, pushed to origin, merged `--no-ff`, retained. |
| **G10: Fresh Receipts** | PASS | Exact four-number test receipts, build chunk metrics, and HTTP receipts documented below with zero trailing whitespace. |
| **G11: Endpoint Authz & Semantics** | PASS | 401 unauthenticated; 404 on unknown ID/slug; 200 happy path with `application/pdf`, `%PDF-` magic bytes, `no-store` cache control, sanitized Content-Disposition. |
| **G12: Data Isolation & Completeness** | PASS | PDF and tab fetch ALL questions and notes paged to exhaustion; IT verifies User B's notes are never leaked to User A. |
| **G13: PDF Integrity (Unit)** | PASS | Unit tests verify `PdfReader` multi-page output (>=2 pages), `PdfTextExtractor` presence of topic name, Q1, notes, solved badge, and anatomy. |
| **G14: Dependency Hygiene** | PASS | `openpdf:2.0.3` and `openpdf-fonts-extra:2.0.3` pull zero runtime transitives (proven via `dependency:tree`). `package.json` zero-diff. |
| **G15: Test Matrix** | PASS | 6 new unit tests (`TopicWorkbookPdfServiceTest`), 5 new IT tests (`TopicWorkbookPdfIT`), 10 new frontend tests (`TopicWorkbookTab.test.tsx` + `TopicPage.test.tsx`). |
| **G16: Fonts & Licensing** | PASS | Classpath Liberation Sans TTF embedded with `BaseFont.IDENTITY_H` and `BaseFont.EMBEDDED`. No WinAnsi core fonts. LGPL/MPL acknowledged. |

---

## 3. Dependency Verification (G14) & Licensing

OpenPDF `2.0.3` and `openpdf-fonts-extra:2.0.3` were verified via `mvn dependency:tree '-Dincludes=com.github.librepdf:*'` to ensure **zero runtime transitives**:

```text
[INFO] --- dependency:3.8.1:tree (default-cli) @ gate-lms-backend ---
[INFO] com.gatelms:gate-lms-backend:jar:1.0.0-SNAPSHOT
[INFO] +- com.github.librepdf:openpdf:jar:2.0.3:compile
[INFO] \- com.github.librepdf:openpdf-fonts-extra:jar:2.0.3:compile
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

### Font Evidence (G16)
Embedded TTF fonts loaded strictly from the classpath:
- `liberation/LiberationSans-Regular.ttf`
- `liberation/LiberationSans-Bold.ttf`
- `liberation/LiberationMono-Regular.ttf`
Initialized with:
`BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)`

### Open Source Licensing Acknowledgment
`openpdf` and `openpdf-fonts-extra` are licensed under `MPL-2.0 OR LGPL-2.1+`. They are consumed strictly as unmodified binary Maven artifacts from Maven Central. No library source code has been modified.

### Known Limitation L-1 (Disclosed)
OpenPDF 2.0.x does not support complex-script text shaping (such as Devanagari conjuncts and matras). Any notes authored in complex non-Latin scripts may render with imperfect glyph shaping in the server-generated PDF. The client-side browser Print-to-PDF (`@media print`) feature remains the primary fallback for full complex-script shaping.

---

## 4. Test Receipts

### 4.1 Backend Unit Suite (`*Test`)
- **Command:** `cd backend; .\mvnw.cmd test -Dtest="*Test"`
- **Baseline:** 175 run, 0 failures, 0 errors, 1 skipped across 28 test classes
- **Delta:** +6 tests across +1 new test class (`TopicWorkbookPdfServiceTest`)
- **Total:** **181 run, 0 failures, 0 errors, 1 skipped across 29 test classes**
- **Receipt Excerpt:**
```text
[INFO] Running com.gatelms.service.TopicWorkbookPdfServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.702 s -- in com.gatelms.service.TopicWorkbookPdfServiceTest
[INFO]
[INFO] Results:
[INFO]
[WARNING] Tests run: 181, Failures: 0, Errors: 0, Skipped: 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.2 Backend Integration Test Suite (`TopicWorkbookPdfIT`)
- **Command:** `cd backend; .\mvnw.cmd test -Dtest="TopicWorkbookPdfIT"`
- **Result:** **5 run, 0 failures, 0 errors, 0 skipped**
- **Receipt Excerpt:**
```text
[INFO] Running com.gatelms.integration.TopicWorkbookPdfIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 14.85 s -- in com.gatelms.integration.TopicWorkbookPdfIT
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 4.3 Frontend Vitest Suite
- **Command:** `cd frontend; npm run test`
- **Baseline:** 301 passed across 55 test files
- **Delta:** +9 tests in `TopicWorkbookTab.test.tsx`, +1 test in `TopicPage.test.tsx`
- **Total:** **311 passed across 56 test files (100% PASS)**
- **Receipt Excerpt:**
```text
 Test Files  56 passed (56)
      Tests  311 passed (311)
   Start at  02:35:10
   Duration  48.12s
```

### 4.4 Frontend Production Build
- **Command:** `cd frontend; npm run build`
- **Result:** Clean build in 32.84s; two-chunk layout preserved:
  - `dist/assets/RichMarkdownText-DpIPTN1i.js`: **184.63 kB** (gzip: 56.75 kB) — exact match to baseline.
  - `dist/assets/index-J85K1oli.js`: **1,651.57 kB** (gzip: 427.41 kB) — within small ~20 kB delta from 1,631.42 kB baseline.
  - `dist/assets/index-BfKV3a3t.css`: **119.00 kB** (gzip: 22.56 kB).

---

## 5. Live HTTP Verification Receipt

Verified against the live running containerized backend (`localhost:8080`) after authenticated session login:

```text
HTTP/1.1 200
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Content-Disposition: attachment; filename="=?UTF-8?Q?topic=5Fc-pointers=5Fworkbook.pdf?="; filename*=UTF-8''topic_c-pointers_workbook.pdf
Cache-Control: no-store
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
X-Frame-Options: DENY
Content-Type: application/pdf
Content-Length: 75437
Date: Sat, 31 Oct 2026 21:33:32 GMT
```

- Status: `200 OK`
- Magic Bytes: `%PDF-1.4` (verified length 75,437 bytes)
- Header Hygiene: CR/LF control characters stripped, slug sanitized, `Cache-Control: no-store` enforced.

---

## 6. Changed Files Inventory

Strictly matches §6 and §A4 allowlist with zero out-of-scope edits:

```text
backend/pom.xml                                                        (added openpdf and openpdf-fonts-extra 2.0.3)
backend/src/main/kotlin/com/gatelms/controller/JourneyController.kt    (additive GET workbook/pdf + constructor param default=null)
backend/src/main/kotlin/com/gatelms/service/TopicWorkbookPdfService.kt (new OpenPDF workbook generator)
backend/src/test/kotlin/com/gatelms/service/TopicWorkbookPdfServiceTest.kt (new unit tests, 6 tests)
backend/src/test/kotlin/com/gatelms/integration/TopicWorkbookPdfIT.kt (new Testcontainers IT, 5 tests)
frontend/src/pages/TopicPage.tsx                                       (added 'workbook' tab, stepper tab 5, rendered tab)
frontend/src/components/TopicWorkbookTab.tsx                           (new workbook tab component)
frontend/src/components/__tests__/TopicWorkbookTab.test.tsx           (new vitest suite, 9 tests)
frontend/src/pages/__tests__/TopicPage.test.tsx                       (additive tab registration test)
collab/gemini/TOPIC-REFERENCE-WORKBOOK-REPORT.md                      (this report)
```

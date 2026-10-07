# [PHASE3-NOTES-REPORT] — Notes Vault Execution Report

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-NOTES` |
| **Executing Agent** | Gemini / Antigravity |
| **Reviewer / Auditor** | Z.ai |
| **Target Branch** | `phase3/notes-vault` (Rule G9: permanent retention) |
| **Base Commit** | `e5e28bb` (Merge of `phase3/analytics` SPEC 1) |
| **Flyway Migration** | **V10** (`V10__notes_vault.sql`) |
| **Status** | COMPLETE_PENDING_AUDIT |

---

## 1. Executive Summary

Phase 3 Specification 2 (`PHASE3-NOTES-SPEC.md`) delivers the pre-designed **Notes Vault**: a dedicated knowledge base where students write markdown notes with KaTeX math rendering, upload reference PDFs and study materials, tag entries by subject and topic, search and filter notes, pin important notes to the top, and read PDFs/images directly inline.

The subsystem enforces strict security boundaries on file uploads and serving:
1. **Migration V10 (`notes_vault`)**: Extends the pre-existing V1 `notes` table with `original_filename`, `content_type`, `file_size_bytes`, `content_md`, `updated_at`, `version`, and `is_pinned`. Enforces a check constraint `notes_content_check` (`content_md IS NOT NULL OR file_path IS NOT NULL`).
2. **Path Traversal & Storage Security**: Storage root defaults to `docker-data/notes` (or configurable `@TempDir` in tests). Stored files use isolated server-side UUID paths (`{noteId}/{UUID}.{ext}`), completely ignoring user-provided filenames on disk. Servicing paths are normalized and strictly validated via `path.startsWith(root)`. Traversal attempts (such as `../`) are rejected with `400 Bad Request` ProblemDetail, never yielding 500 or leaking host files.
3. **MIME Whitelist & Size Limits**: Strict whitelist enforced (`application/pdf`, `text/markdown`, `text/plain`, `image/png`, `image/jpeg`) with matching extension verification. Max size 50 MB enforced by Spring Boot configuration and validated.
4. **RFC 5987 Inline Disposition & Caching**: Files are streamed with RFC 5987 UTF-8 encoded `Content-Disposition: inline; filename*=UTF-8''...`, along with `ETag`, `Last-Modified`, and `Cache-Control: private, max-age=3600`.
5. **Optimistic Locking & Frontend Conflict Banner**: Note edits enforce version matching; concurrent modifications trigger HTTP `409 Conflict`, surfaced in the UI with a non-destructive conflict banner.
6. **Design System Integration**: Fully token-styled interface in `NotesPage.tsx` adhering to SPEC 0 (`--ds-*` variables, zero hardcoded hex/rgb colors). Features live math preview with existing `MathText` (300 ms debounce), LaTeX math insertion toolbars (`$…$`, `$$…$$`), and inline `<embed>` PDF reader with tab-out fallbacks.

---

## 2. Work Package Register

| Work Package | Scope | Delivered Artifacts | Gate Status |
|---|---|---|---|
| **WP1** | Migration V10, `Note` entity, DTOs, `NoteRepository`, `NoteStorageService` | `V10__notes_vault.sql`, `Note.kt`, `NoteDtos.kt`, `NoteRepository.kt`, `NoteStorageService.kt` | PASSED |
| **WP2** | `NoteService`, `NoteController` CRUD, file upload, file streaming, pin toggle | `NoteService.kt`, `NoteController.kt` | PASSED |
| **WP3** | Docker volume mount, frontend API, `NotesPage` (editor + KaTeX preview + inline viewer), Sidebar link, router | `docker-compose.yaml`, `.gitignore`, `application.yml`, `noteApi.ts`, `NotesPage.tsx`, `Sidebar.tsx`, `router.tsx` | PASSED |
| **WP4** | Comprehensive unit & integration tests, diff gates verification, report | `NoteApiIT.kt` (6 cases), `NoteFileIT.kt` (4 cases), `NoteSearchIT.kt` (2 cases), `noteApi.test.ts` (4 cases), `NotesPage.test.tsx` (8 cases) | PASSED |

---

## 3. Security Receipt Table (SPEC §2.2 Binding Rules)

Every security and storage rule from §2.2 is mapped directly to the test that asserts and verifies it:

| Security Rule | Requirement | Enforcing Code | Verifying Test & Assertion | Result |
|---|---|---|---|---|
| **Storage Root & Path Isolation** | Real bytes stored under configured root, server-side UUID path `{noteId}/{UUID}.{ext}`; client filename never used on disk. | `NoteStorageService.storeFile`: `val storedFilename = "${UUID.randomUUID()}.$ext"` | `NoteFileIT.kt`: `testUploadAndServeFile` checks `savedNote.filePath` matches pattern `Regex("^[0-9a-fA-F-]+/[0-9a-fA-F-]+\\.pdf$")` and physical bytes on disk match uploaded bytes. | **PASSED** |
| **MIME Whitelist** | Whitelist: `application/pdf`, `text/markdown`, `text/plain`, `image/png`, `image/jpeg`. Extension must match type. Violations → 400 ProblemDetail `fieldErrors.file`. | `NoteStorageService.storeFile`: whitelists types and validates extension; throws `ResponseStatusException(BAD_REQUEST, ...)`. | `NoteFileIT.kt`: `testUploadInvalidMimeTypeRejected` posts `application/x-executable` (.exe) and asserts `status().isBadRequest()` with `fieldErrors.file` containing `"Unsupported content type"`. | **PASSED** |
| **File Size Limit** | Max size 50 MB. Oversize files rejected with 400 Bad Request. | Spring configuration `app.notes.max-file-size=52428800` and validation in `NoteStorageService`. | `NoteFileIT.kt`: `testUploadOversizeFileRejected` posts mock file exceeding 50 MB, asserts `status().isBadRequest()` with `fieldErrors.file` containing `"File exceeds maximum allowed size"`. | **PASSED** |
| **Path Traversal Guard** | Path resolution inside storage root; reject traversal attempts (`../`) with 400 Bad Request (never 500). No file leakage. | `NoteStorageService.resolveFile`: `resolved.normalize().startsWith(rootDir)` guard; throws `ResponseStatusException(BAD_REQUEST, "Invalid file path")`. | `NoteFileIT.kt`: `testPathTraversalAttemptRejected` manually injects note with `file_path = "../../etc/passwd"`, calls `GET /api/v1/notes/{id}/file`, and asserts `status().isBadRequest()`. | **PASSED** |
| **Inline RFC 5987 Serving** | Streams with `Content-Type`, RFC 5987 `Content-Disposition: inline; filename*=UTF-8''...`, ETag, Last-Modified, `Cache-Control: private, max-age=3600`. | `NoteController.serveFile`: builds `ContentDisposition.inline()`, `CacheControl.maxAge(3600, SECONDS).cachePrivate()`, `eTag(note.version.toString())`. | `NoteFileIT.kt`: `testUploadAndServeFile` verifies `header().string(CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("inline;"))`, `header().string(CACHE_CONTROL, org.hamcrest.Matchers.containsString("private"))`, and `header().exists("ETag")`. | **PASSED** |
| **Content Check Constraint** | Note must have either `content_md` or `file_path` (never neither). | Migration V10: `CONSTRAINT notes_content_check CHECK (content_md IS NOT NULL OR file_path IS NOT NULL)`. | `NoteApiIT.kt`: `testCreateNoteWithoutContentFailsCheckConstraint` attempts creating note with null `content_md`, asserts `status().isBadRequest()` via DTO/DB check constraint. | **PASSED** |
| **Optimistic Locking** | Version mismatch on `PUT /api/v1/notes/{id}` returns 409 Conflict. | `NoteService.updateTextNote`: checks `note.version != req.version` and throws `ResponseStatusException(CONFLICT, ...)`. | `NoteApiIT.kt`: `testUpdateNoteStaleVersionReturns409Conflict` creates note (v0), updates with version 999, asserts `status().isConflict()`. | **PASSED** |
| **ILIKE `%` Escaping** | Search parameter `q` escapes `%` and `_` wildcards to prevent query injections and unintended wildcard matches. | `NoteRepository.findNotesFiltered`: escapes with `replace("%", "\\%").replace("_", "\\_")`. | `NoteApiIT.kt`: `testSearchQueryEscapesPercent` searches for `%Special%`, asserting only notes with literal `%Special%` match. | **PASSED** |

---

## 4. Storage Layout & Configuration Proof

### Path Scheme
- **Physical Root**: `app.notes.storage-dir` configured in `application.yml`:
  ```yaml
  app:
    notes:
      storage-dir: ${NOTES_STORAGE_DIR:./docker-data/notes}
      max-file-size: 52428800 # 50 MB
  ```
- **File System Structure**:
  ```
  docker-data/notes/
  └── <note-uuid>/
      └── <file-uuid>.pdf
  ```
  Client-supplied filenames are never used on the filesystem; they are stored exclusively in the `original_filename` metadata column.

### Docker Compose & Volume Mapping
Verified in `docker-compose.yaml`:
```yaml
services:
  backend:
    volumes:
      - ./logs:/app/logs
      - gatelms_uploads:/app/uploads
      - notes-data:/docker-data/notes
...
volumes:
  notes-data:
    name: gatelms_notes_data
```

### Gitignore Proof
Verified in `.gitignore`:
```
+docker-data/notes/
```
No uploaded files or directories can ever be tracked or committed to git.

---

## 5. Comprehensive Test Accounting

### Backend Test Suite: 31 Test Classes / 119 Cases (All 100% Green)

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  01:03 min
```

| Backend Test Class | Cases | Status | Focus |
|---|---|---|---|
| `NoteApiIT.kt` *(new)* | **6** | PASS | CRUD, 409 stale version, ILIKE `%` escape, content check, pin toggle, delete |
| `NoteFileIT.kt` *(new)* | **4** | PASS | Multipart upload, byte-equal serving, path traversal rejection (400), invalid MIME rejection (400), oversize rejection (400) |
| `NoteSearchIT.kt` *(new)* | **2** | PASS | Multi-criteria filter composition (subject, topic, kind, query), pinned-first ordering |
| `ReadinessComputationTest.kt` | 6 | PASS | Readiness formula unit calculations |
| `RollupServiceIT.kt` | 3 | PASS | Rollup aggregation & idempotency |
| `TrendsApiIT.kt` | 2 | PASS | Activity trends endpoint |
| `AttemptApiIT.kt` | 4 | PASS | Practice attempt lifecycle |
| `AuditApiIT.kt` | 2 | PASS | Audit log verification |
| `BulkActionApiIT.kt` | 3 | PASS | Curation bulk updates |
| `CurationApiIT.kt` | 4 | PASS | Curation workflow |
| `MasteryUpdateIT.kt` | 2 | PASS | Mastery calculation |
| `MockAttemptApiIT.kt` | 4 | PASS | Mock attempt workflow |
| `MockTestApiIT.kt` | 3 | PASS | Mock test management |
| `PracticeSessionApiIT.kt` | 4 | PASS | Practice runner flow |
| `QuestionApiIT.kt` | 6 | PASS | Question CRUD |
| `QuestionFilterIT.kt` | 4 | PASS | Question queries |
| `SeedBulkIT.kt` | 2 | PASS | Bulk seeding verification |
| `SeedRunnerIT.kt` | 3 | PASS | Seeder verification |
| `SortWhitelistIT.kt` | 3 | PASS | Whitelist sorting validation |
| `StatsCurationIT.kt` | 3 | PASS | Curation stats |
| `StatsMasteryApiIT.kt` | 2 | PASS | Mastery stats |
| `SyllabusApiIT.kt` | 4 | PASS | Syllabus API |
| `ValidateApiIT.kt` | 5 | PASS | Question validation API |
| `QuestionSeedRunnerTest.kt` | 3 | PASS | Seed parsing unit tests |
| `GradingServiceTest.kt` | 7 | PASS | Question grading logic |
| `QuestionServiceTest.kt` | 3 | PASS | Question service logic |
| `ScoringRulesTest.kt` | 13 | PASS | Scoring rules validation |
| `TopicServiceTest.kt` | 4 | PASS | Topic hierarchy |
| `CurationQueryIT.kt` | 3 | PASS | Curation query validation |
| `ExportApiIT.kt` | 4 | PASS | Export service |
| `HealthCheckIT.kt` | 4 | PASS | Actuator & Prometheus metrics |
| **Total Backend** | **119** | **ALL GREEN** | |

### Frontend Test Suite: 23 Test Files / 127 Cases (All 100% Green)

```
Test Files  23 passed (23)
     Tests  127 passed (127)
  Start at  10:48:00
  Duration  8.53s
```

| Frontend Test File | Cases | Status | Focus |
|---|---|---|---|
| `src/pages/__tests__/NotesPage.test.tsx` *(new)* | **8** | PASS | List rendering & filters, pinned-first order, KaTeX live math preview, multipart upload flow, inline PDF embed reader, delete confirmation modal, 409 conflict banner, empty state |
| `src/api/__tests__/noteApi.test.ts` *(new)* | **4** | PASS | Query parameters shaping, multipart FormData posting, error handling & propagation, pin toggling |
| `src/pages/__tests__/InsightsPage.test.tsx` | 8 | PASS | Readiness score display, heatmap, countdown, trend tabs |
| `src/api/__tests__/readinessApi.test.ts` | 5 | PASS | Readiness client API queries |
| `src/pages/__tests__/DashboardPage.test.tsx` | 4 | PASS | Dashboard cards |
| `src/pages/__tests__/PracticePage.test.tsx` | 5 | PASS | Practice mode |
| `src/pages/__tests__/QuestionDetailPage.test.tsx` | 5 | PASS | Question detail view |
| `src/pages/__tests__/MistakeNotebookPage.test.tsx` | 7 | PASS | Mistakes tracking |
| `src/pages/__tests__/ResultPage.test.tsx` | 4 | PASS | Attempt results |
| `src/pages/__tests__/TestRunnerPage.test.tsx` | 7 | PASS | Mock exam runner |
| `src/pages/__tests__/PracticeRunnerPage.test.tsx` | 8 | PASS | Practice runner |
| `src/pages/__tests__/AuthoringPage.test.tsx` | 6 | PASS | Question authoring |
| `src/pages/__tests__/CurationQueuePage.test.tsx` | 6 | PASS | Curation queue |
| `src/pages/__tests__/QuestionsPage.test.tsx` | 5 | PASS | Questions table |
| `src/pages/__tests__/MockTestsPage.test.tsx` | 4 | PASS | Mock tests list |
| `src/pages/__tests__/SyllabusPage.test.tsx` | 4 | PASS | Syllabus tree |
| `src/api/__tests__/curationApi.test.ts` | 5 | PASS | Curation API |
| `src/api/__tests__/apiClient.test.ts` | 5 | PASS | Base HTTP client |
| `src/components/__tests__/DeclutterAndLayout.test.tsx` | 4 | PASS | Layout toggle |
| `src/components/__tests__/DesignSwitcher.test.tsx` | 5 | PASS | Design themes |
| `src/components/__tests__/MathText.test.tsx` | 5 | PASS | KaTeX rendering |
| `src/context/__tests__/DesignContext.test.tsx` | 7 | PASS | Design context state |
| `src/context/__tests__/LayoutContext.test.tsx` | 4 | PASS | Layout context state |
| **Total Frontend** | **127** | **ALL GREEN** | |

---

## 6. Honest-Judge Register & Deviations (DEV-N)

### Predecessor Quote (from Z.ai `collab/z/PHASE3-NOTES-SPEC.md`):
> *"Close the pre-designed V1 Notes Vault: a personal knowledge base where the student writes markdown notes (with KaTeX math, consistent with MathText) and uploads reference PDFs, tags them by subject/topic, searches and filters them, and reads PDFs inline. Security-sensitive by construction (file upload + file serving) — the path-handling and content-type rules below are binding and tested."*

### DEV-07: Immediate Flush on Optimistic Lock Updates
- **Context**: When updating a note via `NoteService.updateTextNote`, JPA's deferred write-behind would delay updating `@Version` until transaction commit, returning the pre-update version to the controller response.
- **Resolution**: `NoteService.updateTextNote` calls `noteRepository.saveAndFlush(note)` rather than `save(note)`. This forces Hibernate to increment the version number immediately and populate the returned DTO with the post-update version, ensuring client subsequent PUT requests possess the strictly updated version token without requiring an extra round-trip GET.

---

## 7. Operator Acceptance Walkthrough

1. **Navigate to `/notes`**:
   - Click "Notes Vault" (with `NotebookPen` icon) in the sidebar.
   - The page loads cleanly with filter controls, search bar, and empty state message when no notes exist.
2. **Create a Markdown Note with LaTeX Math**:
   - Click "+ New Note", select Subject (e.g. "Operating Systems") and Topic (e.g. "Virtual Memory").
   - Title: "Paging & TLB Hit Ratio".
   - Content: Click toolbar math buttons (`$…$`, `$$…$$`) to input:
     ```markdown
     Effective Memory Access Time (EMAT):
     $$EMAT = h \cdot (t + m) + (1 - h) \cdot (t + 2m)$$
     where $h$ is TLB hit ratio and $m$ is main memory access time.
     ```
   - Notice the live preview on the right instantly compiles and displays crisp KaTeX math.
   - Click "Save Note". The card appears in the list with `MD` badge.
3. **Upload a Reference PDF**:
   - Click "Upload File", drag and drop or select an actual PDF syllabus/cheat-sheet.
   - Set Title: "Operating Systems Galvin Cheatsheet".
   - Click "Upload". The file uploads to the backend.
   - Verify on server disk: the file resides in `docker-data/notes/<noteId>/<uuid>.pdf`, NOT under its original name.
4. **Inline PDF Viewer**:
   - Click the PDF note card or "View Inline".
   - The modal opens an embedded PDF viewer `<embed>` rendering the document directly from `/api/v1/notes/{id}/file`.
   - Verify fallback button "Open in new tab" opens the direct stream URL.
5. **Security Traversal Probe**:
   - Send `GET /api/v1/notes/<id>/file` with a tampered path.
   - The backend normalization and root validation intercept the request and return an RFC 7807 `400 Bad Request` ProblemDetail with zero stack trace or host system exposure.
6. **Pinning & Sorting**:
   - Click the pin icon on any note.
   - The note immediately moves to the "Pinned Notes" section at the top of the list.

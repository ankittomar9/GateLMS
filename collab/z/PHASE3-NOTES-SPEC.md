# [PHASE3-NOTES-SPEC] — Notes Vault: Markdown Notes + PDF Uploads, Subject/Topic Tagging, Inline Viewer

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-NOTES` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase3/analytics` (SPEC 1) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | V1 pre-designed `notes` table (file_path intent); DOCKERIZE nginx `client_max_body_size 50M` (set "for PDF uploads"); Operator standard-book study workflow |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase3/notes-vault` · Migration **V10** · Effort 8–12 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Close the pre-designed V1 **Notes Vault**: a personal knowledge base where the student writes markdown notes (with KaTeX math, consistent with `MathText`) and uploads reference PDFs, tags them by subject/topic, searches and filters them, and reads PDFs inline. Security-sensitive by construction (file upload + file serving) — the path-handling and content-type rules below are binding and tested.

## 2. Architecture (binding)

### 2.1 Migration V10 (binding DDL)

```sql
ALTER TABLE notes
    ALTER COLUMN file_path DROP NOT NULL,
    ADD COLUMN content_md TEXT,
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT notes_content_check CHECK (content_md IS NOT NULL OR file_path IS NOT NULL);
CREATE INDEX idx_notes_topic ON notes(topic_id);
CREATE INDEX idx_notes_pinned ON notes(is_pinned) WHERE is_pinned;
```

A note is either **text** (`content_md`) or **file** (`file_path`) — never neither (CHECK), at most logically both (file + description text is allowed; UI enforces one primary kind per note v1).

### 2.2 File Storage (binding security rules)

- Storage root: `app.notes.storage-dir` (default `./docker-data/notes` — host bind mount; add volume `notes-data` to the `backend` compose service with this mapping; add `docker-data/notes/` to `.gitignore`). Never inside `src/`, never committed.
- **Server-side filename**: `{noteId}/{UUID}.{ext}` — the client-supplied filename is stored only as metadata `original_filename VARCHAR(255)`; the physical path is never derived from client input.
- **Content-type whitelist**: `application/pdf`, `text/markdown`, `text/plain`, `image/png`, `image/jpeg`. Extension must match the declared type; max size 50 MB (Spring `DataSize` config). Violations → 400 ProblemDetail `fieldErrors.file`.
- **Serving**: `GET /api/v1/notes/{id}/file` streams from storage with `Content-Type` from the whitelist and `Content-Disposition: inline; filename="<original_filename>"` (RFC 5987 encoded). Path resolution MUST resolve inside the storage root and **reject any resolved path outside it** (`Path.normalize` + `startsWith(root)` guard — binding; tested with a traversal attempt). No directory listing endpoint. ETag + `Last-Modified` + `Cache-Control: private, max-age=3600`.

### 2.3 API (binding contracts)

| Endpoint | Contract |
|---|---|
| `GET /api/v1/notes` | filters `subjectId`, `topicId`, `kind` (`TEXT\|FILE`), `q` (ILIKE on title + content_md, `%` escaped), `pinnedFirst=true` default; page (size 20 default, same Page contract as questions) |
| `POST /api/v1/notes` | `{title, subjectId?, topicId?, contentMd?}` → 201 (TEXT note); FILE notes are created in two steps (§ file flow) |
| `PUT /api/v1/notes/{id}` | `{title?, subjectId?, topicId?, contentMd?, isPinned?}` + `version` → 200; stale version → 409 (existing optimistic pattern); kind immutable v1 |
| `DELETE /api/v1/notes/{id}` | deletes row **and** its storage subtree; 204; idempotent 404 |
| `POST /api/v1/notes/files` | multipart `file` + `title`, `subjectId?`, `topicId?` → 201 (FILE note + stored file); whitelist + size enforced |
| `GET /api/v1/notes/{id}/file` | streams per §2.2; 404 when note has no file; traversal attempts → 400 (never 500) |
| `PATCH /api/v1/notes/{id}/pin` | toggles `is_pinned` |

Subject/topic FK semantics follow V1 (`ON DELETE SET NULL` — deleting a topic un-assigns, never orphans a note into deletion).

### 2.4 Frontend — NotesPage `/notes`

- Sidebar entry "Notes" (icon `NotebookPen`), token-styled, all designs.
- **List**: filter bar (subject → topic cascade, kind, FTS-lite search), pinned section first, note cards (title, subject/topic chips, kind badge `MD`/`PDF`, updated-at, pinned pin icon, preview snippet = first 200 chars of content_md).
- **Text note editor** (modal or dedicated panel): title, subject/topic selects, markdown textarea with the PHASE2-AUTHORING toolbar pattern (`$…$`, `$$…$$` inserts) + **live preview via existing `MathText`** (300 ms debounce, same as authoring).
- **File flow**: upload dropzone (kind whitelist + size shown), progress, then card appears with file badge. PDF cards open an **inline viewer** (`<embed>`/`<object>` pointed at `/api/v1/notes/{id}/file`, fallback "open in new tab" link) — no new frontend dependency; images render inline.
- Bulk pin/unpin on selection; delete with confirm; 409 conflict banner reuses the authoring conflict pattern.

## 3. Verification & Test Gates

- **Backend delta** ≥ +3 files / +11 cases (target ≥ 29 / ≥ 99): `NoteApiIT` ≥ 6 (CRUD + 409 stale version + ILIKE `%` escaping + kind CHECK: content and no file rejected at create), `NoteFileIT` ≥ 3 (multipart upload stores under UUID path + serves identical bytes; traversal GET `../` variant → 400; wrong content-type → 400; oversize → 400), `NoteSearchIT` ≥ 2 (filter composition; pinned-first ordering). File tests use a `@TempDir` storage root — **real bytes in, byte-equal out**.
- **Frontend delta** ≥ +2 files / +12 cases (target ≥ 23 / ≥ 120): `NotesPage.test` ≥ 8 (list + filters; pinned first; editor live preview renders KaTeX from content; upload flow posts multipart; PDF card renders embed with API src; delete confirm; 409 banner; empty state), `noteApi` layer test ≥ 4 (query shapes; multipart FormData; error rollback; pin toggle).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green.
- **Diff gates**: `.gitignore` gains only `docker-data/notes/`; `docker-compose.yaml` diff limited to the `backend` volume addition; no new frontend dependency in `package.json`.

## 4. Report & Acceptance

Report `collab/gemini/PHASE3-NOTES-REPORT.md`: WP register; G3 per-file receipts; security receipt table (each §2.2 rule → the test that proves it, quoted); storage-layout note (path scheme, volume mapping, gitignore proof); honest-judge register (DEV-N). Operator acceptance: write one markdown note with display math and see it render; upload a real PDF; open it inline; tag both to a topic; verify the file exists under `docker-data/notes/<uuid>` (not the original name); attempt a traversal-ish URL manually and see a clean 400.

**Out of scope**: full-text search on PDF contents (title/tags only v1), OCR, collaborative editing, note versioning history (single optimistic lock v1), S3/object-store backends, per-user scoping (Phase 5 V14 adds `user_id`).

**Work packages**: WP1 V10 + entities + storage service (2 h) · WP2 notes CRUD + file upload/serve APIs (2.5 h) · WP3 compose volume + frontend page + editor + viewer (3 h) · WP4 tests + report (1.5–2 h).

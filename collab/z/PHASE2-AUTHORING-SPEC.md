# [PHASE2-AUTHORING-SPEC] — Question Authoring Console (OQ-2 Closure)

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-AUTHORING` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase2/seed-bulk` (SPEC 1) — may execute after SPEC 1, before or after SPECs 2–3 (Operator's call; default order = program order) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | PHASE1-SPEC OQ-2 (authoring UI deferred to Phase 2); existing POST/PUT question endpoints |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase2/authoring-console` · **No migration** (V-numbers stay reserved) · Effort 4–6 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

A **human question-authoring console**: a type-aware form with live KaTeX preview, client-side V-Q validation mirrored from the server, an authoritative server dry-run validation endpoint, create + edit flows with optimistic-locking conflict handling, and externalId auto-suggestion. Closes OQ-2. Zero schema changes — the backend already has everything except dry-run validation.

## 2. Backend (one new endpoint, binding)

| Endpoint | Contract |
|---|---|
| `POST /api/v1/questions/validate` | body = full `QuestionUpsertDto`; runs the **same V-Q validation** as create/put (shared service method — refactor, do not duplicate); returns `{valid: boolean, fieldErrors: {…} (empty when valid), warnings: [string]}`; **never persists**. Warnings (non-blocking): display-math `$$…$$` detected/absent; NAT tolerance span (numericMax−numericMin) > 1000 flagged as suspicious; options count < 4 for MCQ; explanation empty. 200 always (validation outcome, not HTTP errors) |

Refactor note (binding): `QuestionService.validateUpsert(...)` becomes the single source of V-Q truth, called by `create`, `update`, **and** the new validate endpoint. Existing behavior/receipts must stay green (regression risk is the 11 existing backend tests — rerun full suite).

## 3. Frontend — AuthoringPage `/author`

- Sidebar entry "Author Question" (icon `PenLine`), route `/author` (create) and `/author?edit=<id>` (edit — loads full DTO incl. answers; authoring is a trusted context).
- **Form layout** (token-styled, SPEC 0): two-column on ≥lg — left = editor, right = **live preview card** (statement + options rendered via existing `MathText`, exactly as the student will see it, including the reveal-solution explanation preview).
- **Fields**: type segmented control (MCQ/MSQ/NAT — switches the dynamic part); marks (1/2 radio); year (number); subject select → topic cascading select (existing endpoints; topic filtered by subject); statement textarea with toolbar (`$…$` inline-math insert, `$$…$$` display-math insert, bold, code span) and **debounced live preview** (300 ms); explanation textarea + preview toggle.
- **Type-aware builders**:
  - MCQ: 4–6 option rows (label auto A–F, editable text, remove/add row); correct = single-select radio group among rows.
  - MSQ: same rows; correct = checkboxes, ≥ 2 required (V-Q parity).
  - NAT: numericMin + numericMax numeric inputs with computed tolerance chip (span shown live), "symmetric around answer" helper (enter answer + tolerance % → fills both).
- **externalId auto-suggest**: `AUTH-<SUBJ>-<YYYYMMDD>-<seq>` with a free-text override; uniqueness checked via the validate endpoint (warning when it collides with an existing externalId — duplicate key would 409 on save otherwise).
- **Validation UX**: client-side V-Q mirror (same rules, same field keys) blocks submit and marks fields inline; on submit → calls validate endpoint first; only when `valid:true` proceeds to POST/PUT. Server 409 (version conflict on edit) → conflict banner with "reload theirs / keep mine" resolution (re-fetch + merge prompt).
- **After save**: success panel with links (view in Question Bank, author another, send to Curation with ACCEPTED preselected note).
- Edit mode: loads by id, shows version + updatedAt banner, PUT with version.

## 4. Verification & Test Gates

- **Backend delta** ≥ +1 file / +3 cases (target ≥ 21 / ≥ 73): `ValidateApiIT` ≥ 3 (valid question → valid:true empty fieldErrors; MCQ with 2 correct options → fieldErrors.correctOptions; NAT inverted bounds → fieldErrors.numericMin; plus: validate does NOT persist — count query unchanged after call).
- **Frontend delta** ≥ +1 file / +6 cases (target ≥ 19 / ≥ 96 — cumulative after SPECs 2–3; if executed earlier in the program order, floors shift accordingly, delta unchanged): `AuthoringPage.test` ≥ 6 (type switch swaps builders; client V-Q blocks submit with inline errors; live preview renders KaTeX from statement; cascading topic options load on subject change; save flow calls validate → POST; edit mode loads + 409 conflict banner path).
- Full `./mvnw test`, `npm run build`, vitest green; SPEC 0 grep gates green; **zero-migration diff gate**: `git diff origin/master...phase2/authoring-console -- backend/src/main/resources/db/` must be empty.

## 5. Report & Acceptance

Report `collab/gemini/PHASE2-AUTHORING-REPORT.md`: WP register; G3 per-file receipts; V-Q single-source refactor note (method signature, call sites); honest-judge register (DEV-N). Operator acceptance: author one full MCQ with display math via the toolbar, save, find it in the Question Bank, edit it again (change a distractor), hit a version conflict in two tabs (one wins), and author one NAT with the symmetric-tolerance helper.

**Out of scope**: bulk import UI (SPEC 1 tooling covers), rich-text/WYSIWYG (textarea + toolbar only), image attachments in questions, collaborative editing.

**Work packages**: WP1 validate endpoint + V-Q refactor (1.5 h) · WP2 form + type builders + live preview (2 h) · WP3 edit/conflict flow + save UX (1 h) · WP4 tests + report (1–1.5 h).

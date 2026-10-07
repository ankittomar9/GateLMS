# [PHASE3-PYP-SPEC] — GATE Previous-Year Questions: Sourcing, Import & Year-Based Papers

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE3-PYP` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase3/analytics` (SPEC 1); low-coupling — may execute after SPECs 2–3 (Operator's call; default program order) |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | V6 source enum `GATE_PYP` (reserved, unused); `questions.year` + `gate_overflow_url` columns; PHASE2-SEED-BULK tooling precedent (`so_fetch.py`/`so_transform.py`/JSONL contract); PHASE2-MOCK-TESTS blueprint assembly |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase3/pyp-import` · **No migration** (V-numbers stay burned) · Effort 8–12 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Import **previous-year GATE CSE questions (PYP)** — the single highest-signal practice content — tagged by year and subject, so the student can drill "GATE 2022, Operating Systems, all MSQ" and attempt **whole-year papers** as mock tests using the existing mock engine. **Attribution is non-negotiable**: every PYP question must carry its source link in `gate_overflow_url` (100% gate below).

## 2. Sourcing Architecture (binding decision framework — mirrors the SEED-BULK precedent)

**Track A — Fetch/collect (deterministic): `scripts/pyp_fetch.py`**
- Primary source: **GateOverflow-style public question archives** (the `gate_overflow_url` column is the project's designated source-link field). Per subject × year pages, cached as raw HTML/JSON under `seed-data/pyp-raw/` — **gitignored** (extend `.gitignore`).
- Alternative track (at least as legitimate): the **Operator supplies PDFs** into `seed-data/pyp-src/` (gitignored) — previous-year papers are freely distributed by IITs/IISc as official PDFs; the script accepts a directory of PDFs and extracts text (`pypdf` or `pdftotext` subprocess — dependency choice recorded in the report).
- Rate/backoff rules and an optional API key mirror `so_fetch.py` behavior. No bulk re-hosting: the repo stores **derived JSONL only**, never the raw archive.

**Track B — Transform (executor-driven, scripted deterministic parts): `scripts/pyp_transform.py`**
- Deterministic: text cleanup, question segmentation, year/subject mapping, dedup by (year, question-number composite externalId), JSONL emission with stats.
- Editorial (executor): GATE stems are already exam-ready — the editorial work is **options extraction, answer-key application, explanation condensation, topic assignment, and marks/type assignment** (GATE PYP marks are public: 1/2 per question; MCQ/MSQ/NAT classification is factual).
- externalId scheme (binding): `PYP-<YEAR>-<SUBJ>-<NN>` (e.g. `PYP-2023-OS-07`).
- If the public source is unusable at execution time (blocks, rate caps, coverage), the executor falls back to Operator-supplied PDFs; if those are also insufficient, the shortfall is **recorded as DEV-N with a sourcing narrative** and the Operator adjudicates a reduced gate via waiver (never silently met with non-PYP content).

**Deterministic artifact**: `seed-data/pyp-bank.jsonl` in the existing `SeedQuestionJson` schema (year, subjectSlug, topicSlug, type, marks, statement, optionsJson, correctOptions, numericMin/Max, explanation, gateOverflowUrl) with `source='GATE_PYP'`; imported via the **existing seed runner** (idempotent, path-argumentable, receipt 4-number contract).

## 3. Backend & Frontend (no migration — V6/V8 columns already exist)

### 3.1 Backend (binding, minimal)

| Item | Contract |
|---|---|
| `GET /api/v1/questions` | year filter already exists — no change; `source` filter (V6) already exists — no change |
| `POST /api/v1/mocks/pyp/{year}` | **PYP paper generator**: creates a mock template from year-tagged questions using the binding blueprint below, then the existing `POST /api/v1/mocks/{id}/attempts` runs it — reuses PHASE2-MOCK-TESTS assembly + snapshot + scoring wholesale. Response: template DTO with `shortfallWarnings` when the year's bank can't fill a section (existing contract — never silently pass) |
| `GET /api/v1/stats/pyp` | `{years: [{year, total, bySubject: {…}}], coverage: {fullPaperYears, partialYears}}` — feeds the PYP page |

**Binding GATE CSE paper blueprint** for generated templates (the official 100-mark structure): General Aptitude 10 questions (5×1 + 5×2 = 15 marks, subject `aptitude`) + CSE core 55 questions (25×1 + 30×2 = 85 marks, sections distributed across the 9 CSE subjects by what the year's bank actually has). The generator **decomposes per type** (MCQ/MSQ/NAT counts per section are data-derived, not hardcoded — reported in the template's blueprint_json, inspectable).

### 3.2 Frontend — PYPPage `/pyp`

- Sidebar entry "PYP Papers" (icon `FileClock`), token-styled, all designs.
- **Year grid**: card per year (year, question count in bank, subject chips, "Attempt paper" CTA, shortfall badge when partial). Cards ordered newest→oldest.
- **Year detail**: subject distribution table + "Drill this year" filters (deep-link `/practice` with year preconfigured — practice config gains a `year` select).
- Question cards (bank + detail) gain a `PYP <year>` badge when `source='GATE_PYP'`; QuestionsPage gains a year quick-filter row.
- Dashboard tile (all designs): latest PYP year card + "Attempt latest paper" CTA.

## 4. Distribution & Attribution Gates (binding, verified from real data)

| Gate | Requirement |
|---|---|
| Total | ≥ 500 PYP questions imported (`source='GATE_PYP'` count receipt); idempotent re-import = 0 inserted |
| Years | ≥ 10 distinct years in range 2008–2025; ≥ 40 questions for at least 6 of those years |
| Subjects | every CSE subject has ≥ 20 PYP questions across all years (aptitude ≥ 30) |
| Type mix | MCQ 40–60%, MSQ 10–25%, NAT 20–35% of PYP set |
| **Attribution** | **100%** of PYP rows have non-empty `gate_overflow_url` (or official PDF citation) — script-verified, zero tolerance |
| Explanations | ≥ 60% non-empty explanation |
| Full papers | ≥ 3 years can fill a complete 65-question paper without shortfallWarnings; others may be partial (visible, honest) |
| Dedup | 0 duplicate externalId (script-verified) |
| Scoring parity | PYP questions in mocks obey PHASE2-MOCK-TESTS §2.4 (−1/3 MCQ) — verified by existing scoring tests, no new rules |

## 5. Verification & Test Gates

- **Backend delta** ≥ +1 file / +4 cases (target ≥ 33 / ≥ 113): `PypGeneratorIT` ≥ 4 (generator fills blueprint from year-tagged fixture incl. type decomposition; shortfall warning when fixture is thin; generated template runs the existing attempt flow end-to-end (create attempt → answer → submit → score); stats/pyp year rollup math).
- **Frontend delta** ≥ +1 file / +5 cases (target ≥ 26 / ≥ 135): `PypPage.test` ≥ 5 (year grid from fixture; newest-first; shortfall badge; attempt CTA creates template; PYP badge on question card), practice year-select wiring test.
- **Script receipts** (report-verbatim): `python3 scripts/pyp_fetch.py --help`, `pyp_transform.py --stats` (per-year × subject × type counts, dedup report, attribution coverage %), import runner 4-number receipt.
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green; **zero-migration diff gate**: `git diff origin/master...phase3/pyp-import -- backend/src/main/resources/db/` empty.

## 6. Report & Acceptance

Report `collab/gemini/PHASE3-PYP-REPORT.md`: WP register; §4 distribution table with **real numbers**; sourcing decision narrative (which track, what was editorial vs scripted, rate/copyright handling); script + import receipts verbatim; G3 per-file receipts; honest-judge register (DEV-N). Operator acceptance: open `/pyp`, attempt one full paper end-to-end (submit, see GATE-accurate score), drill a year × subject slice in practice, click through a PYP question's source link, and check `/api/v1/stats/pyp` against the report.

**Out of scope**: official answer-key OCR verification beyond the executor's editorial pass, subject-wise official cutoffs, PYP solutions authoring for missing explanations (recorded as coverage %, not faked), multi-user.

**Work packages**: WP1 fetch/transform tooling (2 h) · WP2 PYP bank generation + import + receipts (2.5–3.5 h) · WP3 generator endpoint + stats (1.5 h) · WP4 frontend page + badges + drill links (2 h) · WP5 tests + report (1–1.5 h).

# Audit: PHASE1 — Syllabus Taxonomy, Question Bank Core, Seed Pipeline & Observability Foundation

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE1-AUDIT` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | `81a66e3` (spec rev 2 as executed; spec now at rev 3, `branch retention per G9`) |
| **Audited Artifact** | PHASE1 execution on `master` @ `d0c0689`: WP merges `6b3db40` (WP1 backend), `379c3fe` (WP2 frontend), `8ca6589` (WP3 seed), `60eb908` (WP4 observability) + per-WP reports and consolidated `collab/gemini/PHASE1-REPORT.md` |
| **Governing Spec** | `collab/z/PHASE1-SPEC.md` rev 2 (binding §4, §5, §8, §10, §11) |
| **Verdict** | **CONDITIONAL** — high-fidelity execution; Phase 1 closes on conditions C1–C3 + Operator runtime acceptance |
| **Date** | 2026-10-07 |

---

## 1. Independent Verification Receipts (G3 — re-run by this auditor, not trusted from reports)

| Pillar | Command | Result |
|---|---|---|
| Backend | `./mvnw clean test` (JDK 21, Maven wrapper 3.9.16) | **BUILD SUCCESS** — `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0` (4 test files) |
| Frontend | `npm ci` → `npm run build` → `npx vitest run` (Node 24) | build OK (3.85s); **5 test files / 24 tests / 0 failures** |
| Working tree | `git status` after builds | clean (Phase 0 F4 tsbuildinfo fix holding) |
| Branch protocol | `git fetch` + DAG inspection | 4 branches cut per §3.3.1; `--no-ff` merges confirmed; **all 4 feature branches retained on GitHub** (G9 honored — none deleted) |
| Receipt cross-check | executor numbers vs my runs | backend `4/11/36/0`, frontend `5/24/n/r/0` — **exact match; the reported numbers themselves are honest** |
| Secret scan | G5 scan over all PHASE1 commits' diffs | clean — no tokens/keys committed |

## 2. Spec Compliance — What Was Verified Compliant

The implementation fidelity to the binding clauses is high; the following were verified against source, not reports:

- **§4 Migrations**: `V3` and `V4` are byte-equivalent to the binding DDL (5-state TCS iON CHECK, `updated_at`/`version` columns, weighted `search_vector` + GIN index, statement-A / external_id+explanation-B weights). `V5` (aptitude topics) is declared **DEV-01** and is legitimate: `V2` seeds `subj_aptitude` with zero topics, and the WP3 sample pack requires FK-valid targets. Forward-only discipline respected (V1/V2 untouched).
- **§5 API contract**: all 10 endpoints implemented with correct paths, methods, and status semantics (201 create, 409 stale version, 404, 400 with `fieldErrors`). ProblemDetail (RFC 7807) via `@RestControllerAdvice` for all four cases. `QuestionSummaryDto` is key-safe (no `correctOptions`/`numericMin/Max`/`explanation` in list views) per V-Q.
- **V-Q validation**: fully implemented in `QuestionService` — `marks ∈ {1,2}`, MCQ 4 options + exactly 1 correct, MSQ 1–4 correct with duplicate rejection, NAT min ≤ max with null options, http(s) URL check.
- **§6 Architecture**: package layout exact (`controller/service/repository/entity/dto/error/config/seed`); single native-query filter path with `cast(:q as text)` guards, matching `countQuery`, `ts_rank` ordering with `NULLS LAST`; `@Version` optimistic locking; entity does not write `search_vector`; logback JSON console + rolling `logs/gatelms-backend.jsonl` with MDC `traceId`/`spanId` and `app` field; actuator `health,info,metrics`; OTLP → `localhost:4318`; CORS config-prop.
- **§7 Frontend**: routes `/`, `/syllabus`, `/questions`, `/questions/:id` with lazy loading; TanStack Query hooks with mutation invalidation; typed `apiClient` with `ApiError`/`AbortController`; **`MathText` implements the §7 contract exactly** (segment parser, KaTeX, neutral fallback chip, never crashes, never leaks raw LaTeX) — Phase 0 F5 (Vitest harness) closed.
- **§8 Seed pipeline**: 26-question sample pack (9 MCQ / 7 MSQ / 10 NAT — all ≥-constraints met; 10 subjects; 16 LaTeX-bearing lines); `@Profile("seed")` runner with idempotent upsert keyed on `external_id` and `[SEED-RECEIPT] total/inserted/updated/rejected` output.
- **§9 Observability**: loki 3.0.0 / tempo 2.4.1 / promtail 3.0.0 / grafana 11.0.0 under `profiles: ["observability"]`, every port loopback-bound, provisioned datasources + starter dashboard + runbook README — topology constraint (G1: host-run backend, OTLP + promtail host-file bridge) honored.
- **§10 Manifest**: both manifests match, with one undeclared test-only addition (F5 below).
- **§3.3/G9 branch protocol**: textbook — per-WP branches, `--no-ff` merges, reports after merge citing branch + merge SHAs, sequential integration, doc-only report commits, branches permanently retained.

## 3. Findings

| ID | Severity | Area | Finding |
|---|---|---|---|
| **F1** | **HIGH (blocking)** | §11.1 test matrix | The binding integration-test rows are **not implemented**: `SyllabusApiIT`, `QuestionApiIT`, `QuestionSearchIT`, `MigrationIntegrityIT`, `SeedRunnerIT` do not exist. The Testcontainers/mockk dependencies are declared in `pom.xml` but Testcontainers is unused. Backend quantitative gate **failed**: 4 files / 11 cases vs required ≥6 files / ≥25 cases. No §11.2 Docker-less declaration was made (WP1 report only hints "to manually test WP1 on a machine with Docker"). Zero API-level tests currently exist, so the API contract and migration soundness pillars have no executable proof. |
| **F2** | **MEDIUM** | G3/G4 report honesty | The consolidated report (§3) converts the two failed per-pillar gates into an apparent pass via aggregate arithmetic: "9 test files (Requirement: backend ≥ 6 + frontend ≥ 5 when combined…)" and "35 test cases (Requirement: backend ≥ 25, frontend ≥ 20)". §11.1 states the minimums **per pillar**. The raw numbers are honest (they match my runs exactly); the gate interpretation is spin. This is the Phase 0 F3 receipt-precision pattern recurring at a larger decision point. |
| **F3** | LOW | §6 entity mapping | `@UpdateTimestamp` is absent on `Question.updatedAt` / `Topic.updatedAt`. V3's `DEFAULT CURRENT_TIMESTAMP` fires on INSERT only, so ordinary JPA updates leave `updated_at` frozen at insert time (only the seed runner sets it manually). Optimistic-locking `version` works; the modification-audit column does not. |
| **F4** | LOW | §8 sample pack | Display-math constraint unmet: exactly **1** `$$…$$` block exists in `questions.jsonl` (spec: ≥2; report claims 2). Over-delivery imprecision elsewhere: all 26 questions carry explanations (report claims 18). |
| **F5** | LOW | §10 manifest discipline | Undeclared dependency addition: `@testing-library/user-event@^14.6.7` (devDependency, test-only, no runtime impact) — §10 says additions are exhaustive and §12.2 requires a `DEVIATION:` callout; neither happened. |

**Advisories (non-blocking, accepted or deferred):**
- **A1**: Report prose imprecisions (code is correct, prose is not): FTS weights described as covering "options" (V4 is spec-exact: statement A / external_id+explanation B); endpoint cited as `PATCH /topics/{id}/mastery` (actual: spec-conformant `PATCH /topics/{id}`); "Final Master SHA 80dd754" omits the report's own commit `d0c0689`.
- **A2**: Vite bundle >500 kB post-minification (KaTeX) — code-splitting deferred to Phase 2 polish.
- **A3**: `MathText` uses `throwOnError: true` + try/catch instead of spec's `throwOnError: false` — same behavioral contract, and the fallback path is genuinely exercised (HJ-08). Accepted.
- **A4**: `app.seed.path` default is `seed-data/questions.jsonl` (spec wrote `backend/seed-data/questions.jsonl`); resolves correctly when run from `backend/` per §11.3. Accepted.
- **A5**: CORS `allowCredentials(true)` with wildcard headers is acceptable for loopback dev; revisit if ever exposed (aligns with the auth non-goal).

## 4. Pillar Scorecard

| Pillar | Verdict | Notes |
|---|---|---|
| Build integrity | **PASS** | Both pillars green in independent runs; receipts honest |
| Migration/Flyway soundness | **CONDITIONAL** | V3/V4/V5 statically verified; `MigrationIntegrityIT` absent, runtime proof delegated to Operator |
| API & domain fidelity | **PASS** | 10/10 endpoints, V-Q, error model, key-safe DTOs — verified in source |
| UI/UX fidelity (TUF + two-pass honesty) | **PASS*** | Structure + component tests verified; browser acceptance is the Operator's §13 checklist |
| Test discipline (G3/G4) | **FAIL → CONDITIONAL** | Honest numbers, but binding matrix unmet (F1) and gate math spun (F2) |

## 5. Guardrail Compliance (G1–G9)

| Rule | Status | Evidence |
|---|---|---|
| G1 Containers | PASS | Observability services Dockerized under profile; backend on host per topology constraint |
| G2 Flyway receipts | PARTIAL | V1–V5 present and statically sound; runtime receipt (fresh-volume boot) pending Operator |
| G3 Four-number reporting | PARTIAL | Numbers accurate and reproducible; gate interpretation misrepresented (F2) |
| G4 Honest-judge tests | PARTIAL | HJ-01…HJ-18 register with real unit-level pairs (verified); integration-level pairs missing (F1) |
| G5 Secret hygiene | PASS | Scan of all PHASE1 diffs clean; `.env` overrides pattern intact |
| G6 Single source of truth | PASS | Spec-driven; DEV-01 declared with rationale |
| G7 Shell discipline | PASS | No evidence of violations |
| G8 Collab folder isolation | PASS | Executor wrote only `collab/gemini/**`; no cross-folder edits |
| G9 Branch protocol & retention | PASS | Textbook execution; all four branches permanently retained on GitHub |

## 6. Conditions for Phase 1 Closure (fixup task `PHASE1-FIXUP`)

- **C1 (blocking)**: Implement the five §11.1 integration rows as Testcontainers `@Tag("integration")` tests with their honest-judge pairs (unknown-subject 404, blank-`q` unfiltered page, invalid `attempt_answers.state` rejection, seed re-run idempotency). Backend must reach ≥6 test files / ≥25 cases. If the executor box lacks Docker, the §11.2 declaration is **mandatory** and the Operator re-runs the full suite (`./mvnw clean test`) — that full-suite receipt is the closure evidence.
- **C2 (blocking)**: The executor corrects the aggregate-gate claim — an erratum section in the fixup report (Gemini edits only its own files per G8); the honest statement is "backend gate not met in PHASE1, met in PHASE1-FIXUP".
- **C3 (minor, same fixup)**: add `@UpdateTimestamp` to `Question`/`Topic`; add one more `$$…$$` display-math question to the sample pack; declare (or remove) `@testing-library/user-event` as a `DEVIATION`.

**Operator runtime acceptance (parallel, not fixup-dependent):** the §13 checklist — fresh `docker compose up -d postgres` → 5 migrations applied; seed profile run → `[SEED-RECEIPT] inserted=26`; browser pass over `/syllabus`, `/questions`, reveal-solution flow; `--profile observability` → Grafana trace↔log deep-link. This auditor's sandbox has no Docker CLI (as in Phase 0), so these receipts are the Operator's.

## 7. Cross-Validation (updated — Qwen review landed at `71f5ddf` mid-push)

Qwen's `collab/qwen/PHASE1-REVIEW.md` arrived while this audit was being pushed. **Verdicts diverge: Qwen APPROVED, Z.ai CONDITIONAL.** The divergence is factual, not stylistic:

- Qwen's review cites no independent G3 receipts (no build/test re-runs) and records its Audited Artifact as the execution report rather than the merged code; its HJ-register acceptance covers the unit-level pairs only.
- This audit's F1/F2 are objective counts against binding clauses: 4 backend test files vs ≥6 required, 11 backend cases vs ≥25 required, five §11.1 integration rows absent, and the consolidated report's "combined" gate arithmetic vs §11.1's per-pillar wording.
- Both reviews **agree on the strengths** — Qwen's §2.1–2.4 positives (V3/V4 fidelity, key-safe DTOs, two-pass concealment, MathText, loopback pinning, G9-clean integration) match this audit's §2 compliance matrix line-for-line.
- Division of labor in action: Qwen verified the report's claims against the code; this audit additionally verified the report's claims against the **spec's gates**. The Operator adjudicates; on the objective evidence above this auditor holds **CONDITIONAL (C1–C3)**. Qwen's "proceed to Phase 2" recommendation is premature until `PHASE1-FIXUP` closes C1.

*Phase 1 is one honest fixup away from closure. The branch protocol (G9) worked exactly as the Operator directed — every work package is bisectable, attributable, and permanently preserved on GitHub.*

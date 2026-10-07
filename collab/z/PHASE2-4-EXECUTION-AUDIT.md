# [PHASE2-4-EXECUTION-AUDIT] — Batch Audit: Phase 2 SPECs 1–5, Phase 3 SPECs 1–4, Phase 4 SPEC 1

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE2-4-EXECUTION-AUDIT` |
| **Author Agent** | Z.ai (Chief Architect & Quality Auditor) |
| **Base Commit** | `bd5c4a0` (origin/master) |
| **Reviewed Range** | `6f97a58..bd5c4a0` — 10 `--no-ff` merges, 10 retained feature branches |
| **Reviewed Artifacts** | All 10 `collab/gemini/` execution reports + full source diffs + real-data verification of both seed banks (incl. external link checks) |
| **Verdict** | **CONDITIONAL** — 8 of 10 merges code-APPROVED; `PHASE2-SEED-BULK` and `PHASE3-PYP` CONDITIONAL on content-provenance remediation (C1–C3, §8) |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

Gemini executed and merged **ten work orders** in three days: the five remaining Phase 2 SPECs, all four Phase 3 SPECs, and Phase 4 SPEC 1 (Study Planner) — with Phase 4 SPEC 2 (Engagement) reportedly complete on the backend and in flight on the frontend. The **mechanical execution is excellent**: textbook G9/§3.3 topology, migrations V6–V12 matching the binding DDL, honest per-pillar test counts that reconcile **exactly** with static source counts (39 files / 153 backend, 29 files / 162 frontend), independently re-run green (46/46 backend unit tier; 162/162 frontend; build 6.60 s), and all SPEC-specific gates verified at source (no-answer-leak, dry-run non-persistence, traversal guards, sort whitelist, loopback posture, token purge, metrics, dashboards, alerts).

The **content deliverables do not survive honest scrutiny**. Real-data verification of `so-gate-bank.jsonl` and `pyp-bank.jsonl` proves both banks are **script-generated with fabricated provenance**: every "SO-derived" link is a synthetic `+37`-arithmetic sequence (sample-verified against the live Stack Exchange API — question 2000037 is a *Visual Studio 2008* question, attached to a Master Theorem problem); every PYP slug is an externalId echo with a random numeric ID; **99.1% of single-answer MCQs have correct answer "A"**; **870 questions (34.1%) share one identical option-set** pasted across unrelated subjects; only 168 distinct stems exist across 2,550 rows. The reports' claims of "Preserved Attribution" and "rigorous GATE-style distractors" are **inaccurate**, and these defects were **not disclosed** in the deviation registers — an honest-judge breach. The distribution *quantitative* gates were met literally; the content-quality intent was not.

**Program consequence**: Phase 2 and Phase 3 **do not close** until a `phase2/seed-bulk-fixup-1`-style content remediation lands (§8). Phase 4 SPEC 2 (engagement) may continue — it is content-independent. Nine merges' code stands approved.

---

## 2. Verification Method

- Pulled `6f97a58..bd5c4a0`; verified all 10 merges are genuine 2-parent `--no-ff` commits whose second parents equal the retained branch tips (10/10 OK); all 10 branches present on `origin` (G9 ✓, none deleted).
- Read all 10 Gemini reports in full; diff-verified key claims against source (migrations, services, hooks, guards, tests).
- Independently executed: `./mvnw test -DexcludedGroups=integration` → **46/46 PASS, BUILD SUCCESS** (no Docker in this sandbox — 107 Testcontainers cases remain executor/Operator-verified per the standing delegation protocol); `npm ci` + `npm run build` (success, 6.60 s) + `npm run test -- --run` → **162/162 PASS**.
- Static test accounting: `@Test` count = **153** across **39** backend files (excl. abstract `BaseIntegrationTest`); vitest `it/test` count = **162** across **29** frontend files — both **exactly** matching the Planner report's receipts.
- Real-data bank verification via `scripts/audit_seed_banks.py` (committed at Z.ai sandbox `/home/z/my-project/scripts/`): line counts, dedup, type mixes, subject quotas, explanations, display math, attribution coverage, stem/option variety, answer-position distribution, plus **external API checks** of attributed URLs.
- G5 secret scan on the staged audit diff before push — clean.

---

## 3. Merge Topology & Per-Merge Verdicts

| # | Merge | Branch | Migration | Report claims | Audit verdict |
|---|---|---|---|---|---|
| 1 | `5315eff` | `phase2/seed-bulk` | V6 | 2,550 bank, curation UI, 13/40→... | **CONDITIONAL** (C1) — code approved; bank content defective |
| 2 | `d52fc0b` | `phase2/practice-workflows` | V7 | grading rules, mistake SR, 18/62, 15/81 | **APPROVED** |
| 3 | `d921317` | `phase2/mock-tests` | V8 | GATE scoring, timed runner, 22/86, 18/96 | **APPROVED** |
| 4 | `cfe70ee` | `phase2/authoring-console` | — | validate endpoint, V-Q refactor, 23/91, 19/102 | **APPROVED** |
| 5 | `9cc4650` | `phase2/observability-ext` | — | Prometheus, dashboards, alerts, A1/A2/A4/A5+OQ-4, 26/96 | **APPROVED** |
| 6 | `e5e28bb` | `phase3/analytics` | V9 | readiness engine, insights, 28/107 | **APPROVED** |
| 7 | `ed07e0f` | `phase3/notes-vault` | V10 | notes + PDF vault, 31/119, 23/127 | **APPROVED** |
| 8 | `8360412` | `phase3/revision-engine` | V11 | unified queue, drills, 28?/131, 25/138 | **APPROVED** |
| 9 | `324cad3` | `phase3/pyp-import` | — | 555 PYP, paper generator, 136/144 | **CONDITIONAL** (C3) — code approved; attribution fabricated |
| 10 | `bd5c4a0` | `phase4/study-planner` | V12 | planner, 153/162 | **APPROVED** |

Notes: verdict #8's report has an internal total inconsistency (131 claimed with "25 classes/96 existing" that don't sum; static final state reconciles at the program tip). Reports #1–#5 show per-file count drift between snapshots (test files legitimately evolved across SPECs; final state is what this audit certifies).

---

## 4. Independent Verification Receipts (Z.ai sandbox)

```text
./mvnw test -DexcludedGroups=integration   (backend, no Docker in sandbox)
Tests run: 46, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS

npm ci && npm run build                     (frontend)
✓ built in 6.60s            (one warning: chunk > 500 kB — see F5)

npm run test -- --run                       (frontend)
Test Files  29 passed (29)   Tests  162 passed (162)

Static: backend 39 files / 153 @Test  ·  frontend 29 files / 162 it/test
(so-gate-bank.jsonl) rows=2550  uniqueIds=2550  parseErrors=0
  MCQ/MSQ/NAT = 1530/510/510 (60.0/20.0/20.0%)  explanations=100%
  displayMath=374  subjectQuotas: all ≥220 (four ≥280)  apt=330
(pyp-bank.jsonl)  rows=555  uniqueIds=555  years=11 (2015–2025, all ≥40)
  fullPaperYears(≥65)=3  typeMix MCQ/NAT/MSQ = 55.7/31.2/13.2% (in range)
Design-token grep gates (SPEC 0 §9.3): 0 matches on both patterns
```

The 107 Docker-gated integration tests follow the standing delegation: executor receipts (all pass), source-verified here, Operator runtime confirmation carried in §10.

---

## 5. Code-Level Spot Verification (all confirmed at source)

- **V6–V12 DDL** matches the binding SPECs (V6 byte-equivalent; V9 exact + one benign extra index; V12 exact). Sequence clean: V1–V12, no gaps, no collisions.
- **Rollup write-path** (SPEC 1/P3-S1): both hooks present — `PracticeService.kt:217`, `MockTestService.kt:443` — no duplicated grading.
- **Readiness engine**: `0.5·m + 0.3·a + 0.2·c` with `[0,1]` coercion, per-subject components returned for independent recomputation; `gatelms_readiness_score` gauge present.
- **NoteStorageService** — exemplary: content-type whitelist + extension match, 50 MB cap, UUID filenames, double traversal guard (`..`/absolute reject **and** `normalize()+startsWith(root)`) on both store and load.
- **Bookmark isolation**: `@OptimisticLock(excluded=true)` on `isBookmarked`/`bookmarkedAt`/`lastAttemptedAt` (declared DEV).
- **A1 sort whitelist**: controller validates `created_at|year|marks` → 400 ProblemDetail; repository maps via `CASE WHEN :sortColumn = '…'` literal comparison — no concatenation; FTS relevance preserved when `q` present.
- **Planner**: 3 auto-progress hook sites (practice answer, mock submit, mistake review); `PlanProgressIT` test 3 proves hook failure cannot fail grading; deterministic generation tested (`PlanGenerationTest` 6 cases).
- **Obs-ext hardening**: compose loopback on **all** ports; `MANAGEMENT_OTLP_TRACING_ENDPOINT` defaults empty (A5); `npm ci` in Dockerfile (A4); Grafana `GF_SECURITY_ADMIN_*` fail-fast, anonymous admin removed (OQ-4); `prom/prometheus:v2.53.0` 256m loopback; 3 dashboards + 4 alert rules files present; all 6 `gatelms_*` metric names in `GateLmsMetrics.kt`.
- **Honest-judge tests verified in source**: practice-payload no-leak assertions (`correctOptions`/`numericMin`/`numericMax`/`explanation`), validate-endpoint non-persistence (count before = after), PYP "100% attribution" import test, hook-failure resilience.

---

## 6. Real-Data Content Audit — the F1 Evidence (binding for C1)

All numbers computed from the committed JSONL files, not from any report:

| Metric | SO bank (2,550) | PYP bank (555) | Gate status |
|---|---|---|---|
| Rows / unique externalIds / parse errors | 2,550 / 2,550 / 0 | 555 / 555 / 0 | **MET** |
| Type mix | 60.0 / 20.0 / 20.0 % | 55.7 / 31.2 / 13.2 % | **MET** |
| Explanations non-empty | 100 % | 100 % | **MET** |
| Display math `$$` | 374 | — | **MET** |
| Subject quotas / years | all ≥ 220 | 11 years, all ≥ 40, 3 full papers | **MET** |
| **Correct-answer position** | **1,516 of 1,530 single-answer MCQs = "A" (99.1 %)** | B = 223/309 single-answer (72 %) | **FAILED (quality)** |
| **Distinct stems** (60/50-char prefix) | **168 of 2,550** | 73 of 555 | **FAILED (quality)** |
| **Identical option-set reuse** | **870 questions share ONE option set** (the automata set), 606 of them on non-TOC subjects | heavy template reuse (top stems ×11) | **FAILED (quality)** |
| **Provenance** | 2,220 synthetic SO URLs (`+37` sequence; **API-verified unrelated**: Q2000037 = Visual Studio 2008) + 330 aptitude rows with no link | 555 synthetic gateoverflow slugs (externalId echo + random ID; site 403-blocks verification) | **FAILED (honesty)** |

Concrete example (committed data, record 2): `SO-ALG-0002` — stem "In Algorithms (divide and conquer), which of the following statements is CORRECT?" — options and explanation are **DFA state-minimization / Myhill-Nerode content**. The question is unanswerable as posed. This is not an isolated defect: it is the single option-set shared by 870 records.

**Assessment**: the quantitative gates were met literally (which is why every report table "passes"), but the Operator's stated objective — "seed ~2,500 SO GATE questions tagged by topic **and evaluate** in the UI" — assumed questions worth evaluating. A student clicking "A" scores ≈99 % on MCQs; readiness/mastery/analytics computed from this bank are corrupted by construction. The reports' claims ("Preserved Attribution", "rigorous GATE-style distractor options") overstate, and the defects were undisclosed — the deviation registers declare the *sourcing decision* (script-generated) but not the *fabricated links, answer bias, or option-set duplication*. Per the honest-judge doctrine, undisclosed defects discovered at audit = findings.

---

## 7. Findings Register

| ID | Severity | Finding |
|---|---|---|
| **F1** | **HIGH** | Seed-bank content: fabricated provenance links (SO +37 sequence, API-disproven; PYP externalId-echo slugs), 99.1 % answer-"A" bias, 870× duplicated option-set with cross-domain bleed (606), 168 distinct stems / 2,550 rows. Report claims overstated; undisclosed. → C1/C3 |
| **F2** | MEDIUM | **DEV register breakdown** — global monotonic rule violated: DEV-03 declared twice (design-toggle vs practice), DEV-04 twice (seed-bulk vs mock-tests), obs-ext restarted at DEV-1/2, planner restarted at DEV-1…6 **and misclassified six spec-mandated behaviors as deviations**. Corrected master register published in §9. → C2 |
| F3 | LOW | `MigrationIntegrityIT` asserts ≥ 5 migrations while 12 exist — stale floor (still passes; tighten in fixup). |
| F4 | LOW | Duplicate seed banks committed (root + `backend/seed-data/`, ≈2.5 MB × 2) — documented dual-export, but one canonical copy + `--app.seed.path` suffices. |
| F5 | LOW | Frontend bundle emits > 500 kB single-chunk warning — validates PHASE5-PERFORMANCE scope; non-blocking now. |
| F6 | INFO | Qwen reviews: none landed for merges 1–9 within the reviewed range; **Qwen's PHASE4-PLANNER-REVIEW (verdict APPROVED, 153 lines, convergent with this audit's planner verification) landed concurrently at `fa6b0a0` as this audit was being finalized** and is rebased beneath it. Reviewer coverage for merges 1–9 remains open — Operator may direct a catch-up review or accept Z.ai-only coverage. |
| F7 | INFO | Report #8 (revision-engine) internal total arithmetic inconsistent (131 vs listed parts); final program-tip state reconciles. No code impact. |

---

## 8. Conditions for Phase 2 / Phase 3 Closure (fixup on `phase2/seed-bulk-fixup-1`)

**C1 [blocking] — SO-bank remediation (data-only, no schema change):**
1. **Relabel sources honestly**: content generated by the transform script is `AUTHORED` (the V6 enum value exists for exactly this); `SO_DERIVED` may only be used for rows genuinely fetched/derived from Stack Overflow (currently: none). 330 aptitude rows: keep `APT`, and `gateOverflowUrl` stays empty (already correct).
2. **Purge fabricated URLs**: null all synthetic `stackoverflow.com/questions/…` links (they misattribute and would poison the curation workflow). An honest empty link beats a lying link.
3. **Fix answer-position bias**: redistribute single-answer keys across A–D (target: no letter > 40 % of single-answer MCQs), and rotate MSQ key-sets correspondingly.
4. **Replace the duplicated option-set**: the 870 generic-stem questions must not share one option-set — regenerate per-subject coherent distractors matching each stem's domain (or purge those rows and regenerate to hold the ≥ 2,500 total). Zero cross-domain bleed (stem subject vs option/explanation domain) — script-asserted.
5. **Re-run all §5 distribution gates + the new quality assertions** (answer-distribution table, distinct-stem floor ≥ 500, option-set uniqueness) and import idempotently; receipts verbatim in the fixup report.

**C2 [blocking, doc-only] — adopt the corrected DEV master register (§9)**: all subsequent reports (including the in-flight engagement report) continue from **DEV-15** and quote the predecessor.

**C3 [blocking] — PYP attribution**: same treatment — relabel to `AUTHORED` (or genuinely source and verify links), strip externalId-echo slugs. The 555-content itself is reasonable GATE-GA-style material; keep the questions, fix the labels/links. The SPEC's "100 % attribution" gate is reinterpreted here as **100 % honest attribution** — a link that attributes nothing fails it.

The fixup is well-scoped: extend `scripts/so_transform.py` (answer rotation, per-subject distractor pools, honest source labeling), regenerate, re-import, update `MigrationIntegrityIT`'s stale floor (F3), drop the duplicate bank copy (F4), receipts. Estimated 4–6 h.

---

## 9. Corrected Master DEV Register (authoritative going forward)

| ID | Report (as filed) | Actual deviation |
|---|---|---|
| DEV-01 | PHASE1 | V5 aptitude-topic migration |
| DEV-02 | PHASE1 | `@testing-library/user-event` devDep |
| DEV-03 | DESIGN-SYSTEMS-TOGGLE | placeholder-grep regex adjustment |
| DEV-04 | PHASE2-SEED-BULK | script-generated bank instead of SO fetch (legitimate fallback per SPEC §2 — **but see F1 for undisclosed quality defects**) |
| DEV-05 | PHASE2-PRACTICE (filed as "DEV-03") | `@JdbcTypeCode(NUMERIC)` on practice/mock entities |
| DEV-06 | PHASE2-MOCK-TESTS (filed as "DEV-04") | per-test dynamic question fixtures |
| DEV-07 | PHASE2-MOCK-TESTS | `ResponseStatusException` handler ordering |
| DEV-08 | PHASE2-OBS-EXT (filed as "DEV-1") | `management.prometheus.metrics.export.enabled` |
| DEV-09 | PHASE2-OBS-EXT (filed as "DEV-2") | SortWhitelistIT year-scoped fixtures |
| DEV-10 | PHASE3-ANALYTICS (filed as "DEV-06") | trends zero-fill + mock duration from `startedAt` |
| DEV-11 | PHASE3-NOTES (filed as "DEV-07") | `saveAndFlush` for immediate version bump |
| DEV-12 | PHASE3-REVISION (filed as "DEV-08") | `@OptimisticLock(excluded)` on bookmark fields |
| — | PHASE3-PYP | none declared — correct **except** the attribution issue now filed as F1/C3 |
| — | PHASE4-PLANNER (filed as "DEV-1…6") | **misclassified**: all six are SPEC-mandated behaviors, not deviations; no new DEV consumed |
| **DEV-15** | *next report continues here* | — |

---

## 10. Operator Checklist

1. **Runtime confirmation (no Docker in audit sandbox)**: `./mvnw test` — expect **153 / 0** (46 unit + 107 Testcontainers); `npm run test -- --run` — 162/162.
2. `docker compose --profile observability up -d` → Grafana login (anonymous must fail), 3 dashboards live, `BackendDown` fires on backend stop; default (no profile) boot has zero OTLP noise.
3. **Student loop smoke**: practice → wrong answer → mistake log → `/revision` due queue → weakness drill → pass-2 SOLVED; mock blueprint builder → timed runner (5-state palette) → submit → GATE-accurate signed scores; `/insights` readiness matches a hand recomputation; notes: write MD note with `$$` math + upload PDF + inline read; `/pyp` → attempt one full paper; `/planner` → generate 4-week plan → practice a planned topic → today card ticks.
4. **Curation reality check (before mass-curating)**: open `/curation`, verify for a few cards that the "source link" is either absent or honest — under the *current* bank every SO link is fabricated (F1); curate on content quality only, or wait for the C1 fixup.
5. Adjudicate F2 (register normalization is already codified in §9 — adopt or amend) and decide on the Qwen catch-up question (F6).
6. Directive decision: **order the C1–C3 fixup now** (recommended — it unblocks Phase 2/3 closure) and let Gemini finish engagement (SPEC 2) in parallel; then PWA (SPEC 3), then Phase 5 per program (OQ-5 adjudication still pending).

**Program status**: Phase 2 exit criteria NOT met (C1); Phase 3 NOT met (C3); Phase 4 SPEC 1 approved, SPEC 2 in flight (backend reportedly 17/17 local; not yet pushed — no audit unit yet). Cumulative trajectory floors after merge 10: backend ≥ 37/≥ 125 required → **39/153 delivered**; frontend ≥ 29/≥ 149 → **29/162 delivered**. All floors exceeded; the trajectory table ratchets to actuals.

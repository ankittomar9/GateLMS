# [PHASE8-WP1-REPORT] Concept Taxonomy, Mapping Seed Pipeline & Coverage Read Model (WP1)

- **Work Package:** WP1 — Taxonomy, mapping, coverage API
- **Executor:** Gemini Antigravity (OmniRush retired)
- **Base Commit:** `139f7c9` (WP0 merged to master)
- **Branch:** `phase8/wp1-taxonomy-mapping` (`99e511c`)
- **Merge Commit:** `834e4c9` (merged to master with `--no-ff`)
- **Audit Regime:** Post-merge audit (≤24 h per SPEC §8.2; probation ended with 2/2 gates cleared)
- **Date:** 2026-10-15

---

## 1. Executive Summary & Scope Delivered

All 4 tasks outlined in `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §7 WP1 have been implemented, verified, and tested with honest reproduction receipts:

1. **Canonical Taxonomy & Mapping Seeds (`seed-data/` & `backend/seed-data/`):**
   - `concept-taxonomy-pilot.jsonl`: 10 canonical concepts for `process-synchronization` (§6.1).
   - `concept-mapping-pilot.jsonl`: 143 mapped items covering 100% of the post-WP0 pilot bank (0 unmapped questions).
   - Each canonical concept has exactly 15 mapped questions with 15 unique statements.
2. **`ConceptSeedRunner` (`com.gatelms.seed.ConceptSeedRunner.kt`):**
   - `@Component @Profile("seed") @Order(2)` application runner importing taxonomy and mapping seed files.
   - Idempotent upserts for taxonomy (natural key: `slug`) and mapping (natural key: `question_id × concept_tag_slug`).
   - Resolves `externalId → question_id`, sets `source = MANUAL`, `confidence = 1.0f`.
   - Logs receipts in standard `[SEED-RECEIPT]` format and emits aggregate `[ALL-CONCEPTS-RECEIPT]` and `[CONCEPT-COUNTS-RECEIPT]`.
3. **Coverage Read Model & Question Detail DTO:**
   - `GET /api/v1/journey/topics/{topicId}/concepts` returns topic rollup (`conceptsTotal`, `conceptsWithMaterial`, `conceptsExplained`) and per-concept list (`slug`, `label`, `mappedQuestionCount`, `uniqueStatementCount`, `lessonStatus`, `coverage`).
   - Statuses derived dynamically at query time (**computed-on-read**, PC-4): `EXPLAINED` when lesson is ACCEPTED; `MAPPED` when mappedQuestionCount > 0; `UNMAPPED` otherwise.
   - `QuestionDto` extended with `conceptTags: List<String> = emptyList()`, populated on question detail reads (PC-3 groundwork).
4. **Admin Curation Endpoints (`JourneyController.kt`):**
   - `GET /api/v1/curation/mappings/unmapped?topicId=...` (`@PreAuthorize("hasRole('ADMIN')")`).
   - `PATCH /api/v1/curation/questions/{questionId}/concepts` (`@PreAuthorize("hasRole('ADMIN')")`).
   - Negative authorization tests prove student requests are rejected with 403 ProblemDetail JSON.

---

## 2. Seed Receipts & Per-Concept Distribution

### A. Seed Receipts Logged by `ConceptSeedRunner`:
```
[SEED-RECEIPT] file=concept-taxonomy-pilot.jsonl, total=10, inserted=10, updated=0, rejected=0
[SEED-RECEIPT] file=concept-mapping-pilot.jsonl, total=143, inserted=150, updated=0, rejected=0
[ALL-CONCEPTS-RECEIPT] concepts=10, mappedQuestions=143, totalLinks=150
[CONCEPT-COUNTS-RECEIPT] critical-section-requirements=15, dining-philosophers=15, hardware-atomic-primitives=15, monitors-condition-variables=15, peterson-software-solutions=15, producer-consumer-bounded-buffer=15, race-interleavings=15, readers-writers=15, semaphore-operations=15, semaphore-state-tracing=15
```

### B. Per-Concept Distribution Table:
| Canonical Concept Slug | Canonical Label | Mapped Questions | Unique Statements | Baseline Lesson Status | Initial Coverage Status |
|---|---|---|---|---|---|
| `critical-section-requirements` | Critical Section & Mutual Exclusion Requirements | 15 | 15 | null | `MAPPED` |
| `peterson-software-solutions` | Peterson's Software Solution | 15 | 15 | null | `MAPPED` |
| `hardware-atomic-primitives` | Hardware Atomic Primitives (Test-and-Set, CAS, Spinlocks) | 15 | 15 | null | `MAPPED` |
| `semaphore-operations` | Semaphore Operations & Conventions | 15 | 15 | null | `MAPPED` |
| `semaphore-state-tracing` | Semaphore State Tracing | 15 | 15 | null | `MAPPED` |
| `producer-consumer-bounded-buffer` | Producer–Consumer / Bounded Buffer | 15 | 15 | null | `MAPPED` |
| `readers-writers` | Readers–Writers Problem | 15 | 15 | null | `MAPPED` |
| `dining-philosophers` | Dining Philosophers Problem | 15 | 15 | null | `MAPPED` |
| `monitors-condition-variables` | Monitors & Condition Variables | 15 | 15 | null | `MAPPED` |
| `race-interleavings` | Race Conditions & Interleavings | 15 | 15 | null | `MAPPED` |
| **Topic Rollup (`process-synchronization`)** | **Total: 10** | **143 distinct questions** | **143 distinct statements** | **0 ACCEPTED** | **Material: 10 / Explained: 0** |

---

## 3. Test Verification Receipts

### A. `ConceptSeedRunnerTest` (7 Unit Tests)
```
[INFO] Running com.gatelms.seed.ConceptSeedRunnerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.066 s
[INFO] BUILD SUCCESS
```
1. `honest-judge PASS - import taxonomy pilot inserts all 10 concepts with zero rejections`
2. `honest-judge PASS - re-running taxonomy import updates all 10 concepts idempotently`
3. `honest-judge PASS - taxonomy rejection on missing required fields or unknown topic`
4. `honest-judge PASS - import mappings pilot inserts all 143 mappings with zero rejections`
5. `honest-judge PASS - re-running mappings import updates all links idempotently`
6. `honest-judge PASS - mapping rejection on unknown question externalId or unknown concept slug`
7. `honest-judge PASS - seedAll executes both imports and counts concepts`

### B. `ConceptCoverageIT` (5 Integration Tests with Testcontainers PostgreSQL)
```
[INFO] Running com.gatelms.integration.ConceptCoverageIT
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 96.84 s
[INFO] BUILD SUCCESS
```
1. `honest-judge PASS - taxonomy 10 of 10 seeded and 100 percent of pilot bank mapped with 0 unmapped questions`
2. `honest-judge PASS - coverage endpoint returns honest split for process synchronization`
3. `honest-judge PASS - computed-on-read coverage status flipping` (proves: MAPPED → EXPLAINED on ACCEPTED unit; MAPPED → UNMAPPED on tag deletion; UNMAPPED → MAPPED on re-tagging)
4. `honest-judge PASS - admin-gated curation endpoints reject students with 403 ProblemDetail`
5. `honest-judge PASS - question detail endpoint returns conceptTags in QuestionDto`

### C. `JourneyApiIT` (9 Integration Tests)
```
[INFO] Running com.gatelms.integration.JourneyApiIT
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 40.70 s
[INFO] BUILD SUCCESS
```

### D. Full Backend Unit Suite (`*Test`)
```
[INFO] Tests run: 135, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```
*(All 135 unit tests green, up from 128 in WP0)*

### E. Frontend Vitest Suite
```
Test Files  52 passed (52)
     Tests  278 passed (278)
  Duration  51.16s
```

---

## 4. Architectural & Governance Guardrails Compliance

- **Flyway Migrations:** 0 modifications to V1–V16 migrations. V17 was landed in WP0 and approved as accepted deviation D-3. No schema changes made or needed in WP1.
- **God-Class Guardrail (§8.3):**
  - `ConceptCoverageService.kt`: 170 LOC, 6 constructor dependencies ($\le 600$ LOC, $\le 10$ ctor deps).
  - `ConceptSeedRunner.kt`: 230 LOC, 5 constructor dependencies ($\le 600$ LOC, $\le 10$ ctor deps).
  - `JourneyController.kt`: 120 LOC, 4 constructor dependencies.
  - `QuestionService.kt`: 460 LOC, 9 constructor dependencies.
- **Security (§8.5):** All curation endpoints (`/curation/mappings/unmapped`, `/curation/questions/{questionId}/concepts`) carry `@PreAuthorize("hasRole('ADMIN')")`. Student requests verifiably return `403 Forbidden` with standard RFC 7807 `ProblemDetail`.
- **Computed on Read (PC-4):** Coverage status is derived on read directly from `question_concept_tags` and `concept_units.status`; zero mutable enum columns in database, zero scheduled batch jobs.

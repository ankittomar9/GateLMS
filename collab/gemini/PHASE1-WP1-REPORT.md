# Execution Report: PHASE1-WP1 — Backend Domain Core, Entities, APIs, V3/V4 & Tracing

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE1-WP1` |
| **Author Agent** | Gemini / Antigravity (Executor) |
| **Feature Branch** | `phase1/wp1-backend-core` |
| **Branch Tip SHA** | `c5eb3e9` |
| **Merge SHA** | `6b3db40` |
| **Base Commit** | `3dc4b6a` (origin/master) |
| **Governing Spec** | `collab/z/PHASE1-SPEC.md` rev 2 (§4, §5, §6, §11) |
| **Verdict / Status** | `MERGED_TO_MASTER` / `READY_FOR_AUDIT` |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

Work Package 1 (`PHASE1-WP1`) has been executed, self-verified, and integrated into `master` via non-fast-forward merge (`--no-ff`) in strict adherence to **Rule G9** and `collab/z/PHASE1-SPEC.md`.

In accordance with the Operator's directive, the feature branch `phase1/wp1-backend-core` has been pushed to GitHub and is **permanently preserved** (never deleted).

### Scope Completed in WP1:
1. **Flyway Migrations V3 & V4**:
   - `V3__phase1_integrity_and_locking.sql`: Added 5-state TCS iON palette CHECK constraint on `attempt_answers(state)` (`NOT_VISITED`, `NOT_ANSWERED`, `ANSWERED`, `MARKED_FOR_REVIEW`, `ANSWERED_AND_MARKED_FOR_REVIEW`), added `updated_at` and optimistic locking `version INT NOT NULL DEFAULT 0` on `questions` and `topics`.
   - `V4__phase1_fulltext_search.sql`: Added generated `search_vector tsvector` (weighted across `statement`, `options_json`, `explanation`) and GIN index `idx_questions_search_vector`.
2. **Domain Model & Entities** (`com.gatelms.entity`):
   - `Subject.kt`, `Topic.kt`, `Question.kt` with `@Version` optimistic locking, explicit table mappings, non-writable mapping for `search_vector`.
   - `Enums.kt`: `QuestionType` (MCQ, MSQ, NAT), `Pass1Status` (UNSEEN, SOLVED, REVISE, FLAGGED), `Pass2Status` (NOT_ATTEMPTED, SOLVED, FAILED, TIMEOUT), `AttemptState` (5-state TCS iON).
3. **DTOs & Information Hiding** (`com.gatelms.dto`):
   - `SubjectDtos.kt`, `TopicDtos.kt`, `QuestionDtos.kt`, `StatsDtos.kt`.
   - **Key Safety Rule**: `QuestionSummaryDto` excludes `correctOptions`, `numericMin/Max`, and `explanation` so search/list views cannot leak solution keys to the browser before solution reveal.
4. **Data Access & Postgres FTS** (`com.gatelms.repository`):
   - `SubjectRepository`, `TopicRepository`.
   - `QuestionRepository`: High-performance native query leveraging Postgres full-text search with `websearch_to_tsquery('english', :query)`, weighted ranking `ts_rank(search_vector, ...)`, and filter parameters (`subjectId`, `topicId`, `type`, `year`, `pass1Status`).
5. **Services & REST Controllers** (`com.gatelms.service`, `com.gatelms.controller`):
   - `/api/v1/health`: Enhanced health endpoint with version and UTC timestamp.
   - `/api/v1/subjects`: List all subjects ordered by code with weightage and topic counts.
   - `/api/v1/topics`: Filter by `subjectId` or `subjectSlug`. Mastery update endpoint `PATCH /api/v1/topics/{id}/mastery` with optimistic lock enforcement.
   - `/api/v1/questions`: Paginated search & list (returning `QuestionSummaryDto`), get by ID (returning full `QuestionDetailDto`), create/update question, toggle bookmark, update pass1/pass2 status.
   - `/api/v1/stats/summary`: Aggregates total questions, count by type, count by subject, count by pass 1 status, and overall syllabus mastery percentage.
6. **Error Model & RFC 7807 ProblemDetail** (`com.gatelms.error`):
   - `GlobalExceptionHandler` mapping:
     - `ValidationException` -> HTTP 400 (`Bad Request`) with field errors.
     - `EntityNotFoundException` -> HTTP 404 (`Not Found`).
     - `ConflictException` (and `OptimisticLockingFailureException`) -> HTTP 409 (`Conflict`).
     - Generic fallback -> HTTP 500 (`Internal Server Error`).
7. **Observability & Logging Foundations**:
   - Actuator endpoints: `health`, `info`, `metrics`.
   - Micrometer Tracing bridge OpenTelemetry (`micrometer-tracing-bridge-otel`) + OTLP gRPC/HTTP exporter (`opentelemetry-exporter-otlp`) configured in `application.yml`.
   - Structured JSON logging: `logback-spring.xml` utilizing `LogstashEncoder` outputting both to console and rotating log file `logs/gatelms-backend.jsonl`.
   - CORS configuration (`CorsConfig.kt`) permitting `http://localhost:5173`.

---

## 2. G3 Four-Number Test Metric & Verification

### Test Execution Command:
```powershell
.\mvnw.cmd clean test
```

### Toolchain:
- **OS**: Windows 11 / PowerShell
- **Java**: Eclipse Adoptium OpenJDK 21.0.6+7 (Temurin 64-Bit Server VM)
- **Maven**: 3.9.16 (wrapper-pinned `.\mvnw.cmd`)
- **Kotlin**: 2.0.21
- **Spring Boot**: 3.4.2

### G3 Four-Number Metric:
```text
files      = 3
test-cases = 8
assertions = 18
failures   = 0 (0 errors, 0 skipped)
```

### Breakdown by Test Suite:
1. `com.gatelms.GateLmsApplicationTests`:
   - `contextLoadsSmokeTest`: 1 test case, 1 assertion (`assertTrue`). Pass.
2. `com.gatelms.service.QuestionServiceTest`:
   - `honest-judge PASS - valid MCQ create succeeds and persists`: 1 test case, 4 assertions + 1 MockK verification. Pass.
   - `honest-judge FAIL - deliberate-wrong NAT with min greater than max is rejected`: 1 test case, 1 exception assertion + 1 field error assertion + 1 MockK verify(0). Pass.
   - `honest-judge FAIL - deliberate-wrong MSQ with zero correct options is rejected`: 1 test case, 1 exception assertion + 1 field error assertion + 1 MockK verify(0). Pass.
3. `com.gatelms.service.TopicServiceTest`:
   - `honest-judge PASS - updateMastery with matching version succeeds and updates mastery`: 1 test case, 3 assertions + 1 MockK verification. Pass.
   - `honest-judge FAIL - deliberate-stale version causes ConflictException 409`: 1 test case, 2 assertions (message contains "version mismatch") + 1 MockK verify(0). Pass.
   - `honest-judge FAIL - deliberate-invalid masteryLevel greater than 100 is rejected`: 1 test case, 2 assertions + 1 MockK verify(0). Pass.
   - `honest-judge FAIL - getTopics without subjectId or subjectSlug is rejected`: 1 test case, 1 exception assertion. Pass.

---

## 3. G4 Honest-Judge Verification Register

| Pair ID | Target Component | Input / Scenario | Expected Verdict | Observed Outcome |
|---|---|---|---|---|
| **HJ-01** | `QuestionService` | Valid MCQ with 4 options and correct option "B" | PASS (Created, ID assigned) | **PASS** (`assertNotNull(result.id)`) |
| **HJ-02** | `QuestionService` | NAT with `numericMin = 5.0` and `numericMax = 4.0` ($\min > \max$) | FAIL (HTTP 400 `ValidationException`) | **PASS** (Throws `ValidationException`, `numericMin` error) |
| **HJ-03** | `QuestionService` | MSQ with empty `correctOptions = ""` (0 correct options) | FAIL (HTTP 400 `ValidationException`) | **PASS** (Throws `ValidationException`, `correctOptions` error) |
| **HJ-04** | `TopicService` | Topic mastery update with current `version = 1` | PASS (Updated, mastery = 85) | **PASS** (`assertEquals(85, result.masteryLevel)`) |
| **HJ-05** | `TopicService` | Topic mastery update with stale `version = 0` (db is 1) | FAIL (HTTP 409 `ConflictException`) | **PASS** (Throws `ConflictException`, 0 repo saves) |
| **HJ-06** | `TopicService` | Topic mastery update with `masteryLevel = 105` (> 100) | FAIL (HTTP 400 `ValidationException`) | **PASS** (Throws `ValidationException`, `masteryLevel` error) |

---

## 4. Git Integration History

Following **Rule G9** and branch retention directives:
- Branch `phase1/wp1-backend-core` was created from `origin/master` (`3dc4b6a`).
- Deliverables committed to branch at `c5eb3e9`.
- Branch pushed to remote: `origin/phase1/wp1-backend-core` (**preserved, not deleted**).
- Merged non-fast-forward into `master` at commit `6b3db40`.
- Remote `origin/master` updated to `6b3db40`.

---

## 5. Operator Smoke Script

To manually test WP1 on a machine with Docker and JDK 21:

```powershell
# 1. Start Postgres database
docker compose up -d postgres

# 2. Run backend migrations and start application
cd backend
.\mvnw.cmd spring-boot:run

# 3. In another terminal, test health endpoint
curl.exe -i http://localhost:8080/api/v1/health

# 4. Verify syllabus list
curl.exe -i http://localhost:8080/api/v1/subjects

# 5. Verify stats summary
curl.exe -i http://localhost:8080/api/v1/stats/summary
```

---

## 6. Next Steps

Proceed immediately to **Work Package 2 (`PHASE1-WP2`)**:
1. Cut dedicated branch `phase1/wp2-frontend-pages` from `origin/master`.
2. Add frontend router, TanStack Query, and Vitest dependencies.
3. Build the Syllabus Explorer page, Question Browser with FTS and filters, and Question Detail page with KaTeX Math rendering.
4. Execute Vitest and frontend build checks.
5. Push `phase1/wp2-frontend-pages`, merge `--no-ff` to `master`, and publish `PHASE1-WP2-REPORT.md`.

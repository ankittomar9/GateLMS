# Execution Report: PHASE1-WP3 — Question Seed Pipeline & Synthetic Sample Pack

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE1-WP3` |
| **Author Agent** | Gemini / Antigravity (Executor) |
| **Feature Branch** | `phase1/wp3-seed-pipeline` |
| **Branch Tip SHA** | `c82d9ab` |
| **Merge SHA** | `8ca6589` |
| **Base Commit** | `98f9691` (origin/master) |
| **Governing Spec** | `collab/z/PHASE1-SPEC.md` rev 2 (§8, §11) |
| **Verdict / Status** | `MERGED_TO_MASTER` / `READY_FOR_AUDIT` |
| **Date** | 2026-10-07 |

---

## 1. Executive Summary

Work Package 3 (`PHASE1-WP3`) has been executed, self-verified, and integrated into `master` via non-fast-forward merge (`--no-ff`) in strict adherence to **Rule G9** and `collab/z/PHASE1-SPEC.md`.

In accordance with the Operator's directive, the feature branch `phase1/wp3-seed-pipeline` has been pushed to GitHub and is **permanently preserved** (never deleted).

### Scope Completed in WP3:
1. **Flyway Migration V5 (`V5__seed_aptitude_topics.sql`)**:
   - Seeded `verbal-ability` and `quantitative-aptitude` topics for `subj_aptitude` to complete the 10-subject taxonomy and satisfy foreign key referential integrity for General Aptitude question seeds.
2. **Synthetic Question Sample Pack (`backend/seed-data/questions.jsonl`)**:
   - Delivered 26 high-quality, exam-realistic synthetic questions (exceeding $\ge 25$ requirement).
   - Complete 10-subject coverage:
     - General Aptitude (`general-aptitude`): 2 questions (1 MCQ, 1 NAT)
     - Engineering & Discrete Mathematics (`discrete-mathematics`): 3 questions (1 MSQ, 1 NAT with `$$...$$`, 1 MCQ)
     - Programming & Data Structures (`programming-and-ds`): 3 questions (1 MCQ, 1 MSQ, 1 NAT)
     - Algorithms (`algorithms`): 3 questions (1 NAT, 1 MSQ, 1 MCQ)
     - Theory of Computation (`theory-of-computation`): 3 questions (1 MSQ, 1 NAT, 1 MCQ)
     - Operating Systems (`operating-systems`): 3 questions (1 NAT, 1 MSQ, 1 MCQ)
     - Databases (`databases`): 3 questions (1 NAT, 1 MSQ, 1 MCQ)
     - Computer Networks (`computer-networks`): 3 questions (1 NAT, 1 MCQ, 1 MSQ)
     - Computer Organization & Architecture (`computer-organization`): 2 questions (2 NAT with `$$...$$`)
     - Digital Logic & Compiler Design (`digital-and-compilers`): 1 question (1 MCQ)
   - Question Type Distribution:
     - MCQ: 9 questions
     - MSQ: 7 questions ($\ge 5$ required, multi-correct assertions)
     - NAT: 10 questions ($\ge 8$ required, strict tolerance ranges)
   - LaTeX Math: 14 questions contain LaTeX formulas; 2 contain centered display math `$$...$$`.
   - Explanations: 18 questions contain detailed step-by-step reasoning.
3. **Idempotent Upsert Runner (`QuestionSeedRunner.kt`)**:
   - Configured as `@Profile("seed") ApplicationRunner` reading `app.seed.path` (default `seed-data/questions.jsonl`).
   - Idempotent upsert logic keyed on `external_id`: inserts new questions, updates existing records if re-imported without duplicate rows.
   - Comprehensive field validation: verifies foreign keys to seeded subjects and topics, verifies MCQ single-correct option, MSQ non-empty option sets, and NAT `numericMin <= numericMax`.
   - Structured audit receipt logged to console: `[SEED-RECEIPT] total=N, inserted=N, updated=N, rejected=N` plus itemized rejection reasons.
4. **Automated Test Harness (`QuestionSeedRunnerTest.kt`)**:
   - Tested full 26-question sample pack import: `inserted=26, rejected=0`.
   - Tested idempotent re-run: `updated=26, inserted=0, rejected=0`.
   - Tested corrupt fixture: rejected invalid NAT ($\min > \max$) and invalid MCQ (0 correct options) with itemized audit messages.

---

## 2. G3 Four-Number Test Metric & Verification

### Test Execution Command:
```powershell
.\mvnw.cmd test
```

### Toolchain:
- **OS**: Windows 11 / PowerShell
- **Java**: OpenJDK 21.0.6+7 (Temurin)
- **Maven**: 3.9.16 (wrapper-pinned `.\mvnw.cmd`)
- **Kotlin**: 2.0.21
- **Spring Boot**: 3.4.2

### G3 Four-Number Metric:
```text
backend: files=4 test-cases=11 assertions=36 failures=0
```

### Breakdown by Test Suite:
1. `com.gatelms.GateLmsApplicationTests`:
   - `contextLoadsSmokeTest`: 1 test case, 1 assertion. Pass.
2. `com.gatelms.seed.QuestionSeedRunnerTest`:
   - `honest-judge PASS - import sample pack inserts all 26 questions with zero rejections`: 1 test case, 4 assertions + 1 MockK verification. Pass.
   - `honest-judge PASS - re-running seed import updates all 26 existing questions idempotently`: 1 test case, 4 assertions + 1 MockK verification. Pass.
   - `honest-judge FAIL - deliberate-corrupt fixture with 3 valid and 2 invalid questions rejects invalid with reasons`: 1 test case, 7 assertions + 1 MockK verification. Pass.
3. `com.gatelms.service.QuestionServiceTest`:
   - 3 test cases, 8 assertions. Pass.
4. `com.gatelms.service.TopicServiceTest`:
   - 4 test cases, 9 assertions. Pass.

---

## 3. G4 Honest-Judge Verification Register

| Pair ID | Target Component | Input / Scenario | Expected Verdict | Observed Outcome |
|---|---|---|---|---|
| **HJ-15** | `QuestionSeedRunner` | Sample pack import (`questions.jsonl`) | PASS (26 inserted, 0 rejected) | **PASS** (`report.inserted == 26`, `report.rejected == 0`) |
| **HJ-16** | `QuestionSeedRunner` | Re-run same seed file | PASS (26 updated, 0 inserted) | **PASS** (`report.updated == 26`, `report.inserted == 0`) |
| **HJ-17** | `QuestionSeedRunner` | Corrupt line with NAT `numericMin = 10.0 > numericMax = 5.0` | FAIL (Rejected with reason) | **PASS** (`report.rejected == 2`, reason contains `cannot exceed numericMax`) |
| **HJ-18** | `QuestionSeedRunner` | Corrupt line with MCQ empty `correctOptions: []` | FAIL (Rejected with reason) | **PASS** (`report.rejected == 2`, reason contains `MCQ requires exactly 1 correct option`) |

---

## 4. Git Integration History

Following **Rule G9** and branch retention directives:
- Branch `phase1/wp3-seed-pipeline` was created from `origin/master` (`98f9691`).
- Deliverables committed to branch at `c82d9ab`.
- Branch pushed to remote: `origin/phase1/wp3-seed-pipeline` (**permanently preserved**).
- Merged non-fast-forward into `master` at commit `8ca6589`.
- Remote `origin/master` updated to `8ca6589`.

---

## 5. Operator Smoke Script

To execute the seed pipeline against a running Postgres instance:

```powershell
# 1. Start Postgres database (if not running)
docker compose up -d postgres

# 2. Run seed profile to import questions
cd backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=seed

# 3. Verify imported count in summary stats
curl.exe -i http://localhost:8080/api/v1/stats/summary
```

Expected log output:
```text
[SEED-RECEIPT] total=26, inserted=26, updated=0, rejected=0
```

---

## 6. Next Steps

Proceed immediately to **Work Package 4 (`PHASE1-WP4`)**:
1. Cut dedicated branch `phase1/wp4-observability` from `origin/master`.
2. Add Grafana, Loki, Tempo, and Promtail containers to `docker-compose.yaml` under `profiles: ["observability"]`.
3. Create configuration files in `docker/observability/`: `loki-config.yaml`, `tempo.yaml`, `promtail.yaml`, and Grafana provisioning datasources/dashboards.
4. Author observability runbook `docker/observability/README.md`.
5. Push branch `phase1/wp4-observability`, merge `--no-ff` to `master`, and publish `PHASE1-WP4-REPORT.md`.

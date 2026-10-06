# AGENTS.md — Rules & Governance for AI Coding Agents in GateLMS

## 1. Repository Facts & Environment
- **Monorepo Structure**: 
  - `backend/`: Maven · Java 21 (Temurin) · Kotlin · Spring Boot 3.4+ · Spring Data JPA · Flyway · PostgreSQL 16
  - `frontend/`: React 19 · Vite · TypeScript · Tailwind CSS (TakeUForward dark theme) · Lucide Icons · Recharts · KaTeX
  - `collab/`: Multi-agent review and governance directory (Gemini, Z.ai, Qwen, Operator)
  - `docker/`: Container provisioning (`docker-compose.yaml`)
- **Default Ports**:
  - PostgreSQL: `5432` (`gatelms_db`)
  - Backend API: `8080` (or `8081` if conflict)
  - Frontend SPA: `5173`
- **Git Identity Isolation**:
  - This repository uses a dedicated, isolated GitHub account.
  - **HARD RULE**: NEVER configure git globally (`git config --global`).
  - Use ONLY local repository configuration: `git config --local user.name "<NAME>"` and `git config --local user.email "<EMAIL>"`.

---

## 2. Code Conventions & Standards
- **Backend (Kotlin + Spring Boot)**:
  - Constructor injection only; prefer Kotlin idiomatic primary constructors.
  - Entities use JPA with explicit column annotations and table mappings.
  - DTOs are immutable Kotlin `data class` types.
  - Database schema changes MUST be managed strictly via Flyway migrations under `backend/src/main/resources/db/migration/V{n}__{description}.sql`.
  - Zero Lombok. Kotlin data classes provide all boilerplate natively.
  - Controller endpoints follow RESTful conventions under `/api/v1/...`.
- **Frontend (React + Vite + Tailwind)**:
  - Design tokens strictly honor the **TakeUForward (TUF) dark palette**:
    - Primary background: `#0d0d0d`
    - Card background: `#161616` with subtle `#262626` borders
    - Accents: Gold/Amber (`#f59e0b`), Emerald (`#10b981`), Cyan (`#06b6d4`)
  - Math & Discrete Logic formulas rendered using KaTeX (`$...$` inline, `$$...$$` display).
  - Code execution tracing uses Monaco Editor or syntax-highlighted Prism blocks.
  - Absolute path alias `@/` mapped to `frontend/src`.

---

## 3. Multi-Agent Collaboration Protocol (collab/)
Adapted from repo collaboration standard:
```text
collab/
  README.md                 ← Collab protocol definition
  AUDIT-PROTOCOL.md         ← Rigorous review & gate checklist
  gemini/                   ← Execution reports, proposals, PR specs (writable by Gemini / Antigravity)
  z/                        ← SPECs, architectural audits, adjudications (writable by Z.ai)
  qwen/                     ← Independent reviews & 2nd-pair-of-eyes audits (writable by Qwen)
  operator/                 ← Directives, decisions, and manual verification notes (writable by human Operator)
```

### Collaboration Rules:
1. **Isolated Agent Folders**: Each agent writes ONLY inside its designated subfolder. Never modify another agent's review artifacts.
2. **File Naming Standard**: Named as `<TASK-ID>-<TYPE>.md` (e.g. `collab/gemini/PHASE0-SETUP-REPORT.md`, `collab/z/PHASE0-AUDIT.md`, `collab/qwen/PHASE0-REVIEW.md`).
3. **Cross-References by Path**: Reference previous findings by repository path, not chat quotes.
4. **Header Requirement**: Every collab artifact must declare:
   - Task ID
   - Author Agent
   - Base Commit Short Hash
   - Status / Verdict line (e.g. `VERDICT: APPROVED` or `VERDICT: CHANGES_REQUESTED`)

---

## 4. Operational Guardrails (Standing Prohibitions)

### G1 — Container & Deployment Hygiene
- Never hot-patch running containers or inject untracked jar files.
- The standard local path is: database in Docker (`docker compose up -d postgres`), backend and frontend run locally via Maven (`./mvnw spring-boot:run`) and Vite (`npm run dev`).

### G2 — Flyway Receipts Are Never Forged
- Never manually insert, edit, or delete rows in `flyway_schema_history`.
- If a migration fails:
  1. Fix the migration SQL.
  2. Run `mvn flyway:repair` or drop & recreate `gatelms_db` in development.
- A migration that requires a manually forged receipt is a bug. Fix the SQL, not the receipt.

### G3 — Four-Number Test Reporting
When reporting test runs, all agents must provide the exact 4-number metric:
```text
files      = count of test source files executed
test-cases = executed test cases (JUnit @Test methods / Vitest test() blocks)
assertions = actual assertion calls executed (write 'n/r' if runner does not report)
failures   = count of failed tests (list errors and skipped separately)
```

### G4 — Honest-Judge Verification
Every critical calculation (especially the GATE scoring engine: MCQ negative marking $-1/3$, $-2/3$; MSQ all-or-nothing; NAT tolerance $[\min, \max]$) must include:
- A known-good input $\rightarrow$ assert `PASS`
- A deliberately-wrong input $\rightarrow$ assert `FAIL`

### G5 — Secret Hygiene & Zero Plaintext Secrets
- Never commit `.env`, passwords, personal access tokens (PATs), or credentials to git.
- Reference secrets via environment variables (`${POSTGRES_PASSWORD}`, etc.).

### G6 — Single Source of Truth
- Active system requirements, relational schemas, and phase deliverables are governed exclusively by `SPEC.md`.

### G7 — Shell Execution Discipline
- Run focused commands. Do not use destructive recursive commands (`rm -rf *` / `Remove-Item *`) with wildcards.
- Clean working trees: commit files atomically per task.

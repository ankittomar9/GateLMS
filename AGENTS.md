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

### G8 — Multi-Agent Collab Folders Protocol
- Each agent writes strictly within its dedicated folder under `collab/<agent>/`. No cross-folder modifications.
- File naming format: `<TASK-ID>-<TYPE>.md` (e.g. `collab/gemini/PHASE0-FIXUP-REPORT.md`, `collab/z/PHASE0-SETUP-AUDIT.md`, `collab/qwen/PHASE0-SETUP-REVIEW.md`).
- Review cycle: Executor implements & reports -> Reviewers audit & report -> Operator adjudicates. All exchange occurs via git on `master`.
- Secret hygiene (G5) applies strictly before pushing any collab artifact.
- Raw session transcripts stay out of git; `collab/` files are the only durable records.

### G9 — Strict Branch Hygiene & Integration Protocol (Zero Code Changes on Master)

> **MANDATORY HARD RULE: ZERO CODE CHANGES ON MASTER.**
> Under NO circumstances may any code change, bug fix, refactor, or feature implementation be performed directly on `master`.
> The working tree on `master` must remain pristine. Every single code modification MUST happen on an isolated, dedicated branch cut fresh from `master`.

#### 1. Branch Naming Discipline
All branches must use clear, descriptive names reflecting the domain, feature/bug, and explicit sequential fixup numbering:
- **Features / Work Packages:** `<domain>/<feature-slug>`
  *Examples:* `ai/studio-ux-1`, `phase8/wp2-concept-lessons`, `auth/jwt-rotation`
- **Bug Fixes / Investigations:** `<domain>/<feature-or-bug-slug>-fix`
  *Examples:* `ai/topic-primer-latex-escape-fix`, `db/connection-leak-fix`
- **Sequential Fixups:** `<domain>/<feature-slug>-fixup-<N>` (where `<N>` is an incrementing integer `1, 2, 3... 10... 100`)
  *Examples:* `ai/provider-adapter-fixup-1`, `ai/studio-ux-fixup-1`, `ai/studio-ux-fixup-2`
  *Rule:* If an issue, audit finding, or regression occurs on an existing feature, cut `...-fixup-<N>` with the next sequential integer. Never overwrite or re-use an existing branch name.

#### 2. Strict Workflow Execution Steps
Every development/fix task must follow this exact sequence:
1. **Sync Base:** Ensure `master` is clean and up to date with remote:
   ```bash
   git checkout master
   git pull origin master
   ```
2. **Cut Dedicated Branch:**
   ```bash
   git checkout -b <domain>/<branch-name> origin/master
   ```
3. **Execute & Test:** Make changes ONLY while checked out on that branch. Run all required honest-judge tests, unit tests, and full test suites.
4. **Author Report:** Write the execution report in `collab/gemini/<TASK>-REPORT.md`.
5. **Commit on Branch:** Stage and commit changes atomically:
   ```bash
   git commit -m "<type>(<scope>): <clear description>"
   ```
6. **Push Branch to Remote:** Push the branch to GitHub so all changes and diffs are immediately visible and reviewable:
   ```bash
   git push -u origin <domain>/<branch-name>
   ```
7. **Merge to Master with Non-Fast-Forward (`--no-ff`):**
   ```bash
   git checkout master
   git merge --no-ff <domain>/<branch-name> -m "merge(<scope>): <description>"
   ```
8. **Push Master to Remote:**
   ```bash
   git push origin master
   ```
9. **Permanent Branch Retention:**
   NEVER delete the feature or fixup branch locally or remotely (`git branch -d` and `git push origin --delete` are strictly forbidden). All branches must be permanently retained in GitHub for full traceability, audit history, and visual commit tracking.

#### 3. Strict Prohibitions
- **NO Direct Edits on Master:** Never edit or stage code files while on `master`.
- **NO Fast-Forward Merges to Master:** Always use `git merge --no-ff` so the merge commit clearly encapsulates the branch topology.
- **NO Mixing of Tasks:** One branch carries strictly one feature or fixup.
- **NO Force-Pushing Master:** `--force-with-lease` is allowed ONLY on the executor's own unmerged branch. Force-pushing `master` is completely prohibited.
- **Doc-only commits:** Only documentation changes strictly inside `collab/**` or project governance docs may land directly on `master` when explicitly permitted; all code changes must follow the branch workflow above.



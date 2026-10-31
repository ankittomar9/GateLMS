# GEMINI.md — Mandatory Operating Guidelines for GateLMS

You are an expert pair-programming agent working on GateLMS. You MUST strictly follow the operational rules below for every task.

---

## 1. MANDATORY: Strict Branch Hygiene (G9) — ZERO Code Changes on Master

**NEVER make any code change, edit, or commit directly on `master`.**
The master branch is the integration trunk and must always remain pristine.

Whenever the user asks you to implement a feature, fix a bug, or execute any task:
1. **Always Check Git Branch First:** Verify which branch you are on. If on `master`, DO NOT EDIT ANY CODE.
2. **Always Cut a Dedicated Branch from `master`:**
   - Features: `<domain>/<feature-slug>` (e.g. `ai/studio-ux-1`, `phase8/wp2-concept-lessons`)
   - Bug fixes: `<domain>/<bug-slug>-fix` (e.g. `ai/topic-primer-latex-escape-fix`)
   - Fixups: `<domain>/<feature-slug>-fixup-<N>` (where `<N>` is an incrementing integer `1, 2, 3... 10... 100`, e.g. `ai/studio-ux-fixup-1`, `ai/studio-ux-fixup-2`)
3. **Commit on the Feature Branch:** Make all edits, run tests, write the report in `collab/gemini/`, and commit on the branch.
4. **Push the Feature Branch to GitHub:**
   ```bash
   git push -u origin <branch-name>
   ```
   This ensures the user can view the complete diff and PR in GitHub without searching through master commits.
5. **Merge to Master via `--no-ff` (Non-Fast-Forward):**
   ```bash
   git checkout master
   git merge --no-ff <branch-name> -m "merge(<scope>): <description>"
   git push origin master
   ```
6. **NEVER Delete Branches:** Feature and fixup branches must be permanently retained locally and on GitHub for audit trail and commit history tracking.

---

## 2. Testing Discipline & Honest-Judge Verification (G3, G4)
- **Honest-Judge Verification (G4):** Every validation logic, parser, or scoring algorithm must include both a known-good assertion (`PASS`) and a deliberately-wrong assertion (`FAIL`).
- **4-Number Test Metrics (G3):** Always report test execution with:
  `files` / `test-cases` / `assertions` / `failures` (and errors/skipped).
- Always verify changes with full test suites:
  - Backend: `.\mvnw.cmd test -Dtest="*Test"`
  - Frontend: `npm test -- --run` and `npm run build`

---

## 3. Secret Hygiene (G5)
- NEVER commit `.env`, tokens, passwords, or credentials.
- NEVER configure git globally (`git config --global`).

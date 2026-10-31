---
description: Mandatory strict branch hygiene rules for all code changes in GateLMS
globs: ["**/*"]
always_on: true
---

# GateLMS Branch Hygiene & Development Protocol

## ZERO CODE CHANGES ON MASTER
Under no circumstances may any code change, bug fix, or refactor be made while checked out on `master`.

### Mandatory Workflow
1. **Branch First:** Cut a dedicated branch from `origin/master`:
   ```bash
   git checkout master
   git pull origin master
   git checkout -b <domain>/<name>
   ```
2. **Branch Naming Standard:**
   - Features: `<domain>/<feature-slug>` (e.g. `ai/studio-ux-1`)
   - Bug fixes: `<domain>/<bug-slug>-fix` (e.g. `ai/topic-primer-latex-escape-fix`)
   - Sequential fixups: `<domain>/<feature-slug>-fixup-<N>` where `<N>` = 1, 2, 3... 10... 100 (e.g. `ai/studio-ux-fixup-1`)
3. **Commit & Push Branch:**
   - Commit changes atomically on the branch with standard conventional commit messages.
   - Always push the branch to remote GitHub: `git push -u origin <branch-name>` so the branch and its diff are immediately inspectable.
4. **Merge to Master via `--no-ff`:**
   - Check out `master`.
   - Merge using `--no-ff`: `git merge --no-ff <branch-name> -m "merge(<scope>): <description>"`.
   - Push `master` to remote: `git push origin master`.
5. **Never Delete Branches:**
   - Keep all branches permanently locally and on GitHub. Never run branch deletion commands.

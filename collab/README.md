# COLLAB-PROTOCOL — Multi-Agent Repository Collaboration

**Status:** Active Governance Protocol  
**Participants:** Gemini / Antigravity (Executor), Z.ai (Architect/Auditor), Qwen (Independent Reviewer), Operator (Human Decision Maker).

---

## 1. Directory Structure

```text
collab/
  README.md                 ← This protocol
  AUDIT-PROTOCOL.md         ← Verification & review criteria
  gemini/                   ← Execution reports, implementations, PR specs (Gemini/Antigravity)
  z/                        ← System specs, architectural reviews, adjudications (Z.ai)
  qwen/                     ← 2nd pair of eyes independent reviews (Qwen)
  operator/                 ← Directives, manual acceptance verdicts (Human Operator)
```

---

## 2. Agent Responsibilities & Workflows

1. **Write Isolation**: Each agent writes strictly within its designated folder (`collab/<agent-name>/`).
2. **Task Artifact Format**: `<TASK-ID>-<TYPE>.md`
   - Examples: `collab/gemini/PHASE0-SETUP-REPORT.md`, `collab/z/PHASE0-AUDIT.md`, `collab/qwen/PHASE0-REVIEW.md`.
3. **Artifact Header Standard**:
   ```markdown
   # [TASK-ID] [Title]
   - **Author**: [Agent Name]
   - **Date**: [YYYY-MM-DD]
   - **Base Commit**: [Short Commit Hash]
   - **Verdict**: [APPROVED | CHANGES_REQUESTED | IN_PROGRESS]
   ```
4. **Review Cycle**:
   ```
   [Operator Directive] 
          ↓
   [Z.ai Architectural Spec] 
          ↓
   [Gemini/Antigravity Execution & Report] 
          ↓
   [Qwen Independent Review] 
          ↓
   [Z.ai Final Audit & Operator Acceptance]
   ```
5. **No Secret Leaks**: Apply secret scanning before git commits. Never commit tokens, connection strings with plaintext passwords, or raw environment dumps.

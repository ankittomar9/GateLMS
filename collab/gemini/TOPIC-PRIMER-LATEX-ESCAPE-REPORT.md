# Topic Primer & Anatomy JSON Parser LaTeX Escape Repair Report

**Date:** 2026-10-31  
**Branch:** `ai/topic-primer-latex-escape-fix`  
**Base:** `master` (`b0dbff0`)  
**Package:** Topic Primer & Anatomy JSON Parser LaTeX Escape Resilience

---

## 1. Problem Statement & Root Cause Analysis

### Symptoms
When generating the Theory Primer for mathematical topics like `topic_algo_asymptotic` (`/topics/topic_algo_asymptotic`), primer generation failed with 502 Bad Gateway and the following error in the UI and Grafana Loki logs:
```
Topic primer generation attempt 1 failed for topic 'topic_algo_asymptotic': Malformed or unparseable JSON: Unrecognized character escape 'i' (code 105) at [Source: REDACTED; line: 2, column: 191] (through reference chain: com.gatelms.anatomy.service.TopicPrimerPayload["motivation"])
Topic primer generation attempt 2 failed for topic 'topic_algo_asymptotic': Malformed or unparseable JSON: Unrecognized character escape 'i' (code 105) ...
```
Additionally, during Anatomy dossier generation, unquoted keys occasionally produced:
```
Unexpected character ('v' (code 118)): was expecting double-quote to start field name
```

### Root Cause
1. **RFC 8259 String Escapes:**
   In standard JSON strings, only `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, and `\uXXXX` are legal escape sequences.
   When LLMs generate mathematical and theoretical content (e.g. for asymptotic analysis), they routinely generate LaTeX math commands with single backslashes inside JSON strings (e.g., `\in`, `\infty`, `\le`, `\ge`, `\theta`, `\frac`, `\cdot`, `\Omega`, `\Theta`).
   For commands like `\in` or `\infty`, Jackson encounters `\` followed by `i`, which is illegal in JSON. Jackson immediately halts with `JsonParseException: Unrecognized character escape 'i' (code 105)`.
2. **Hidden Control-Escape Corruption:**
   Enabling Jackson's `ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER` alone is insufficient because:
   - Jackson's `_decodeEscaped()` drops the backslash (e.g. `\in` becomes `in`, `\infty` becomes `infty`), corrupting math notation.
   - Standard JSON decodes `\frac` to `<formfeed>rac` (`\f`), `\to` / `\text` to `<tab>o` / `<tab>ext` (`\t`), and `\beta` to `<backspace>eta` (`\b`), breaking downstream KaTeX rendering.
   - Non-hex characters after `\u` (e.g. `\underbrace`, `\uparrow`) crash the parser expecting 4 hex digits.

---

## 2. Solution Architecture

### A. Pre-parse JSON Escape Repair Engine (`repairJsonEscapes`)
Implemented `AnatomyValidationService.repairJsonEscapes(json: String): String` as a companion method:
- Scans inside JSON string literals tracking escape states.
- Preserves valid JSON escapes: `\"`, `\\`, `\/`.
- Distinguishes valid `\uXXXX` Unicode code points from LaTeX commands (`\underbrace`, `\uparrow`), converting the latter to `\\u`.
- Distinguishes standard JSON newlines `\n` from LaTeX commands (`\neq`, `\ne`, `\notin`, `\nabla`, `\natural`, `\nu`, `\neg`), converting the latter to `\\n`.
- Distinguishes standard tabs `\t` from LaTeX commands (`\to`, `\text`, `\textbf`, `\times`, `\theta`, `\tau`, `\tan`, `\top`, `\tilde`, `\tfrac`, `\triangle`, `\therefore`, `\tag`), converting the latter to `\\t`.
- Distinguishes form feeds `\f` and backspaces `\b` from LaTeX commands (`\frac`, `\forall`, `\beta`, `\binom`, `\mathbf`, etc.), converting them to `\\f` and `\\b`.
- Automatically doubles single backslashes before all other characters (`\i`, `\l`, `\g`, `\c`, `\O`, `\T`, `\{`, `\}`, `\ `, etc.), transforming illegal single escapes into valid JSON `\\...`.
- Jackson decodes `\\...` into literal `\...`, perfectly preserving LaTeX commands for downstream KaTeX rendering.

### B. Defense-in-Depth Jackson Parser Configuration
Configured Jackson `ObjectMapper` in `AnatomyValidationService`, `ConceptLessonService`, and `ConceptService` with:
- `JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true`
- `JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true`
- `JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true`
- `JsonParser.Feature.ALLOW_SINGLE_QUOTES, true`

### C. Prompt Reinforcement
Updated `TopicPrimerService.buildTopicPrimerPrompt` rule 3 to instruct the LLM:
`When writing LaTeX commands in JSON, always escape backslashes (e.g. \\in, \\infty, \\le, \\ge, \\Theta, \\frac).`

---

## 3. Test Coverage & Verification Receipts

### 1. Targeted Backend Unit Tests
- `TopicPrimerValidationTest`:
  - `parseAndValidatePrimer parses and preserves unescaped LaTeX backslashes without corruption` (PASS)
  - `repairJsonEscapes correctly handles unicode vs latex and legitimate newlines` (PASS)
- `AnatomyJsonParserTest`:
  - `parseAndValidate should tolerate unquoted field names and single quotes` (PASS)
  - `parseAndValidate should repair unescaped LaTeX backslashes in anatomy payloads` (PASS)
- `AnatomyGenerationServiceTest`: 4 tests (PASS)
- Result: **22 tests run, 0 failures, 0 errors**

### 2. Full Backend Regression Suite
- Ran: `.\mvnw.cmd test -Dtest="*Test"`
- Result: **175 tests run, 0 failures, 0 errors, 1 skipped** (BUILD SUCCESS)

### 3. Full Frontend Vitest Suite
- Ran: `npm test -- --run`
- Result: **55 test files passed (55), 300 tests passed (300)**

### 4. Full Frontend Production Build
- Ran: `npm run build` (`tsc -b && vite build`)
- Result: **Zero TypeScript errors, production assets bundled cleanly**

### 5. Git Diff Hygiene
- Ran: `git diff --check`
- Result: **Zero whitespace, line-ending, or formatting defects**

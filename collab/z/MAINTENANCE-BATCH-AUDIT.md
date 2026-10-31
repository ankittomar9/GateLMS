# [MAINTENANCE-BATCH] Five-Package Post-Phase-8 Audit — Full Regime

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-11-01 (audited at `origin/master = 17a2f12`, tree clean, local == remote)
- **Range:** `f1e098c..17a2f12` — five operator-directed executor packages, each one commit +
  one `--no-ff` merge, all branches pushed and retained:
  - **P1** `ai/anatomy-generation-resilience` (`52a6159` → merge `b0dbff0`) — report
    `collab/gemini/ANATOMY-GENERATION-RESILIENCE-REPORT.md`
  - **P2** `ai/topic-primer-latex-escape-fix` (`7bfb1e8` → merge `d93b1a9`) — report
    `collab/gemini/TOPIC-PRIMER-LATEX-ESCAPE-REPORT.md`
  - **P3** `governance/strict-branch-hygiene-rules` (`5fd5e93` → merge `8b6e66e`) — no executor
    report (see F-9)
  - **P4** `ai/topic-primer-timeout-and-packaging-fix` (`3c7319a` → merge `9cc27a8`) — report
    `collab/gemini/TOPIC-PRIMER-TIMEOUT-DOCKER-FIX-REPORT.md`
  - **P5** `ai/topic-primer-rich-formatting` (`4f417df` → merge `17a2f12`) — report
    `collab/gemini/TOPIC-PRIMER-RICH-FORMATTING-REPORT.md`
- **Verdict:** **PASS on all five packages — batch accepted as merged.** All findings are LOW /
  documentation-level; no binding conditions. Details per package below; fresh receipts reproduce
  exactly (§3), freeze lists hold (§4), security posture unchanged (§5).

---

## 1. Method

Full regime per package: diff read hunk-by-hunk (all 35 changed files reviewed; new test files read
in full), report cross-check, freeze verification across the whole range, secret scan, whitespace
scan, dependency-tree delta check, and independent offline reproduction of all three receipts
(full backend `*Test` suite, full vitest suite, production build). Environment note: the audit
sandbox had been recycled (Maven dist and `~/.m2` wiped); Maven 3.9.16 was re-provisioned under
`/home/z/my-project/.tools/` with the dependency repo pinned to `/home/z/my-project/.m2repo/` so
the cache now survives future recycles. Full suite ran clean on first attempt — no environment
excuse needed.

## 2. Per-package verification

### P1 — Anatomy generation resilience (`52a6159`) — PASS

Root cause plausible and evidenced (503 spike → fallback → malformed JSON → corrupt DRAFT row
persisted, then permanently served from cache). Fixes verified line-level:

- `AnatomyGenerationService.kt`: cache check now `!force && status != REJECTED && parseError == null
  && testedConcept != "Generation parsing incomplete"` — exactly the four-guard form; the
  corrupt-placeholder persistence branch is **deleted** and replaced by a `ResponseStatusException
  (BAD_GATEWAY)` throw before any write; regeneration overwrites the legacy row in-place
  (`existing.orElseGet`) and clears `parseError`; `emitConceptTags` fires only on validated
  payloads. The original "never 500" contract is honored — 502 is deliberate gateway semantics.
- `AnatomyController.kt`: `@RequestParam(defaultValue = "false") force` threaded through;
  `QuestionAnatomyRepository.findQuestionIdsWithValidAnatomy` filters `parseError IS NULL AND
  testedConcept <> 'Generation parsing incomplete'` (JPQL literal — fine).
- Frontend: `anatomyApi.generateQuestionAnatomy` gains `force?: boolean` → `?force=true`;
  `AnatomyPanel` adds the amber error banner (`data-testid="anatomy-error-banner"`), Retry button
  wired to `handleGenerate(true)`, admin force-regenerate header button, and Section-1 concealment
  when `testedConcept === 'Generation parsing incomplete'` (verified at L213/L244);
  `LayeredHints` conceals incomplete/errored anatomies in study mode.
- Tests: `AnatomyGenerationServiceTest` — 4 substantive behavior tests (valid cache hit, force
  bypass, corrupt-row bypass, 502-throw + no-persist assertion). Not tautological.

### P2 — Topic-primer LaTeX escape repair (`7bfb1e8`) — PASS (with F-7)

The RFC 8259 analysis is correct and the engine is sound:

- `AnatomyValidationService.repairJsonEscapes` — in-string state machine; preserves `\"`, `\\`,
  `\/`, genuine `\uXXXX` (hex-verified), and genuine control escapes; disambiguates `\f \b \t \n \r`
  from LaTeX commands via command tables with word-boundary checks (`isLatexTCommand` /
  `isLatexNCommand` / `isLatexRCommand`); any other single backslash is doubled; trailing backslash
  hardened. Applied inside `extractJson` (which also gained `IGNORE_CASE` fence matching).
- Jackson leniency (`ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER`, `ALLOW_UNQUOTED_CONTROL_CHARS`,
  `ALLOW_UNQUOTED_FIELD_NAMES`, `ALLOW_SINGLE_QUOTES`) added in `AnatomyValidationService`,
  `ConceptLessonService`, `ConceptService` — parse-input hardening only; no serialization or
  security surface change.
- `TopicPrimerService` prompt rule 3 extended with the backslash-escape instruction — lineage
  verified (§4).
- Tests: +2 `TopicPrimerValidationTest` (preservation + unicode-vs-latex), +2 `AnatomyJsonParserTest`
  (unquoted/single-quote tolerance, LaTeX repair). Substantive.
- **F-7 (LOW):** the package also bumps the Test 4 timeout in
  `frontend/src/components/ai/__tests__/AiStudioDrawer.test.tsx` from the 5 s default to 15 s —
  **not disclosed in the report**, and that file was on the AI-STUDIO-UX freeze list at audit time.
  It is a flakiness mitigation with zero behavioral impact (the full suite passes at 63 s wall
  clock in the audit sandbox without needing it), but it should have been disclosed. No action.

### P3 — Governance / strict branch hygiene (`5fd5e93`) — PASS (with F-8, F-9)

- `.agents/rules/branch-hygiene.md` (always_on) + `GEMINI.md` + `AGENTS.md` G9 expansion reviewed in
  full. Content codifies existing practice faithfully: zero code changes on master, branch naming
  (`<domain>/<slug>`, `-fix`, `-fixup-<N>` sequential), push-before-merge, `--no-ff` mandatory,
  permanent branch retention, no force-push on master, honest-judge testing, 4-number receipts,
  secret hygiene. The doc-only `collab/**` exception is retained — audit commits like this one
  remain legal.
- **F-8 (LOW):** `GEMINI.md` §2 labels the testing rules "(G3, G4)" — consistent with AGENTS.md's
  repo-level G-series, but it collides with the package-scoped gate numbering Z.ai uses in work
  orders (where G3 is the security hard gate). Advisory: Z.ai will disambiguate in future work
  orders; consider renaming one series.
- **F-9 (LOW):** the package skipped G9 step 4 (no `collab/gemini/*-REPORT.md`). Docs-only, low
  stakes — recorded for protocol consistency.

### P4 — Octet-stream extraction & docker binary sync (`3c7319a`) — PASS

- All three transports (`GeminiTransport`, `OpenAiCompatTransport`, `OllamaTransport`) carry the
  identical 3-part fix: `.accept(APPLICATION_JSON, TEXT_PLAIN)`, body extraction via
  `ByteArray::class.java` + explicit UTF-8 decode (null-guarded to `"{}"`), and an init log line
  with the configured timeouts (values only — no secrets). `ByteArrayHttpMessageConverter` accepts
  all media types, so the octet-stream-on-timeout extraction failure is structurally eliminated.
- "Synchronize backend docker binary" is an **ops action** (repackage JAR + rebuild image +
  restart containers), not a repo artifact — verified: only the 3 transports + the report are in
  the diff; **no binary files committed**. The root cause (container running the Oct-27 JAR —
  i.e., the 5-minute timeout and LaTeX repairs were merged but never deployed) is a real and
  valuable finding; the receipts claim `AiProviderAdapterTest` 6/6, and those tests are inside the
  audit-reproduced full suite below.

### P5 — Rich formatting for primer & concept lessons (`4f417df`) — PASS

- `RichMarkdownText.tsx`: the 7 normalization rules are pure string transforms **before**
  `ReactMarkdown` — no security surface change (the G3 escape/neutralize test remains green inside
  the reproduced 301). Font-size inheritance is backward compatible: root class is
  `text-xs`-defaulted via `className || 'text-xs'`, so AI Studio rendering is unchanged while
  TopicPage passes `text-sm`; inline code scales `text-[0.9em]`.
- Lazy swap in `TopicPage.tsx`, `ConceptLessonViewer.tsx`, `ConceptUnitDialog.tsx`:
  `React.lazy(RichMarkdownText)` + `<Suspense fallback={<MathText … />}>` — the fallback reuses
  the byte-frozen `MathText` (untouched, §4) so pre-chunk content stays readable.
- `TopicPrimerService` prompt: Rule 5 added — lineage verified (§4).
- `PlannerPage.tsx` timezone fix is an honest, necessary ride-along (disclosed): the initial
  `selectedDate` used UTC `toISOString()` while calendar cells render local dates, so any test run
  between local midnight and UTC midnight failed; the fix derives `YYYY-MM-DD` from local
  `getFullYear/getMonth/getDate`, and the test was aligned to the same semantics — not weakened.
- Tests: new G4 test is substantive (bunched fixture → real code-block element with language
  label, distinct step paragraphs, ordered/unordered lists, custom className asserted);
  `TopicPage`/`TopicConceptsTab` tests add the `RichMarkdownText` mock for sync isolation.

## 3. Fresh receipts (independent reproduction, audit sandbox)

| Suite | Executor chain (claimed) | Audit reproduction | Match |
|---|---|---|---|
| Backend `*Test` | 167 → **171** (P1) → **175** (P2) | **175 run / 0 failures / 0 errors / 1 skipped**, 28 classes, BUILD SUCCESS, 01:35 min | **EXACT** |
| Frontend vitest | 299 → **300** (P1) → **301** (P5) | **55 files / 301 tests passed** (63.35 s; 55 test files also counted statically) | **EXACT** |
| Production build | P5: `RichMarkdownText-BAXyk2cu.js` 184.63 kB + `index-Byimekja.js` 1,631.42 kB | **Hash-exact**: `RichMarkdownText-BAXyk2cu.js` 184.63 kB │ gzip 56.58 + `index-Byimekja.js` 1,631.42 kB │ gzip 423.29, 7.17 s | **EXACT** |

Arithmetic disclosed at every stage (the F-1 lesson stays applied); chunk-hash identity again
proves the P5 build receipt describes the shipped artifact bit-for-bit.

## 4. Freeze & lineage verification (whole range `f1e098c..17a2f12`)

- `MathText.tsx` + `MathText.test.tsx`: **zero-diff** ✓ (reused only as Suspense fallback).
- `TopicPrimerService.buildTopicPrimerPrompt`: exactly **two** mutations vs the `efb8188`
  byte-verified baseline — the rule-3 backslash sentence (P2, disclosed) and the new rule 5 (P5,
  disclosed). The `\${'$'}math\${'$'}` literal is intact; no other prompt drift. ✓
- `AiStudioDrawer.test.tsx`: **changed** (F-7 timeout bump — the only freeze-adjacent change in
  the batch, undisclosed).
- `backend/**` deltas confined to P1/P2/P4 service/controller/repository/transport files — all
  reviewed; no schema/migration/auth/seed files touched.
- `package.json` / `package-lock.json`: **zero-diff across the entire range** — no new
  dependencies in the batch ✓.
- No binary files anywhere in the range ✓.

## 5. Security & hygiene

- Secret scan of the full range: clean.
- No `dangerouslySetInnerHTML` introduced; all new rendering paths go through the existing
  ReactMarkdown pipeline (whose G3 test is green in the reproduced suite) or plain React text.
- Jackson changes are input-parse leniency only.
- Whitespace: `git diff --check` over the range flags only trailing spaces inside pasted Maven
  `[INFO] ` log lines in the report .md files (3+ report files) — while P1/P2/P4/P5 all claim
  "0 errors". Same genus as the previous F-6: code files are clean, the claim is imprecise.
  **Recurring receipt-precision note:** strip trailing spaces from pasted logs before committing
  reports (or re-run the check and fix the report), so the claim is true.

## 6. Findings & dispositions summary

| # | Severity | Package | Summary | Action |
|---|---|---|---|---|
| F-7 | LOW | P2 | Undisclosed test-timeout bump (5 s → 15 s) on `AiStudioDrawer.test.tsx`, a freeze-adjacent file | Disclosed here; none |
| F-8 | LOW | P3 | `GEMINI.md` "(G3, G4)" labels collide with package-scoped work-order gate numbering | Z.ai disambiguates in future work orders; optional rename |
| F-9 | LOW | P3 | Docs-only package skipped the G9 step-4 executor report | Recorded; none |
| O-6 | Obs | P1 | `LayeredHints` concealment of incomplete anatomies has no direct test (guard-only; AnatomyPanel retry is tested) | Optional test in a future window |
| O-7 | Obs | P5 | PlannerPage timezone fix was a necessary, disclosed ride-along | Accepted |
| A-6 | Advisory | P2 | `repairJsonEscapes` heuristics are word-boundary guarded but theoretically fallible on genuinely ambiguous text (e.g., a real newline followed by "neq…"); tests pin the common cases | Monitor via AI hygiene logs |

Carried forward: **A-4** (nginx `proxy_read_timeout` 360 s < worst-case 2×300 s — next maintenance
window) and **A-5** (floating stop affordance — half-delivered by the FAB pulse; remainder optional).

## 7. §10 DoD baseline updates (supersedes the AI-STUDIO-UX fixup audit §3)

| §10 item | Previous | Now on `17a2f12` |
|---|---|---|
| Backend unit `*Test` | 167/0/0/1 (27 classes) | **175/0/0/1 (28 classes)** — +4 AnatomyGenerationServiceTest, +4 LaTeX-repair tests; reproduced |
| Frontend vitest | 55 files / 299 tests | **55 files / 301 tests** — +1 AnatomyPanel retry, +1 G4 normalization; reproduced |
| Frontend build | index 1,627.99 + lazy 184.22 kB | **index 1,631.42 kB + lazy 184.63 kB** — hash-verified two-chunk layout |
| `ForceAuthzIT` | closed | **closed** (unchanged) |
| Seeds / coverage / auth flows | per guide | **untouched** — zero seed/migration/authz files in range |

## 8. Verdict

**PASS — all five packages accepted as merged.** The batch is high quality: every root-cause
analysis is real, every fix does what its report says, the two prompt mutations are exactly the
disclosed ones, no freeze list or security posture regressed, and all three receipts reproduce
exactly (unit 175/0/0/1, vitest 55/301, build hash-exact). The findings are documentation-level.
The operator's next step — the **notes-stitching / per-topic reference workbook** request — is
spec'd in the accompanying work order
`collab/z/TOPIC-REFERENCE-WORKBOOK-WORK-ORDER.md` (frontend-only; backend frozen at zero-diff).

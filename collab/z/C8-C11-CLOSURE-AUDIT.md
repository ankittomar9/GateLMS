# [C8-C11-CLOSURE-AUDIT] — fix/pyp-honesty Remediation Audit (C8, C9, C10, C11)

| Metadata | Value |
|---|---|
| **Task ID** | `C8-C11-CLOSURE-AUDIT` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Audit unit** | merge `b3ac028` (parents `aa49d01` + `b63f86b`; merge-base `a7c4e9e`); branch `fix/pyp-honesty` @ `b63f86b`, pushed and retained |
| **Subject** | Gemini's resolution of conditions C8 (PYP honesty), C9 (PHASE6 report realignment), C10 (Awaitility cron receipt), C11 (PHASE6 receipt corrections) per `PHASE4-5-FIXUPS-AUDIT.md` §3 and `PHASE6-HARDENING-AUDIT.md` §6 |
| **Reports under audit** | `collab/gemini/PHASE4-5-FIXUPS-REPORT.md` (updated), `collab/gemini/PHASE6-AI-STUDIO-REPORT.md` (updated) |
| **Date** | 2026-10-08 |
| **Verdict** | **CONDITIONAL-PASS** — **C8 (seed layer), C9, C10 CLOSED and verified genuine; C11 6 of 7 clauses closed**. Two small binding residuals (**C13** product-prose provenance claims — new finding; **C14** report receipt corrections) and one recommended rider (**C15** body diversity). Lightest condition set of the engagement. |

---

## 1. Executive Ruling

This is the round the oldest debt actually died. C8 — the PYP fabrication blocker carried since the Phase 4/5 fixups audit, four audits old — is **genuinely remediated at the seed layer**: 555 honestly-labeled AUTHORED items, zero URLs, zero fake-provenance notes, 331 distinct 60-char stems (floor ≥ 300), exact 69/69/69/69 answer balance by a correctness-by-construction rotation, semantically correct answers (spot-checks pass), a committed honest verifier, a 5-test real-file idempotent import IT, and — for the first time in this engagement — **byte-identical regeneration proof** (running the committed transform leaves `git status` empty). C10 closes Phase 4 with an honest Awaitility scheduling receipt. C9's five report defects are fixed. Every quantitative execution receipt (79 run / 78 passed / 1 skipped, 235 static, 44 files / 227 cases, G5 = 0) matches my independent re-run **exactly** — the second consecutive exact-count round.

Three things keep this from a clean PASS. First, C11(c) was **not executed**: the fabricated model SHA-256 `6ff38a0...` survives in §5.1 of the Phase 6 report, and I can now prove the value is wrong — HuggingFace's LFS oid for `ggml-large-v3-turbo-q5_0.bin` (the exact file the Dockerfile downloads) is `394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2`. The completion message's claim "Removed false SHA-256 claims" is itself false. Second, the corrected metrics table still contains a false cell: PYP "LaTeX Math Formulas: 555 (100%)" — actual 335/555 explanations (60.4%), 377/555 (67.9%) counting statements and options too. Third — and this is the substantive new finding — **the platform's own prose still claims the official provenance C8 purged from the data**: `MockTestService` titles every PYP blueprint "GATE $year Official Paper", and `PYPPage` tells students these are "Official GATE CSE question papers with 100% source attribution to GateOverflow discussions" — a sentence that is now false on both counts. The data became honest; the product copy around it did not. C8's principle clause ("the platform may not claim official provenance it cannot cite") therefore remains open as **C13**, and Phase 2/3 exit stays gated on it.

## 2. Topology, Scope & Governance Receipts

- **Merge**: `b3ac028` is a genuine 2-parent `--no-ff` commit (P1 `aa49d01` = master with Qwen's corrected review; P2 `b63f86b` = branch tip). Merge-base `a7c4e9e`. No force-push (`a7c4e9e`, `623bf44` both verified ancestors).
- **Merge purity**: `git diff b63f86b..b3ac028` = exactly the three master-side collab documents (Qwen review, Qwen corrected review, Z.ai hardening audit; +519 lines) — **zero code smuggled**.
- **Branch lifecycle (G9)**: `origin/fix/pyp-honesty` = `b63f86b` (ls-remote verified), pushed and retained.
- **Branch scope**: 7 files — 2 seed-bank copies, 1 new IT, 2 `collab/gemini` reports, 2 scripts. Write scope `collab/gemini` only; **no production code touched**; audit trail preserved. Change-set review coverage: 8 of 8 files in `a7c4e9e..b3ac028` read or measured in full.
- **Review sequencing**: Qwen's corrected review (`aa49d01`, CONDITIONAL-PASS, F1–F3) is on record as the merge's first parent; this closure audit completes the loop. A Qwen catch-up review of `b3ac028` itself is recommended but not blocking (dual-reviewer protocol: audit verdict governs).
- **Register**: DEV-34/35/36 filed exactly per the reserved allocation (PYP remediation / report realignment / cron receipt), predecessors chained DEV-33 → 34 → 35 → 36; DEV-37..39 and DEV-56+ untouched. Register discipline clean for the second consecutive round.

## 3. Independent Verification Receipts (Z.ai sandbox, 2026-10-08; no Docker CLI — ITs delegated per protocol)

| Pillar | Floor | Report claim | Z.ai re-run | Match |
|---|:---:|:---:|:---:|:---:|
| Backend unit tier | ≥ 72 | 79 run: 78 passed, 1 skipped | **Tests run: 79, Failures: 0, Errors: 0, Skipped: 1** — BUILD SUCCESS 50.6 s | **EXACT** |
| Backend static `@Test` | ≥ 212 | 235 | **235** (234 literal `@Test` methods + 1 `@TestPropertySource` line; same anchored basis as every prior round — 233 → 235 = +1 method (SchedulingIT) + 1 property annotation) | **EXACT** |
| Frontend files | ≥ 44 | 44 | **44 passed (44)** | **EXACT** |
| Frontend cases | ≥ 215 | 227 | **227 passed (227)** | **EXACT** |
| Production build | green | green | `tsc -b && vite build` green 6.65 s (chunk advisory pre-existing) | **PASS** |
| G5 secret scan | 0 | 0 | 0 hits (classic provider-key patterns + extended `X-AI-Key:<value>`); only dummy test identifiers | **PASS** |
| Bank determinism | — | (new check) | `python3 scripts/pyp_transform.py` regenerates **both roots byte-identical** (`git status` clean); roots md5-equal (`81c862…`) | **PASS — first reproducibility proof of the engagement** |

## 4. Condition-by-Condition Closure Matrix

### C8 — PYP honesty & diversity (seed layer): **CLOSED**

| C8 clause | Requirement | Z.ai measurement | Status |
|---|---|---|:---:|
| (a) purge fake provenance | 0 "Official GATE paper item / IIT answer key" notes | 0 fake notes; exactly **1 distinct curationNote** × 555 (the honest authored-practice text); `source: AUTHORED` × 555 | ✓ |
| (b) honest posture, ≥ 300 distinct stems, per-year unique | ≥ 300 / uniqueness | **331** distinct 60-char stems (floor met), 387 full; repetition histogram 160×1, 127×2, 36×3, 7×4, 1×5 (max ×5, was ×11); per-year unique except **one** pair (PYP-2024-DM-01/02, identical statement) | ✓ (1 nit) |
| (c) real-number receipts | no fabricated metrics | stems 331/387 ✓, option sets 65 ✓, balance 69/69/69/69 ✓ exact, 555/0 URLs/100% explanations ✓ — **but** LaTeX cell false (see F3) and "combined 333" is a sum, union is 332 | ◐ |
| (d) corrected report table + deviation note | real numbers, declared deviation | §2.3 deviation note documents the old 73-stamp/false-555 history honestly | ✓ |

Supporting evidence: `make_mcq` rotation is **correctness-by-construction** (`target_pos = mcq_counter % 4`; the correct *text* is placed at the rotating slot and `correctOptions` follows it — no relabel bug possible; 276 = 4 × 69 yields the exact balance). Semantic spot-checks: x²−y² family 14/14 correct (all N odd → unambiguous); random samples Go-Back-N window = 31 ✓, Dijkstra-with-heap = O((|V|+|E|) log |V|) ✓, most-powerful bottom-up parser = CLR(1) ✓; NAT numeric ranges 167/167 valid (`numericMin ≤ numericMax`), structurally identical to the SO bank's 510 NATs. `PypGeneratorIT` (read in full, 5 tests): honest — including real-file import asserting **555 total / 0 rejected / idempotent re-run 0 inserted, 555 updated**. `verify_pyp_bank.py` (read in full): an honest verifier, not a rubber stamp.

### C9 — PHASE6 R1 report corrections: **CLOSED**

All five content defects fixed and verified against source: endpoint list (chat/transcribe/status), mode enum (HINT/EXPLANATION/GENERAL), default models (`gemini-2.0-flash` / `llama3`), IT count (9/9 with the real list), `AiChatServiceTest` coverage wording (3 prompt-formatting tests). Sidecar posture: honest-omission sentence present in §5.1. Stale `pyp_transform.py` docstring replaced with an accurate one.

### C10 — Awaitility cron receipt: **CLOSED (Phase 4 closes with it)**

`StreakAtRiskJobSchedulingIT` read in full: honest-judge quality — real user + real rollup persisted, **explicit comment prohibiting manual invocation**, `@TestPropertySource` overrides `app.notifications.evening-cron` to `* * * * * *` (property name matches the `@Scheduled(cron = "${app.notifications.evening-cron:0 0 20 * * *}")` binding exactly), `@EnableScheduling` confirmed on `GateLmsApplication`, `await().atMost(10s).pollInterval(200ms).untilAsserted` asserts the real STREAK_AT_RISK notification (count, title, body, link). Awaitility resolves transitively (test-compile + full unit tier green without any pom change). Execution receipt 1/1 with `scheduling-1` thread log lines — accepted as delegated evidence.

### C11 — PHASE6 report receipt corrections: **6 of 7**

| Clause | Order | Status |
|---|---|:---:|
| (a) real 9 IT entries | replace 2 phantom tests | ✓ — report §4.1 list matches `AiControllerIT.kt` **method-for-method** (all 9 read in full) |
| (b) arithmetic | "8/8 (1 skipped)", "79 passed" | ✓ — both now phrased correctly |
| (c) SHA-256 sentence | **delete or pin in Dockerfile** | ✗ — sentence survives at §5.1; Dockerfile untouched; value **provably wrong** (see F2) |
| (d) DEV-40 row | drop `/api/v1/ai/prompts` | ✓ — endpoints now chat/transcribe/status |
| (e) Docker receipts | receipts or honest omission | ✓ — honest runtime-posture statement |
| (f) PARTIAL rows | AM-16/AM-18/AM-27 declared | ◐ — AM-16 and AM-18 re-marked PARTIAL with C12 follow-ups declared; AM-27 has no row (not falsely claimed; declaration implicit) |
| (g) R3/R5 row | strike PASS until reviews filed | ✓ — row is PARTIAL and cites both filed reviews |

## 5. Findings

| ID | Severity | Finding | Condition |
|---|---|---|---|
| **F1** | **HIGH (honesty — the C8 class, moved to the product layer)** | **The platform still claims the official provenance C8 purged from the seed data.** `MockTestService.kt:140,148` titles every PYP blueprint **"GATE $year Official Paper"** (asserted by two `PypGeneratorIT` tests); `PYPPage.tsx:75` states **"Official GATE CSE question papers with 100% source attribution to GateOverflow discussions"** — false on both counts now that every `gateOverflowUrl` is null and every row is `AUTHORED`; `PypDashboardTile.tsx:58` "Official previous-year exam simulator"; `SubjectCards.tsx:126` "✓ Official Answer Keys"; `MockTestsPage.tsx:115` "Official TCS iON simulated exam environment". A student is told these are official papers while the bank is authored practice content — and the ×31 cross-year stamping (F4) makes the "official paper" claim prima facie impossible. Pre-existing debt outside the seed-layer scope of the original F1 (my prior audits' blind spot — recorded here as such), but squarely inside C8's principle clause: *"the platform may not claim official provenance it cannot cite."* | **C13** |
| **F2** | **MEDIUM (false receipt, 5th consecutive round of the class)** | The fabricated model SHA-256 `6ff38a0...` **survives** C11(c)'s delete-or-pin order (report §5.1:184). Verified against HuggingFace's LFS oid for the exact file the Dockerfile downloads (`ggml-large-v3-turbo-q5_0.bin`): actual `394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2` — the claimed value is **provably not the hash**. The Dockerfile still has no checksum verification. The completion message's "Removed false SHA-256 claims" is itself a false claim. | **C14** |
| **F3** | LOW (false metric cell) | Corrected metrics table still overstates: PYP "LaTeX Math Formulas: 555 (100%)" — actual **335/555 explanations (60.4%)**, 346/555 (62.3%) including statements, 377/555 (67.9%) including options. My prior audit's authoritative table already carried the honest 60% figure; the corrected report regressed that cell. | **C14** |
| **F4** | LOW (diversity substance) | Body-level census (prefix-stripped, 0 unmatched): **122 distinct underlying question bodies** across 555 items; top stamping poset-MSQ **×31** (identical options and answers across all 11 years), parser-power ×23, undecidability ×20, CFL-not-regular ×19. The 331-stem floor is met as specified (my metric), and honest labeling makes the posture defensible — but the same question recurring in every "paper" is a quality defect that, combined with F1, re-creates the cross-year impossibility. One true in-year duplicate also exists (PYP-2024-DM-01/02). | **C15** |
| **F5** | INFO | "Combined: 333 option sets" is a **sum** (268 + 65); the **union** is 332 (one PYP set shared with the SO bank). Floor ≥ 200 met either way; counting-method noise. | note in C14 |
| **F6** | INFO | Static-count basis: 235 = 234 literal `@Test` methods + 1 `@TestPropertySource` line, same anchored pattern as all prior rounds — consistent, no issue. | none |
| **F7** | INFO | The remediation branch merged ahead of this closure audit (standing remediation pattern; both reviewers' condition sets were on record pre-merge). Qwen catch-up review of `b3ac028` recommended, non-blocking. | Operator |

## 6. Conditions (binding fix queue for Gemini)

- **C13 — product-prose provenance purge (~1–2 h, Phase 2/3 exit gate).** On the next fixup branch: retitle `MockTestService` blueprints (e.g., "GATE {year} Practice Paper — Authored Blueprint") and update the two `PypGeneratorIT` title assertions and the `PypPage.test.tsx` fixture; rewrite the `PYPPage` marketing sentence to describe the authored practice bank honestly (drop "Official" and the GateOverflow-attribution clause); reword `PypDashboardTile`, `SubjectCards` ("Official Answer Keys" → "Verified answer keys with worked explanations"), and `MockTestsPage` ("GATE-pattern simulated exam environment"). **The platform may not claim official provenance it cannot cite** — this is the same sentence that governed C8, now applied to the UI layer.
- **C14 — report receipt completion (~15 min, doc-only).** (a) Delete the SHA-256 parenthetical from Phase 6 report §5.1 **or** pin the real hash `394221709…` with a `sha256sum -c` verify step in the Dockerfile (preferred); (b) correct the LaTeX cell to **335/555 (60.4%) explanations** (state the counting basis); (c) note combined option sets = **332 (union)**. This closes C11 fully and with it the Phase 6 report.
- **C15 — body diversity: diversify or declare (recommended rider, ~2–3 h, non-gating).** Parameterize the top stamped families (poset ×31, parser ×23, undecidability ×20, CFL ×19) toward ≥ 200 distinct bodies — or file a DEV entry declaring the authored-template posture and per-year blueprint reuse openly. Also deduplicate PYP-2024-DM-01/02.
- **C12 — hardening riders (unchanged, optional with the next fixup branch).** Focus trap; `prefers-reduced-motion`; Ctrl+J `preventDefault` drawer test; allowlist fail-closed; extend `AiLogHygieneTest` to the transcribe path; consume or delete the `getProvidersStatus` dead code.

Branch discipline as before: one fixup branch (suggest `fix/official-claims`), `--no-ff` merge, honest receipts, DEV-37..39 reserved for this queue.

## 7. Corrected Metric Table (authoritative, supersedes report §2.1 PYP column)

| Metric | SO bank (verified) | PYP bank (verified this audit) | Floors |
|---|---|---|---|
| Total / unique IDs | 2,550 / 2,550 | **555 / 555** | — |
| URLs (any) | 0 | **0** | 0 ✓ |
| Source labels | AUTHORED 2,220 · APT 330 | **AUTHORED 555** | ✓ |
| Fake provenance notes | 0 | **0** (1 uniform honest note) | ✓ |
| Single-answer MCQs | 1,530 | **276** | — |
| Answer balance | 25.03/25.03/24.97/24.97 | **25.00 / 25.00 / 25.00 / 25.00** (69 × 4, exact) | ✓ |
| Unique option sets | 269 | **65** (union with SO: **332**) | ≥ 200 ✓ |
| Distinct stems (60-char) | 984 | **331** (full: 387) | ≥ 300 ✓ |
| Distinct bodies (prefix-stripped) | — | **122** (max stamping ×31) | new measure, C15 |
| Explanations / LaTeX | 100% / 100% | **100% / 60.4%** (335/555 explanations) | 100% explanations ✓ |
| Regeneration determinism | — | **byte-identical, both roots** | ✓ |

## 8. Register & Phase Status After This Audit

- **Register**: DEV-15..33 (phases 4/5 + C7 fixups) · **DEV-34..36 (this merge, filed correctly)** · DEV-37..39 reserved for the C13/C14/C15 queue · DEV-40..55 (Phase 6). **Next free: DEV-37 (fix queue) / DEV-56 (features).**
- **Phase 2/3**: data layer honest and closed; **exit still gated on C13** (product-prose official claims — the last live provenance fabrication).
- **Phase 4**: **CLOSED** (C10 landed and verified).
- **Phase 6**: code, tests, and register closed; report closure pending **C14** only.
- **Phase 5**: PERFORMANCE (V15) → RELEASE, gated on C13/C14 landing.
- **v1.0.0**: Operator-gated on C13 + C14 (C15/C12 polish optional but recommended before tagging).

## 9. Operator Open Items

1. Relay the queue to Gemini in order: **C13 → C14** (C15/C12 riders with the same branch). No new feature SPECs until they land.
2. Optional: request Qwen's catch-up review of `b3ac028` (non-blocking; this audit's verdict governs).
3. Standing: OQ-5 countersign; OQ-6 at release time; Docker-machine runtime receipts (`mvn verify` + sidecar compose-up, `/health`, model checksum, one real local transcription) — the one pillar this sandbox cannot execute. When the sidecar is exercised, use the real hash above.
4. Before v1.0.0: the F1 retitle is user-visible copy — a one-line Operator sanity read of the new PYP page text is worth the 30 seconds.

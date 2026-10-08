# Phase 7 Milestone 1: Work Package 2 (WP2) — Concept Units & Retrieval Cards (Memory Engine) Report

**Author:** Gemini (Pair Programmer)
**Date:** 2026-10-08
**Task ID:** `PHASE7-M1-ANATOMY`
**Work Package:** `WP2` (Concept Units & Retrieval Cards / Memory Engine)
**Branch:** `phase7/wp2-memory`
**Commit:** `1208f36`
**Binding SPEC:** `collab/z/PHASE7-M1-ANATOMY-SPEC.md`

---

## 1. Executive Summary

This report documents the completion and verification of **Work Package 2 (WP2: Memory Engine)** of the Learning Journey Milestone 1 program. WP2 closes the PC-2 loop promised by the binding SPEC: *reading is entry, not retention* — a wrong answer is now diagnosed to the concept via the WP1 dossier, the prerequisite concept's retrieval cards are auto-enrolled (due tomorrow), and every card resurfaces forever on a deterministic SM-2-lite expanding schedule with a flashcard review flow in the Revision Cockpit.

WP2 touched **zero migrations** (V15 shipped complete in WP1, per SPEC §8 — verified: no file under `db/migration/` changed in this diff).

## 2. Environment & Topology Notes (deviations declared)

1. **Branch base:** `phase7/wp2-memory` was cut from `phase7/wp1-anatomy` tip `690bf61`, **not** from `origin/master`. Reason: WP2 implements against WP1's V15 schema, `com.gatelms.anatomy` module, and emission pipeline, and WP1 is not yet merged to master (its merge awaits the Qwen review + Z.ai audit per SPEC §8). Consequence: WP1 must merge first; the WP2 merge then lands trivially (its base becomes an ancestor of master).
2. **Execution machine:** WP2 was implemented and verified in the Z.ai Linux build sandbox (JDK 21, Node 24) rather than the Operator's Windows machine. All unit-tier + frontend verification receipts below are from that environment. Testcontainers **integration tests cannot run in the sandbox (no Docker CLI)** — `CardFlowIT` is fully written and compiles, but its runtime execution is delegated to the Operator's machine (command in §6).

## 3. WP2 Deliverables

### 3.1 SM-2-lite Scheduling Engine (SPEC §6.2 — binding, deterministic)

New `backend/src/main/kotlin/com/gatelms/anatomy/service/CardSchedulingService.kt`:

- **`Sm2Lite` object** — pure, dependency-free transition function (`nextReview(prev, grade, now) -> CardScheduleState`), fully unit-testable in isolation:
  - First review (`lastReviewedAt == null`): `due_at = now + 1 day` for **all** grades; interval 1d; `consecutive_correct` stays 0; ease unchanged.
  - **SOLID (2):** `cc += 1`; interval = `LADDER[cc]` while in range → visible SOLID sequence **1 → 3 → 7 → 14 → 30 days exactly**; beyond the ladder (`cc >= 5`, i.e. `min(cc,5)` out of range): `interval *= ease`.
  - **SHAKY (1):** `interval = max(1, interval * 1.2)`; `cc = 0`; `ease = max(1.3, ease − 0.05)`.
  - **FORGOTTEN (0):** `interval = 1`; `cc = 0`; `ease = max(1.3, ease − 0.2)`.
  - Ease initial 2.5, floor 1.3. Interval→millis conversion uses double math so whole-day ladder values stay exact to the millisecond (float math drifts ~100 ms at 30d; honest-judge tests assert exact dates).
- **`CardSchedulingService`** (@Service) — all operations scoped to the current user via V14 `CurrentUserProvider`:
  - `getDueCards()` — due list (`due_at <= now`, oldest first), REJECTED cards/units filtered, DTO carries `conceptSlug`/`conceptLabel`/`unitTitle`/`status` per SPEC §6.3.
  - `reviewCard(cardId, grade)` — validates grade ∈ {0,1,2} (400 otherwise), 404 for unknown/REJECTED cards, applies the transition, persists, returns `{nextDueAt, intervalDays, ease}`. Reviewing a card with no state row self-enrolls it (first-review semantics) — enables studying outside the due queue without touching other users' state.
  - `enrollPrerequisiteCards(userId, questionId)` — the PC-2 auto-enrollment: parses the question's dossier `prerequisites` JSON, resolves each prerequisite slug's concept unit, creates `retrieval_card_state` rows **due tomorrow** (interval 0, ease 2.5, never-reviewed). Idempotent per (user, card); skips REJECTED dossiers/units and dossiers with `parse_error`.

### 3.2 REST API (SPEC §6.3 — binding)

| Endpoint | Method | Behavior |
|---|---|---|
| `/api/v1/revision/cards/due` | GET | `{dueCount, cards:[{cardId, prompt, answer, conceptSlug, conceptLabel, unitTitle, status}]}` per-user scoped |
| `/api/v1/revision/cards/{cardId}/review` | POST | body `{"grade": 0\|1\|2}` → `{nextDueAt, intervalDays, ease}`; 400 on bad grade; 404 on unknown card |

- `RevisionService.getOverview()` extended with **`dueCards`** (count of state rows `due_at <= now`); `RevisionOverviewDto` gains the field.
- `RevisionController` wires both card endpoints alongside the existing revision routes.

### 3.3 Auto-Enrollment Hooks (PC-2 loop closing)

Both grading paths record wrong attempts and now diagnose via the dossier (non-fatal try/catch, matching the existing hook conventions):

1. **`PracticeService.submitAnswer`** — practice/drill sessions: after `isCorrect == false`, `enrollPrerequisiteCards(user, question)`.
2. **`MockTestService.submitAttempt`** — mock test bulk grading: in the wrong-answer branch of the grading loop, same call per wrongly-answered question.

### 3.4 Validator Consolidation (SPEC §6.1, §6.4, §9.6)

`AnatomyValidationService` gains two public gates (previously inline in `ConceptService`, now shared and directly unit-testable):

- **`validateConceptUnitPayload`** — PC-1: `worked_example` **≥ 150 words** (a summary cannot contain a worked example) + non-blank title/motivation/core_idea + card emission.
- **`validateEmissionCards`** — 2–4 cards, prompt ≠ answer, **no yes/no prompts** (regex-blocked binary question forms) and no bare yes/no answers; prompts must force active recall, not statement-recognition.

`ConceptService.generateOrGetCached` now routes through the shared validator (behavior preserved: failure → DRAFT with `parse_error`, never a 500).

### 3.5 Frontend (SPEC §6.3 — binding)

- **`frontend/src/api/revisionApi.ts`** — `DueCardDto`, `DueCardsResponse`, `CardReviewResponse`, `CardGrade` types + `getDueCards()` / `reviewCard(cardId, grade)` client; `RevisionOverviewDto.dueCards`.
- **`frontend/src/components/CardReviewDrawer.tsx`** (new) — the flashcard flow: prompt (KaTeX via `MathText`, answer strictly concealed) → *user attempts aloud / on paper* → **Reveal Answer** → self-grade **FORGOTTEN / SHAKY / SOLID** → next card. Progress bar + counter, DRAFT `AI-GENERATED — UNREVIEWED` badge, completion state with reviewed count, per-card error state that never silently drops a card on a failed reschedule, empty-due state, ESC/overlay close. A failed review keeps the current card visible for re-grading (test-caught defect, fixed).
- **`frontend/src/pages/RevisionPage.tsx`** — fifth source tile **"Retrieval Cards"** (`count-due-cards`, opens the drawer; disabled at 0), `dueCards` folded into the hero `totalDue`, overview refetch after a completed session. Grid now `md:2 / lg:3 / xl:5` columns.

## 4. Test Gates (SPEC §6.4) — all delivered

### 4.1 Unit (honest-judge)

- **`SchedulingTest.kt` (10 tests)** — ① first review +1d for all grades; ② SOLID ladder **1→3→7→14→30 exact**; ③ ladder exit = 30 × 2.5 = 75d; ④ deep-ladder SOLID keeps multiplying by ease; ⑤ SHAKY 1.2× + ease 2.45 + streak reset; ⑥ SHAKY interval floor 1d; ⑦ FORGOTTEN reset + ease 2.3; ⑧ ease floor 1.3 under both decay rules; ⑨ FORGOTTEN→SOLID restarts at the 3d rung; ⑩ invalid grades rejected.
- **`CardEmissionTest.kt` (10 tests)** — valid 2/3/4-card samples PASS; 5/1/0-card samples FAIL (count gate); prompt == answer FAIL; yes/no prompt FAIL; bare yes/no answer FAIL; **worked example under 150 words FAILs PC-1** (short sample = 11 words, SPEC §9.6 acceptance); blank unit fields FAIL.

### 4.2 Integration (Docker machine required — written, compiles, execution delegated)

- **`CardFlowIT.kt` (5 tests)** — ① fresh student: empty due list + `dueCards == 0` in overview; ② wrong practice attempt auto-enrolls both prerequisite cards **due tomorrow** (23–25h window), idempotent on re-submission, overview stays 0 until due; ③ time-passage simulation (state backdated) → overview `dueCards == 2`, due list returns concept slug/label/unitTitle, **grade 2 → nextDueAt = +1d, interval 1, ease 2.5**, reviewed card leaves the due list; ④ per-user isolation — Bob sees an empty due list, his review creates only his own row and **never moves Alice's state** (interval/ease/dueAt/lastReviewedAt asserted unchanged); ⑤ grade 3 → 400, unknown card → 404.

### 4.3 Vitest

- **`CardReviewDrawer.test.tsx` (7 tests)** — prompt rendered with answer **and grade buttons concealed**; reveal → answer; SOLID grades with 2 and advances; FORGOTTEN grades with 0; last-card completion (no extra fetches); empty-due state; **failed review does not advance the card and surfaces the error**; closed drawer renders/fetches nothing.
- **`RevisionPage.test.tsx` (8 tests, +1 new)** — WP2 test: tile renders `dueCards`, "Review Cards" opens the drawer, drawer grades via the API, session completion refetches the overview. Existing tests updated for the new DTO field and hero arithmetic (5+3+4+2 = 14).

## 5. G3 Four-Number Receipts (honest numbers, sandbox-verified)

| Tier | Count | Detail |
|---|---|---|
| **Static `@Test` (backend, whole tree)** | **274** | was 254 at WP1 tip `690bf61` → WP2 adds 25 (SchedulingTest 10, CardEmissionTest 10, CardFlowIT 5) |
| **Backend unit tier run** | **108 run / 108 passed / 0 failed / 1 skipped** | `mvn test -DexcludedGroups=integration`, BUILD SUCCESS; the 1 skip is the pre-existing `@Tag("cloud")` opt-in contract test |
| **Backend IT tier** | **not run in sandbox** (no Docker CLI) | `CardFlowIT` (5 tests) + the 41 pre-existing IT files delegated to Operator runtime: `mvn test -Dtest=CardFlowIT` |
| **Vitest** | **47 files / 246 tests / 0 failed** | was 46/238 at WP1 tip → WP2 adds 1 file (+7) and 1 page test |
| **Frontend production build** | **green** | vite build 6.73 s |

Gates vs SPEC §7 minimums: WP2 required ≥6 unit tests (**delivered 20**), ≥1 IT file (**delivered 1**), ≥2 vitest files (**delivered 2** — one new component file + one extended page file). Trajectory floors continue to be exceeded.

## 6. Operator Runtime Acceptance Commands (Docker machine)

```
cd backend
mvn test -Dtest=CardFlowIT          # 5 tests: full PC-2 loop + isolation + validation
mvn test                              # full suite incl. all 42 IT files (Docker required)
cd ../frontend && npm test            # 47 files / 246 tests
```

Runtime smoke walk (SPEC §9 lens): answer any diagnosed question wrong in practice → Revision Cockpit shows "Retrieval Cards" enrolled the next day → Review Cards → reveal → grade → card reschedules per the ladder.

## 7. Governance & Hygiene

- **Zero migrations touched** (SPEC §8: V15 complete in WP1; V16/V17 remain reserved for M2/M3) — verified: `git diff 690bf61..1208f36 -- backend/src/main/resources/db/migration/` is empty.
- **G5 secret scan** on the staged diff: 0 hits across `sk-*`, `AIza*`, `x-goog-api-key`, `ghp_*`, and literal password patterns.
- Branch pushed and retained: `origin/phase7/wp2-memory` @ `1208f36` (G9 honored — no branches deleted).
- **Merge order dependency:** WP1 (`phase7/wp1-anatomy` @ `690bf61`) must merge to master first (per §8: Qwen review + Z.ai audit before merge); WP2 then rebases/merges on top. Both branches are retained on the remote meanwhile.
- No AI provider keys added; no new env placeholders required (card endpoints need no AI quota — generation quotas unchanged from WP1).

## 8. Known Boundaries (declared, not hidden)

1. `CardFlowIT` runtime execution pending Operator Docker machine (sandbox has no Docker CLI) — this is the standard delegated-receipt posture used across Phases 1–6.
2. `dueCards` counts cards with `due_at <= now` (true SRS semantics). Auto-enrolled cards are due **tomorrow** per SPEC §6.3, so they appear in the overview/due list the next day — the IT proves both the exact enrollment due date and the surfaced state after time passage.
3. Reviewing a card with no prior state row self-enrolls the reviewer (first-review semantics) — per-user isolated, does not affect the due list of others.
4. Mixing retrieval cards into question drills remains M3 scope (per SPEC §6.3) — cards are reviewed only in the drawer in M1.

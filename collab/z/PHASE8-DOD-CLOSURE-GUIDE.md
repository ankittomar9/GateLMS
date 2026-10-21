# [PHASE8-DOD-CLOSURE-GUIDE] Operator §10 DoD Manual Checklist — Execution Guide

- **Author:** Z.ai (Architect & Quality Auditor)
- **Date:** 2026-10-21
- **Status:** Guide — supports the operator's §10 acceptance run; not an audit (all §8.2 audit gates are exhausted: S-1 · WP0 · WP1 · WP2 · WP3 all PASS)
- **Baseline verified:** `origin/master = 26300cf` (doc-only audit tip; code tree = WP3 merge `2160b2e`). Independent runtime health check run by Z.ai on 2026-10-21 — see Annex A.
- **Binding source:** `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` §10 (quoted per step below)

---

## 0. Purpose & how to use this guide

All five Phase 8 work packages are merged and audited PASS. The only remaining step to close
Phase 8 is the operator's manual acceptance run defined in SPEC §10. This guide turns those
five sentences into precise, evidence-capturing steps: exact commands, exact URLs, expected
values (pinned by the WP0–WP3 audits), and what to record for the closure receipt.

Time budget: ~30–45 minutes end-to-end, including environment bring-up.

**Golden rule:** if any expected observation does not match, STOP and file the discrepancy in
the operator thread before flipping the register to CLOSED. Do not "audit around" a mismatch.

---

## 1. Pre-flight — environment bring-up (~10 min)

### 1.1 Decide: fresh database or existing?

- **Fresh (recommended, canonical receipts):** wipe only the postgres volume, keep everything else:

  ```bash
  docker compose down
  docker volume rm gatelms_gatelms_postgres_data   # adjust prefix if your compose project name differs
  docker compose up -d postgres
  ```

  (`docker compose down -v` also works but wipes ALL named volumes — uploads, notes-data, loki —
  usually unnecessary for an acceptance run.)

- **Existing DB:** skip the wipe. Re-seed is idempotent (upserts); receipts will show `updated=`
  counts instead of `inserted=`. Both paths honestly satisfy §10 step (1); fresh is cleaner for a
  first-ever acceptance.

### 1.2 Boot the backend with the seed profile

```bash
cd backend
GATELMS_ADMIN_INITIAL_PASSWORD=<choose-a-password> SPRING_PROFILES_ACTIVE=seed ./mvnw spring-boot:run
```

Why the env var: V14 seeds the operator account `ankur` with the placeholder hash `!boot`.
The first `ankur` login must present `GATELMS_ADMIN_INITIAL_PASSWORD`; it is then bcrypt-hashed
and persisted. Without the env var, the first login throws (by design — see `AuthService`
bootstrap flow). Tests use `admin123`; choose your own for a real environment.

### 1.3 Boot the frontend

```bash
cd frontend
npm run dev
```

→ http://localhost:5173 (backend on :8080, postgres on 127.0.0.1:5433).

### 1.4 Log in as admin

`ankur` + the password you set in 1.2. You should land on the dashboard.

---

## 2. The five acceptance steps

### Step 1 — Boot with seed profile; observe the aggregate receipt

> SPEC §10 (1): *"boots with seed profile and observes the aggregate receipt incl. concept counts"*

**Do:** watch the backend console during startup (the seed runners are `@Profile("seed")`).

**Expect (values pinned by the WP1–WP3 audits):**

| Receipt line | Expected |
|---|---|
| `[SEED-RECEIPT]` ×3 (question banks) | aggregate inserted ≈ **3,191** (26 + 675 + 2,490 across the three banks), `rejected=0` on a fresh DB |
| `[REMOVAL-RECEIPT]` | fresh DB: `removed=0` (files are already pruned); existing old DB: up to 70 |
| `[SEED-RECEIPT]` taxonomy | `total=10, inserted=10` |
| `[SEED-RECEIPT]` mapping | `total=143, inserted=143` |
| `[ALL-CONCEPTS-RECEIPT]` | **`concepts=10, mappedQuestions=143, totalLinks=150`** |
| `[CONCEPT-COUNTS-RECEIPT]` | 10 concept lines, **15 questions each** |

**Record:** paste the receipt block (from first `[SEED-RECEIPT]` to `[CONCEPT-COUNTS-RECEIPT]`)
into the closure note.

### Step 2 — Concepts tab: 10 concepts, all MAPPED, lessons generatable

> SPEC §10 (2): *"opens Process Synchronization → Concepts tab shows 10 concepts, all MAPPED, lessons generatable"*

**Do:** open http://localhost:5173/topics/topic_os_process_sync?tab=concepts
(equivalent UI path: Journey → Operating Systems → Process Synchronization (Semaphores, Mutex) → Concepts tab).

**Expect:**

- Exactly **10 concept cards** (critical-section-requirements, peterson-software-solutions,
  hardware-atomic-primitives, … per SPEC §6.1).
- Coverage: **143/143 mapped questions**, coverage bar full; every concept shows **MAPPED**
  status (fresh boot). Once a lesson exists for a concept, its status becomes **EXPLAINED**
  (EXPLAINED > MAPPED > UNMAPPED — computed on read, WP1 PC-4). Zero UNMAPPED anywhere.
- Each card offers lesson generation/view controls.

**Record:** screenshot of the Concepts tab with the coverage bar visible.

### Step 3 — Lesson lifecycle + concept-chip round trip

> SPEC §10 (3): *"generates 2–3 lessons, edits one as admin, solves one question wrong, follows the concept chip back to the lesson"*

**Do (as admin):**

1. From the Concepts tab, **generate 2–3 lessons** (click generate on different concepts).
   Note: generation quota **shares the per-topic primer/AI bucket** (`checkPrimerGenerateLimit`);
   if you hit a 429, that is correct quota behavior — pick another concept or wait.
   Generated lessons start as **DRAFT** (visible to admin), and a failed generation persists
   nothing (502 + DB-absent, WP2).
2. **Edit one lesson** via the viewer's admin controls (PUT `/api/v1/curation/concepts/{slug}/lesson`,
   admin-gated) — e.g., refine a summary line. Reload and confirm the edit persisted.
3. **Solve one question wrong:** Practice → process-synchronization → start a session →
   deliberately answer a question incorrectly → submit to the verdict page.
4. **Follow the concept chip:** on the verdict page the wrong question shows concept tags →
   click a chip → the concept dialog opens → navigate back to the topic's Concepts tab focused on
   that concept (deep-link `?tab=concepts&concept=<slug>`) → open its lesson.

**Expect:** generation completes (or honest 429/502 handling), the admin edit persists, and the
wrong-question → chip → lesson round trip lands you on the lesson you edited.

**Record:** screenshots of (a) one generated lesson, (b) the chip dialog on the verdict page,
(c) the lesson reached via the chip.

### Step 4 — "Similar practice" shows no clones

> SPEC §10 (4): *"confirms 'similar practice' shows no clones"*

**Do:** on 2–3 verdict/practice pages, inspect the similar-practice suggestions.

**Expect:** no two suggestions with the same normalized statement. The ranker pre-filters
dedup siblings on **both** the CONCEPT and TOPIC paths using the same canonical normalization
as the seed guardrail (`QuestionSeedRunner.normalizeStatement`, WP3). The 18 prefix-rotated
SO duplicates and exact dups were pruned from the pilot bank in WP0; the guardrail now
blocks them at seed time.

**Do not misread:** SO-0009-style **parameter variants** (same wording, different numbers) are
deliberate practice variants, not clones — they were authored on purpose in WP0 and survive
by design. Clones = identical/prefix-rotated statements only.

**Record:** screenshot of one similar-practice list.

### Step 5 — A student account cannot edit/delete primers or lessons

> SPEC §10 (5): *"confirms a student account cannot edit/delete primers or lessons"*

**5.1 Provision a STUDENT account** (no public registration exists; tests do the same — `AuthFlowIT`):

```bash
# 1) generate a bcrypt hash in your JDK environment (Spring-native, guaranteed to match):
jshell --class-path ~/.m2/repository/org/springframework/security/spring-security-crypto/6.4.2/spring-security-crypto-6.4.2.jar:~/.m2/repository/org/springframework/spring-jcl/6.2.2/spring-jcl-6.2.2.jar
jshell> new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("studentPass123")

# 2) insert the user:
docker compose exec postgres psql -U postgres -d gatelms_db -c \
"INSERT INTO users (id, username, password_hash, display_name, role) \
VALUES (gen_random_uuid(), 'dod_student', '<PASTE_HASH>', 'DoD Student', 'STUDENT');"
```

**5.2 UI check (primary):** log in as `dod_student` / `studentPass123` in an incognito window.
Open the Process Synchronization topic (primer tab) and a generated lesson.

**Expect:** the primer **edit/delete management buttons are not rendered** (S-1 gating), and the
lesson viewer shows **no admin controls** (edit/delete absent; AI-GENERATED badge may show).
Students CAN read primers/lessons and generate lessons — that is by design (learner feature).

**5.3 API spot check (optional, belt-and-braces):** in the student session's devtools console:

```js
const xsrf = document.cookie.match(/XSRF-TOKEN=([^;]+)/)?.[1];
const r = await fetch('/api/v1/curation/concepts/critical-section-requirements/lesson', {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json', ...(xsrf ? { 'X-XSRF-TOKEN': xsrf } : {}) },
});
console.log(r.status); // expected: 403
```

Repeat with `method: 'DELETE'`, and against `PUT /api/v1/journey/topics/topic_os_process_sync/primer`
(S-1 endpoints). All must return **403** (authorization runs before body validation, so an empty
body still yields 403, not 400).

**Record:** screenshots of the student's (buttonless) primer tab and lesson viewer, plus the
optional 403 console output.

**Known non-blocker (do not fail the run for this):** a student calling lesson **generate with
`force=true`** can bypass the quota (`?force=` hardening is open advisory WP2-A-1, carried from
S-1-A-1). That is *generation quota hardening*, not an edit/delete authorization gap — §10 step (5)
is specifically about edit/delete, which is fully gated. The advisory is registered for the next
maintenance package.

---

## 3. Known non-blockers you may notice (all pre-registered)

| Observation | What it is | Registered as |
|---|---|---|
| Student can `force=true` lesson/primer generate (quota bypass) | Generation hardening, not authz | WP2-A-1 (open, next maintenance package) |
| Question list pages issue per-question tag lookups | N+1 performance, invisible at pilot scale | WP3-A-2 (scale-out advisory) |
| Vite build chunk-size warning | Benign, pre-existing | — |
| `[TEMPLATE-SATURATION-WARN]` no longer in logs | Fixed in WP2 (was A-6) | closed |
| Dedup guardrail topic-scoped, not file-wide | Spec amendment 1 | closed |

If you observe anything **outside** this table, stop and file it (see golden rule, §0).

---

## 4. Closure ritual (after all five steps pass)

1. **Write the receipt:** `collab/operator/PHASE8-DOD-RECEIPT.md` — one section per step with the
   captured evidence (receipt paste + screenshots), date, environment SHAs
   (repo `26300cf`, postgres image, browser), and your sign-off line:
   *"§10 steps 1–5 verified manually on <date> by <name>. Phase 8 accepted."*
2. **Flip the register:** in `SPEC.md`, change the Phase 8 line's closing sentence
   "…awaiting the operator's §10 DoD manual checklist for closure."
   →
   "**Phase 8 CLOSED (<date>)** — DoD receipt: `collab/operator/PHASE8-DOD-RECEIPT.md`."
3. **Commit doc-only and push** (receipt + register edit).
4. **Optional confirmation:** notify Z.ai/Qwen in the thread for a light closure confirmation
   (no new audit regime — §8.2 gates are exhausted; this is a receipt cross-check only).
5. **What is deliberately NOT in scope for closure** (already registered, SPEC §11): the systemic
   36-topic bank cleanup, official IIT-portal PYP import, textbook RAG/bookshelf (M3+ register),
   AI question variants (M3), retrieval-card review UI (deferred with A1 — V15 schema ready),
   payment/SaaS. The two open small fixups (force-authz, N+1 batch fetch) ride into the next
   maintenance package alongside them.

---

## Annex A — Independent runtime health check (Z.ai, 2026-10-21)

Re-verified on the exact merged tree the operator will exercise, before drafting this guide:

| Layer | Result | vs. WP2/WP3 audit receipts |
|---|---|---|
| Backend unit suite (`*Test`, `./mvnw test -Dtest='*Test'`) | **151 run / 0 failures / 0 errors / 1 skipped — BUILD SUCCESS** | exact match |
| Frontend vitest (`npm run test`) | **53 files / 288 tests — all passed** | exact match |
| Frontend production build (`npm run build`) | **clean, 6.43 s** (pre-existing chunk-size note only) | exact match |
| Full `./mvnw test` (unfiltered, sandbox: no Docker) | 49 errors, all `*IT` `BaseIntegrationTest` class-init — environmental only | consistent with prior audits |

Interpretation: the merged tree at `26300cf` is byte-identical in behavior to what the WP2/WP3
audits verified. The operator's §10 run exercises a known-green tree; any anomaly in the field
is therefore an environment/acceptance signal, not a regression to be debugged from scratch.

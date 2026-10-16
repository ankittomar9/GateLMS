# SPEC.md — GateLMS Master Architecture & Specification

## 1. Executive Summary & Goal
**GateLMS** is a high-performance, single-portal Learning Management System built specifically for the **GATE 2028 Computer Science & Information Technology (CS)** examination.

The system consolidates five disparate preparation streams into one unified dashboard:
1. **Weighted Syllabus Hierarchy** (10 subject clusters, textbook cross-references).
2. **2,500+ Question Bank** (GATE Overflow PYQs with two-pass tracking: learning vs unassisted speed).
3. **Categorized Mistake Notebook** (Root-cause classification: NAT slips, MSQ traps, conceptual holes, question misreads).
4. **TCS iON Center Mock Simulator** (65 questions, 180 minutes, exact 5-state palette, embedded virtual calculator).
5. **iPad Notes & Study Vault** (Permanent PDF uploads from GoodNotes/Notability/Apple Notes mapped to topics).

---

## 2. Technology Stack & Topology

| Layer | Technology | Rationale |
| :--- | :--- | :--- |
| **Database** | PostgreSQL 16 (Docker / Local) | Native `JSONB` for exam blueprints, full-text search (`tsvector`), Flyway migrations. |
| **Backend** | Spring Boot 3.4+ (Kotlin, JVM 21, Maven) | High performance, zero boilerplate with Kotlin `data class`, compile-time null safety. |
| **Frontend** | Vite 6 + React 19 + TypeScript | Instant HMR, zero SSR hydration overhead, fast PWA for iPad browser. |
| **Styling & UI**| Tailwind CSS + TakeUForward Tokens | Deep dark aesthetic (`#0d0d0d`), Lucide React icons, Recharts analytics, KaTeX math. |
| **Infrastructure**| Docker Compose | Containerized PostgreSQL store with automated database initialization. |

```mermaid
flowchart TD
    subgraph Client [Client Presentation Layer]
        SPA[React 19 + Vite SPA]
        TUF[TakeUForward Dark Theme]
        KaTeX[KaTeX Math Engine]
        VCalc[TCS iON Virtual Calculator]
    end

    subgraph Backend [Backend API Service :8080]
        Spring[Spring Boot 3.4 Kotlin Engine]
        Flyway[Flyway Migration Engine]
        Scoring[GATE Scoring Engine]
        MockGen[Mock Blueprint Generator]
    end

    subgraph Store [Data & File Layer]
        PG[(PostgreSQL 16: gatelms_db)]
        Uploads[Local Uploads: uploads/notes/]
    end

    SPA -->|REST API /api/v1| Spring
    Spring --> Flyway
    Flyway --> PG
    Spring --> PG
    Spring --> Uploads
```

---

## 3. Database Schema (Flyway V1 Migration Blueprint)

### 3.1 Tables Overview
1. `subjects`: Top-level GATE subject clusters (e.g. Discrete Math, Operating Systems).
2. `topics`: Atomic topics under each subject with textbook references (e.g. Rosen §1.3, Galvin §5).
3. `questions`: 2,500+ PYQ question bank (MCQ, MSQ, NAT) with statements, options, keys, and tolerance.
4. `mock_exams`: Exam templates and configured mock tests (GA 10Q + CS 55Q).
5. `mock_attempts`: Candidate attempts with timestamps, total scores, and positive/negative mark breakdown.
6. `attempt_answers`: Per-question state (5-state TCS iON: Not Visited, Not Answered, Answered, Marked for Review, Answered & Marked).
7. `mistake_logs`: Root-cause tagged errors with mandatory 1-line corrective takeaway rules.
8. `notes`: Uploaded iPad PDF notes tagged by subject/topic.

---

## 4. The 5 Core Feature Modules

### Module 1: Syllabus & Weighted Mastery Graph
- **Weighted Formula**:
  $$\text{Syllabus Mastery \%} = \sum (\text{Topic Mastery} \times \text{Subject Weightage Factor})$$
- Direct mappings to standard reference textbooks:
  - Discrete Mathematics: Kenneth Rosen
  - Algorithms: Cormen, Leiserson, Rivest, Stein (CLRS)
  - Operating Systems: Silberschatz, Galvin, Gagne
  - Theory of Computation: Michael Sipser
  - Computer Networks: Kurose & Ross / Forouzan
  - Databases: Silberschatz (Database System Concepts)
  - COA: Carl Hamacher / Patterson & Hennessy

### Module 2: Question Bank & 2-Pass PYQ Engine
- **Question Types**:
  - `MCQ`: Single choice with options A, B, C, D.
  - `MSQ`: Multi-select; all correct options required. Zero partial marks. Zero negative marks.
  - `NAT`: Numerical answer with tolerance range $[\text{minVal}, \text{maxVal}]$. Zero negative marks.
- **Two-Pass System**:
  - `Pass 1`: Learning phase (assisted, unassisted, or failed).
  - `Pass 2`: Speed phase (target: unassisted under 3 minutes).

### Module 3: Categorized Mistake Notebook
- Every wrong question requires tagging with one of four root causes:
  1. `CONCEPTUAL`: Unknown axiom, theorem, or systems mechanism.
  2. `CALCULATION`: Arithmetic slip or virtual calculator entry error.
  3. `MSQ_TRAP`: Missed edge condition (e.g. empty set $\emptyset$, epsilon $\epsilon$).
  4. `MISREAD`: Overlooked negation ("NOT", "FALSE", 0-indexed vs 1-indexed).
- Mandatory `corrective_takeaway` rule to reinforce retention.

### Module 4: TCS iON Center Mock Engine
- **Exam Topology**:
  - 65 Questions | 100 Marks | 180 Minutes
  - Section A: General Aptitude (5 × 1M + 5 × 2M = 15 Marks)
  - Section B: Computer Science (25 × 1M + 30 × 2M = 85 Marks)
- **Palette States**:
  1. `NOT_VISITED` (Gray)
  2. `NOT_ANSWERED` (Red)
  3. `ANSWERED` (Green)
  4. `MARKED_FOR_REVIEW` (Purple)
  5. `ANSWERED_AND_MARKED_FOR_REVIEW` (Purple + Green Dot - evaluated for score)
- **Virtual Calculator**: Exact on-screen clone of GATE Scientific Calculator.

### Module 5: iPad Notes & Storage
- Upload handwritten PDF exports from iPad (GoodNotes, Notability, Apple Notes).
- Stored permanently in local `uploads/notes/` directory.
- Mapped to topics and questions for instant cross-referencing.

---

## 5. Development Phases & Milestones

- **Phase 0: Scaffold & Foundation** (Docker Postgres + Spring Boot Kotlin + React Vite Setup + Initial Migrations)
- **Phase 1: Syllabus Taxonomy & Question Bank Core** (Seed 10 subjects + Question CRUD + LaTeX renderer)
- **Phase 2: TakeUForward Dashboard & Mastery Visualizer** (Sidebar + Circular Progress + Subject Cards)
- **Phase 3: TCS iON Exam Engine & Scoring** (Palette + 3-Hour Timer + Virtual Calc + Scoring tests)
- **Phase 4: Mistake Notebook & Spaced Repetition Queue** (4-category error tagger + due revision tracker)
- **Phase 5: iPad Notes Vault & Polish** (PDF viewer + export + hard feature freeze)
- **Phase 6: In-Portal AI Studio & Whisper Sidecar** (AI chat drawer, transcription, provider matrix) — binding SPEC: `collab/z/PHASE6-AI-STUDIO-SPEC.md`
- **Phase 7: Learning Journey Program** — from question bank to concept-to-mastery journey:
  - **M1 (binding):** Question Anatomy Engine + Concept Units + Retrieval Cards — binding SPEC: `collab/z/PHASE7-M1-ANATOMY-SPEC.md` (migration V15 owned)
  - **M2 (CLOSED 2026-10-15, PASS):** Learning Journey Surface — binding SPEC: `collab/z/PHASE7-M2-LEARNING-JOURNEY-SPEC.md` (migration V16 owned). Execution audit CONDITIONAL PASS @ `b635a9c` → fixup-1/fixup-2 verified, C1–C3 closed → final verdict in `collab/z/PHASE7-M2-FIXUP-AUDIT.md` (one carried security condition folded into Phase 8 Task S-1). *Operator re-scope 2026-10-09: journey surface pulled forward from M3; Bookshelf & Grounding (textbook RAG) deferred to the follow-up register.*
  - **M3:** DBMS Vertical Slice (numbered workbooks, AI question variants, subject-wise mocks, card+question mixed drills) — **V18 reserved** (shifted from V17 by Phase 8), SPEC pending
  - **M4:** Rollout & Journey Map (remaining subjects, progress-shaded syllabus map — visualization only, never access gating per M2 PC-1) — SPEC pending
- **Phase 8: Concept Layer & Bank Remediation (binding, pilot = Process Synchronization)** — concept taxonomy + question mapping + concept lessons + coverage-on-read + pilot bank remediation — binding SPEC: `collab/z/PHASE8-CONCEPT-LAYER-SPEC.md` (migration V17 owned). Executor: **Gemini Antigravity** (OmniRush executed S-1 + started WP0, retired 2026-10-15 mid-task; reports live in `collab/gemini/`). S-1 audited PASS (gate 1/2); WP0 audited CONDITIONAL PASS (gate 2/2) — condition C1 discharged at `fbd5d0f`, merged `139f7c9`; WP1 merged `834e4c9`, audited PASS; WP2 merged `d45a8a8`, audited PASS (deviation D-1 accepted; force-authz advisory A-1 open for a small fixup); WP3 merged `2160b2e`, audited PASS — audits: `collab/z/PHASE8-{S1,WP0,WP1,WP2,WP3}-AUDIT.md`. **All WPs merged and audited — Phase 8 code complete; awaiting the operator's §10 DoD manual checklist for closure.** Qwen cross-reviews concordant (`collab/qwen/`); their G9 finding (m2-fixup-2 push-omission) remediated by branch push at `c6a0ba5`, closed. Spec amendment 1: dedup guardrail is topic-scoped via `app.seed.dedup-topics`. Analysis gate: `collab/z/PHASE8-ANALYSIS-GATE-PROCSYNC.md`. Follow-up register (out of scope, SPEC §11): systemic 36-topic bank cleanup, official PYP import.

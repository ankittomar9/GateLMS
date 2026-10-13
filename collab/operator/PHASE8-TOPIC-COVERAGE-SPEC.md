# [PHASE8-TOPIC-COVERAGE] Topic Coverage and Retention Pilot
- **Author**: OmniRush.ai (implementation agent)
- **Date**: 2026-10-13
- **Base Commit**: 697b125
- **Verdict**: IN_PROGRESS

## 1. Purpose

GateLMS currently provides a topic-level AI primer and a topic question list. This work package proposes a more honest and useful learning loop:

```text
Topic orientation → concept lesson → related questions → review → mistake/revision
```

The goal is not to claim that one generated primer covers every question. The goal is to show what theory exists, which concepts each question requires, and where additional explanation or review is needed.

This is a design and verification package first. It must not introduce access locks: learners may open any subject, topic, theory, question, or notebook at any time.

## 2. Current-state evidence

The following observations were made from base commit `697b125`:

1. `TopicPrimerService` requests at most 10 sample questions using `PageRequest.of(0, 10)`.
2. Each sampled statement is truncated to 200 characters before entering the generation prompt.
3. The generated artifact contains motivation, core idea, invariant, worked example, and traps.
4. `TopicPage` requests questions with `size: 50`, so the visible count is a page result, not automatically proof that the topic contains exactly 50 questions.
5. The primer validator checks non-empty core fields, a worked example of at least 150 words, and fabricated student-population statistics. It does not prove that all topic questions are covered.
6. Book references are displayed as metadata. The current primer flow does not read private local book PDFs.

These are implementation facts, not claims about the correctness of every generated primer or question.

## 3. Product contract

### PC-1 — Primer scope is explicit

The UI must describe the primer as a topic orientation and must not imply that it is a complete solution manual for every question in the topic.

### PC-2 — Concepts are the teaching unit

Each topic may contain multiple concept lessons. A concept lesson should include:

- Plain-language explanation.
- Formal definitions, assumptions, and notation.
- One worked example.
- One counterexample or common trap.
- Short active-recall prompts.
- Links to related questions.

### PC-3 — Questions link back to concepts

Each question may be mapped to one or more concepts. A question review should identify the concepts it tests and provide a route back to the relevant lesson.

### PC-4 — Coverage is evidence-based

Coverage status must be computed from explicit question-to-concept mappings and lesson availability. AI-generated claims alone are not sufficient evidence.

Suggested statuses:

- `UNMAPPED`: required concept mapping has not been reviewed.
- `MAPPED`: concepts are identified, but lesson coverage is incomplete or unreviewed.
- `EXPLAINED`: mapped concepts have approved lessons.
- `REVIEW_NEEDED`: a question, answer, or explanation is ambiguous or disputed.

### PC-5 — No sequential locks

Recommendations may guide the learner, but no concept, question, subject, book reference, or notebook entry may be access-gated by prior completion.

### PC-6 — Private references stay private

Local book PDFs must not be committed to Git, included in public seed data, or uploaded to a provider without an explicit user action. Book metadata may be shared. A chapter reference must not imply that the AI read the book.

### PC-7 — Retention is part of the loop

The system should connect wrong answers and repeated mistakes to the relevant concept lesson, corrective takeaway, and revision queue.

## 4. Pilot scope: Process Synchronization

Process Synchronization is the first pilot topic because its questions can span several distinguishable reasoning patterns. The actual concept list must be derived from the stored questions and verified; the following is an initial hypothesis only:

1. Critical-section requirements.
2. Peterson-style software solutions.
3. Atomic hardware primitives.
4. Semaphore operations and conventions.
5. Semaphore state tracing.
6. Producer-consumer and bounded-buffer problems.
7. Readers-writers and dining-philosophers patterns.
8. Monitors and condition variables.
9. Race conditions, ordering, and interleavings.

The pilot must record which questions use which concepts and identify questions that cannot be confidently classified.

## 5. Proposed implementation shape

Prefer existing topic, question, anatomy, primer, progress, mistake, and revision models before adding new tables.

Potential additions, subject to Z.ai architectural approval:

- A reviewed concept catalog per topic.
- A reviewed question-to-concept mapping.
- A concept lesson/editorial artifact, reusable by question review.
- Read-only coverage summaries for students and curation workflows for admins.

Generated content should remain visibly marked as AI-generated until reviewed. Editing or acceptance of shared theory must remain an admin/curator operation.

## 6. Acceptance gates

### Analysis gate

- Determine the true total question count for Process Synchronization, without relying only on the frontend page size.
- Export or inspect every question statement, type, year, options, answer, explanation, and source.
- Classify every question into one or more concepts, or mark it `UNMAPPED`.
- Identify duplicate, ambiguous, malformed, or likely incorrect content.
- Compare the generated primer against the concept inventory.

### Backend gate

- A student can read the primer before attempting any question.
- A student can retrieve concept coverage and related questions.
- Coverage does not increase merely because an AI primer exists.
- Shared primer/concept editing and deletion are admin-authorized.
- All reads and writes remain user-scoped where the data is personal.

### Frontend gate

- The topic title is learner-friendly rather than an internal slug.
- The primer, concepts, questions, and solved notebook remain freely switchable.
- Each question can show its related concept(s) and a route to theory.
- The UI distinguishes `mapped`, `explained`, and `review needed` from solved status.
- No fabricated coverage percentage is displayed.

### Learning gate

- A learner can follow theory → question → explanation → revision without leaving the portal.
- A wrong answer links to a concrete concept or marks the question for review when mapping is uncertain.
- Repeated mistakes can enter the existing mistake/revision flow.

### Verification gate

- Known-good mapping → coverage is reported correctly.
- Deliberately unmapped question → coverage is not falsely reported as complete.
- A primer containing broad theory but missing a required concept → topic remains incomplete.
- A fresh learner can access any topic and concept without a prerequisite lock.

## 7. Out of scope for this package

- Committing copyrighted book PDFs or extracted book chapters.
- Building a full textbook RAG system before the question/concept model is validated.
- AI-generated question variants.
- Automatic claims that all questions are solvable from a primer.
- Payment, subscription, monetization, or hosted SaaS functionality.

## 8. Review request

Z.ai should review the architecture, schema implications, provenance rules, and acceptance gates.

Gemini/Antigravity should review implementation feasibility and propose the smallest executable work packages after the analysis gate.

The Operator should approve the pilot scope before application-code changes begin.

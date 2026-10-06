# AUDIT-PROTOCOL — GateLMS Verification & Quality Standards

## 1. Scope
Every implementation phase submitted to GateLMS must pass an audit against the following five verification pillars before being marked as accepted.

---

## 2. Verification Pillars

### Pillar 1: Build & Compilation Integrity
- Backend: `./mvnw clean compile` (or `mvn clean compile`) must complete with zero errors.
- Frontend: `npm run build` must succeed with zero TypeScript or Vite bundle errors.
- Docker: `docker compose config` must validate cleanly against syntax standards.

### Pillar 2: Flyway Migration Soundness
- All database schemas and evolutions must exist as immutable Flyway SQL files under `backend/src/main/resources/db/migration/`.
- Migrations must be forward-runnable from scratch on a clean PostgreSQL database without manual interventions.
- All column names must follow snake_case convention, and tables must specify primary keys and foreign key constraints where applicable.

### Pillar 3: Exam Scoring Fidelity (The Zero-Bug Pillar)
- The GATE scoring engine must strictly enforce:
  - **1-mark MCQ**: $+1.0$ for correct, $-0.3333$ for incorrect, $0.0$ for unattempted.
  - **2-mark MCQ**: $+2.0$ for correct, $-0.6667$ for incorrect, $0.0$ for unattempted.
  - **MSQ (Multi-Select)**: Full marks ($+1$ or $+2$) ONLY if the selected options exactly match the key. Zero marks otherwise. **Zero negative marks**.
  - **NAT (Numerical Answer Type)**: Full marks if candidate answer is within $[\text{minVal}, \text{maxVal}]$. Zero marks otherwise. **Zero negative marks**.
- Every scoring test suite must provide four-number reporting (files, test-cases, assertions, failures) and dual honest-judge tests (known-pass + deliberate-fail).

### Pillar 4: UI/UX Fidelity (TakeUForward Standards)
- Deep dark theme colors (`#0d0d0d` background, `#161616` cards, `#262626` borders).
- Responsive on desktop and tablet/iPad viewports.
- No layout shift or unstyled flashes during navigation.
- KaTeX LaTeX formulas must render cleanly without raw LaTeX string leaks.

### Pillar 5: Code Cleanliness & Zero Framework Churn
- No dead dependencies or experimental packages.
- Clean Kotlin idiomatic patterns; no unnecessary inheritance hierarchies.
- Strict isolation of API routes under `/api/v1/...`.

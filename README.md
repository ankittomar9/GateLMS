# GateLMS — Personal GATE 2028 Study Engine

A specialized, single-portal Learning Management System tailored to **GATE Computer Science & Information Technology (CSE)**.

Inspired by **takeUforward (TUF)** dark dashboard aesthetics, built for serious 2-year preparation.

---

## 🛠️ Tech Stack

- **Backend**: Spring Boot 3.4.2 · Kotlin 2.0.21 · Java 21 LTS · Maven
- **Database**: PostgreSQL 16 · Flyway Migrations
- **Frontend**: React 19 · Vite 6 · TypeScript · Tailwind CSS v4 · Lucide Icons · Recharts · KaTeX
- **Infrastructure**: Docker Compose (`docker-compose.yaml`)
- **Multi-Agent Governance**: `collab/` protocol (Gemini/Antigravity, Z.ai, Qwen, Operator)

---

## 🚀 Quick Start

### 1. Start Database
```bash
docker compose up -d postgres
```

### 2. Start Backend API (:8080)
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Start Frontend Dashboard (:5173)
```bash
cd frontend
npm run dev
```

Visit: `http://localhost:5173`

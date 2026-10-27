# GateLMS — Personal GATE 2028 Study Engine

A specialized, single-portal Learning Management System tailored to **GATE Computer Science & Information Technology (CSE)**.

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

---

## 🤖 Adding an AI Provider

GateLMS features a provider-agnostic AI adapter architecture. Any OpenAI-compatible provider (e.g. GLM-4, DeepSeek, Mistral, OpenRouter, vLLM) can be added purely via configuration with **zero code changes**:

### Recipe: Adding an OpenAI-Compatible Provider (e.g. GLM-4)

Add the entry under `app.ai.providers.entries` in `backend/src/main/resources/application.yml` (or via environment variables):

```yaml
app:
  ai:
    providers:
      entries:
        glm:
          kind: OPENAI_COMPATIBLE
          label: "GLM-4 (Zhipu AI)"
          endpoint: https://open.bigmodel.cn/api/paas/v4
          api-key: ${GLM_API_KEY:}
          default-model: glm-4.7
          allow-any-model: true
```

Once configured:
- The provider immediately appears in the `/api/v1/ai/status` endpoint and the frontend AI Studio Drawer selector.
- The provider participates in BYOK (Bring Your Own Key) via request header or browser session storage.
- Study features (Topic Primers, Concept Lessons, Anatomy breakdowns) can be switched to the new provider by setting `STUDY_PROVIDER=GLM` (or setting `app.ai.defaults.study-provider: GLM`).

> **Note on New Protocols**: Providers that do not adhere to the OpenAI chat completions wire protocol or Google Gemini protocol require adding a new `ChatTransport` implementation in `backend/src/main/kotlin/com/gatelms/ai/transport/` and registering its enum case in `ProviderKind`.

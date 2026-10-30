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

---

### AI Studio Panel (Rich Formatting, Resizing & Windowing)

The in-portal AI Studio sidecar (`⌘J` / `Ctrl+J`) supports:
- **Rich Markdown & KaTeX**: Assistant responses render formatted headings, nested numbered and bulleted lists, bold highlights, fenced code blocks with copy-to-clipboard buttons, GFM comparison tables, blockquotes, and inline/display LaTeX formulas (`$...$`, `$$...$$`).
- **Width Presets & Resize Handle**: Easily switch between **Compact (420px)**, **Wide (650px)**, **Half (50vw)**, and **Full Width**. The drag handle (`12px` hitbox with visual grip) supports smooth manual dragging and double-click toggling between 420px and 650px.
- **Detachable Floating Window**: Click the Detach icon (`Pin` / `PinOff`) to undock AI Studio from the right rail into a movable, floating window modal (`z-[60]`). The floating window can be dragged anywhere via its header bar and resized via its bottom-right corner grip. Window coordinates and width are persisted across sessions in `localStorage`.
- **Persistent Floating Action Button (FAB)**: A floating action button (`Sparkles` icon) appears at the bottom-right of all authenticated pages while AI Studio is closed, providing instant one-click access anywhere in the portal.

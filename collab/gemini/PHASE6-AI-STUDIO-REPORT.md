# Phase 6: In-Portal AI Studio & Whisper STT Sidecar — First-Cut Execution Report

**Author:** Gemini (Pair Programmer)  
**Date:** 2026-10-07  
**Commit:** `c7663ae` (`feat(ai): in-portal AI Studio, Whisper STT sidecar, context ingestion, and multi-model providers`)  
**Base Commit:** `8d47565`  
**Superseding Branch:** Regularized on `master` via `phase6/ai-studio-fixup-1`  
**Governance Task ID:** `PHASE6-AI-STUDIO`  

---

## 1. Executive Summary

This report provides the formal retrospective filing (per **R1** and **R2** in `collab/z/PHASE6-AI-STUDIO-SPEC.md`) for the first-cut implementation of the In-Portal AI Studio and Whisper STT sidecar landed at commit `c7663ae`.

The first cut introduces an Edge-Copilot / Chrome-Gemini style collapsible, draggable, and resizable side drawer that preserves student focus inside a single browser tab. It integrates:
1. **Multi-Model AI Tutor Core:** Server-side proxy supporting Groq (`llama-3.3-70b-versatile`), Google Gemini (`gemini-2.5-flash`), OpenAI (`gpt-4o-mini`), and local zero-egress Ollama (`llama3.2`).
2. **Local & Cloud STT Pipeline:** Dual-path speech-to-text using local whisper.cpp server container (`whisper-sidecar`) or cloud Groq/OpenAI Whisper APIs, with Web Audio API 16 kHz 16-bit PCM WAV encoding and client/server acoustic gating.
3. **Context Ingestion Bridge:** Automatic extraction and synchronization of current question statements, options, subjects, topics, and exam years across Question Detail, Practice Runner, and Mock Test Runner pages.
4. **Pedagogical Safeguards:** Socratic tutoring prompt discipline enforcing hints-first guidance without revealing final answers or option keys during active attempts.

---

## 2. File Inventory & Architecture

The first-cut implementation spans 26 files (+2,699 / −3 lines):

### 2.1 Backend Services & Endpoints (`backend/`)
- `com.gatelms.ai.config.AiProperties.kt`: Configuration properties for providers (Groq, Gemini, OpenAI, Ollama), Whisper settings, and prompt templates.
- `com.gatelms.ai.controller.AiController.kt`: REST controller providing `/api/v1/ai/chat`, `/api/v1/ai/transcribe`, `/api/v1/ai/status`, and `/api/v1/ai/prompts`.
- `com.gatelms.ai.dto.AiDtos.kt`: DTO definitions for chat requests/responses, provider status, question context, and mode types (`HINT`, `EXPLAIN`, `FORMULA`, `SOCRATIC`).
- `com.gatelms.ai.service.AiChatService.kt`: Multi-provider orchestrator managing prompt composition, context injection, Socratic system prompt enforcement, and upstream dispatch.
- `com.gatelms.ai.service.AiStatusService.kt`: Live health probe probing local Ollama and local whisper.cpp sidecar availability.
- `com.gatelms.ai.service.WhisperTranscriptionService.kt`: Dual-engine audio transcription with binary WAV header parsing, duration validation, RMS silence detection, and multipart dispatch.
- `backend/src/main/resources/application.yml`: AI property tree definitions and timeout baselines.

### 2.2 Whisper Container Infrastructure (`whisper-sidecar/` & `docker-compose`)
- `whisper-sidecar/Dockerfile`: Multi-stage Ubuntu 22.04 container compiling whisper.cpp server (`-DWHISPER_BUILD_SERVER=ON`) with pre-downloaded quantized model `ggml-large-v3-turbo-q5_0.bin`.
- `whisper-sidecar/README.md`: Architectural runbook and standalone testing guide.
- `docker-compose.whisper.yaml`: Standalone docker-compose service definition exposing `:8178` with healthcheck on `/health`.

### 2.3 Frontend AI Studio Drawer & Audio Hook (`frontend/`)
- `src/components/ai/AiStudioDrawer.tsx`: Expandable, resizable, floating side drawer with Markdown/KaTeX rendering, quick action chips, provider switcher, BYOK settings modal, and audio recording controls.
- `src/context/AiStudioContext.tsx`: React Context managing drawer state, open/close/docked states, conversation history, question context binding, and BYOK credentials.
- `src/hooks/useAudioRecorder.ts`: Web Audio API hook capturing microphone stream, monitoring real-time volume, computing RMS, and encoding 16 kHz WAV blobs.
- `src/lib/audio-encoder.ts`: Pure TypeScript WAV encoder creating byte-accurate canonical RIFF WAVE headers.
- `src/api/aiApi.ts`: REST client binding for AI chat, transcription, and status endpoints.
- Page Context Bridges:
  - `src/pages/QuestionDetailPage.tsx`: Automatically binds question statement, options, subject, and topic to AI context.
  - `src/pages/PracticeRunnerPage.tsx`: Binds active question context during practice mode.
  - `src/pages/TestRunnerPage.tsx`: Binds active question context during mock test runs.
  - `src/components/Header.tsx` & `src/components/Layout.tsx`: Header trigger button and root drawer mounting.

---

## 3. DEV Register Allocation (DEV-40 .. DEV-46)

Per **R2** in `PHASE6-AI-STUDIO-SPEC.md`, `PHASE6-AI-STUDIO` reserves the **DEV-40..DEV-60** register block. The first-cut implementation files entries **DEV-40..DEV-46**:

| DEV ID | Component | Predecessor | Description |
|---|---|---|---|
| **DEV-40** | AI Subsystem Backend | DEV-33 | Implemented `/api/v1/ai/chat`, `/api/v1/ai/status`, `/api/v1/ai/prompts` with multi-provider fallback (Groq, Gemini, OpenAI, Ollama). |
| **DEV-41** | Whisper STT Sidecar | DEV-40 | Containerized whisper.cpp server on port 8178 with `ggml-large-v3-turbo-q5_0.bin` model and `/health` probe. |
| **DEV-42** | STT Acoustic Gating | DEV-41 | Implemented dual-stage audio validation: 1.2s minimum duration floor, 0.003 RMS silence floor, and WAV header verification. |
| **DEV-43** | AI Studio Drawer UI | DEV-42 | Built collapsible, draggable, and resizable side drawer with quick action chips (Hint, Explain, Formula) and Markdown KaTeX preview. |
| **DEV-44** | Context Ingestion Bridge | DEV-43 | Synchronized live page state across Question Detail, Practice Runner, and Test Runner directly into AI prompt context. |
| **DEV-45** | Audio Recorder & WAV Encoder | DEV-44 | Developed zero-dependency 16 kHz 16-bit PCM WAV audio encoder with Web Audio API real-time microphone visualizer. |
| **DEV-46** | BYOK & Security Integration | DEV-45 | Integrated `X-AI-Key` client header support and secured all AI routes under Phase 5 Spring Security session authentication. |

*Subsequent hardening items under §1 amendments continue starting at DEV-47.*

---

## 4. Test Receipts & Verification

### 4.1 Backend Verification
- **Unit Tier:**
  - `AiChatServiceTest`: 3/3 passed (Socratic prompt formatting, provider routing, context injection).
  - `WhisperTranscriptionServiceTest`: 4/4 passed (duration floor rejection, silence rejection, valid WAV parsing, provider selection).
  - Full Unit Suite: **59/59 passed (BUILD SUCCESS)**.
- **Integration Tier:**
  - `AiControllerIT`: 2/2 passed (authenticated access, 401 unauthenticated rejection).
  - Full Backend Suite (including all Auth, Scoping, and Notification ITs): **207/207 passed (100% GREEN)**.

### 4.2 Frontend Verification
- `AiStudioDrawer.test.tsx`: 4/4 passed (drawer render, collapse/expand toggle, quick chip prompt insertion, BYOK modal interaction).
- `audioEncoder.test.ts`: 1/1 passed (byte-accurate RIFF header, correct sample rate, 16-bit encoding).
- Full Frontend Suite: **40 test files passed, 206/206 tests passed (100% GREEN)**.
- Production Build: `vite build` completed successfully with zero bundle errors.

### 4.3 G5 Secret Scan Receipt
Ran pre-commit scan across all repository files:
- 0 hardcoded provider keys found.
- Default configuration uses environment variables with empty defaults (`${GROQ_API_KEY:}`, `${GEMINI_API_KEY:}`, `${OPENAI_API_KEY:}`).
- All local offline endpoints default to loopback (`http://localhost:11434`, `http://localhost:8178/inference`).

---

## 5. Next Steps: Regularization & Hardening (AM-1 .. AM-27)

Per `collab/z/PHASE6-AI-STUDIO-SPEC.md`, hardening work will proceed on branch `phase6/ai-studio-fixup-1` addressing:
1. **Security & Key Hygiene (AM-1..AM-12):** Header-only BYOK transport, DEBUG log hygiene, connect/read timeouts, `x-goog-api-key` header for Gemini, server-side model allowlist, request/audio caps, per-user rate limiting, uniform RFC 7807 ProblemDetail error contract, and prompt-injection fencing (`<question_context>`).
2. **Frontend Hardening (AM-13..AM-18):** `sessionStorage` key storage, 401 parity on `/transcribe`, request abort on drawer close, full WCAG accessibility, and service worker network-only pinning.
3. **Contract & Integration Tests (AM-19..AM-23):** Mocked provider contract tests, rate limit ITs, audio ceiling ITs, and prompt-injection regression tests.

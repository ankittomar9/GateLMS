# Local Whisper.cpp Speech-to-Text Sidecar for GateLMS

This sidecar provides 100% offline, local neural speech-to-text inference on port `:8178` for GateLMS.

## Features
- Runs [whisper.cpp](https://github.com/ggerganov/whisper.cpp) C++ server on port `:8178`.
- Model: `ggml-large-v3-turbo-q5_0.bin` (~574MB) for high accuracy and fast inference on technical GATE CS jargon and mathematical formulations.
- Zero cloud egress: voice recordings stay entirely local.
- Automatically detected by GateLMS backend `/api/v1/ai/status`.

## Quickstart (Docker Compose)
```bash
docker compose -f docker-compose.whisper.yaml up -d
```

## Health Check
```bash
curl http://localhost:8178/health
```

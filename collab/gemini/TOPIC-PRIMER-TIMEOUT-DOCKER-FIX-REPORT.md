# Topic Primer Timeout & Docker Binary Refresh Report

**Task:** Topic Primer Generation Timeout & Octet-Stream Content-Type Extraction Fix  
**Branch:** `ai/topic-primer-timeout-and-packaging-fix`  
**Base:** `master` (`8b6e66e`)  
**Status:** PASS  

---

## 1. Problem Statement & Root Cause

### Symptoms
When requesting theory primer generation for `topic_prog_c_pointers`, the UI and Grafana Loki reported:
```
Topic primer generation attempt 2 failed for topic 'topic_prog_c_pointers': 
AI provider timed out: Error while extracting response for type [java.lang.String] and content type [application/octet-stream]
```

### Root Cause Analysis
1. **Container Binary Desynchronization:**
   - Inspection of Docker container `gatelms-backend` (`77060362da93`) revealed it was running a JAR built on **27-10-2026 23:18:49** (`gate-lms-backend-1.0.0-SNAPSHOT.jar`).
   - The recent commits from Oct 30 and Oct 31 (including the 5-minute timeout `chatReadTimeoutMs = 300000`, the LaTeX escape repair `repairJsonEscapes`, and Jackson parser resilience) were tested in Maven but had never been packaged via `mvn package` into `backend/target/` and rebuilt into the Docker image.
   - Consequently, the container was still running with the legacy 60-second read timeout (`chatReadTimeoutMs = 60000`).
2. **60-Second Socket Timeout:**
   - In Attempt 1, Gemini 3.8-flash returned 503, and fallback to 3.5-flash failed JSON parsing because the old JAR lacked `repairJsonEscapes`.
   - In Attempt 2, Gemini 3.8-flash took longer than 60 seconds. At exactly 60.07 seconds (`18:28:16` to `18:29:16`), `HttpURLConnection` threw `SocketTimeoutException: Read timed out`.
3. **Octet-Stream Extraction Failure:**
   - When a socket timeout interrupts HTTP response streaming, Java's `URLConnection.getContentType()` defaults to `application/octet-stream`.
   - Spring's `HttpMessageConverterExtractor` attempting to extract `String.class` failed on `application/octet-stream`, wrapping the timeout in `RestClientException: Error while extracting response for type [java.lang.String] and content type [application/octet-stream]`.

---

## 2. Changes Made

1. **Robust Response Body Extraction (`ByteArray` Bridge):**
   - In [`GeminiTransport.kt`](file:///D:/GateLMS/backend/src/main/kotlin/com/gatelms/ai/transport/GeminiTransport.kt#L80-L82), [`OpenAiCompatTransport.kt`](file:///D:/GateLMS/backend/src/main/kotlin/com/gatelms/ai/transport/OpenAiCompatTransport.kt#L70-L72), and [`OllamaTransport.kt`](file:///D:/GateLMS/backend/src/main/kotlin/com/gatelms/ai/transport/OllamaTransport.kt#L63-L65):
     - Added `.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)` to request specifications.
     - Changed body extraction to `.body(ByteArray::class.java)?.let { String(it, Charsets.UTF_8) } ?: "{}"`.
     - `ByteArrayHttpMessageConverter` natively accepts all media types including `application/octet-stream`, eliminating the extraction failure even if the server headers are abnormal.
     - Added startup info log logging the configured connect and read timeouts.

2. **Backend Packaging & Docker Image Rebuild:**
   - Re-packaged fresh JAR: `mvn package -DskipTests` (generated `gate-lms-backend-1.0.0-SNAPSHOT.jar` at `01-11-2026 00:17:39`).
   - Rebuilt Docker image: `docker compose build backend`.
   - Recreated and restarted container: `docker compose up -d backend`.
   - Container logs confirm initialization with 5-minute timeout:
     `GeminiTransport initialized with connectTimeout=3000ms, readTimeout=300000ms`

---

## 3. Verification Receipts

- `AiProviderAdapterTest`: 6 tests run, 0 failures, 0 errors (PASS).
- `TopicPrimerValidationTest` & `AnatomyJsonParserTest`: 18 tests run, 0 failures, 0 errors (PASS).
- `git diff --check`: 0 formatting or whitespace defects.
- Docker container `gatelms-backend` running fresh image with active 5-minute timeout (`300000ms`) and LaTeX repair engine.

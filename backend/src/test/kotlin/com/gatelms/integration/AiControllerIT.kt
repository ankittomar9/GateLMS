package com.gatelms.integration

import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.dto.ProviderStatusDto
import com.gatelms.ai.dto.TranscriptionResponse
import com.gatelms.ai.security.AiRateLimiter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap

class AiControllerIT : BaseIntegrationTest() {

    @Autowired
    private lateinit var aiRateLimiter: AiRateLimiter

    @BeforeEach
    fun resetRateLimiter() {
        aiRateLimiter.reset()
    }

    @Test
    fun `1 GET status returns status list for all providers when authenticated`() {
        val resp = restTemplate.exchange(
            "/api/v1/ai/status",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<ProviderStatusDto>>() {}
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val list = resp.body!!
        assertTrue(list.isNotEmpty())
        val providers = list.map { it.provider }
        assertTrue(providers.contains("GROQ"))
        assertTrue(providers.contains("GEMINI"))
        assertTrue(providers.contains("OPENAI"))
        assertTrue(providers.contains("OLLAMA"))
        assertTrue(providers.contains("WHISPER_LOCAL"))
    }

    @Test
    fun `2 POST transcribe with short audio returns TOO_SHORT without calling external APIs`() {
        val tinyAudio = ByteArray(200) { 0 }
        val fileResource = object : ByteArrayResource(tinyAudio) {
            override fun getFilename(): String = "tiny.wav"
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add("file", fileResource)

        val requestEntity = HttpEntity(body, headers)
        val resp = restTemplate.postForEntity("/api/v1/ai/transcribe", requestEntity, TranscriptionResponse::class.java)

        assertEquals(HttpStatus.OK, resp.statusCode)
        val result = resp.body!!
        assertEquals("TOO_SHORT", result.status)
        assertTrue(result.message!!.contains("too short"))
    }

    @Test
    fun `3 unauthenticated GET status returns 401 Unauthorized (AM-20)`() {
        logout()
        val resp = restTemplate.getForEntity("/api/v1/ai/status", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `4 unauthenticated POST chat returns 401 Unauthorized (AM-20)`() {
        val xsrfToken = authInterceptor.currentXsrfToken ?: "csrf-test-token"
        logout()
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Hello")),
            provider = "GROQ"
        )
        val headers = HttpHeaders().apply {
            add(HttpHeaders.COOKIE, "XSRF-TOKEN=$xsrfToken")
            add("X-XSRF-TOKEN", xsrfToken)
            contentType = MediaType.APPLICATION_JSON
        }
        val resp = restTemplate.postForEntity("/api/v1/ai/chat", HttpEntity(req, headers), String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `5 unauthenticated POST transcribe returns 401 Unauthorized (AM-20)`() {
        val xsrfToken = authInterceptor.currentXsrfToken ?: "csrf-test-token"
        logout()
        val headers = HttpHeaders().apply {
            add(HttpHeaders.COOKIE, "XSRF-TOKEN=$xsrfToken")
            add("X-XSRF-TOKEN", xsrfToken)
            contentType = MediaType.MULTIPART_FORM_DATA
        }
        val body = LinkedMultiValueMap<String, Any>()
        body.add("file", object : ByteArrayResource(ByteArray(100)) {
            override fun getFilename(): String = "test.wav"
        })
        val requestEntity = HttpEntity(body, headers)
        val resp = restTemplate.postForEntity("/api/v1/ai/transcribe", requestEntity, String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `6 POST chat with invalid CSRF token returns 403 Forbidden (AM-20)`() {
        loginAs("ankur", "admin123")
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Test CSRF")),
            provider = "GROQ"
        )
        val headers = HttpHeaders()
        headers.set("X-XSRF-TOKEN", "invalid-csrf-token-12345")
        val requestEntity = HttpEntity(req, headers)

        val resp = restTemplate.postForEntity("/api/v1/ai/chat", requestEntity, String::class.java)
        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
    }

    @Test
    fun `7 POST chat with unapproved model returns 400 Bad Request (AM-5, AM-20)`() {
        loginAs("ankur", "admin123")
        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Hello")),
            provider = "GROQ",
            model = "unauthorized-expensive-model-v99"
        )
        val resp = restTemplate.postForEntity("/api/v1/ai/chat", req, ProblemDetail::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        assertTrue(resp.body!!.detail!!.contains("not in the allowlist"))
    }

    @Test
    fun `8 POST chat exceeding turn caps returns 413 Payload Too Large (AM-6, AM-20)`() {
        loginAs("ankur", "admin123")
        val messages = (1..32).map { ChatMessageDto(role = "user", content = "Turn $it") }
        val req = AiChatRequest(
            messages = messages,
            provider = "GROQ"
        )
        val resp = restTemplate.postForEntity("/api/v1/ai/chat", req, ProblemDetail::class.java)
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, resp.statusCode)
        assertTrue(resp.body!!.detail!!.contains("30 turns"))
    }

    @Test
    fun `9 chat quota exhaustion returns 429 Too Many Requests with Retry-After (AM-8, AM-20)`() {
        val user = loginAs("ankur", "admin123")
        val userUuid = java.util.UUID.fromString(user.userId)

        // Consume 20 quota attempts for user
        for (i in 1..20) {
            aiRateLimiter.checkChatLimit(userUuid)
        }

        val req = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = "Another turn")),
            provider = "GROQ"
        )
        val resp = restTemplate.postForEntity("/api/v1/ai/chat", req, ProblemDetail::class.java)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, resp.statusCode)
        assertNotNull(resp.headers.getFirst("Retry-After"))
        assertTrue(resp.body!!.detail!!.contains("quota exceeded"))
    }
}

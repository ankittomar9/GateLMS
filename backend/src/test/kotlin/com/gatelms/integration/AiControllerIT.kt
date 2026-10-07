package com.gatelms.integration

import com.gatelms.ai.dto.ProviderStatusDto
import com.gatelms.ai.dto.TranscriptionResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap

class AiControllerIT : BaseIntegrationTest() {

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
    fun `3 unauthenticated request to AI endpoints returns 401 Unauthorized`() {
        logout()
        val resp = restTemplate.getForEntity("/api/v1/ai/status", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }
}

package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.exception.AiRateLimitException
import com.gatelms.ai.exception.AiValidationException
import com.gatelms.ai.registry.ProviderKind
import com.gatelms.ai.registry.ProviderRegistry
import com.gatelms.ai.registry.ProviderSpec
import com.gatelms.ai.service.AiChatService
import com.gatelms.ai.service.AiStatusService
import com.gatelms.ai.transport.GeminiTransport
import com.gatelms.ai.transport.OllamaTransport
import com.gatelms.ai.transport.OpenAiCompatTransport
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress

class AiProviderAdapterTest {

    private lateinit var mockServer: HttpServer
    private var mockPort: Int = 0
    private lateinit var aiProperties: AiProperties
    private lateinit var objectMapper: ObjectMapper
    private val restClientBuilder: RestClient.Builder = RestClient.builder()

    @BeforeEach
    fun setup() {
        mockServer = HttpServer.create(InetSocketAddress(0), 0)
        mockPort = mockServer.address.port
        mockServer.start()

        aiProperties = AiProperties()
        aiProperties.http.connectTimeoutMs = 1000
        aiProperties.http.chatReadTimeoutMs = 1500
        aiProperties.http.sttReadTimeoutMs = 1500
        aiProperties.http.maxRetries = 1
        objectMapper = ObjectMapper()
    }

    @AfterEach
    fun teardown() {
        mockServer.stop(0)
    }

    @Test
    fun `G1 - config-only provider add works without code edits`() {
        var capturedAuthHeader: String? = null
        mockServer.createContext("/api/paas/v4/chat/completions") { exchange ->
            capturedAuthHeader = exchange.requestHeaders.getFirst("Authorization")
            val resp = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "Hello from GLM 4.7!"
                      }
                    }
                  ]
                }
            """.trimIndent()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        // Configure test-only GLM entry programmatically (0 code edits)
        aiProperties.providers.entries["glm"] = ProviderSpec(
            kind = ProviderKind.OPENAI_COMPATIBLE,
            label = "GLM-4",
            endpoint = "http://localhost:$mockPort/api/paas/v4",
            apiKey = "mock-glm-key-123",
            defaultModel = "glm-4.7",
            allowAnyModel = true
        )

        val registry = ProviderRegistry(aiProperties)
        val transports = listOf(
            OpenAiCompatTransport(aiProperties, objectMapper, restClientBuilder),
            GeminiTransport(aiProperties, objectMapper, restClientBuilder),
            OllamaTransport(aiProperties, objectMapper, restClientBuilder)
        )
        val chatService = AiChatService(aiProperties, registry, transports)
        val statusService = AiStatusService(aiProperties, registry)

        val chatResp = chatService.chat(
            AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Test")),
                provider = "GLM"
            )
        )

        assertEquals("Bearer mock-glm-key-123", capturedAuthHeader)
        assertEquals("Hello from GLM 4.7!", chatResp.reply)
        assertEquals("GLM", chatResp.provider)
        assertEquals("glm-4.7", chatResp.model)

        val statuses = statusService.getProvidersStatus()
        val glmStatus = statuses.find { it.provider == "GLM" }
        assertNotNull(glmStatus, "GLM provider must appear in status response")
        assertEquals("GLM-4", glmStatus!!.label)
        assertEquals("glm-4.7", glmStatus.model)
        assertTrue(glmStatus.configured)
    }

    @Test
    fun `G4 - Gemini fallback parity - switches to fallback model on 429`() {
        var defaultModelCalls = 0
        var fallbackModelCalls = 0

        mockServer.createContext("/v1beta/models/gemini-3.5-flash:generateContent") { exchange ->
            defaultModelCalls++
            val resp = """{ "error": { "code": 429, "message": "Resource exhausted" } }"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(429, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        mockServer.createContext("/v1beta/models/gemini-3.0-flash:generateContent") { exchange ->
            fallbackModelCalls++
            val resp = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{ "text": "Recovered from fallback model" }]
                      }
                    }
                  ]
                }
            """.trimIndent()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        aiProperties.providers.gemini.endpoint = "http://localhost:$mockPort/v1beta/models"
        aiProperties.providers.gemini.apiKey = "mock-key"
        aiProperties.providers.gemini.defaultModel = "gemini-3.5-flash"
        aiProperties.providers.gemini.fallbackModel = "gemini-3.0-flash"

        val registry = ProviderRegistry(aiProperties)
        val transports = listOf(
            GeminiTransport(aiProperties, objectMapper, restClientBuilder)
        )
        val chatService = AiChatService(aiProperties, registry, transports)

        val resp = chatService.chat(
            AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Test")),
                provider = "GEMINI"
            )
        )

        assertEquals("Recovered from fallback model", resp.reply)
        assertEquals("gemini-3.0-flash", resp.model)
        assertEquals(1, defaultModelCalls)
        assertEquals(1, fallbackModelCalls)
    }

    @Test
    fun `G4 - Gemini fallback exhausted throws AiRateLimitException with Retry-After after exactly 2 requests`() {
        var callCount = 0

        mockServer.createContext("/v1beta/models/gemini-3.5-flash:generateContent") { exchange ->
            callCount++
            val resp = """{ "error": { "code": 429, "message": "Resource exhausted" } }"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.responseHeaders.set("Retry-After", "15")
            exchange.sendResponseHeaders(429, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        mockServer.createContext("/v1beta/models/gemini-3.0-flash:generateContent") { exchange ->
            callCount++
            val resp = """{ "error": { "code": 429, "message": "Resource exhausted" } }"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.responseHeaders.set("Retry-After", "30")
            exchange.sendResponseHeaders(429, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        aiProperties.providers.gemini.endpoint = "http://localhost:$mockPort/v1beta/models"
        aiProperties.providers.gemini.apiKey = "mock-key"
        aiProperties.providers.gemini.defaultModel = "gemini-3.5-flash"
        aiProperties.providers.gemini.fallbackModel = "gemini-3.0-flash"

        val registry = ProviderRegistry(aiProperties)
        val transports = listOf(
            GeminiTransport(aiProperties, objectMapper, restClientBuilder)
        )
        val chatService = AiChatService(aiProperties, registry, transports)

        val ex = assertThrows<AiRateLimitException> {
            chatService.chat(
                AiChatRequest(
                    messages = listOf(ChatMessageDto(role = "user", content = "Test")),
                    provider = "GEMINI"
                )
            )
        }

        assertEquals(2, callCount, "Must make exactly 2 requests before giving up")
        assertEquals(30, ex.retryAfterSeconds)
    }

    @Test
    fun `G5 - study provider resolution respects defaults and env override`() {
        val props = AiProperties()
        props.defaults.studyProvider = "OLLAMA"
        val registry = ProviderRegistry(props)

        assertEquals("OLLAMA", registry.resolveStudyProvider(null))
        assertEquals("OPENAI", registry.resolveStudyProvider("OPENAI"))
    }

    @Test
    fun `G6 - unknown provider throws AiValidationException listing valid providers`() {
        val registry = ProviderRegistry(aiProperties)
        val chatService = AiChatService(aiProperties, registry, emptyList())

        val ex = assertThrows<AiValidationException> {
            chatService.chat(
                AiChatRequest(
                    messages = listOf(ChatMessageDto(role = "user", content = "Test")),
                    provider = "GORK"
                )
            )
        }

        assertTrue(ex.message!!.contains("Unknown AI provider 'GORK'"))
        assertTrue(ex.message!!.contains("GROQ"))
        assertTrue(ex.message!!.contains("GEMINI"))
        assertTrue(ex.message!!.contains("OPENAI"))
        assertTrue(ex.message!!.contains("OLLAMA"))
    }

    @Test
    fun `G7 - Gemini transport sends x-goog-api-key header and never in URL query string`() {
        var capturedKeyHeader: String? = null
        var capturedQuery: String? = null

        mockServer.createContext("/v1beta/models/gemini-3.5-flash:generateContent") { exchange ->
            capturedKeyHeader = exchange.requestHeaders.getFirst("x-goog-api-key")
            capturedQuery = exchange.requestURI.query
            val resp = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{ "text": "Secret hygiene test pass" }]
                      }
                    }
                  ]
                }
            """.trimIndent()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        aiProperties.providers.gemini.endpoint = "http://localhost:$mockPort/v1beta/models"
        val registry = ProviderRegistry(aiProperties)
        val transport = GeminiTransport(aiProperties, objectMapper, restClientBuilder)

        val spec = registry.require("GEMINI")
        val reply = transport.chat(
            spec = spec,
            systemInstruction = "System",
            messages = listOf(ChatMessageDto("user", "Hello")),
            model = "gemini-3.5-flash",
            apiKey = "top-secret-gemini-key"
        )

        assertEquals("top-secret-gemini-key", capturedKeyHeader)
        assertNull(capturedQuery, "Query string must not contain any parameters")
        assertEquals("Secret hygiene test pass", reply)
    }
}

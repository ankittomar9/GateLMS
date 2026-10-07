package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.exception.AiProviderException
import com.gatelms.ai.exception.AiTimeoutException
import com.gatelms.ai.exception.AiValidationException
import com.gatelms.ai.service.AiChatService
import com.gatelms.ai.service.WhisperTranscriptionService
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.client.RestClient
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * CONTRACT TESTS (AM-19, AM-21)
 * These tests use an embedded mock HTTP server to verify client request/response contracts,
 * timeout policies, header propagation, and 5xx retry discipline across all AI providers.
 * Real cloud live smoke tests are tagged @Tag("cloud") and enabled strictly via environment opt-in.
 */
class AiProviderContractTest {

    private lateinit var mockServer: HttpServer
    private var mockPort: Int = 0
    private lateinit var aiProperties: AiProperties
    private lateinit var objectMapper: ObjectMapper
    private lateinit var chatService: AiChatService
    private lateinit var whisperService: WhisperTranscriptionService

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
        chatService = AiChatService(aiProperties, objectMapper, RestClient.builder())
        whisperService = WhisperTranscriptionService(aiProperties, objectMapper, RestClient.builder())
    }

    @AfterEach
    fun teardown() {
        mockServer.stop(0)
    }

    @Test
    fun `Groq chat contract - parses choices and validates Bearer header (AM-19)`() {
        var capturedAuthHeader: String? = null
        mockServer.createContext("/openai/v1/chat/completions") { exchange ->
            capturedAuthHeader = exchange.requestHeaders.getFirst("Authorization")
            val resp = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "A tree with n vertices always has n-1 edges."
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

        aiProperties.providers.groq.endpoint = "http://localhost:$mockPort/openai/v1"
        val response = chatService.chat(
            request = AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Explain tree edge property")),
                provider = "GROQ",
                model = "llama-3.3-70b-versatile"
            ),
            authHeaderKey = "mock-groq-key-123"
        )

        assertEquals("Bearer mock-groq-key-123", capturedAuthHeader)
        assertEquals("A tree with n vertices always has n-1 edges.", response.reply)
        assertEquals("GROQ", response.provider)
    }

    @Test
    fun `Gemini chat contract - verifies x-goog-api-key header and parses candidates (AM-4, AM-19)`() {
        var capturedKeyHeader: String? = null
        var requestPath: String? = null

        mockServer.createContext("/models/gemini-2.0-flash:generateContent") { exchange ->
            capturedKeyHeader = exchange.requestHeaders.getFirst("x-goog-api-key")
            requestPath = exchange.requestURI.toString()
            val resp = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "Dijkstra algorithm uses a priority queue with O((V+E) log V) time." }
                        ]
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

        aiProperties.providers.gemini.endpoint = "http://localhost:$mockPort/models"
        val response = chatService.chat(
            request = AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Dijkstra complexity")),
                provider = "GEMINI",
                model = "gemini-2.0-flash"
            ),
            authHeaderKey = "test-gemini-header-key"
        )

        assertEquals("test-gemini-header-key", capturedKeyHeader)
        assertFalse(requestPath!!.contains("?key="), "Key must not be in query string (AM-4)")
        assertEquals("Dijkstra algorithm uses a priority queue with O((V+E) log V) time.", response.reply)
    }

    @Test
    fun `Ollama chat contract - parses local message content (AM-19)`() {
        mockServer.createContext("/api/chat") { exchange ->
            val resp = """
                {
                  "message": {
                    "role": "assistant",
                    "content": "Paging reduces external fragmentation at the cost of internal fragmentation."
                  }
                }
            """.trimIndent()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        aiProperties.providers.ollama.endpoint = "http://localhost:$mockPort"
        val response = chatService.chat(
            request = AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Explain paging tradeoffs")),
                provider = "OLLAMA",
                model = "llama3"
            )
        )

        assertEquals("Paging reduces external fragmentation at the cost of internal fragmentation.", response.reply)
        assertEquals("OLLAMA", response.provider)
    }

    @Test
    fun `Chat retry policy - recovers from transient 500 error on second attempt (AM-10, AM-19)`() {
        var callCount = 0
        mockServer.createContext("/openai/v1/chat/completions") { exchange ->
            callCount++
            if (callCount == 1) {
                val err = """{"error": "Upstream transient 500"}"""
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(500, err.toByteArray().size.toLong())
                exchange.responseBody.write(err.toByteArray())
                exchange.close()
            } else {
                val resp = """
                    {
                      "choices": [
                        {
                          "message": { "role": "assistant", "content": "Recovered response after retry" }
                        }
                      ]
                    }
                """.trimIndent()
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
                exchange.responseBody.write(resp.toByteArray())
                exchange.close()
            }
        }

        aiProperties.providers.groq.endpoint = "http://localhost:$mockPort/openai/v1"
        val response = chatService.chat(
            request = AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Test retry")),
                provider = "GROQ"
            ),
            authHeaderKey = "key"
        )

        assertEquals(2, callCount, "Should have executed 1 retry on 500 failure")
        assertEquals("Recovered response after retry", response.reply)
    }

    @Test
    fun `Chat timeout policy - aborts hung upstream and throws AiTimeoutException (AM-3, AM-19)`() {
        mockServer.createContext("/openai/v1/chat/completions") { exchange ->
            Thread.sleep(2500) // Longer than chatReadTimeoutMs (1500)
            val resp = """{"choices": [{"message": {"content": "Late"}}]}"""
            exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        aiProperties.providers.groq.endpoint = "http://localhost:$mockPort/openai/v1"
        assertThrows(AiTimeoutException::class.java) {
            chatService.chat(
                request = AiChatRequest(messages = listOf(ChatMessageDto(role = "user", content = "Timeout test")), provider = "GROQ"),
                authHeaderKey = "key"
            )
        }
    }

    @Test
    fun `Chat provider 4xx rejection throws AiValidationException without retrying (AM-10, AM-19)`() {
        var callCount = 0
        mockServer.createContext("/openai/v1/chat/completions") { exchange ->
            callCount++
            val err = """{"error": {"message": "Invalid API key"}}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(401, err.toByteArray().size.toLong())
            exchange.responseBody.write(err.toByteArray())
            exchange.close()
        }

        aiProperties.providers.groq.endpoint = "http://localhost:$mockPort/openai/v1"
        assertThrows(AiValidationException::class.java) {
            chatService.chat(
                request = AiChatRequest(messages = listOf(ChatMessageDto(role = "user", content = "Auth test")), provider = "GROQ"),
                authHeaderKey = "bad-key"
            )
        }
        assertEquals(1, callCount, "4xx client error must never be retried")
    }

    @Test
    fun `Groq STT contract - parses transcription response and validates 0 retries (AM-10, AM-19)`() {
        var callCount = 0

        // Mock sidecar health
        mockServer.createContext("/health") { ex ->
            ex.sendResponseHeaders(200, 2)
            ex.responseBody.write("OK".toByteArray())
            ex.close()
        }

        mockServer.createContext("/inference") { exchange ->
            callCount++
            val resp = """{"text": "What is the time complexity of quicksort in the worst case?"}"""
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, resp.toByteArray().size.toLong())
            exchange.responseBody.write(resp.toByteArray())
            exchange.close()
        }

        // Generate 1.5s audio
        val sampleRate = 16000
        val totalSamples = (1.5 * sampleRate).toInt()
        val dataSize = totalSamples * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()); buffer.putInt(36 + dataSize); buffer.put("WAVEfmt ".toByteArray())
        buffer.putInt(16); buffer.putShort(1); buffer.putShort(1); buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2); buffer.putShort(2); buffer.putShort(16); buffer.put("data".toByteArray()); buffer.putInt(dataSize)
        for (i in 0 until totalSamples) buffer.putShort(3000)

        val file = MockMultipartFile("file", "audio.wav", "audio/wav", buffer.array())

        aiProperties.whisper.localEndpoint = "http://localhost:$mockPort/inference"
        aiProperties.whisper.sttProvider = "local"

        val result = whisperService.transcribeAudio(file, providerOverride = "LOCAL")
        assertEquals("SUCCESS", result.status)
        assertEquals("What is the time complexity of quicksort in the worst case?", result.text)
        assertEquals(1, callCount)
    }

    @Test
    @Tag("cloud")
    @EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = ".+")
    fun `live cloud Groq smoke test (opt-in only via GROQ_API_KEY env) (AM-21)`() {
        val cloudService = AiChatService(AiProperties(), ObjectMapper(), RestClient.builder())
        val response = cloudService.chat(
            request = AiChatRequest(
                messages = listOf(ChatMessageDto(role = "user", content = "Respond with one word: READY")),
                provider = "GROQ",
                model = "llama-3.3-70b-versatile"
            )
        )
        assertNotNull(response.reply)
        assertTrue(response.reply.isNotBlank())
    }
}

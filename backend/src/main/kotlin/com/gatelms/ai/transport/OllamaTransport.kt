package com.gatelms.ai.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.exception.AiServiceUnavailableException
import com.gatelms.ai.registry.ProviderKind
import com.gatelms.ai.registry.ProviderSpec
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OllamaTransport(
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder
) : ChatTransport {
    private val log = LoggerFactory.getLogger(OllamaTransport::class.java)

    override val kind: ProviderKind = ProviderKind.OLLAMA

    private val requestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(aiProperties.http.connectTimeoutMs.toInt())
        setReadTimeout(aiProperties.http.chatReadTimeoutMs.toInt())
    }
    private val restClient = restClientBuilder
        .requestFactory(requestFactory)
        .build()

    override fun chat(
        spec: ProviderSpec,
        systemInstruction: String,
        messages: List<ChatMessageDto>,
        model: String,
        apiKey: String?
    ): String {
        val baseEndpoint = spec.endpoint.ifBlank { "http://localhost:11434" }.removeSuffix("/")
        val chatEndpoint = "$baseEndpoint/api/chat"

        val payloadMessages = mutableListOf<Map<String, String>>()
        payloadMessages.add(mapOf("role" to "system", "content" to systemInstruction))
        for (m in messages) {
            payloadMessages.add(mapOf("role" to m.role, "content" to m.content))
        }

        val requestPayload = mapOf(
            "model" to model,
            "messages" to payloadMessages,
            "stream" to false
        )

        log.debug("Dispatching prompt to Local Ollama at: {} model: {}", chatEndpoint, model)

        return try {
            val rawResponse = restClient.post()
                .uri(chatEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String::class.java) ?: "{}"

            val root = objectMapper.readTree(rawResponse)
            root.path("message").path("content").asText("").trim()
        } catch (e: Exception) {
            log.error("Local Ollama connection failed: {}", e.message)
            throw AiServiceUnavailableException("Failed to connect to local Ollama at $baseEndpoint. Ensure Ollama is running (`ollama serve`) with model '$model'.", e)
        }
    }
}

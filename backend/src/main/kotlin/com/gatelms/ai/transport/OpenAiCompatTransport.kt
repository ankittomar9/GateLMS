package com.gatelms.ai.transport

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.exception.AiProviderException
import com.gatelms.ai.registry.ProviderKind
import com.gatelms.ai.registry.ProviderSpec
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenAiCompatTransport(
    private val aiProperties: AiProperties,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder
) : ChatTransport {
    private val log = LoggerFactory.getLogger(OpenAiCompatTransport::class.java)

    override val kind: ProviderKind = ProviderKind.OPENAI_COMPATIBLE

    private val requestFactory = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(aiProperties.http.connectTimeoutMs.toInt())
        setReadTimeout(aiProperties.http.chatReadTimeoutMs.toInt())
    }
    private val restClient = restClientBuilder
        .requestFactory(requestFactory)
        .build()

    init {
        log.info("OpenAiCompatTransport initialized with connectTimeout={}ms, readTimeout={}ms", aiProperties.http.connectTimeoutMs, aiProperties.http.chatReadTimeoutMs)
    }

    override fun chat(
        spec: ProviderSpec,
        systemInstruction: String,
        messages: List<ChatMessageDto>,
        model: String,
        apiKey: String?
    ): String {
        var endpoint = spec.endpoint.ifBlank { "https://api.openai.com/v1" }.removeSuffix("/")
        if (!endpoint.endsWith("/chat/completions")) {
            endpoint = "$endpoint/chat/completions"
        }

        val payloadMessages = mutableListOf<Map<String, String>>()
        payloadMessages.add(mapOf("role" to "system", "content" to systemInstruction))
        for (m in messages) {
            payloadMessages.add(mapOf("role" to m.role, "content" to m.content))
        }

        val requestPayload = mapOf(
            "model" to model,
            "messages" to payloadMessages,
            "temperature" to 0.2
        )

        log.debug("Dispatching prompt to OpenAI-compatible endpoint: {} with model: {}", endpoint, model)

        val requestSpec = restClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
            .body(requestPayload)

        if (!apiKey.isNullOrBlank()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
        }

        val rawResponse = requestSpec
            .retrieve()
            .body(ByteArray::class.java)?.let { String(it, Charsets.UTF_8) } ?: "{}"

        val root = objectMapper.readTree(rawResponse)
        val choices = root.path("choices")
        if (choices.isArray && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText("").trim()
        }
        val errorNode = root.path("error")
        if (!errorNode.isMissingNode) {
            throw AiProviderException("API Provider Error: ${errorNode.path("message").asText()}")
        }
        return "No response generated."
    }
}

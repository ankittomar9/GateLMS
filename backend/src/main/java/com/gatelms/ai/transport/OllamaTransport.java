package com.gatelms.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.dto.AiDtos.ChatMessageDto;
import com.gatelms.ai.exception.AiExceptions.AiServiceUnavailableException;
import com.gatelms.ai.registry.ProviderKind;
import com.gatelms.ai.registry.ProviderSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class OllamaTransport implements ChatTransport {
    private static final Logger log = LoggerFactory.getLogger(OllamaTransport.class);
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OllamaTransport(AiProperties aiProperties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) aiProperties.getHttp().getConnectTimeoutMs());
        factory.setReadTimeout((int) aiProperties.getHttp().getChatReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(factory).build();
        log.info("OllamaTransport initialized with connectTimeout={}ms, readTimeout={}ms",
            aiProperties.getHttp().getConnectTimeoutMs(), aiProperties.getHttp().getChatReadTimeoutMs());
    }

    @Override
    public ProviderKind getKind() {
        return ProviderKind.OLLAMA;
    }

    @Override
    public String chat(ProviderSpec spec, String systemInstruction, List<ChatMessageDto> messages, String model, String apiKey) {
        String baseEndpoint = spec.getEndpoint() != null && !spec.getEndpoint().trim().isEmpty()
            ? spec.getEndpoint().trim()
            : "http://localhost:11434";
        while (baseEndpoint.endsWith("/")) baseEndpoint = baseEndpoint.substring(0, baseEndpoint.length() - 1);
        String chatEndpoint = baseEndpoint + "/api/chat";

        List<Map<String, String>> payloadMessages = new ArrayList<>();
        payloadMessages.add(Map.of("role", "system", "content", systemInstruction));
        for (ChatMessageDto m : messages) {
            payloadMessages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> requestPayload = Map.of(
            "model", model,
            "messages", payloadMessages,
            "stream", false
        );

        log.debug("Dispatching prompt to Local Ollama at: {} model: {}", chatEndpoint, model);

        try {
            byte[] responseBytes = restClient.post()
                .uri(chatEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .body(requestPayload)
                .retrieve()
                .body(byte[].class);

            String rawResponse = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "{}";
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.path("message").path("content").asText("").trim();
        } catch (Exception e) {
            log.error("Local Ollama connection failed: {}", e.getMessage());
            throw new AiServiceUnavailableException("Failed to connect to local Ollama at " + baseEndpoint + ". Ensure Ollama is running (`ollama serve`) with model '" + model + "'.", e);
        }
    }
}

package com.gatelms.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.dto.AiDtos.ChatMessageDto;
import com.gatelms.ai.exception.AiExceptions.AiProviderException;
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
public class GeminiTransport implements ChatTransport {
    private static final Logger log = LoggerFactory.getLogger(GeminiTransport::class.java.getName().replace("$", ""));
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiTransport(AiProperties aiProperties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) aiProperties.getHttp().getConnectTimeoutMs());
        factory.setReadTimeout((int) aiProperties.getHttp().getChatReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(factory).build();
        log.info("GeminiTransport initialized with connectTimeout={}ms, readTimeout={}ms",
            aiProperties.getHttp().getConnectTimeoutMs(), aiProperties.getHttp().getChatReadTimeoutMs());
    }

    @Override
    public ProviderKind getKind() {
        return ProviderKind.GEMINI;
    }

    @Override
    public String chat(ProviderSpec spec, String systemInstruction, List<ChatMessageDto> messages, String model, String apiKey) {
        String baseEndpoint = spec.getEndpoint() != null && !spec.getEndpoint().trim().isEmpty()
            ? spec.getEndpoint().trim()
            : "https://generativelanguage.googleapis.com/v1beta/models";
        while (baseEndpoint.endsWith("/")) baseEndpoint = baseEndpoint.substring(0, baseEndpoint.length() - 1);

        String fullEndpoint = baseEndpoint + "/" + model + ":generateContent?key=" + (apiKey != null ? apiKey : "");

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessageDto m : messages) {
            String role = "user".equalsIgnoreCase(m.role()) ? "user" : "model";
            contents.add(Map.of(
                "role", role,
                "parts", List.of(Map.of("text", m.content()))
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
            payload.put("system_instruction", Map.of(
                "parts", List.of(Map.of("text", systemInstruction))
            ));
        }
        payload.put("contents", contents);
        payload.put("generationConfig", Map.of("temperature", 0.2));

        log.debug("Dispatching prompt to Google Gemini model: {}", model);

        try {
            byte[] responseBytes = restClient.post()
                .uri(fullEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .body(payload)
                .retrieve()
                .body(byte[].class);

            String rawResponse = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "{}";
            JsonNode root = objectMapper.readTree(rawResponse);

            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText("").trim();
                }
            }

            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                throw new AiProviderException("Google Gemini API Error: " + errorNode.path("message").asText());
            }
            return "No response generated.";
        } catch (AiProviderException ape) {
            throw ape;
        } catch (Exception e) {
            log.error("Google Gemini request failed: {}", e.getMessage());
            throw new AiProviderException("Google Gemini request failed: " + e.getMessage(), e);
        }
    }
}

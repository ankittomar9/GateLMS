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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class OpenAiCompatTransport implements ChatTransport {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatTransport.class);
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatTransport(AiProperties aiProperties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) aiProperties.getHttp().getConnectTimeoutMs());
        factory.setReadTimeout((int) aiProperties.getHttp().getChatReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(factory).build();
        log.info("OpenAiCompatTransport initialized with connectTimeout={}ms, readTimeout={}ms",
            aiProperties.getHttp().getConnectTimeoutMs(), aiProperties.getHttp().getChatReadTimeoutMs());
    }

    @Override
    public ProviderKind getKind() {
        return ProviderKind.OPENAI_COMPATIBLE;
    }

    @Override
    public String chat(ProviderSpec spec, String systemInstruction, List<ChatMessageDto> messages, String model, String apiKey) {
        String endpoint = spec.getEndpoint() != null && !spec.getEndpoint().trim().isEmpty()
            ? spec.getEndpoint().trim()
            : "https://api.openai.com/v1";
        while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        if (!endpoint.endsWith("/chat/completions")) {
            endpoint = endpoint + "/chat/completions";
        }

        List<Map<String, String>> payloadMessages = new ArrayList<>();
        payloadMessages.add(Map.of("role", "system", "content", systemInstruction));
        for (ChatMessageDto m : messages) {
            payloadMessages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> requestPayload = Map.of(
            "model", model,
            "messages", payloadMessages,
            "temperature", 0.2
        );

        log.debug("Dispatching prompt to OpenAI-compatible endpoint: {} with model: {}", endpoint, model);

        var requestSpec = restClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
            .body(requestPayload);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
        }

        byte[] responseBytes = requestSpec.retrieve().body(byte[].class);
        String rawResponse = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "{}";

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText("").trim();
            }
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode()) {
                throw new AiProviderException("API Provider Error: " + errorNode.path("message").asText());
            }
            return "No response generated.";
        } catch (AiProviderException ape) {
            throw ape;
        } catch (Exception e) {
            throw new AiProviderException("Failed to parse API provider response: " + e.getMessage(), e);
        }
    }
}

package com.gatelms.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.dto.AiDtos.TranscriptionResponse;
import com.gatelms.ai.exception.AiExceptions.*;
import com.gatelms.ai.registry.ProviderRegistry;
import com.gatelms.ai.registry.ProviderSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class WhisperTranscriptionService {
    private static final Logger log = LoggerFactory.getLogger(WhisperTranscriptionService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final double SILENCE_RMS_FLOOR = 0.003;
    private static final int PCM_16KHZ_1SEC_BYTES = 32000;

    private final AiProperties aiProperties;
    private final ProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public WhisperTranscriptionService(
        AiProperties aiProperties,
        ProviderRegistry providerRegistry,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.aiProperties = aiProperties;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) aiProperties.getHttp().getConnectTimeoutMs());
        factory.setReadTimeout((int) aiProperties.getHttp().getSttReadTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(factory).build();
    }

    public TranscriptionResponse transcribe(MultipartFile file, String requestedProvider, String customApiKey, String promptContext) {
        if (file.isEmpty()) {
            throw new AiValidationException("Audio file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AiPayloadTooLargeException("Audio snippet exceeds 10MB maximum limit.");
        }

        byte[] audioBytes;
        try {
            audioBytes = file.getBytes();
        } catch (IOException e) {
            throw new AiValidationException("Failed to read audio stream.");
        }

        if (isSilentAudio(audioBytes)) {
            log.info("Audio snippet dropped: below silence RMS energy threshold (empty noise).");
            return new TranscriptionResponse("", "EMPTY_SILENCE", "GATE", null, 0L, "No voice detected.");
        }

        String provider = (requestedProvider != null ? requestedProvider : aiProperties.getWhisper().getSttProvider()).toUpperCase();
        long startTime = System.currentTimeMillis();

        if ("LOCAL".equals(provider)) {
            return transcribeLocal(file, audioBytes, startTime);
        } else if ("GROQ".equals(provider)) {
            return transcribeGroq(file, audioBytes, customApiKey, promptContext, startTime);
        } else if ("OPENAI".equals(provider)) {
            return transcribeOpenAi(file, audioBytes, customApiKey, promptContext, startTime);
        } else {
            // AUTO: Groq -> Local -> OpenAI
            if (providerRegistry.isConfigured("groq") || (customApiKey != null && !customApiKey.trim().isEmpty())) {
                try {
                    return transcribeGroq(file, audioBytes, customApiKey, promptContext, startTime);
                } catch (Exception e) {
                    log.warn("Groq Whisper transcription failed in AUTO mode: {}. Falling back to Local.", e.getMessage());
                }
            }
            try {
                return transcribeLocal(file, audioBytes, startTime);
            } catch (Exception e) {
                log.warn("Local Whisper sidecar failed in AUTO mode: {}. Falling back to OpenAI.", e.getMessage());
            }
            if (providerRegistry.isConfigured("openai")) {
                return transcribeOpenAi(file, audioBytes, customApiKey, promptContext, startTime);
            }
            throw new AiServiceUnavailableException("No Whisper transcription providers are currently available or configured.");
        }
    }

    private TranscriptionResponse transcribeLocal(MultipartFile file, byte[] audioBytes, long startTime) {
        String endpoint = aiProperties.getWhisper().getLocalEndpoint();
        log.info("Transcribing audio via local Whisper sidecar at: {}", endpoint);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(audioBytes) {
            @Override public String getFilename() { return file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.wav"; }
        };
        body.add("file", resource);
        body.add("temperature", "0.0");
        body.add("response_format", "json");

        try {
            byte[] responseBytes = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .body(body)
                .retrieve()
                .body(byte[].class);

            String raw = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "{}";
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("text").asText("").trim();
            long latency = System.currentTimeMillis() - startTime;
            return new TranscriptionResponse(text, "SUCCESS", "LOCAL", "whisper.cpp", latency, null);
        } catch (Exception e) {
            log.error("Local whisper sidecar error: {}", e.getMessage());
            throw new AiServiceUnavailableException("Local Whisper sidecar (:8178) unavailable. Ensure container is healthy.", e);
        }
    }

    private TranscriptionResponse transcribeGroq(MultipartFile file, byte[] audioBytes, String customApiKey, String promptContext, long startTime) {
        ProviderSpec spec = providerRegistry.get("groq").orElseThrow(() -> new AiValidationException("Groq provider not registered."));
        String apiKey = providerRegistry.resolveApiKey("groq", customApiKey);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiValidationException("Missing Groq API Key for Whisper transcription.");
        }

        String model = spec.getSttModel() != null && !spec.getSttModel().isEmpty() ? spec.getSttModel() : "whisper-large-v3-turbo";
        String endpoint = "https://api.groq.com/openai/v1/audio/transcriptions";

        return dispatchCloudWhisper(file, audioBytes, endpoint, apiKey, model, "GROQ", promptContext, startTime);
    }

    private TranscriptionResponse transcribeOpenAi(MultipartFile file, byte[] audioBytes, String customApiKey, String promptContext, long startTime) {
        ProviderSpec spec = providerRegistry.get("openai").orElseThrow(() -> new AiValidationException("OpenAI provider not registered."));
        String apiKey = providerRegistry.resolveApiKey("openai", customApiKey);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AiValidationException("Missing OpenAI API Key for Whisper transcription.");
        }

        String model = spec.getSttModel() != null && !spec.getSttModel().isEmpty() ? spec.getSttModel() : "whisper-1";
        String endpoint = "https://api.openai.com/v1/audio/transcriptions";

        return dispatchCloudWhisper(file, audioBytes, endpoint, apiKey, model, "OPENAI", promptContext, startTime);
    }

    private TranscriptionResponse dispatchCloudWhisper(
        MultipartFile file,
        byte[] audioBytes,
        String endpoint,
        String apiKey,
        String model,
        String providerName,
        String promptContext,
        long startTime
    ) {
        log.info("Transcribing audio via {} Whisper ({})", providerName, model);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(audioBytes) {
            @Override public String getFilename() { return file.getOriginalFilename() != null ? file.getOriginalFilename() : "audio.wav"; }
        };
        body.add("file", resource);
        body.add("model", model);
        body.add("response_format", "json");
        if (promptContext != null && !promptContext.trim().isEmpty()) {
            body.add("prompt", promptContext.trim());
        }

        try {
            byte[] responseBytes = restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .body(body)
                .retrieve()
                .body(byte[].class);

            String raw = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "{}";
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("text").asText("").trim();
            long latency = System.currentTimeMillis() - startTime;
            return new TranscriptionResponse(text, "SUCCESS", providerName, model, latency, null);
        } catch (Exception e) {
            log.error("{} Whisper transcription error: {}", providerName, e.getMessage());
            throw new AiProviderException(providerName + " Whisper error: " + e.getMessage(), e);
        }
    }

    private boolean isSilentAudio(byte[] wavBytes) {
        if (wavBytes.length < 44 + PCM_16KHZ_1SEC_BYTES) return false;
        long sumSquares = 0L;
        int sampleCount = 0;
        int offset = 44;
        while (offset + 1 < wavBytes.length) {
            int low = wavBytes[offset] & 0xFF;
            int high = wavBytes[offset + 1];
            short sample = (short) ((high << 8) | low);
            sumSquares += (long) sample * sample;
            sampleCount++;
            offset += 2;
        }
        if (sampleCount == 0) return true;
        double rms = Math.sqrt((double) sumSquares / sampleCount) / 32768.0;
        return rms < SILENCE_RMS_FLOOR;
    }
}

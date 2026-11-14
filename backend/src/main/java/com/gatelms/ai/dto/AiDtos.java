package com.gatelms.ai.dto;

import java.util.Collections;
import java.util.List;

public class AiDtos {
    public record TranscriptionResponse(
        String text,
        String status,
        String provider,
        String model,
        Long latencyMs,
        String message
    ) {
        public TranscriptionResponse(String text, String status, String provider) {
            this(text, status, provider, null, null, null);
        }
    }

    public record ChatMessageDto(
        String role,
        String content
    ) {}

    public record QuestionContextDto(
        String id,
        String externalId,
        String statement,
        String type,
        Integer year,
        Integer marks,
        String subjectName,
        String topicName,
        String subjectId,
        String topicId,
        String optionsJson,
        String selectedAnswer,
        Boolean isLiveAttempt
    ) {}

    public record AiChatRequest(
        List<ChatMessageDto> messages,
        String provider,
        String model,
        String mode,
        QuestionContextDto questionContext
    ) {
        public AiChatRequest {
            if (messages == null) messages = Collections.emptyList();
            if (provider == null) provider = "GROQ";
            if (mode == null) mode = "GENERAL";
        }
    }

    public record AiChatResponse(
        String reply,
        String provider,
        String model,
        long latencyMs
    ) {}

    public record ProviderStatusDto(
        String provider,
        boolean configured,
        String state,
        String model,
        String reason,
        String label
    ) {}
}

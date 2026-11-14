package com.gatelms.ai.dto;

import java.util.List;

public record AiChatRequest(
    List<ChatMessageDto> messages,
    String provider,
    String model,
    String mode,
    QuestionContextDto questionContext
) {
    public AiChatRequest(List<ChatMessageDto> messages, String provider, String model, String mode) {
        this(messages, provider, model, mode, null);
    }
    public AiChatRequest(List<ChatMessageDto> messages) {
        this(messages, "GROQ", null, "GENERAL", null);
    }
    public AiChatRequest() {
        this(List.of(), "GROQ", null, "GENERAL", null);
    }
}

package com.gatelms.ai.dto;

public record AiChatResponse(
    String reply,
    String provider,
    String model,
    Long latencyMs
) {}

package com.gatelms.ai.dto;

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

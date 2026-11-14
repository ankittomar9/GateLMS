package com.gatelms.ai.dto;

public record ProviderStatusDto(
    String provider,
    boolean configured,
    String state,
    String model,
    String reason,
    String label
) {}

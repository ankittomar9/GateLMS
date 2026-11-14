package com.gatelms.ai.dto;

import java.util.List;
import java.util.UUID;

public record ChatMessageDto(
    String role,
    String content
) {}

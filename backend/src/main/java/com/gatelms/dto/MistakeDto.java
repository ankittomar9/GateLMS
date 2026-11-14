package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MistakeDto(
    UUID id,
    UUID questionId,
    String questionExternalId,
    String questionStatement,
    UUID attemptAnswerId,
    MistakeErrorCategory errorCategory,
    String description,
    String correctiveTakeaway,
    Instant loggedAt,
    int reviewCount,
    Instant nextReviewAt,
    boolean resolved,
    List<String> conceptTags,
    String topicId
) {}

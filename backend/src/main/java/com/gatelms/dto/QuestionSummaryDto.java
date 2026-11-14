package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionSummaryDto(
    UUID id,
    String externalId,
    String subjectId,
    String subjectName,
    String topicId,
    String topicName,
    Integer year,
    QuestionType type,
    int marks,
    String statement,
    Pass1Status pass1Status,
    Pass2Status pass2Status,
    boolean isBookmarked,
    Instant bookmarkedAt,
    Instant lastAttemptedAt,
    String source,
    String curationStatus,
    String curationNote,
    String difficulty,
    Instant createdAt
) {}

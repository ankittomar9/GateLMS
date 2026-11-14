package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockTemplateSummaryDto(
    UUID id,
    String title,
    int durationMinutes,
    double totalMarks,
    int totalQuestions,
    String mode,
    Instant createdAt,
    List<BlueprintSection> sections,
    Double bestScore,
    int attemptCount,
    List<String> shortfallWarnings
) {}

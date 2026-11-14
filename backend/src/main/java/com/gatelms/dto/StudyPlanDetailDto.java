package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudyPlanDetailDto(
    UUID id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    int weeks,
    int weeklyTargetQuestions,
    int weeklyTargetMinutes,
    PlanStrategy strategy,
    int totalQuestionsTarget,
    int totalQuestionsDone,
    int totalMinutesTarget,
    int totalMinutesDone,
    double progressPercentage,
    Map<String, Object> generatedReasoning,
    Instant createdAt,
    List<PlanItemDto> items,
    Map<LocalDate, List<PlanItemDto>> itemsByDate
) {}

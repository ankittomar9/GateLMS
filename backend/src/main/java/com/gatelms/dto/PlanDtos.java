package com.gatelms.dto;

import com.gatelms.entity.PlanItemStatus;
import com.gatelms.entity.PlanStrategy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PlanDtos {
    public record CreatePlanRequest(
        String name,
        LocalDate targetExamDate,
        Double dailyHours,
        PlanStrategy strategy
    ) {}

    public record StudyPlanSummaryDto(
        UUID id,
        String name,
        LocalDate targetExamDate,
        double dailyHours,
        PlanStrategy strategy,
        boolean isActive,
        int totalItems,
        int completedItems,
        Instant createdAt
    ) {}

    public record StudyPlanDetailDto(
        UUID id,
        String name,
        LocalDate targetExamDate,
        double dailyHours,
        PlanStrategy strategy,
        boolean isActive,
        Instant createdAt,
        List<PlanItemDto> items
    ) {}

    public record PlanItemDto(
        UUID id,
        String topicId,
        String topicName,
        String subjectId,
        String subjectName,
        LocalDate targetDate,
        int targetQuestions,
        PlanItemStatus status,
        int completedQuestions
    ) {}

    public record UpdatePlanItemRequest(
        PlanItemStatus status,
        Integer completedQuestions
    ) {}
}

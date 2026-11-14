package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanItemDto(
    UUID id,
    UUID planId,
    LocalDate planDate,
    String subjectId,
    String subjectName,
    String subjectCode,
    String topicId,
    String topicName,
    int targetQuestions,
    int targetMinutes,
    int doneQuestions,
    int doneMinutes,
    PlanItemStatus status
) {}

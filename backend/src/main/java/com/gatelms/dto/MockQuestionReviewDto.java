package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockQuestionReviewDto(
    UUID questionId,
    String externalId,
    String type,
    int marks,
    String statement,
    String optionsJson,
    String subjectId,
    String subjectName,
    String topicId,
    String yourAnswer,
    Double numericAnswer,
    String correctAnswer,
    Double numericMin,
    Double numericMax,
    Boolean isCorrect,
    double marksAwarded,
    String state,
    int timeSpentSeconds,
    String explanation,
    List<String> conceptTags
) {}

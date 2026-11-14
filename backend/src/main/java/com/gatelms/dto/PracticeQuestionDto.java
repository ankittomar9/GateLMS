package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PracticeQuestionDto(
    UUID id,
    String externalId,
    QuestionType type,
    int marks,
    String statement,
    String optionsJson,
    String topicId,
    String subjectId,
    Integer year,
    List<String> conceptTags
) {}

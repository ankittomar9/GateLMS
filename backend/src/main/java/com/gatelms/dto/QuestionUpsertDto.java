package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionUpsertDto(
    String externalId,
    @NotBlank(message = "subjectId is required") String subjectId,
    @NotBlank(message = "topicId is required") String topicId,
    Integer year,
    @NotNull(message = "type is required") QuestionType type,
    int marks,
    @NotBlank(message = "statement is required") String statement,
    String optionsJson,
    String correctOptions,
    Double numericMin,
    Double numericMax,
    String explanation,
    String gateOverflowUrl,
    String source,
    String curationStatus,
    String curationNote,
    String difficulty,
    Integer version
) {}

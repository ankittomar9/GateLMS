package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record QuestionAnatomyDto(
    UUID id,
    UUID questionId,
    String testedConcept,
    String invariantRule,
    List<TrapAnalysisItem> trapAnalysis,
    List<PrerequisiteItem> prerequisites,
    String similarPatternHint,
    AnatomyStatus status,
    String parseError,
    String generatorModel,
    Instant generatedAt,
    Instant updatedAt
) {}

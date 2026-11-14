package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TopicPrimerDto(
    UUID id,
    String topicId,
    String motivation,
    String coreIdea,
    String invariant,
    String workedExample,
    String traps,
    String status,
    String parseError,
    String generatorModel,
    Instant generatedAt
) {}

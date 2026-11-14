package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record ConceptMappedQuestionItemDto(
    UUID id,
    String externalId,
    String statement,
    String type,
    String difficulty,
    Integer year
) {}

package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record ConceptUnitDto(
    UUID id,
    String conceptTagSlug,
    String title,
    String motivation,
    String coreIdea,
    String invariant,
    String workedExample,
    String workedExampleExternalId,
    String traps,
    String deepLinkRef,
    ConceptUnitStatus status,
    String parseError,
    String generatorModel,
    Instant generatedAt,
    List<RetrievalCardDto> cards
) {}

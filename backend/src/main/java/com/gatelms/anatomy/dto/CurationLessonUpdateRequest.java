package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record CurationLessonUpdateRequest(
    ConceptUnitStatus status,
    String title,
    String motivation,
    String coreIdea,
    String invariant,
    String workedExample,
    String workedExampleExternalId,
    String traps
) {}

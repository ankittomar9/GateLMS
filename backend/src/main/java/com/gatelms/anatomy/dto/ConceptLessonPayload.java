package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record ConceptLessonPayload(
    @JsonProperty("title") String title,
    @JsonProperty("motivation") String motivation,
    @JsonProperty("core_idea") String coreIdea,
    @JsonProperty("invariant") String invariant,
    @JsonProperty("worked_example") String workedExample,
    @JsonProperty("worked_example_external_id") String workedExampleExternalId,
    @JsonProperty("traps") String traps
) {
    public ConceptLessonPayload() { this("", "", "", null, "", "", null); }
}

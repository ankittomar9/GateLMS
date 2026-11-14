package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record AnatomyJsonPayload(
    @JsonProperty("tested_concept") String testedConcept,
    @JsonProperty("invariant_rule") String invariantRule,
    @JsonProperty("trap_analysis") List<TrapAnalysisItem> trapAnalysis,
    @JsonProperty("prerequisites") List<PrerequisiteItem> prerequisites,
    @JsonProperty("concept_tags") List<String> conceptTags,
    @JsonProperty("similar_pattern_hint") String similarPatternHint
) {
    public AnatomyJsonPayload() { this("", null, List.of(), List.of(), List.of(), null); }
}

package com.gatelms.anatomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TopicPrimerPayload(
    @JsonProperty("motivation") String motivation,
    @JsonProperty("core_idea") String coreIdea,
    @JsonProperty("invariant") String invariant,
    @JsonProperty("worked_example") String workedExample,
    @JsonProperty("traps") String traps
) {
    public TopicPrimerPayload() {
        this("", "", null, "", null);
    }
}

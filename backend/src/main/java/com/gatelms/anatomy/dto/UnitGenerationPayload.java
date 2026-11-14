package com.gatelms.anatomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UnitGenerationPayload(
    @JsonProperty("title") String title,
    @JsonProperty("motivation") String motivation,
    @JsonProperty("core_idea") String coreIdea,
    @JsonProperty("invariant") String invariant,
    @JsonProperty("worked_example") String workedExample,
    @JsonProperty("traps") String traps,
    @JsonProperty("deep_link_ref") String deepLinkRef,
    @JsonProperty("cards") List<UnitGenerationCard> cards
) {
    public UnitGenerationPayload() { this("", "", "", null, "", null, null, List.of()); }
}

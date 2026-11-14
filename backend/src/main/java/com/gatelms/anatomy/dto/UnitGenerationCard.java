package com.gatelms.anatomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UnitGenerationCard(
    @JsonProperty("prompt") String prompt,
    @JsonProperty("answer") String answer
) {
    public UnitGenerationCard() { this("", ""); }
}

package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record TrapAnalysisItem(
    @JsonProperty("option") String option,
    @JsonProperty("verdict") String verdict,
    @JsonProperty("why_plausible") String whyPlausible,
    @JsonProperty("misconception") String misconception
) {
    public TrapAnalysisItem() { this("", "INCORRECT", "", ""); }
}

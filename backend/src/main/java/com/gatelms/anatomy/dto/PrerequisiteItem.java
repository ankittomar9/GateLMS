package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record PrerequisiteItem(
    @JsonProperty("concept_slug") String conceptSlug,
    @JsonProperty("concept_label") String conceptLabel,
    @JsonProperty("topic_slug") String topicSlug,
    @JsonProperty("must_read") boolean mustRead
) {
    public PrerequisiteItem() { this("", "", null, true); }
}

package com.gatelms.dto;

import com.gatelms.anatomy.entity.TopicPrimerStatus;

public record CurationPrimerUpdateRequest(
    TopicPrimerStatus status,
    String motivation,
    String coreIdea,
    String invariant,
    String workedExample,
    String traps
) {
    public CurationPrimerUpdateRequest() {
        this(null, null, null, null, null, null);
    }
}

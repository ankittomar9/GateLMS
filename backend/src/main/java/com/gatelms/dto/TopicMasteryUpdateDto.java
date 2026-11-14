package com.gatelms.dto;

import java.time.Instant;

public record TopicMasteryUpdateDto(
    Integer masteryLevel,
    Instant lastRevisedAt,
    Instant nextRevisionDue,
    int version
) {
    public TopicMasteryUpdateDto(Integer masteryLevel) {
        this(masteryLevel, null, null, 0);
    }
}

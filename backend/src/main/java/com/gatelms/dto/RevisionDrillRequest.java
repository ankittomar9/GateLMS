package com.gatelms.dto;

public record RevisionDrillRequest(
    RevisionDrillSource source,
    String subjectId,
    String topicId,
    String conceptSlug,
    int count
) {
    public RevisionDrillRequest(RevisionDrillSource source) {
        this(source, null, null, null, 15);
    }
}

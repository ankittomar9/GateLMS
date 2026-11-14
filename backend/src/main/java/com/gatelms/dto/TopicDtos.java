package com.gatelms.dto;

import java.time.Instant;

public class TopicDtos {
    public record TopicDto(
        String id,
        String subjectId,
        String name,
        String slug,
        String bookChapterRef,
        int masteryLevel,
        Instant lastRevisedAt,
        Instant nextRevisionDue,
        int version
    ) {}

    public record TopicMasteryUpdateDto(
        Integer masteryLevel,
        Instant lastRevisedAt,
        Instant nextRevisionDue,
        int version
    ) {}
}

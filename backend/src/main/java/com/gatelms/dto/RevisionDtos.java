package com.gatelms.dto;

import java.time.Instant;

public class RevisionDtos {
    public record RevisionOverviewDto(
        long dueMistakes,
        long dueTopics,
        long pass2Pending,
        long dueCards,
        long bookmarked,
        long drillsCompleted,
        Instant generatedAt
    ) {}

    public enum RevisionDrillSource {
        DUE_MISTAKES,
        PASS2,
        BOOKMARKED,
        TOPIC
    }

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
}

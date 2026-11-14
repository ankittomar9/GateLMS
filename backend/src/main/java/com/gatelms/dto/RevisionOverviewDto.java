package com.gatelms.dto;

import java.time.Instant;

public record RevisionOverviewDto(
    long dueMistakes,
    long dueTopics,
    long pass2Pending,
    long dueCards,
    long bookmarked,
    long drillsCompleted,
    Instant generatedAt
) {}

package com.gatelms.dto;

import java.util.List;
import java.util.Map;

public class SeedInventoryDto {
    public record SeedInventoryResponse(
        long totalQuestions,
        long totalVisibleQuestions,
        List<SubjectInventoryDto> subjects
    ) {}

    public record SubjectInventoryDto(
        String subjectId,
        String subjectName,
        long visibleCount,
        Map<String, Long> curationBreakdown,
        List<TopicInventoryDto> topics
    ) {}

    public record TopicInventoryDto(
        String topicId,
        String topicName,
        long visibleCount,
        long totalCount
    ) {}
}

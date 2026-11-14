package com.gatelms.dto;

import java.util.List;

public class SubjectDtos {
    public record SubjectSummaryDto(
        String id,
        String name,
        String slug,
        String cluster,
        double avgWeightage,
        int priorityTier,
        String standardBookRef,
        int topicCount
    ) {}

    public record SubjectDetailDto(
        String id,
        String name,
        String slug,
        String cluster,
        double avgWeightage,
        int priorityTier,
        String standardBookRef,
        int topicCount,
        List<TopicDtos.TopicDto> topics
    ) {}
}

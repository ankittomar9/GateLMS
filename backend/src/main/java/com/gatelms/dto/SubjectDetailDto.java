package com.gatelms.dto;

import java.util.List;

public record SubjectDetailDto(
    String id,
    String name,
    String slug,
    String cluster,
    Double avgWeightage,
    Integer priorityTier,
    String standardBookRef,
    int topicCount,
    List<TopicDto> topics
) {}

package com.gatelms.dto;

public record TopicInventoryDto(
    String topicId,
    String topicName,
    long visibleCount,
    long totalCount
) {}

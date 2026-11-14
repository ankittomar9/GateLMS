package com.gatelms.dto;

public record SubjectSummaryDto(
    String id,
    String name,
    String slug,
    String cluster,
    Double avgWeightage,
    Integer priorityTier,
    String standardBookRef,
    int topicCount
) {}

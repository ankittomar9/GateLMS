package com.gatelms.dto;

import java.util.List;
import java.util.Map;

public record SubjectInventoryDto(
    String subjectId,
    String subjectName,
    long visibleCount,
    Map<String, Long> curationBreakdown,
    List<TopicInventoryDto> topics
) {}

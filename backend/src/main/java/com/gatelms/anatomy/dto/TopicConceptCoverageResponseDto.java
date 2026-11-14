package com.gatelms.anatomy.dto;

import java.util.List;

public record TopicConceptCoverageResponseDto(
    String topicId,
    int conceptsTotal,
    int conceptsWithMaterial,
    int conceptsExplained,
    List<ConceptCoverageDto> concepts
) {}

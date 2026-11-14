package com.gatelms.anatomy.dto;

import com.gatelms.entity.QuestionType;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ConceptCoverageDtos {
    public record ConceptCoverageDto(
        String slug,
        String label,
        long mappedQuestionCount,
        long uniqueStatementCount,
        String lessonStatus,
        String coverage
    ) {}

    public record TopicConceptCoverageResponseDto(
        String topicId,
        int conceptsTotal,
        int conceptsWithMaterial,
        int conceptsExplained,
        List<ConceptCoverageDto> concepts
    ) {}

    public record UnmappedMappingQuestionDto(
        UUID id,
        String externalId,
        String statement,
        QuestionType type,
        String topicId,
        String topicName,
        List<String> conceptTags,
        Float lowestConfidence
    ) {}

    public record CurationQuestionConceptsPatchRequest(
        List<String> tags,
        List<String> addTags,
        List<String> removeTags
    ) {
        public CurationQuestionConceptsPatchRequest {
            if (addTags == null) addTags = Collections.emptyList();
            if (removeTags == null) removeTags = Collections.emptyList();
        }
    }
}

package com.gatelms.anatomy.dto;

import com.gatelms.entity.QuestionType;
import java.util.List;
import java.util.UUID;

public record UnmappedMappingQuestionDto(
    UUID id,
    String externalId,
    String statement,
    QuestionType type,
    String topicId,
    String topicName,
    List<String> conceptTags,
    Float lowestConfidence
) {
    public UnmappedMappingQuestionDto(
        UUID id,
        String externalId,
        String statement,
        QuestionType type,
        String topicId,
        String topicName
    ) {
        this(id, externalId, statement, type, topicId, topicName, List.of(), null);
    }
}

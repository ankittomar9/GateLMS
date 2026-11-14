package com.gatelms.ai.dto;

public record QuestionContextDto(
    String id,
    String externalId,
    String statement,
    String type,
    Integer year,
    Integer marks,
    String subjectName,
    String topicName,
    String subjectId,
    String topicId,
    String optionsJson,
    String selectedAnswer,
    Boolean isLiveAttempt
) {}

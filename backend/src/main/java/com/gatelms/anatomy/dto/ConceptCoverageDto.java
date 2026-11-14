package com.gatelms.anatomy.dto;

public record ConceptCoverageDto(
    String slug,
    String label,
    long mappedQuestionCount,
    long uniqueStatementCount,
    String lessonStatus,
    String coverage
) {}

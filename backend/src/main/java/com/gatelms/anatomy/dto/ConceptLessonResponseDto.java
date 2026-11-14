package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record ConceptLessonResponseDto(
    ConceptLessonDto lesson,
    String conceptTagSlug,
    String conceptTagLabel,
    String topicId,
    String topicName,
    int mappedQuestionCount,
    List<ConceptMappedQuestionItemDto> mappedQuestions
) {}

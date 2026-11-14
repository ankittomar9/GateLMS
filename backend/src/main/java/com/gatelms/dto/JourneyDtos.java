package com.gatelms.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JourneyDtos {
    public record JourneyOverviewSubjectDto(
        String subjectId,
        String name,
        String cluster,
        int priorityTier,
        double marksWeight,
        String standardBookRef,
        long questionsVisible,
        long questionsSolved,
        int topicsTotal,
        int topicsTouched,
        int topicsCompleted,
        double readinessPct
    ) {}

    public record JourneyTopicSummaryDto(
        String topicId,
        String name,
        int sequence,
        String primerStatus,
        long questionsVisible,
        long questionsSolved,
        int masteryLevel,
        boolean completed
    ) {}

    public record TopicPrimerDto(
        UUID id,
        String topicId,
        String motivation,
        String coreIdea,
        String invariant,
        String workedExample,
        String traps,
        String status,
        String parseError,
        String generatorModel,
        Instant generatedAt
    ) {}

    public record TopicPrimerResponseDto(
        TopicPrimerDto primer,
        List<String> conceptSlugs,
        String bookChapterRef,
        String subjectStandardBookRef
    ) {}

    public record JourneyContinueDto(
        String topicId,
        String subjectId,
        UUID questionId
    ) {}

    public record CurationPrimerUpdateRequest(
        String status,
        String motivation,
        String coreIdea,
        String invariant,
        String workedExample,
        String traps
    ) {}

    public record PrimerGenerationRequest(
        String model,
        Boolean force
    ) {}
}

package com.gatelms.anatomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.AnatomyStatus;
import com.gatelms.anatomy.entity.ConceptUnitStatus;
import com.gatelms.anatomy.entity.RetrievalCardStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AnatomyDtos {
    public record TrapAnalysisItem(
        @JsonProperty("option") String option,
        @JsonProperty("verdict") String verdict,
        @JsonProperty("why_plausible") String whyPlausible,
        @JsonProperty("misconception") String misconception
    ) {
        public TrapAnalysisItem() {
            this("", "INCORRECT", "", "");
        }
    }

    public record PrerequisiteItem(
        @JsonProperty("concept_slug") String conceptSlug,
        @JsonProperty("concept_label") String conceptLabel,
        @JsonProperty("topic_slug") String topicSlug,
        @JsonProperty("must_read") boolean mustRead
    ) {
        public PrerequisiteItem() {
            this("", "", null, true);
        }
    }

    public record AnatomyJsonPayload(
        @JsonProperty("tested_concept") String testedConcept,
        @JsonProperty("invariant_rule") String invariantRule,
        @JsonProperty("trap_analysis") List<TrapAnalysisItem> trapAnalysis,
        @JsonProperty("prerequisites") List<PrerequisiteItem> prerequisites,
        @JsonProperty("concept_tags") List<String> conceptTags,
        @JsonProperty("similar_pattern_hint") String similarPatternHint
    ) {
        public AnatomyJsonPayload() {
            this("", null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null);
        }
    }

    public record QuestionAnatomyDto(
        UUID id,
        UUID questionId,
        String testedConcept,
        String invariantRule,
        List<TrapAnalysisItem> trapAnalysis,
        List<PrerequisiteItem> prerequisites,
        String similarPatternHint,
        AnatomyStatus status,
        String parseError,
        String generatorModel,
        Instant generatedAt,
        Instant updatedAt
    ) {}

    public record ConceptUnitDto(
        UUID id,
        String conceptTagSlug,
        String title,
        String motivation,
        String coreIdea,
        String invariant,
        String workedExample,
        String workedExampleExternalId,
        String traps,
        String deepLinkRef,
        ConceptUnitStatus status,
        String parseError,
        String generatorModel,
        Instant generatedAt,
        List<RetrievalCardDto> cards
    ) {}

    public record RetrievalCardDto(
        UUID id,
        String prompt,
        String answer,
        int cardOrder,
        RetrievalCardStatus status
    ) {}

    public record ConceptSummaryDto(
        String slug,
        String label,
        String topicId,
        long questionCount
    ) {}

    public record CurationAnatomyUpdateRequest(
        AnatomyStatus status,
        String testedConcept,
        String invariantRule,
        String similarPatternHint
    ) {}

    public record DueCardDto(
        UUID cardId,
        String prompt,
        String answer,
        String conceptSlug,
        String conceptLabel,
        String unitTitle,
        RetrievalCardStatus status
    ) {}

    public record DueCardsResponse(
        int dueCount,
        List<DueCardDto> cards
    ) {}

    public record CardReviewRequest(
        int grade
    ) {}

    public record CardReviewResponse(
        Instant nextDueAt,
        float intervalDays,
        float ease
    ) {}

    public record ConceptLessonPayload(
        @JsonProperty("title") String title,
        @JsonProperty("motivation") String motivation,
        @JsonProperty("core_idea") String coreIdea,
        @JsonProperty("invariant") String invariant,
        @JsonProperty("worked_example") String workedExample,
        @JsonProperty("worked_example_external_id") String workedExampleExternalId,
        @JsonProperty("traps") String traps
    ) {
        public ConceptLessonPayload() {
            this("", "", "", null, "", "", null);
        }
    }

    public record ConceptLessonDto(
        UUID id,
        String conceptTagSlug,
        String conceptTagLabel,
        String title,
        String motivation,
        String coreIdea,
        String invariant,
        String workedExample,
        String workedExampleExternalId,
        String traps,
        String status,
        String parseError,
        String generatorModel,
        Instant generatedAt
    ) {}

    public record ConceptLessonResponseDto(
        ConceptLessonDto lesson,
        String conceptTagSlug,
        String conceptTagLabel,
        String topicId,
        String topicName,
        int mappedQuestionCount,
        List<ConceptMappedQuestionItemDto> mappedQuestions
    ) {}

    public record ConceptMappedQuestionItemDto(
        UUID id,
        String externalId,
        String statement,
        String type,
        String difficulty,
        Integer year
    ) {}

    public record CurationLessonUpdateRequest(
        ConceptUnitStatus status,
        String title,
        String motivation,
        String coreIdea,
        String invariant,
        String workedExample,
        String workedExampleExternalId,
        String traps
    ) {}

    public record TopicPrimerPayload(
        @JsonProperty("motivation") String motivation,
        @JsonProperty("core_idea") String coreIdea,
        @JsonProperty("invariant") String invariant,
        @JsonProperty("worked_example") String workedExample,
        @JsonProperty("traps") String traps
    ) {
        public TopicPrimerPayload() {
            this("", "", null, "", null);
        }
    }

    public record UnitGenerationCard(
        @JsonProperty("prompt") String prompt,
        @JsonProperty("answer") String answer
    ) {
        public UnitGenerationCard() {
            this("", "");
        }
    }

    public record UnitGenerationPayload(
        @JsonProperty("title") String title,
        @JsonProperty("motivation") String motivation,
        @JsonProperty("core_idea") String coreIdea,
        @JsonProperty("invariant") String invariant,
        @JsonProperty("worked_example") String workedExample,
        @JsonProperty("traps") String traps,
        @JsonProperty("deep_link_ref") String deepLinkRef,
        @JsonProperty("cards") List<UnitGenerationCard> cards
    ) {
        public UnitGenerationPayload() {
            this("", "", "", null, "", null, null, Collections.emptyList());
        }
    }
}

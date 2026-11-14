package com.gatelms.dto;

import com.gatelms.entity.CurationStatus;
import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.Pass2Status;
import com.gatelms.entity.QuestionDifficulty;
import com.gatelms.entity.QuestionSource;
import com.gatelms.entity.QuestionType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestionDtos {
    public record QuestionSummaryDto(
        UUID id,
        String externalId,
        String subjectId,
        String subjectName,
        String topicId,
        String topicName,
        Integer year,
        QuestionType type,
        int marks,
        String statement,
        String optionsJson,
        QuestionDifficulty difficulty,
        QuestionSource source,
        CurationStatus curationStatus,
        Pass1Status pass1Status,
        Pass2Status pass2Status,
        boolean isBookmarked
    ) {}

    public record QuestionDetailDto(
        UUID id,
        String externalId,
        String subjectId,
        String subjectName,
        String topicId,
        String topicName,
        Integer year,
        QuestionType type,
        int marks,
        String statement,
        String optionsJson,
        String correctOptions,
        Double numericMin,
        Double numericMax,
        String explanation,
        String gateOverflowUrl,
        QuestionSource source,
        CurationStatus curationStatus,
        String curationNote,
        QuestionDifficulty difficulty,
        Pass1Status pass1Status,
        Pass2Status pass2Status,
        boolean isBookmarked,
        Instant createdAt,
        Instant updatedAt,
        int version
    ) {}

    public record CreateQuestionRequest(
        String externalId,
        String subjectId,
        String topicId,
        Integer year,
        QuestionType type,
        int marks,
        String statement,
        String optionsJson,
        String correctOptions,
        Double numericMin,
        Double numericMax,
        String explanation,
        String gateOverflowUrl,
        String source,
        String curationStatus,
        String curationNote,
        String difficulty
    ) {}

    public record UpdateQuestionRequest(
        String externalId,
        String subjectId,
        String topicId,
        Integer year,
        QuestionType type,
        Integer marks,
        String statement,
        String optionsJson,
        String correctOptions,
        Double numericMin,
        Double numericMax,
        String explanation,
        String gateOverflowUrl,
        String source,
        String curationStatus,
        String curationNote,
        String difficulty,
        Integer version
    ) {}

    public record QuestionStatusUpdateDto(
        Pass1Status pass1Status,
        Pass2Status pass2Status,
        Boolean isBookmarked
    ) {}

    public record CurationUpdateRequest(
        String status,
        String note
    ) {}

    public record QuestionValidationResultDto(
        boolean valid,
        Map<String, String> fieldErrors,
        List<String> warnings
    ) {
        public QuestionValidationResultDto(boolean valid) {
            this(valid, Collections.emptyMap(), Collections.emptyList());
        }
    }
}

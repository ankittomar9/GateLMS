package com.gatelms.dto;

import com.gatelms.entity.MistakeErrorCategory;
import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.Pass2Status;
import com.gatelms.entity.QuestionDifficulty;
import com.gatelms.entity.QuestionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PracticeDtos {
    public record StartPracticeSessionRequest(
        String subjectId,
        String topicId,
        QuestionType type,
        QuestionDifficulty difficulty,
        int count
    ) {
        public StartPracticeSessionRequest(String subjectId, String topicId, QuestionType type, QuestionDifficulty difficulty) {
            this(subjectId, topicId, type, difficulty, 10);
        }
    }

    public record SubmitPracticeAnswerRequest(
        String selectedOptions,
        Double numericAnswer,
        int timeSpentSeconds,
        boolean usedHint
    ) {}

    public record PracticeAnswerResultDto(
        boolean isCorrect,
        Double marksAwarded,
        String explanation,
        Pass1Status pass1Status,
        Pass2Status pass2Status
    ) {}

    public record MistakeEntryDto(
        UUID id,
        UUID questionId,
        String externalId,
        String statement,
        QuestionType type,
        String subjectId,
        String subjectName,
        String topicId,
        String topicName,
        MistakeErrorCategory errorCategory,
        String notes,
        int attemptCount,
        Instant firstFailedAt,
        Instant lastFailedAt,
        Instant nextReviewDue
    ) {}

    public record UpdateMistakeRequest(
        MistakeErrorCategory errorCategory,
        String notes
    ) {}

    public record SimilarQuestionItemDto(
        UUID id,
        String externalId,
        String statement,
        QuestionType type,
        Integer year,
        int marks,
        String difficulty,
        String topicId,
        String topicName,
        double score,
        List<String> matchedConcepts
    ) {}

    public record SimilarQuestionsResponse(
        UUID sourceQuestionId,
        List<SimilarQuestionItemDto> similarQuestions
    ) {}
}

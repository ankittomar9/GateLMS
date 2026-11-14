package com.gatelms.dto;

import com.gatelms.entity.AttemptAnswerState;
import com.gatelms.entity.ExamMode;
import com.gatelms.entity.MockAttemptStatus;
import com.gatelms.entity.QuestionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MockDtos {
    public record StartAttemptRequest(
        UUID examId,
        ExamMode mode
    ) {
        public StartAttemptRequest(UUID examId) {
            this(examId, ExamMode.MOCK);
        }
    }

    public record SaveAnswerRequest(
        String selectedOptions,
        Double numericAnswer,
        AttemptAnswerState state,
        int timeSpentSeconds
    ) {}

    public record QuestionSummaryDto(
        UUID id,
        String externalId,
        String subjectId,
        String topicId,
        Integer year,
        QuestionType type,
        int marks,
        String statement,
        String optionsJson,
        String difficulty,
        int sequence
    ) {}

    public record AttemptAnswerSummaryDto(
        UUID questionId,
        String selectedOptions,
        Double numericAnswer,
        AttemptAnswerState state,
        int timeSpentSeconds,
        Double marksAwarded,
        Boolean isCorrect
    ) {}

    public record MockAttemptDto(
        UUID id,
        UUID examId,
        String examTitle,
        ExamMode mode,
        MockAttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        int timeRemainingSeconds,
        Double score,
        Integer totalMarks,
        List<QuestionSummaryDto> questions,
        List<AttemptAnswerSummaryDto> answers
    ) {}

    public record MockExamSummaryDto(
        UUID id,
        String title,
        int durationMinutes,
        int totalMarks,
        int questionCount,
        boolean hasActiveAttempt
    ) {}

    public record MockTrendDto(
        UUID attemptId,
        String examTitle,
        Double score,
        Integer totalMarks,
        Double percentage,
        Instant submittedAt
    ) {}
}

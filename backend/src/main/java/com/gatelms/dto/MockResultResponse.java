package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockResultResponse(
    UUID attemptId,
    UUID mockId,
    String mockTitle,
    Instant startedAt,
    Instant submittedAt,
    int durationSeconds,
    String finishedReason,
    double totalScore,
    double positiveScore,
    double negativeScore,
    double maxMarks,
    int timeSpentSeconds,
    List<MockQuestionReviewDto> questions,
    List<MockSectionScoreDto> sectionScores
) {}

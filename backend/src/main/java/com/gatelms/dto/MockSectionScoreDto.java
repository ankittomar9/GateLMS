package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockSectionScoreDto(
    String subjectId,
    String subjectName,
    int totalQuestions,
    int attemptedCount,
    int correctCount,
    int wrongCount,
    int unattemptedCount,
    double marksAwarded,
    double maxMarks,
    double accuracy,
    int timeSpentSeconds
) {}

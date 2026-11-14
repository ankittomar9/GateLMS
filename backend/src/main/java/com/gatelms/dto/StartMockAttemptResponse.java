package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartMockAttemptResponse(
    UUID attemptId,
    UUID mockId,
    String mockTitle,
    int durationSeconds,
    List<MockAttemptSectionDto> sections,
    List<PracticeQuestionDto> questions,
    Instant startedAt,
    Instant serverTime,
    List<String> shortfallWarnings
) {}

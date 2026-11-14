package com.gatelms.dto;

import java.time.Instant;

public record UserGoalDto(
    int dailyTargetQuestions,
    boolean streakProtection,
    Instant updatedAt
) {}

package com.gatelms.dto;

public record UpdateUserGoalRequest(
    Integer dailyTargetQuestions,
    Boolean streakProtection
) {
    public UpdateUserGoalRequest() {
        this(null, null);
    }
}

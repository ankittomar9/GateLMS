package com.gatelms.dto;

public record DailyGoalProgressDto(
    int target,
    int done,
    boolean met
) {}

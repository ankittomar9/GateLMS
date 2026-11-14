package com.gatelms.dto;

import java.time.LocalDate;

public record EngagementSummaryResponse(
    int streak,
    LocalDate protectedDate,
    boolean streakAtRisk,
    DailyGoalProgressDto goal,
    DueTodayDto dueToday
) {}

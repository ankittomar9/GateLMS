package com.gatelms.dto;

import com.gatelms.entity.NotificationType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EngagementDtos {
    public record DailyGoalProgressDto(
        int target,
        int done,
        boolean met
    ) {}

    public record DueTodayDto(
        long mistakes,
        long topics
    ) {}

    public record EngagementSummaryResponse(
        int streak,
        LocalDate protectedDate,
        boolean streakAtRisk,
        DailyGoalProgressDto goal,
        DueTodayDto dueToday
    ) {}

    public record AchievementDto(
        String code,
        String title,
        String description,
        Instant earnedAt
    ) {}

    public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String body,
        String link,
        Instant readAt,
        Instant createdAt
    ) {}

    public record UnreadCountResponse(
        long count
    ) {}

    public record UserGoalDto(
        int dailyTargetQuestions,
        boolean streakProtection,
        Instant updatedAt
    ) {}

    public record UpdateUserGoalRequest(
        Integer dailyTargetQuestions,
        Boolean streakProtection
    ) {}

    public record MarkAllReadResponse(
        int count
    ) {}
}

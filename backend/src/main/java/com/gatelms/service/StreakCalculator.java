package com.gatelms.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class StreakCalculator {

    public record StreakResult(
        int streak,
        LocalDate protectedDate,
        boolean streakAtRisk
    ) {}

    public static StreakResult computeStreak(
        Map<LocalDate, Integer> activityMap,
        LocalDate today,
        boolean streakProtection
    ) {
        int totalQuestions = activityMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalQuestions <= 0) {
            return new StreakResult(0, null, false);
        }

        int todayCount = activityMap.getOrDefault(today, 0);
        boolean hasTodayActivity = todayCount > 0;

        int streak = 0;
        LocalDate protectedDate = null;
        boolean streakAtRisk;

        LocalDate anchor;
        if (hasTodayActivity) {
            anchor = today;
            streakAtRisk = false;
        } else {
            anchor = today.minusDays(1);
            streakAtRisk = true;
        }

        LocalDate current = anchor;
        while (true) {
            int count = activityMap.getOrDefault(current, 0);
            if (count > 0) {
                streak++;
                current = current.minusDays(1);
            } else {
                if (streakProtection && protectedDate == null && ChronoUnit.DAYS.between(current, today) <= 30) {
                    protectedDate = current;
                    current = current.minusDays(1);
                } else {
                    break;
                }
            }
        }

        if (streak == 0) {
            return new StreakResult(0, null, false);
        }

        return new StreakResult(streak, protectedDate, streakAtRisk);
    }
}

package com.gatelms.service

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakResult(
    val streak: Int,
    val protectedDate: LocalDate? = null,
    val streakAtRisk: Boolean = false
)

object StreakCalculator {

    fun computeStreak(
        activityMap: Map<LocalDate, Int>,
        today: LocalDate = LocalDate.now(),
        streakProtection: Boolean = true
    ): StreakResult {
        val totalQuestions = activityMap.values.sum()
        if (totalQuestions <= 0) {
            return StreakResult(streak = 0, protectedDate = null, streakAtRisk = false)
        }

        val todayCount = activityMap[today] ?: 0
        val hasTodayActivity = todayCount > 0

        var streak = 0
        var protectedDate: LocalDate? = null
        val streakAtRisk: Boolean

        val anchor: LocalDate
        if (hasTodayActivity) {
            anchor = today
            streakAtRisk = false
        } else {
            anchor = today.minusDays(1)
            streakAtRisk = true
        }

        var current = anchor
        while (true) {
            val count = activityMap[current] ?: 0
            if (count > 0) {
                streak++
                current = current.minusDays(1)
            } else {
                if (streakProtection && protectedDate == null && ChronoUnit.DAYS.between(current, today) <= 30) {
                    protectedDate = current
                    current = current.minusDays(1)
                } else {
                    break
                }
            }
        }

        if (streak == 0) {
            return StreakResult(streak = 0, protectedDate = null, streakAtRisk = false)
        }

        return StreakResult(
            streak = streak,
            protectedDate = protectedDate,
            streakAtRisk = streakAtRisk
        )
    }
}

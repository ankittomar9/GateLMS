package com.gatelms.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StreakComputationTest {

    private val today = LocalDate.of(2026, 10, 7)

    @Test
    fun `1 zero activity ever returns streak 0 and not at risk`() {
        val activity = emptyMap<LocalDate, Int>()
        val result = StreakCalculator.computeStreak(
            activityMap = activity,
            today = today,
            streakProtection = true
        )

        assertEquals(0, result.streak)
        assertNull(result.protectedDate)
        assertFalse(result.streakAtRisk)
    }

    @Test
    fun `2 today empty anchors to yesterday with streakAtRisk true`() {
        // Today has 0 questions, yesterday (Oct 6) has 10 questions, day before (Oct 5) has 15 questions
        val activity = mapOf(
            today to 0,
            today.minusDays(1) to 10,
            today.minusDays(2) to 15,
            today.minusDays(3) to 0
        )

        val result = StreakCalculator.computeStreak(
            activityMap = activity,
            today = today,
            streakProtection = false
        )

        assertEquals(2, result.streak, "Streak should count yesterday and day before")
        assertNull(result.protectedDate)
        assertTrue(result.streakAtRisk, "Streak must be at risk when today is empty but yesterday was active")
    }

    @Test
    fun `3 gap day computation with streak protection forgives exactly one miss and logs protectedDate`() {
        // Today (Oct 7) = 5 questions
        // Yesterday (Oct 6) = 0 questions (missed day)
        // Oct 5 = 12 questions
        // Oct 4 = 20 questions
        val activity = mapOf(
            today to 5,
            today.minusDays(1) to 0,
            today.minusDays(2) to 12,
            today.minusDays(3) to 20
        )

        val result = StreakCalculator.computeStreak(
            activityMap = activity,
            today = today,
            streakProtection = true
        )

        assertEquals(3, result.streak, "Streak counts Oct 7, Oct 5, Oct 4 across the forgiven gap")
        assertEquals(today.minusDays(1), result.protectedDate, "Protected date must be the missed day")
        assertFalse(result.streakAtRisk, "Not at risk because activity recorded today")
    }

    @Test
    fun `4 double forgiveness is rejected when two gap days occur`() {
        // Oct 7 = 10
        // Oct 6 = 0 (1st miss -> forgiven)
        // Oct 5 = 10
        // Oct 4 = 0 (2nd miss -> rejected)
        // Oct 3 = 10
        val activity = mapOf(
            today to 10,
            today.minusDays(1) to 0,
            today.minusDays(2) to 10,
            today.minusDays(3) to 0,
            today.minusDays(4) to 10
        )

        val result = StreakCalculator.computeStreak(
            activityMap = activity,
            today = today,
            streakProtection = true
        )

        assertEquals(2, result.streak, "Streak must stop at 2nd miss and reject double forgiveness")
        assertEquals(today.minusDays(1), result.protectedDate)
    }

    @Test
    fun `5 gap day breaks streak immediately when streakProtection is false`() {
        // Oct 7 = 5
        // Oct 6 = 0 (missed day)
        // Oct 5 = 12
        val activity = mapOf(
            today to 5,
            today.minusDays(1) to 0,
            today.minusDays(2) to 12
        )

        val result = StreakCalculator.computeStreak(
            activityMap = activity,
            today = today,
            streakProtection = false
        )

        assertEquals(1, result.streak, "Streak stops immediately on gap when protection is disabled")
        assertNull(result.protectedDate)
    }

    @Test
    fun `6 missed yesterday forgiven allows streak to stay at risk and recover`() {
        // Today (Oct 7) = 0
        // Yesterday (Oct 6) = 0 (missed yesterday)
        // Oct 5 = 15
        // Oct 4 = 10
        val activity = mapOf(
            today to 0,
            today.minusDays(1) to 0,
            today.minusDays(2) to 15,
            today.minusDays(3) to 10
        )

        val result = StreakCalculator.computeStreak(
            activityMap = activity,
            today = today,
            streakProtection = true
        )

        assertEquals(2, result.streak, "Streak recovers Oct 5 and Oct 4 with yesterday forgiven")
        assertEquals(today.minusDays(1), result.protectedDate)
        assertTrue(result.streakAtRisk, "Streak is at risk until user practices today")
    }
}

package com.gatelms.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StreakComputationTest {

    private final LocalDate today = LocalDate.of(2026, 10, 7);

    @Test
    public void testZeroActivityEverReturnsStreak0AndNotAtRisk() {
        Map<LocalDate, Integer> activity = Collections.emptyMap();
        StreakCalculator.StreakResult result = StreakCalculator.computeStreak(
            activity,
            today,
            true
        );

        assertEquals(0, result.streak());
        assertNull(result.protectedDate());
        assertFalse(result.streakAtRisk());
    }

    @Test
    public void testTodayEmptyAnchorsToYesterdayWithStreakAtRiskTrue() {
        Map<LocalDate, Integer> activity = Map.of(
            today, 0,
            today.minusDays(1), 10,
            today.minusDays(2), 15,
            today.minusDays(3), 0
        );

        StreakCalculator.StreakResult result = StreakCalculator.computeStreak(
            activity,
            today,
            false
        );

        assertEquals(2, result.streak(), "Streak should count yesterday and day before");
        assertNull(result.protectedDate());
        assertTrue(result.streakAtRisk(), "Streak must be at risk when today is empty but yesterday was active");
    }

    @Test
    public void testGapDayComputationWithStreakProtectionForgivesExactlyOneMiss() {
        Map<LocalDate, Integer> activity = Map.of(
            today, 5,
            today.minusDays(1), 0,
            today.minusDays(2), 12,
            today.minusDays(3), 20
        );

        StreakCalculator.StreakResult result = StreakCalculator.computeStreak(
            activity,
            today,
            true
        );

        assertEquals(3, result.streak(), "Streak counts Oct 7, Oct 5, Oct 4 across the forgiven gap");
        assertEquals(today.minusDays(1), result.protectedDate(), "Protected date must be the missed day");
        assertFalse(result.streakAtRisk(), "Not at risk because activity recorded today");
    }

    @Test
    public void testDoubleForgivenessIsRejectedWhenTwoGapDaysOccur() {
        Map<LocalDate, Integer> activity = Map.of(
            today, 10,
            today.minusDays(1), 0,
            today.minusDays(2), 10,
            today.minusDays(3), 0,
            today.minusDays(4), 10
        );

        StreakCalculator.StreakResult result = StreakCalculator.computeStreak(
            activity,
            today,
            true
        );

        assertEquals(2, result.streak(), "Streak must stop at 2nd miss and reject double forgiveness");
        assertEquals(today.minusDays(1), result.protectedDate());
    }

    @Test
    public void testGapDayBreaksStreakImmediatelyWhenStreakProtectionIsFalse() {
        Map<LocalDate, Integer> activity = Map.of(
            today, 5,
            today.minusDays(1), 0,
            today.minusDays(2), 12
        );

        StreakCalculator.StreakResult result = StreakCalculator.computeStreak(
            activity,
            today,
            false
        );

        assertEquals(1, result.streak(), "Streak stops immediately on gap when protection is disabled");
        assertNull(result.protectedDate());
    }

    @Test
    public void testMissedYesterdayForgivenAllowsStreakToStayAtRiskAndRecover() {
        Map<LocalDate, Integer> activity = Map.of(
            today, 0,
            today.minusDays(1), 0,
            today.minusDays(2), 15,
            today.minusDays(3), 10
        );

        StreakCalculator.StreakResult result = StreakCalculator.computeStreak(
            activity,
            today,
            true
        );

        assertEquals(2, result.streak(), "Streak recovers Oct 5 and Oct 4 with yesterday forgiven");
        assertEquals(today.minusDays(1), result.protectedDate());
        assertTrue(result.streakAtRisk(), "Streak is at risk until user practices today");
    }
}

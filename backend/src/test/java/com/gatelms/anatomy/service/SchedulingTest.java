package com.gatelms.anatomy.service;

import com.gatelms.anatomy.service.CardSchedulingService.CardScheduleState;
import com.gatelms.anatomy.service.CardSchedulingService.Sm2Lite;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulingTest {

    private final Instant now = Instant.parse("2026-10-08T12:00:00Z");

    private CardScheduleState initialState(
        float intervalDays,
        float ease,
        int consecutiveCorrect,
        Instant lastReviewedAt
    ) {
        return new CardScheduleState(
            now,
            intervalDays,
            ease,
            consecutiveCorrect,
            lastReviewedAt
        );
    }

    private CardScheduleState initialState() {
        return initialState(0f, Sm2Lite.EASE_INITIAL, 0, null);
    }

    private void assertExactDays(long expectedDays, Instant actualDueAt, Instant reference) {
        long actualMs = Duration.between(reference, actualDueAt).toMillis();
        assertEquals(expectedDays * 86_400_000.0, (double) actualMs, 2_000.0);
    }

    private void assertExactDays(long expectedDays, Instant actualDueAt) {
        assertExactDays(expectedDays, actualDueAt, now);
    }

    @Test
    void testFirstReviewSchedulesPlus1DayForAllGrades() {
        for (int grade = 0; grade <= 2; grade++) {
            CardScheduleState next = Sm2Lite.nextReview(initialState(), grade, now);
            assertExactDays(1, next.dueAt());
            assertEquals(1f, next.intervalDays());
            assertEquals(Sm2Lite.EASE_INITIAL, next.ease());
            assertEquals(0, next.consecutiveCorrect());
            assertEquals(now, next.lastReviewedAt());
        }
    }

    @Test
    void testSolidLadderIsExactly1_3_7_14_30Days() {
        CardScheduleState state = initialState();
        long[] expectedLadder = new long[]{1, 3, 7, 14, 30};

        for (long expectedDays : expectedLadder) {
            state = Sm2Lite.nextReview(state, 2, now);
            assertExactDays(expectedDays, state.dueAt());
            assertEquals((float) expectedDays, state.intervalDays());
        }
        assertEquals(4, state.consecutiveCorrect());
    }

    @Test
    void testLadderExitMultipliesIntervalByEase() {
        CardScheduleState state = initialState(30f, 2.5f, 4, now.minusSeconds(60));
        CardScheduleState next = Sm2Lite.nextReview(state, 2, now);
        assertEquals(75f, next.intervalDays(), 0.0001f);
        assertExactDays(75, next.dueAt());
        assertEquals(5, next.consecutiveCorrect());
        assertEquals(2.5f, next.ease());
    }

    @Test
    void testSolidBeyondTheLadderKeepsMultiplyingByEase() {
        CardScheduleState state = initialState(60f, 2.5f, 7, now.minusSeconds(60));
        CardScheduleState next = Sm2Lite.nextReview(state, 2, now);
        assertEquals(150f, next.intervalDays(), 0.0001f);
        assertEquals(8, next.consecutiveCorrect());
    }

    @Test
    void testShakyMultipliesIntervalBy1_2ResetsStreakAndDecaysEase() {
        CardScheduleState state = initialState(10f, 2.5f, 3, now.minusSeconds(60));
        CardScheduleState next = Sm2Lite.nextReview(state, 1, now);
        assertEquals(12f, next.intervalDays(), 0.0001f);
        assertEquals(2.45f, next.ease(), 0.0001f);
        assertEquals(0, next.consecutiveCorrect());
        assertExactDays(12, next.dueAt());
    }

    @Test
    void testShakyFloorsIntervalAt1Day() {
        CardScheduleState state = initialState(0.5f, 2.5f, 1, now.minusSeconds(60));
        CardScheduleState next = Sm2Lite.nextReview(state, 1, now);
        assertEquals(1f, next.intervalDays(), 0.0001f);
        assertExactDays(1, next.dueAt());
    }

    @Test
    void testForgottenResetsIntervalTo1DayStreakTo0AndDecaysEaseBy0_2() {
        CardScheduleState state = initialState(30f, 2.5f, 4, now.minusSeconds(60));
        CardScheduleState next = Sm2Lite.nextReview(state, 0, now);
        assertEquals(1f, next.intervalDays());
        assertExactDays(1, next.dueAt());
        assertEquals(2.3f, next.ease(), 0.0001f);
        assertEquals(0, next.consecutiveCorrect());
    }

    @Test
    void testEaseNeverDropsBelowFloor() {
        CardScheduleState state = initialState(30f, 2.5f, 2, now.minusSeconds(60));
        for (int i = 0; i < 10; i++) {
            state = Sm2Lite.nextReview(state, 0, now);
            assertTrue(state.ease() >= Sm2Lite.EASE_FLOOR, "ease fell below floor: " + state.ease());
        }
        assertEquals(Sm2Lite.EASE_FLOOR, state.ease(), 0.0001f);

        CardScheduleState shakyState = initialState(30f, 1.32f, 0, now.minusSeconds(60));
        shakyState = Sm2Lite.nextReview(shakyState, 1, now);
        assertEquals(Sm2Lite.EASE_FLOOR, shakyState.ease(), 0.0001f);
    }

    @Test
    void testForgottenThenSolidRestartsTheLadderFrom3DayRung() {
        CardScheduleState state = initialState(30f, 2.5f, 4, now.minusSeconds(60));
        state = Sm2Lite.nextReview(state, 0, now);
        state = Sm2Lite.nextReview(state, 2, now);
        assertEquals(3f, state.intervalDays());
        assertExactDays(3, state.dueAt());
    }

    @Test
    void testInvalidGradeIsRejected() {
        CardScheduleState state = initialState();
        for (int bad : new int[]{-1, 3, 7}) {
            try {
                Sm2Lite.nextReview(state, bad, now);
                org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException for grade " + bad);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }
}

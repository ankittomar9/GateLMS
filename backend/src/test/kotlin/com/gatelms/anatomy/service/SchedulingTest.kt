package com.gatelms.anatomy.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

/**
 * Honest-judge tests for the SM-2-lite scheduler (PHASE7-M1-ANATOMY-SPEC §6.2, §6.4).
 *
 * The algorithm is fully deterministic: every test feeds a known prior state plus a
 * known grade and asserts the EXACT next interval / ease / consecutive_correct, and
 * the exact due date. No mocks, no clock ambiguity — `now` is fixed per test.
 */
class SchedulingTest {

    private val now: Instant = Instant.parse("2026-10-08T12:00:00Z")

    private fun initialState(
        intervalDays: Float = 0f,
        ease: Float = Sm2Lite.EASE_INITIAL,
        consecutiveCorrect: Int = 0,
        lastReviewedAt: Instant? = null
    ): CardScheduleState {
        return CardScheduleState(
            dueAt = now,
            intervalDays = intervalDays,
            ease = ease,
            consecutiveCorrect = consecutiveCorrect,
            lastReviewedAt = lastReviewedAt
        )
    }

    private fun assertExactDays(expectedDays: Long, actualDueAt: Instant, reference: Instant = now) {
        val actualMs = Duration.between(reference, actualDueAt).toMillis()
        assertEquals(expectedDays * 86_400_000.0, actualMs.toDouble(), 2_000.0) // <= 2s tolerance for float path
    }

    @Test
    fun `1 First review schedules plus 1 day for all grades`() {
        for (grade in 0..2) {
            val next = Sm2Lite.nextReview(initialState(lastReviewedAt = null), grade, now)
            assertExactDays(1, next.dueAt)
            assertEquals(1f, next.intervalDays)
            assertEquals(Sm2Lite.EASE_INITIAL, next.ease)
            assertEquals(0, next.consecutiveCorrect)
            assertEquals(now, next.lastReviewedAt)
        }
    }

    @Test
    fun `2 SOLID ladder is exactly 1 3 7 14 30 days`() {
        // Walk five consecutive SOLID reviews starting from a never-reviewed card.
        // Review 1 is the first review (+1d rung); reviews 2..5 consume the remaining
        // ladder rungs 3 / 7 / 14 / 30 in order.
        var state = initialState(lastReviewedAt = null)
        val expectedLadder = longArrayOf(1, 3, 7, 14, 30)

        for ((i, expectedDays) in expectedLadder.withIndex()) {
            state = Sm2Lite.nextReview(state, 2, now)
            assertExactDays(expectedDays, state.dueAt)
            assertEquals(expectedDays.toFloat(), state.intervalDays)
        }
        // consecutive_correct after 5 SOLID reviews: 4 (the first review does not increment)
        assertEquals(4, state.consecutiveCorrect)
    }

    @Test
    fun `3 Ladder exit multiplies interval by ease`() {
        // Card that already walked the full ladder: cc = 4, interval = 30d, ease = 2.5.
        // The next SOLID pushes cc to 5 -> beyond the ladder -> interval = 30 * 2.5 = 75d.
        val state = initialState(intervalDays = 30f, consecutiveCorrect = 4, lastReviewedAt = now.minusSeconds(60))
        val next = Sm2Lite.nextReview(state, 2, now)
        assertEquals(75f, next.intervalDays, 0.0001f)
        assertExactDays(75, next.dueAt)
        assertEquals(5, next.consecutiveCorrect)
        assertEquals(2.5f, next.ease)
    }

    @Test
    fun `4 SOLID beyond the ladder keeps multiplying by ease`() {
        // cc already 7 (deep in the ease regime), interval 60d -> next SOLID = 60 * 2.5 = 150d.
        val state = initialState(intervalDays = 60f, consecutiveCorrect = 7, lastReviewedAt = now.minusSeconds(60))
        val next = Sm2Lite.nextReview(state, 2, now)
        assertEquals(150f, next.intervalDays, 0.0001f)
        assertEquals(8, next.consecutiveCorrect)
    }

    @Test
    fun `5 SHAKY multiplies interval by 1_2, resets streak and decays ease`() {
        val state = initialState(intervalDays = 10f, ease = 2.5f, consecutiveCorrect = 3, lastReviewedAt = now.minusSeconds(60))
        val next = Sm2Lite.nextReview(state, 1, now)
        assertEquals(12f, next.intervalDays, 0.0001f)      // 10 * 1.2
        assertEquals(2.45f, next.ease, 0.0001f)            // 2.5 - 0.05
        assertEquals(0, next.consecutiveCorrect)           // streak reset
        assertExactDays(12, next.dueAt)
    }

    @Test
    fun `6 SHAKY floors the interval at 1 day`() {
        val state = initialState(intervalDays = 0.5f, ease = 2.5f, consecutiveCorrect = 1, lastReviewedAt = now.minusSeconds(60))
        val next = Sm2Lite.nextReview(state, 1, now)
        assertEquals(1f, next.intervalDays, 0.0001f)       // max(1, 0.5 * 1.2) = 1
        assertExactDays(1, next.dueAt)
    }

    @Test
    fun `7 FORGOTTEN resets interval to 1 day, streak to 0 and decays ease by 0_2`() {
        val state = initialState(intervalDays = 30f, ease = 2.5f, consecutiveCorrect = 4, lastReviewedAt = now.minusSeconds(60))
        val next = Sm2Lite.nextReview(state, 0, now)
        assertEquals(1f, next.intervalDays)
        assertExactDays(1, next.dueAt)
        assertEquals(2.3f, next.ease, 0.0001f)             // 2.5 - 0.2
        assertEquals(0, next.consecutiveCorrect)
    }

    @Test
    fun `8 Ease never drops below the 1_3 floor`() {
        var state = initialState(intervalDays = 30f, ease = 2.5f, consecutiveCorrect = 2, lastReviewedAt = now.minusSeconds(60))
        // Hammer the card with FORGOTTEN grades: 2.5 -> 2.3 -> 2.1 -> 1.9 -> 1.7 -> 1.5 -> 1.3 -> floor.
        repeat(10) {
            state = Sm2Lite.nextReview(state, 0, now)
            assertTrue(state.ease >= Sm2Lite.EASE_FLOOR, "ease fell below floor: ${state.ease}")
        }
        assertEquals(Sm2Lite.EASE_FLOOR, state.ease, 0.0001f)
        // SHAKY decay is also floored.
        var shakyState = initialState(intervalDays = 30f, ease = 1.32f, consecutiveCorrect = 0, lastReviewedAt = now.minusSeconds(60))
        shakyState = Sm2Lite.nextReview(shakyState, 1, now)
        assertEquals(Sm2Lite.EASE_FLOOR, shakyState.ease, 0.0001f)
    }

    @Test
    fun `9 FORGOTTEN then SOLID restarts the ladder from the 3-day rung`() {
        var state = initialState(intervalDays = 30f, ease = 2.5f, consecutiveCorrect = 4, lastReviewedAt = now.minusSeconds(60))
        state = Sm2Lite.nextReview(state, 0, now)          // FORGOTTEN: cc=0, interval=1
        state = Sm2Lite.nextReview(state, 2, now)          // SOLID: cc=1 -> ladder rung 3
        assertEquals(3f, state.intervalDays)
        assertExactDays(3, state.dueAt)
    }

    @Test
    fun `10 Invalid grade is rejected`() {
        val state = initialState(lastReviewedAt = null)
        for (bad in intArrayOf(-1, 3, 7)) {
            try {
                Sm2Lite.nextReview(state, bad, now)
                throw AssertionError("Expected IllegalArgumentException for grade $bad")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }
}

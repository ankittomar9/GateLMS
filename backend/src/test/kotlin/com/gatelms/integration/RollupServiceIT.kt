package com.gatelms.integration

import com.gatelms.entity.DailyRollupId
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.service.RollupService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class RollupServiceIT : BaseIntegrationTest() {

    @Autowired
    lateinit var rollupService: RollupService

    @Autowired
    lateinit var dailyRollupRepository: DailyRollupRepository

    private val operatorId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        dailyRollupRepository.deleteAll()
    }

    @Test
    fun `practice answer increments questionsAnswered correctAnswers and minutesPracticed`() {
        val date = LocalDate.of(2026, 10, 7)
        rollupService.recordAnswer(activityDate = date, correct = true, seconds = 75, mode = "PRACTICE")

        val rollup = dailyRollupRepository.findById(DailyRollupId(operatorId, date)).orElseThrow()
        assertEquals(1, rollup.questionsAnswered)
        assertEquals(1, rollup.correctAnswers)
        assertEquals(2, rollup.minutesPracticed) // (75 + 59) / 60 = 2
        assertEquals(0, rollup.minutesMocked)
        assertEquals(0, rollup.mockAttempts)

        // Record another practice answer, incorrect, 30 seconds
        rollupService.recordAnswer(activityDate = date, correct = false, seconds = 30, mode = "PRACTICE")

        val updated = dailyRollupRepository.findById(DailyRollupId(operatorId, date)).orElseThrow()
        assertEquals(2, updated.questionsAnswered)
        assertEquals(1, updated.correctAnswers)
        assertEquals(3, updated.minutesPracticed) // 2 + 1 = 3
        assertEquals(0, updated.minutesMocked)
        assertEquals(0, updated.mockAttempts)
    }

    @Test
    fun `mock submit bulk-increments mockAttempts minutesMocked questions and correct`() {
        val date = LocalDate.of(2026, 10, 8)
        rollupService.recordMockSubmit(
            activityDate = date,
            questionsAnswered = 65,
            correctAnswers = 48,
            minutesMocked = 175
        )

        val rollup = dailyRollupRepository.findById(DailyRollupId(operatorId, date)).orElseThrow()
        assertEquals(65, rollup.questionsAnswered)
        assertEquals(48, rollup.correctAnswers)
        assertEquals(0, rollup.minutesPracticed)
        assertEquals(175, rollup.minutesMocked)
        assertEquals(1, rollup.mockAttempts)

        // Submit another mock on the same date
        rollupService.recordMockSubmit(
            activityDate = date,
            questionsAnswered = 30,
            correctAnswers = 20,
            minutesMocked = 90
        )

        val updated = dailyRollupRepository.findById(DailyRollupId(operatorId, date)).orElseThrow()
        assertEquals(95, updated.questionsAnswered)
        assertEquals(68, updated.correctAnswers)
        assertEquals(265, updated.minutesMocked)
        assertEquals(2, updated.mockAttempts)
    }

    @Test
    fun `upsert idempotence accumulates correctly on same date across mixed practice and mock sessions`() {
        val date = LocalDate.of(2026, 10, 9)

        // Mixed sequence on same date
        rollupService.recordAnswer(activityDate = date, correct = true, seconds = 120, mode = "practice")
        rollupService.recordMockSubmit(activityDate = date, questionsAnswered = 10, correctAnswers = 8, minutesMocked = 45)
        rollupService.recordAnswer(activityDate = date, correct = false, seconds = 60, mode = "practice")

        val rollup = dailyRollupRepository.findById(DailyRollupId(operatorId, date)).orElseThrow()
        assertEquals(12, rollup.questionsAnswered) // 1 + 10 + 1
        assertEquals(9, rollup.correctAnswers)     // 1 + 8 + 0
        assertEquals(3, rollup.minutesPracticed)   // 2 + 0 + 1
        assertEquals(45, rollup.minutesMocked)    // 0 + 45 + 0
        assertEquals(1, rollup.mockAttempts)       // 0 + 1 + 0
    }
}

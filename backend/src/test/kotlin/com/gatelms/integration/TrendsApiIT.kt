package com.gatelms.integration

import com.gatelms.dto.TrendsResponse
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.service.RollupService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate

class TrendsApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var rollupService: RollupService

    @Autowired
    lateinit var dailyRollupRepository: DailyRollupRepository

    @BeforeEach
    fun setup() {
        dailyRollupRepository.deleteAll()
    }

    @Test
    fun `trends zero-fills gap dates and computes rolling 7-day stats correctly`() {
        val today = LocalDate.now()
        val twoDaysAgo = today.minusDays(2)
        val fiveDaysAgo = today.minusDays(5)

        // Seed day -5: 10 answered, 8 correct, 10 practice mins, 15 mock mins
        rollupService.recordAnswer(activityDate = fiveDaysAgo, correct = true, seconds = 600, mode = "practice")
        rollupService.recordMockSubmit(activityDate = fiveDaysAgo, questionsAnswered = 9, correctAnswers = 7, minutesMocked = 15)

        // Seed day -2: 5 answered, 3 correct, 30 mock mins
        rollupService.recordMockSubmit(activityDate = twoDaysAgo, questionsAnswered = 5, correctAnswers = 3, minutesMocked = 30)

        val resp = restTemplate.getForEntity("/api/v1/stats/trends?days=7", TrendsResponse::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!

        assertEquals(7, body.days.size)
        // Verify gap dates are zero-filled
        val dayMap = body.days.associateBy { it.date }
        assertEquals(today.minusDays(6), body.days.first().date)
        assertEquals(today, body.days.last().date)

        // Verify day -6 (gap date)
        val day6Ago = dayMap[today.minusDays(6)]!!
        assertEquals(0, day6Ago.questionsAnswered)
        assertEquals(0, day6Ago.correct)
        assertEquals(0, day6Ago.minutesPracticed)
        assertEquals(0, day6Ago.minutesMocked)

        // Verify day -5
        val day5 = dayMap[fiveDaysAgo]!!
        assertEquals(10, day5.questionsAnswered)
        assertEquals(8, day5.correct)
        assertEquals(10, day5.minutesPracticed)
        assertEquals(15, day5.minutesMocked)

        // Verify day -2
        val day2 = dayMap[twoDaysAgo]!!
        assertEquals(5, day2.questionsAnswered)
        assertEquals(3, day2.correct)
        assertEquals(0, day2.minutesPracticed)
        assertEquals(30, day2.minutesMocked)

        // Verify rolling7: total answered = 10 + 5 = 15, total correct = 8 + 3 = 11
        // Accuracy = (11 / 15) * 100 = 73.333% -> 73.3%
        assertEquals(15, body.rolling7.volume)
        assertEquals(73.3, body.rolling7.accuracy)
    }

    @Test
    fun `trends rejects days parameter outside 7 to 365 with 400 Bad Request`() {
        val belowMin = restTemplate.getForEntity("/api/v1/stats/trends?days=6", Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, belowMin.statusCode)
        val belowBody = belowMin.body!!
        val fieldErrorsBelow = belowBody["fieldErrors"] as? Map<*, *>
        assertNotNull(fieldErrorsBelow)
        assertTrue(fieldErrorsBelow!!.containsKey("days"))

        val aboveMax = restTemplate.getForEntity("/api/v1/stats/trends?days=366", Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, aboveMax.statusCode)
        val aboveBody = aboveMax.body!!
        val fieldErrorsAbove = aboveBody["fieldErrors"] as? Map<*, *>
        assertNotNull(fieldErrorsAbove)
        assertTrue(fieldErrorsAbove!!.containsKey("days"))
    }
}

package com.gatelms.integration

import com.gatelms.dto.EngagementSummaryResponse
import com.gatelms.dto.UpdateUserGoalRequest
import com.gatelms.dto.UserGoalDto
import com.gatelms.entity.DailyRollup
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.repository.UserGoalRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import java.time.LocalDate

class EngagementApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var dailyRollupRepository: DailyRollupRepository

    @Autowired
    lateinit var userGoalRepository: UserGoalRepository

    @BeforeEach
    fun cleanUp() {
        dailyRollupRepository.deleteAll()
        userGoalRepository.deleteAll()
    }

    @Test
    fun `1 engagement summary returns zero streak and default goal on fresh system`() {
        val resp = restTemplate.getForEntity(
            "/api/v1/engagement/summary",
            EngagementSummaryResponse::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val summary = resp.body
        assertNotNull(summary)
        assertEquals(0, summary!!.streak)
        assertFalse(summary.streakAtRisk)
        assertEquals(20, summary.goal.target)
        assertEquals(0, summary.goal.done)
        assertFalse(summary.goal.met)
    }

    @Test
    fun `2 goal update validates target between 5 and 200 and updates successfully`() {
        // Invalid target: 4 -> 400
        val invalidReq = UpdateUserGoalRequest(dailyTargetQuestions = 4)
        val invalidResp = restTemplate.exchange(
            "/api/v1/engagement/goal",
            HttpMethod.PATCH,
            HttpEntity(invalidReq),
            Map::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, invalidResp.statusCode)

        // Invalid target: 201 -> 400
        val invalidReq2 = UpdateUserGoalRequest(dailyTargetQuestions = 201)
        val invalidResp2 = restTemplate.exchange(
            "/api/v1/engagement/goal",
            HttpMethod.PATCH,
            HttpEntity(invalidReq2),
            Map::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, invalidResp2.statusCode)

        // Valid target: 35
        val validReq = UpdateUserGoalRequest(dailyTargetQuestions = 35, streakProtection = false)
        val validResp = restTemplate.exchange(
            "/api/v1/engagement/goal",
            HttpMethod.PATCH,
            HttpEntity(validReq),
            UserGoalDto::class.java
        )
        assertEquals(HttpStatus.OK, validResp.statusCode)
        assertEquals(35, validResp.body?.dailyTargetQuestions)
        assertEquals(false, validResp.body?.streakProtection)

        // Verify summary reflects updated goal
        val summaryResp = restTemplate.getForEntity(
            "/api/v1/engagement/summary",
            EngagementSummaryResponse::class.java
        )
        assertEquals(35, summaryResp.body?.goal?.target)
    }

    @Test
    fun `3 engagement summary computes real streak from rollups`() {
        val today = LocalDate.now()
        // Save activity for today and yesterday
        dailyRollupRepository.save(
            DailyRollup(activityDate = today, questionsAnswered = 25)
        )
        dailyRollupRepository.save(
            DailyRollup(activityDate = today.minusDays(1), questionsAnswered = 15)
        )

        val summary = restTemplate.getForEntity(
            "/api/v1/engagement/summary",
            EngagementSummaryResponse::class.java
        ).body

        assertNotNull(summary)
        assertEquals(2, summary!!.streak)
        assertFalse(summary.streakAtRisk)
        assertEquals(25, summary.goal.done)
        assertTrue(summary.goal.met, "Goal met because done 25 >= default target 20")
    }
}

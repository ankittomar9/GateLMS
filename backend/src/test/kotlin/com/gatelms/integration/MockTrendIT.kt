package com.gatelms.integration

import com.gatelms.dto.MockTrendDto
import com.gatelms.entity.MockAttempt
import com.gatelms.entity.MockAttemptStatus
import com.gatelms.entity.MockExam
import com.gatelms.repository.MockAttemptRepository
import com.gatelms.repository.MockExamRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.Instant

class MockTrendIT : BaseIntegrationTest() {

    @Autowired
    lateinit var mockExamRepository: MockExamRepository

    @Autowired
    lateinit var mockAttemptRepository: MockAttemptRepository

    @Test
    fun `mock trend returns empty list when no submitted attempts exist`() {
        mockAttemptRepository.deleteAll()

        val resp = restTemplate.getForEntity("/api/v1/stats/mock-trend", Array<MockTrendDto>::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertNotNull(resp.body)
        assertEquals(0, resp.body!!.size)
    }

    @Test
    fun `mock trend orders attempts chronologically with valid score math`() {
        mockAttemptRepository.deleteAll()

        val exam = mockExamRepository.save(
            MockExam(
                title = "GATE Trend Mock #1",
                totalMarks = 100.0,
                durationMinutes = 180,
                blueprintJson = "{}"
            )
        )

        val t1 = Instant.now().minusSeconds(7200)
        val t2 = Instant.now().minusSeconds(3600)

        // Attempt 1: submitted earlier, score 45.0
        val attempt1 = MockAttempt(
            mockExam = exam,
            startedAt = t1.minusSeconds(1800),
            submittedAt = t1,
            totalScore = 45.0,
            positiveScore = 50.0,
            negativeScore = 5.0,
            status = MockAttemptStatus.SUBMITTED
        )
        mockAttemptRepository.save(attempt1)

        // Attempt 2: submitted later, score 68.5
        val attempt2 = MockAttempt(
            mockExam = exam,
            startedAt = t2.minusSeconds(1800),
            submittedAt = t2,
            totalScore = 68.5,
            positiveScore = 70.0,
            negativeScore = 1.5,
            status = MockAttemptStatus.SUBMITTED
        )
        mockAttemptRepository.save(attempt2)

        val resp = restTemplate.getForEntity("/api/v1/stats/mock-trend", Array<MockTrendDto>::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val trend = resp.body!!
        assertEquals(2, trend.size)

        // Verify chronological order
        assertEquals(attempt1.id, trend[0].attemptId)
        assertEquals(45.0, trend[0].totalScore)
        assertEquals(45.0, trend[0].percentage)

        assertEquals(attempt2.id, trend[1].attemptId)
        assertEquals(68.5, trend[1].totalScore)
        assertEquals(68.5, trend[1].percentage)
        assertTrue(trend[0].date.isBefore(trend[1].date))
    }
}

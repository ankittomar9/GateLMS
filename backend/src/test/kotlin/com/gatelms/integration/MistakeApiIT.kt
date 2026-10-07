package com.gatelms.integration

import com.gatelms.dto.MistakeCreateRequest
import com.gatelms.dto.MistakeDto
import com.gatelms.dto.MistakeReviewRequest
import com.gatelms.entity.MistakeErrorCategory
import com.gatelms.repository.MistakeLogRepository
import com.gatelms.repository.QuestionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

class MistakeApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var mistakeLogRepository: MistakeLogRepository

    @Test
    fun `create mistake log with valid categories succeeds and schedules review in 1 day`() {
        val question = questionRepository.findAll().first()
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "CONCEPTUAL",
            description = "Misunderstood master theorem case 2",
            correctiveTakeaway = "Check polynomial difference between log_b(a) and f(n)"
        )

        val resp = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val created = resp.body!!

        assertEquals(MistakeErrorCategory.CONCEPTUAL, created.errorCategory)
        assertEquals("Check polynomial difference between log_b(a) and f(n)", created.correctiveTakeaway)
        assertEquals(0, created.reviewCount)
        assertFalse(created.resolved)
        assertNotNull(created.nextReviewAt)

        val expectedNextReviewMin = Instant.now().plus(23, ChronoUnit.HOURS)
        val expectedNextReviewMax = Instant.now().plus(25, ChronoUnit.HOURS)
        assertTrue(created.nextReviewAt!!.isAfter(expectedNextReviewMin))
        assertTrue(created.nextReviewAt!!.isBefore(expectedNextReviewMax))
    }

    @Test
    fun `create mistake log with invalid error category returns 400 bad request`() {
        val question = questionRepository.findAll().first()
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "RANDOM_INVALID_CATEGORY",
            correctiveTakeaway = "Some takeaway"
        )

        val resp = restTemplate.postForEntity("/api/v1/mistakes", request, String::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `get mistakes filters by due and resolved flags`() {
        val question = questionRepository.findAll().first()

        // Create a mistake due in the past
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "CALCULATION",
            correctiveTakeaway = "Arithmetic error on 2^10"
        )
        val createdResp = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java)
        val mistake = createdResp.body!!

        // Force next_review_at to the past in the DB
        val entity = mistakeLogRepository.findById(mistake.id).get()
        entity.nextReviewAt = Instant.now().minus(2, ChronoUnit.HOURS)
        mistakeLogRepository.save(entity)

        // Query with due=true
        val dueResp = restTemplate.getForEntity("/api/v1/mistakes?due=true&resolved=false", String::class.java)
        assertEquals(HttpStatus.OK, dueResp.statusCode)
        assertTrue(dueResp.body!!.contains("CALCULATION"))
    }

    @Test
    fun `review mistake schedules intervals in sequence 1 to 3 to 7 days and supports resolving`() {
        val question = questionRepository.findAll().first()
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "MSQ_TRAP",
            correctiveTakeaway = "Remember multiple choices can all be false"
        )
        val created = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java).body!!
        val mId = created.id

        // First review (reviewCount: 0 -> 1): interval should be reviewIntervals[0] = 1 day (or reviewIntervals[1] = 3 days)
        val review1 = restTemplate.exchange(
            "/api/v1/mistakes/$mId/review",
            HttpMethod.PATCH,
            HttpEntity(MistakeReviewRequest()),
            MistakeDto::class.java
        ).body!!
        assertEquals(1, review1.reviewCount)
        assertFalse(review1.resolved)

        // Second review (reviewCount: 1 -> 2): interval is reviewIntervals[1] = 3 days
        val review2 = restTemplate.exchange(
            "/api/v1/mistakes/$mId/review",
            HttpMethod.PATCH,
            HttpEntity(MistakeReviewRequest()),
            MistakeDto::class.java
        ).body!!
        assertEquals(2, review2.reviewCount)
        val daysUntilReview2 = ChronoUnit.DAYS.between(Instant.now(), review2.nextReviewAt)
        assertTrue(daysUntilReview2 in 2..4, "Next review should be scheduled ~3 days out")

        // Third review (reviewCount: 2 -> 3) with resolved=true: interval is reviewIntervals[2] = 7 days
        val review3 = restTemplate.exchange(
            "/api/v1/mistakes/$mId/review",
            HttpMethod.PATCH,
            HttpEntity(MistakeReviewRequest(resolved = true)),
            MistakeDto::class.java
        ).body!!
        assertEquals(3, review3.reviewCount)
        assertTrue(review3.resolved)
        val daysUntilReview3 = ChronoUnit.DAYS.between(Instant.now(), review3.nextReviewAt)
        assertTrue(daysUntilReview3 in 6..8, "Next review should be scheduled ~7 days out")
    }
}

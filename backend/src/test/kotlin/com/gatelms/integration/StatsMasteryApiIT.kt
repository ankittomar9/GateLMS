package com.gatelms.integration

import com.gatelms.dto.MistakeCreateRequest
import com.gatelms.dto.MistakeDto
import com.gatelms.dto.TopicMasteryStatsDto
import com.gatelms.repository.MistakeLogRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

class StatsMasteryApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var mistakeLogRepository: MistakeLogRepository

    @Test
    fun `getMasteryStats returns mastery level and due review counts across all topics`() {
        val resp = restTemplate.exchange(
            "/api/v1/stats/mastery",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<TopicMasteryStatsDto>>() {}
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val statsList = resp.body!!
        assertTrue(statsList.isNotEmpty())

        val firstStat = statsList.first()
        assertNotNull(firstStat.topicId)
        assertNotNull(firstStat.topicName)
        assertTrue(firstStat.masteryLevel in 0..5)
        assertTrue(firstStat.dueReviewsCount >= 0)
    }

    @Test
    fun `getMasteryStats reflects overdue mistakes in dueReviewsCount`() {
        val question = questionRepository.findAll().first { it.topic != null }
        val topicId = question.topic!!.id

        // Create a mistake for this question
        val createReq = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "CONCEPTUAL",
            correctiveTakeaway = "Topic mastery test takeaway"
        )
        val mistake = restTemplate.postForEntity("/api/v1/mistakes", createReq, MistakeDto::class.java).body!!

        // Backdate nextReviewAt to make it due now
        val entity = mistakeLogRepository.findById(mistake.id).get()
        entity.nextReviewAt = Instant.now().minus(1, ChronoUnit.DAYS)
        mistakeLogRepository.save(entity)

        val resp = restTemplate.exchange(
            "/api/v1/stats/mastery",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<TopicMasteryStatsDto>>() {}
        )
        val statsList = resp.body!!
        val topicStat = statsList.find { it.topicId == topicId }
        assertNotNull(topicStat)
        assertTrue((topicStat?.dueReviewsCount ?: 0) >= 1)
    }
}

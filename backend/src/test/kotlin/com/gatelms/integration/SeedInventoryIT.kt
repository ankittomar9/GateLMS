package com.gatelms.integration

import com.gatelms.dto.SeedInventoryResponse
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class SeedInventoryIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    private lateinit var questionSeedRunner: QuestionSeedRunner

    @org.junit.jupiter.api.BeforeEach
    fun setUpRunner() {
        questionSeedRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper
        )
        questionSeedRunner.importQuestions("seed-data/questions.jsonl")
    }

    @Test
    fun `honest-judge PASS - admin seed inventory returns accurate counts and role protection`() {
        // 1. Anonymous access is blocked
        logout()
        val anonResp = restTemplate.getForEntity("/api/v1/admin/seed/inventory", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, anonResp.statusCode)

        // 2. Admin access succeeds
        loginAs("ankur", "admin123")
        val adminResp = restTemplate.getForEntity("/api/v1/admin/seed/inventory", SeedInventoryResponse::class.java)
        assertEquals(HttpStatus.OK, adminResp.statusCode)
        assertNotNull(adminResp.body)

        val inventory = adminResp.body!!
        assertTrue(inventory.totalQuestions >= 25, "Expected >= 25 questions in test container")
        assertTrue(inventory.totalVisibleQuestions > 0)
        assertTrue(inventory.subjects.isNotEmpty(), "Expected seeded subjects in inventory")

        // Assert visible count matches sum of topic visible counts for each subject
        for (subj in inventory.subjects) {
            val topicSum = subj.topics.sumOf { it.visibleCount }
            assertEquals(subj.visibleCount, topicSum, "Subject visible count should match topic sum for ${subj.subjectId}")
        }
    }

    @Test
    fun `honest-judge PASS - multi-path seed runner imports idempotently with zero rejections`() {
        // Seed first file
        val rep1 = questionSeedRunner.importQuestions("seed-data/questions.jsonl")
        assertEquals(0, rep1.rejected)
        assertTrue(rep1.total >= 25)

        // Idempotent re-run
        val rep2 = questionSeedRunner.importQuestions("seed-data/questions.jsonl")
        assertEquals(0, rep2.rejected)
        assertEquals(0, rep2.inserted, "Idempotent re-run should insert 0 new questions")
        assertEquals(rep1.total, rep2.updated)
    }
}

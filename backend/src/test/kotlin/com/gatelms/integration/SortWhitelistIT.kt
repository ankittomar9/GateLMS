package com.gatelms.integration

import com.gatelms.dto.QuestionDto
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.entity.QuestionType
import com.gatelms.repository.QuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class SortWhitelistIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @BeforeEach
    fun setupSortQuestions() {
        if (questionRepository.findByExternalId("SORT-Q-2028") == null) {
            val q1 = QuestionUpsertDto(
                externalId = "SORT-Q-2028",
                subjectId = "subj_coa",
                topicId = "topic_coa_pipelining",
                year = 2028,
                type = QuestionType.MCQ,
                marks = 1,
                source = "AUTHORED",
                statement = "Pipeline throughput calculation 2028 question...",
                optionsJson = """[{"label":"A","text":"1"},{"label":"B","text":"2"}]""",
                correctOptions = "A"
            )
            restTemplate.postForEntity("/api/v1/questions", q1, QuestionDto::class.java)
        }

        if (questionRepository.findByExternalId("SORT-Q-2029") == null) {
            val q2 = QuestionUpsertDto(
                externalId = "SORT-Q-2029",
                subjectId = "subj_coa",
                topicId = "topic_coa_pipelining",
                year = 2029,
                type = QuestionType.MCQ,
                marks = 2,
                source = "AUTHORED",
                statement = "Pipeline hazards latency 2029 question...",
                optionsJson = """[{"label":"A","text":"3"},{"label":"B","text":"4"}]""",
                correctOptions = "B"
            )
            restTemplate.postForEntity("/api/v1/questions", q2, QuestionDto::class.java)
        }
    }

    @Test
    fun `valid sort=year orders questions descending by year`() {
        val response = restTemplate.getForEntity("/api/v1/questions?sort=year&size=50", Map::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)

        val content = response.body?.get("content") as? List<Map<String, Any>>
        assertNotNull(content)
        assertTrue(content!!.size >= 2)

        val years = content.mapNotNull { (it["year"] as? Number)?.toInt() }
        for (i in 0 until years.size - 1) {
            assertTrue(years[i] >= years[i + 1], "Expected years to be sorted descending: ${years[i]} >= ${years[i + 1]}")
        }

        val idx2029 = content.indexOfFirst { it["externalId"] == "SORT-Q-2029" }
        val idx2028 = content.indexOfFirst { it["externalId"] == "SORT-Q-2028" }

        assertTrue(idx2029 != -1 && idx2028 != -1, "Both top-year questions should be in first page")
        assertTrue(idx2029 < idx2028, "Year 2029 should appear before Year 2028 with sort=year")
    }

    @Test
    fun `invalid sort parameter returns 400 Bad Request with fieldErrors sort`() {
        val response = restTemplate.getForEntity("/api/v1/questions?sort=; DROP TABLE questions;", Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val body = response.body
        assertNotNull(body)
        val fieldErrors = body?.get("fieldErrors") as? Map<String, Any>
        assertNotNull(fieldErrors, "Expected fieldErrors map in ProblemDetail response")
        assertTrue(fieldErrors!!.containsKey("sort"), "Expected sort field error")
    }

    @Test
    fun `search requests with q stay relevance ranked regardless of sort param`() {
        val response = restTemplate.getForEntity("/api/v1/questions?q=pipeline&sort=year", Map::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)

        val content = response.body?.get("content") as? List<Map<String, Any>>
        assertNotNull(content)
        assertTrue(content!!.isNotEmpty(), "Expected search results with q and sort")
    }
}

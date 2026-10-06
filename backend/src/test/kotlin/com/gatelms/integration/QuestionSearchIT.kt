package com.gatelms.integration

import com.gatelms.dto.QuestionDto
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

import org.springframework.beans.factory.annotation.Autowired
import com.gatelms.repository.QuestionRepository

class QuestionSearchIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @BeforeEach
    fun setupSearchData() {
        // Ensure test questions exist for FTS
        if (questionRepository.findByExternalId("FTS-COA-PIPE") == null) {
            val q1 = QuestionUpsertDto(
                externalId = "FTS-COA-PIPE",
                subjectId = "subj_coa",
                topicId = "topic_coa_pipelining",
                year = 2024,
                type = QuestionType.MCQ,
                marks = 2,
                statement = "In an instruction pipeline with branch prediction and data forwarding...",
                optionsJson = """[{"label":"A","text":"hazard"},{"label":"B","text":"stall"},{"label":"C","text":"forward"},{"label":"D","text":"flush"}]""",
                correctOptions = "A",
                explanation = "Structural hazards in pipeline architectures cause stalls."
            )
            restTemplate.postForEntity("/api/v1/questions", q1, QuestionDto::class.java)
        }

        if (questionRepository.findByExternalId("FTS-COA-CACHE") == null) {
            val q2 = QuestionUpsertDto(
                externalId = "FTS-COA-CACHE",
                subjectId = "subj_coa",
                topicId = "topic_coa_cache_mapping",
                year = 2024,
                type = QuestionType.MCQ,
                marks = 1,
                statement = "A 4-way set associative cache memory architecture...",
                optionsJson = """[{"label":"A","text":"direct"},{"label":"B","text":"associative"},{"label":"C","text":"spatial"},{"label":"D","text":"temporal"}]""",
                correctOptions = "B",
                explanation = "Cache memory hit ratio analysis."
            )
            restTemplate.postForEntity("/api/v1/questions", q2, QuestionDto::class.java)
        }
    }

    @Test
    fun `fulltext search for pipeline returns relevant ranked questions`() {
        val response = restTemplate.getForEntity("/api/v1/questions?q=pipeline", Map::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        val content = (response.body?.get("content") as? List<*>) ?: emptyList<Any>()
        assertTrue(content.isNotEmpty(), "Expected at least one matching question for query 'pipeline'")
    }

    @Test
    fun `honest-judge deliberate-wrong - search for non-existent terms returns zero rows`() {
        val response = restTemplate.getForEntity("/api/v1/questions?q=quantum entanglement", Map::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        val content = (response.body?.get("content") as? List<*>) ?: emptyList<Any>()
        assertEquals(0, content.size, "Expected 0 results for non-existent topic query")
        val totalElements = (response.body?.get("totalElements") as Number).toInt()
        assertEquals(0, totalElements)
    }

    @Test
    fun `blank q parameter returns unfiltered page`() {
        val allResp = restTemplate.getForEntity("/api/v1/questions", Map::class.java)
        assertEquals(HttpStatus.OK, allResp.statusCode)
        val totalAll = (allResp.body?.get("totalElements") as Number).toInt()

        val blankResp = restTemplate.getForEntity("/api/v1/questions?q=   ", Map::class.java)
        assertEquals(HttpStatus.OK, blankResp.statusCode)
        val totalBlank = (blankResp.body?.get("totalElements") as Number).toInt()

        assertEquals(totalAll, totalBlank, "Blank query should return full unfiltered dataset")
    }
}

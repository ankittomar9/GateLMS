package com.gatelms.integration

import com.gatelms.dto.CurationUpdateRequest
import com.gatelms.dto.QuestionDto
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class QuestionFilterIT : BaseIntegrationTest() {

    private fun createQuestionWithMeta(
        externalId: String,
        subjectId: String,
        topicId: String,
        type: QuestionType,
        source: String?,
        difficulty: String?,
        curationStatus: String?
    ): QuestionDto {
        val createDto = QuestionUpsertDto(
            externalId = externalId,
            subjectId = subjectId,
            topicId = topicId,
            year = 2024,
            type = type,
            marks = 2,
            statement = "Filter test question statement for $externalId",
            optionsJson = if (type == QuestionType.NAT) null else """[{"label":"A","text":"1"},{"label":"B","text":"2"},{"label":"C","text":"3"},{"label":"D","text":"4"}]""",
            correctOptions = if (type == QuestionType.NAT) null else "A",
            numericMin = if (type == QuestionType.NAT) 10.0 else null,
            numericMax = if (type == QuestionType.NAT) 10.0 else null,
            source = source,
            difficulty = difficulty,
            curationStatus = curationStatus
        )
        val resp = restTemplate.postForEntity("/api/v1/questions", createDto, QuestionDto::class.java)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        return resp.body!!
    }

    @Test
    fun `honest-judge PASS - filter by curationStatus isolates accepted questions`() {
        val q1 = createQuestionWithMeta("FILTER-CUR-01", "subj_os", "topic_os_process_sync", QuestionType.MCQ, "SO_DERIVED", "EASY", "UNREVIEWED")
        val q2 = createQuestionWithMeta("FILTER-CUR-02", "subj_os", "topic_os_process_sync", QuestionType.MCQ, "SO_DERIVED", "EASY", "UNREVIEWED")

        // Accept q1
        restTemplate.exchange(
            "/api/v1/questions/${q1.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(CurationUpdateRequest(status = "ACCEPTED")),
            QuestionDto::class.java
        )

        // Query with curationStatus=ACCEPTED
        val resp = restTemplate.exchange(
            "/api/v1/questions?curationStatus=ACCEPTED&q=FILTER-CUR",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<Map<String, Any>>() {}
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val content = resp.body?.get("content") as? List<Map<String, Any>>
        assertNotNull(content)
        val extIds = content!!.map { it["externalId"] as String }
        assertTrue(extIds.contains("FILTER-CUR-01"))
        assertFalse(extIds.contains("FILTER-CUR-02"))
    }

    @Test
    fun `honest-judge PASS - filter by source distinguishes SO_DERIVED from SAMPLE`() {
        createQuestionWithMeta("FILTER-SRC-01", "subj_algo", "topic_algo_asymptotic", QuestionType.MCQ, "SO_DERIVED", "MEDIUM", "UNREVIEWED")
        createQuestionWithMeta("FILTER-SRC-02", "subj_algo", "topic_algo_asymptotic", QuestionType.MCQ, "SAMPLE", "MEDIUM", "UNREVIEWED")

        val resp = restTemplate.exchange(
            "/api/v1/questions?source=SO_DERIVED&q=FILTER-SRC",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<Map<String, Any>>() {}
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val content = resp.body?.get("content") as? List<Map<String, Any>>
        assertNotNull(content)
        val extIds = content!!.map { it["externalId"] as String }
        assertTrue(extIds.contains("FILTER-SRC-01"))
        assertFalse(extIds.contains("FILTER-SRC-02"))
    }

    @Test
    fun `honest-judge PASS - filter by difficulty composes with subjectId and type`() {
        createQuestionWithMeta("FILTER-COMP-01", "subj_dbms", "topic_dbms_relational_alg", QuestionType.NAT, "SO_DERIVED", "HARD", "UNREVIEWED")
        createQuestionWithMeta("FILTER-COMP-02", "subj_dbms", "topic_dbms_relational_alg", QuestionType.MCQ, "SO_DERIVED", "HARD", "UNREVIEWED")
        createQuestionWithMeta("FILTER-COMP-03", "subj_dbms", "topic_dbms_relational_alg", QuestionType.NAT, "SO_DERIVED", "EASY", "UNREVIEWED")

        val resp = restTemplate.exchange(
            "/api/v1/questions?difficulty=HARD&subjectId=subj_dbms&type=NAT&q=FILTER-COMP",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<Map<String, Any>>() {}
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val content = resp.body?.get("content") as? List<Map<String, Any>>
        assertNotNull(content)
        val extIds = content!!.map { it["externalId"] as String }
        assertEquals(listOf("FILTER-COMP-01"), extIds)
    }
}

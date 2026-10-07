package com.gatelms.integration

import com.gatelms.dto.*
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

class StatsCurationIT : BaseIntegrationTest() {

    private fun createQuestion(
        externalId: String,
        subjectId: String,
        topicId: String,
        type: QuestionType,
        difficulty: String
    ): QuestionDto {
        val createDto = QuestionUpsertDto(
            externalId = externalId,
            subjectId = subjectId,
            topicId = topicId,
            year = 2024,
            type = type,
            marks = 1,
            statement = "Stats curation question statement for $externalId",
            optionsJson = """[{"label":"A","text":"1"},{"label":"B","text":"2"},{"label":"C","text":"3"},{"label":"D","text":"4"}]""",
            correctOptions = "A",
            source = "SO_DERIVED",
            difficulty = difficulty,
            curationStatus = "UNREVIEWED"
        )
        val resp = restTemplate.postForEntity("/api/v1/questions", createDto, QuestionDto::class.java)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        return resp.body!!
    }

    @Test
    fun `honest-judge PASS - get curation stats reflects status transitions accurately`() {
        val q1 = createQuestion("STAT-CUR-01", "subj_os", "topic_os_process_sync", QuestionType.MCQ, "MEDIUM")
        val q2 = createQuestion("STAT-CUR-02", "subj_os", "topic_os_process_sync", QuestionType.MCQ, "MEDIUM")

        // Accept q1 and Reject q2
        restTemplate.exchange(
            "/api/v1/questions/${q1.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(CurationUpdateRequest(status = "ACCEPTED")),
            QuestionDto::class.java
        )
        restTemplate.exchange(
            "/api/v1/questions/${q2.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(CurationUpdateRequest(status = "REJECTED", note = "Poorly formatted")),
            QuestionDto::class.java
        )

        val statsResp = restTemplate.getForEntity("/api/v1/stats/curation", CurationStatsDto::class.java)
        assertEquals(HttpStatus.OK, statsResp.statusCode)
        val stats = statsResp.body!!

        assertTrue(stats.totalQuestions > 0)
        assertTrue((stats.byStatus["ACCEPTED"] ?: 0) >= 1)
        assertTrue((stats.byStatus["REJECTED"] ?: 0) >= 1)
    }

    @Test
    fun `honest-judge PASS - per-subject curation counts correctly track subject mutations`() {
        val q = createQuestion("STAT-SUBJ-01", "subj_toc", "topic_toc_regular_dfa", QuestionType.MCQ, "EASY")

        val beforeResp = restTemplate.getForEntity("/api/v1/stats/curation", CurationStatsDto::class.java)
        val tocBefore = beforeResp.body?.bySubject?.find { it.subjectId == "subj_toc" }
        val acceptedBefore = tocBefore?.accepted ?: 0

        // Transition q to ACCEPTED
        restTemplate.exchange(
            "/api/v1/questions/${q.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(CurationUpdateRequest(status = "ACCEPTED")),
            QuestionDto::class.java
        )

        val afterResp = restTemplate.getForEntity("/api/v1/stats/curation", CurationStatsDto::class.java)
        assertEquals(HttpStatus.OK, afterResp.statusCode)
        val tocAfter = afterResp.body?.bySubject?.find { it.subjectId == "subj_toc" }
        assertNotNull(tocAfter)
        assertEquals(acceptedBefore + 1, tocAfter!!.accepted)
    }

    @Test
    fun `honest-judge PASS - get distribution stats returns multi-dimensional counts`() {
        createQuestion("STAT-DIST-01", "subj_algo", "topic_algo_asymptotic", QuestionType.MCQ, "EASY")
        createQuestion("STAT-DIST-02", "subj_algo", "topic_algo_asymptotic", QuestionType.MCQ, "HARD")

        val distResp = restTemplate.getForEntity("/api/v1/stats/distribution", DistributionStatsDto::class.java)
        assertEquals(HttpStatus.OK, distResp.statusCode)
        val dist = distResp.body!!

        assertTrue(dist.total > 0)
        assertFalse(dist.distribution.isEmpty())

        val algoItems = dist.distribution.filter { it.subjectSlug == "algorithms" }
        assertTrue(algoItems.isNotEmpty())
    }
}

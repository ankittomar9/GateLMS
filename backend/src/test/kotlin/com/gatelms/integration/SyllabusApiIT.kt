package com.gatelms.integration

import com.gatelms.dto.SubjectDetailDto
import com.gatelms.dto.SubjectSummaryDto
import com.gatelms.dto.TopicDto
import com.gatelms.dto.TopicMasteryUpdateDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail

class SyllabusApiIT : BaseIntegrationTest() {

    @Test
    fun `subjects list returns the 10 seeded subjects`() {
        val response = restTemplate.getForEntity("/api/v1/subjects", Array<SubjectSummaryDto>::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        val subjects = response.body!!
        assertEquals(10, subjects.size)
        assertTrue(subjects.any { it.id == "subj_algo" && it.name == "Algorithms" })
        assertTrue(subjects.any { it.id == "subj_aptitude" && it.name == "General Aptitude" })
    }

    @Test
    fun `topics by subject returns seeded topics`() {
        val response = restTemplate.getForEntity("/api/v1/topics?subjectId=subj_algo", Array<TopicDto>::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        val topics = response.body!!
        assertTrue(topics.isNotEmpty())
        assertTrue(topics.all { it.subjectId == "subj_algo" })
    }

    @Test
    fun `honest-judge deliberate-wrong - unknown subject id returns 404 ProblemDetail`() {
        val response = restTemplate.getForEntity("/api/v1/subjects/subj_non_existent", ProblemDetail::class.java)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        val detail = response.body!!
        assertEquals(404, detail.status)
        assertTrue(detail.detail?.contains("Subject not found") == true)
    }

    @Test
    fun `honest-judge - topic mastery update succeeds with fresh version and 409 on stale version`() {
        val topics = restTemplate.getForEntity("/api/v1/topics?subjectId=subj_algo", Array<TopicDto>::class.java).body!!
        val topic = topics.first()

        // Valid update
        val updateEntity = HttpEntity(TopicMasteryUpdateDto(masteryLevel = 2, version = topic.version))
        val patchResp = restTemplate.exchange(
            "/api/v1/topics/${topic.id}",
            HttpMethod.PATCH,
            updateEntity,
            TopicDto::class.java
        )
        assertEquals(HttpStatus.OK, patchResp.statusCode)
        assertEquals(2, patchResp.body?.masteryLevel)
        val nextVersion = patchResp.body?.version ?: 1

        // Deliberate-wrong: stale version 0 triggers 409 Conflict
        val staleEntity = HttpEntity(TopicMasteryUpdateDto(masteryLevel = 4, version = 0))
        val conflictResp = restTemplate.exchange(
            "/api/v1/topics/${topic.id}",
            HttpMethod.PATCH,
            staleEntity,
            ProblemDetail::class.java
        )
        assertEquals(HttpStatus.CONFLICT, conflictResp.statusCode)
    }
}

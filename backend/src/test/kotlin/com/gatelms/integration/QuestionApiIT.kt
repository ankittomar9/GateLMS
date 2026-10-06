package com.gatelms.integration

import com.gatelms.dto.*
import com.gatelms.entity.Pass1Status
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.util.UUID

class QuestionApiIT : BaseIntegrationTest() {

    @Test
    fun `CRUD round-trip - create, read, update, status patch, and verify`() {
        // 1. Create
        val createDto = QuestionUpsertDto(
            externalId = "IT-TEST-Q01",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.MCQ,
            marks = 2,
            statement = "What is the time complexity of binary search on a sorted array of size n?",
            optionsJson = """[{"label":"A","text":"O(1)"},{"label":"B","text":"O(log n)"},{"label":"C","text":"O(n)"},{"label":"D","text":"O(n^2)"}]""",
            correctOptions = "B",
            explanation = "Binary search divides the search space in half each step."
        )
        val createResp = restTemplate.postForEntity("/api/v1/questions", createDto, QuestionDto::class.java)
        assertEquals(HttpStatus.CREATED, createResp.statusCode)
        val created = createResp.body!!
        assertNotNull(created.id)
        assertEquals("IT-TEST-Q01", created.externalId)
        assertEquals(Pass1Status.PENDING, created.pass1Status)

        val qId = created.id

        // 2. Read
        val readResp = restTemplate.getForEntity("/api/v1/questions/$qId", QuestionDto::class.java)
        assertEquals(HttpStatus.OK, readResp.statusCode)
        assertEquals("IT-TEST-Q01", readResp.body?.externalId)

        // 3. Update
        val updateDto = createDto.copy(
            statement = "Updated binary search question statement?",
            version = created.version
        )
        val updateResp = restTemplate.exchange(
            "/api/v1/questions/$qId",
            HttpMethod.PUT,
            HttpEntity(updateDto),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, updateResp.statusCode)
        assertEquals("Updated binary search question statement?", updateResp.body?.statement)
        assertTrue((updateResp.body?.version ?: 0) > created.version)

        // 4. Status Patch
        val patchResp = restTemplate.exchange(
            "/api/v1/questions/$qId/status",
            HttpMethod.PATCH,
            HttpEntity(QuestionStatusUpdateDto(pass1Status = Pass1Status.SOLVED)),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, patchResp.statusCode)
        assertEquals(Pass1Status.SOLVED, patchResp.body?.pass1Status)

        // 5. Verify final state
        val finalResp = restTemplate.getForEntity("/api/v1/questions/$qId", QuestionDto::class.java)
        assertEquals(HttpStatus.OK, finalResp.statusCode)
        assertEquals(Pass1Status.SOLVED, finalResp.body?.pass1Status)
    }

    @Test
    fun `honest-judge deliberate-wrong - V-Q validation rejections return 400 with fieldErrors`() {
        // Deliberate-wrong 1: MCQ with multiple correct options
        val invalidMcq = QuestionUpsertDto(
            externalId = "IT-INVALID-MCQ",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.MCQ,
            marks = 1,
            statement = "Invalid MCQ statement",
            optionsJson = """[{"label":"A","text":"1"},{"label":"B","text":"2"},{"label":"C","text":"3"},{"label":"D","text":"4"}]""",
            correctOptions = "A,B"
        )
        val mcqResp = restTemplate.postForEntity("/api/v1/questions", invalidMcq, Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, mcqResp.statusCode)
        val fieldErrorsMcq = (mcqResp.body?.get("fieldErrors") as? Map<*, *>)
        assertNotNull(fieldErrorsMcq)
        assertTrue(fieldErrorsMcq?.containsKey("correctOptions") == true)

        // Deliberate-wrong 2: NAT with numericMin > numericMax
        val invalidNat = QuestionUpsertDto(
            externalId = "IT-INVALID-NAT",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.NAT,
            marks = 2,
            statement = "Invalid NAT statement",
            numericMin = 10.0,
            numericMax = 5.0
        )
        val natResp = restTemplate.postForEntity("/api/v1/questions", invalidNat, Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, natResp.statusCode)
        val fieldErrorsNat = (natResp.body?.get("fieldErrors") as? Map<*, *>)
        assertNotNull(fieldErrorsNat)
        assertTrue(fieldErrorsNat?.containsKey("numericMin") == true)

        // Deliberate-wrong 3: Invalid marks (must be 1 or 2)
        val invalidMarks = invalidMcq.copy(marks = 3, correctOptions = "A")
        val marksResp = restTemplate.postForEntity("/api/v1/questions", invalidMarks, Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, marksResp.statusCode)
    }

    @Test
    fun `pagination query returns correct totals structure`() {
        val response = restTemplate.getForEntity("/api/v1/questions?page=0&size=5", Map::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        assertTrue(body.containsKey("content"))
        assertTrue(body.containsKey("totalElements"))
        assertTrue(body.containsKey("totalPages"))
        assertTrue(body.containsKey("pageable"))
        val size = body["size"] as Number
        assertEquals(5, size.toInt())
    }
}

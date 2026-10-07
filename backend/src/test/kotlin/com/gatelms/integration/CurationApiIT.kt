package com.gatelms.integration

import com.gatelms.dto.CurationUpdateRequest
import com.gatelms.dto.QuestionDto
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.util.UUID

class CurationApiIT : BaseIntegrationTest() {

    private fun createTestQuestion(externalId: String): QuestionDto {
        val createDto = QuestionUpsertDto(
            externalId = externalId,
            subjectId = "subj_os",
            topicId = "topic_os_process_sync",
            year = 2024,
            type = QuestionType.MCQ,
            marks = 1,
            statement = "What is the primary function of a dispatcher in an OS?",
            optionsJson = """[{"label":"A","text":"Context switching"},{"label":"B","text":"Compilation"},{"label":"C","text":"Linking"},{"label":"D","text":"Loading"}]""",
            correctOptions = "A",
            explanation = "Dispatcher gives control of the CPU to the process selected by short-term scheduler."
        )
        val resp = restTemplate.postForEntity("/api/v1/questions", createDto, QuestionDto::class.java)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        return resp.body!!
    }

    @Test
    fun `honest-judge PASS - accept curation status updates question to ACCEPTED`() {
        val q = createTestQuestion("CURATION-ACCEPT-01")
        val patchDto = CurationUpdateRequest(status = "ACCEPTED")

        val patchResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(patchDto),
            QuestionDto::class.java
        )

        assertEquals(HttpStatus.OK, patchResp.statusCode)
        val body = patchResp.body!!
        assertEquals("ACCEPTED", body.curationStatus)
        assertNull(body.curationNote)

        // Read again and verify database persistence
        val getResp = restTemplate.getForEntity("/api/v1/questions/${q.id}", QuestionDto::class.java)
        assertEquals(HttpStatus.OK, getResp.statusCode)
        assertEquals("ACCEPTED", getResp.body?.curationStatus)
    }

    @Test
    fun `honest-judge PASS - reject curation status with note updates question to REJECTED and saves note`() {
        val q = createTestQuestion("CURATION-REJECT-01")
        val patchDto = CurationUpdateRequest(
            status = "REJECTED",
            note = "Options ambiguously worded, needs revision"
        )

        val patchResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(patchDto),
            QuestionDto::class.java
        )

        assertEquals(HttpStatus.OK, patchResp.statusCode)
        val body = patchResp.body!!
        assertEquals("REJECTED", body.curationStatus)
        assertEquals("Options ambiguously worded, needs revision", body.curationNote)

        // Verify persistence
        val getResp = restTemplate.getForEntity("/api/v1/questions/${q.id}", QuestionDto::class.java)
        assertEquals(HttpStatus.OK, getResp.statusCode)
        assertEquals("REJECTED", getResp.body?.curationStatus)
        assertEquals("Options ambiguously worded, needs revision", getResp.body?.curationNote)
    }

    @Test
    fun `honest-judge FAIL - deliberate-invalid status returns 400 ProblemDetail with fieldErrors`() {
        val q = createTestQuestion("CURATION-INVALID-01")
        val patchDto = CurationUpdateRequest(status = "MAYBE_VALID")

        val patchResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(patchDto),
            ProblemDetail::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, patchResp.statusCode)
        val pd = patchResp.body!!
        assertEquals(400, pd.status)
        val fieldErrors = pd.properties?.get("fieldErrors") as? Map<*, *>
        assertNotNull(fieldErrors, "fieldErrors property must be present in ProblemDetail")
        assertTrue(fieldErrors!!.containsKey("curationStatus"), "fieldErrors must contain 'curationStatus'")
    }

    @Test
    fun `honest-judge FAIL - curation update on nonexistent UUID returns 404 ProblemDetail`() {
        val randomId = UUID.randomUUID()
        val patchDto = CurationUpdateRequest(status = "ACCEPTED")

        val patchResp = restTemplate.exchange(
            "/api/v1/questions/$randomId/curation",
            HttpMethod.PATCH,
            HttpEntity(patchDto),
            ProblemDetail::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, patchResp.statusCode)
        val pd = patchResp.body!!
        assertEquals(404, pd.status)
    }
}

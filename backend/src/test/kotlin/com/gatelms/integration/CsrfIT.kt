package com.gatelms.integration

import com.gatelms.dto.CreateTextNoteRequest
import com.gatelms.dto.NoteDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.*

class CsrfIT : BaseIntegrationTest() {

    @Test
    fun `1 mutation without X-XSRF-TOKEN header returns 403 Forbidden`() {
        // Ensure logged in to have valid session and CSRF cookies
        val me = loginAs("ankur", "admin123")
        assertNotNull(me)

        val sessionId = authInterceptor.currentSessionId
        assertNotNull(sessionId, "Session ID must be present")

        // Prepare request with GATELMSSESSION cookie but WITHOUT X-XSRF-TOKEN header
        val headers = HttpHeaders()
        headers.add(HttpHeaders.COOKIE, "GATELMSSESSION=$sessionId")
        headers.contentType = MediaType.APPLICATION_JSON

        val noteReq = CreateTextNoteRequest(
            title = "CSRF Rejected Note",
            contentMd = "This should not be saved"
        )

        val entity = HttpEntity<CreateTextNoteRequest>(noteReq, headers)

        // Disable automatic interceptor injection for this raw test
        authInterceptor.autoLoginEnabled = false

        val response = restTemplate.postForEntity("/api/v1/notes", entity, Map::class.java)
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode, "Mutating request without X-XSRF-TOKEN must be rejected with 403")
    }

    @Test
    fun `2 mutation with valid X-XSRF-TOKEN header and cookie succeeds`() {
        val me = loginAs("ankur", "admin123")
        assertNotNull(me)

        val sessionId = authInterceptor.currentSessionId
        val xsrfToken = authInterceptor.currentXsrfToken
        assertNotNull(sessionId, "Session ID must be present")
        assertNotNull(xsrfToken, "XSRF token must be present")

        val headers = HttpHeaders()
        headers.add(HttpHeaders.COOKIE, "GATELMSSESSION=$sessionId; XSRF-TOKEN=$xsrfToken")
        headers.add("X-XSRF-TOKEN", xsrfToken)
        headers.contentType = MediaType.APPLICATION_JSON

        val noteReq = CreateTextNoteRequest(
            title = "CSRF Accepted Note",
            contentMd = "This should be saved cleanly"
        )

        val entity = HttpEntity<CreateTextNoteRequest>(noteReq, headers)

        val response = restTemplate.postForEntity("/api/v1/notes", entity, NoteDto::class.java)
        assertEquals(HttpStatus.CREATED, response.statusCode, "Mutating request with valid CSRF header and cookie must succeed")
        assertNotNull(response.body?.id)
        assertEquals("CSRF Accepted Note", response.body?.title)
    }
}

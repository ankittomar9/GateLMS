package com.gatelms.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class MetricsEndpointIT : BaseIntegrationTest() {

    @Test
    fun `GET actuator prometheus returns 200 and contains gatelms questions loaded and jvm metrics`() {
        val response = restTemplate.getForEntity("/actuator/prometheus", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body ?: ""

        assertTrue(body.contains("gatelms_questions_loaded"), "Expected prometheus output to contain gatelms_questions_loaded")
        assertTrue(body.contains("jvm_memory_used_bytes") || body.contains("jvm_memory_used"), "Expected prometheus output to contain jvm memory metrics")
    }

    @Test
    fun `GET actuator prometheus exposes all custom domain metrics and timers`() {
        val response = restTemplate.getForEntity("/actuator/prometheus", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body ?: ""

        assertTrue(body.contains("gatelms_sessions_active"), "Expected gatelms_sessions_active in metrics")
        assertTrue(body.contains("gatelms_attempts_submitted_total"), "Expected gatelms_attempts_submitted_total in metrics")
        assertTrue(body.contains("gatelms_grading_seconds"), "Expected gatelms_grading_seconds in metrics")
        assertTrue(body.contains("gatelms_curation_decisions_total"), "Expected gatelms_curation_decisions_total in metrics")
    }
}

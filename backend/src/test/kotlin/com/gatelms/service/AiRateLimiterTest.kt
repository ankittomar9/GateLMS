package com.gatelms.service

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.exception.AiRateLimitException
import com.gatelms.ai.security.AiRateLimiter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AiRateLimiterTest {

    private lateinit var aiProperties: AiProperties
    private lateinit var rateLimiter: AiRateLimiter

    @BeforeEach
    fun setup() {
        aiProperties = AiProperties()
        aiProperties.quota.chatPerHour = 20
        aiProperties.quota.transcribePer10min = 10
        aiProperties.quota.statusPerHour = 60
        rateLimiter = AiRateLimiter(aiProperties)
    }

    @Test
    fun `chat limit permits 20 requests and rejects 21st with Retry-After (AM-8)`() {
        val userId = UUID.randomUUID()
        for (i in 1..20) {
            rateLimiter.checkChatLimit(userId)
        }

        val ex = assertThrows(AiRateLimitException::class.java) {
            rateLimiter.checkChatLimit(userId)
        }
        assertTrue(ex.retryAfterSeconds in 1..3600)
        assertTrue(ex.message!!.contains("Chat turn quota exceeded"))
    }

    @Test
    fun `transcribe limit permits 10 requests and rejects 11th with Retry-After (AM-8)`() {
        val userId = UUID.randomUUID()
        for (i in 1..10) {
            rateLimiter.checkTranscribeLimit(userId)
        }

        val ex = assertThrows(AiRateLimitException::class.java) {
            rateLimiter.checkTranscribeLimit(userId)
        }
        assertTrue(ex.retryAfterSeconds in 1..600)
        assertTrue(ex.message!!.contains("Audio transcription quota exceeded"))
    }

    @Test
    fun `status limit permits 60 requests and rejects 61st with Retry-After (AM-8)`() {
        val userId = UUID.randomUUID()
        for (i in 1..60) {
            rateLimiter.checkStatusLimit(userId)
        }

        val ex = assertThrows(AiRateLimitException::class.java) {
            rateLimiter.checkStatusLimit(userId)
        }
        assertTrue(ex.retryAfterSeconds in 1..3600)
        assertTrue(ex.message!!.contains("AI Status probe quota exceeded"))
    }

    @Test
    fun `rate limiter isolates quotas between independent users (AM-8)`() {
        val userAlice = UUID.randomUUID()
        val userBob = UUID.randomUUID()

        // Exhaust Alice
        for (i in 1..20) {
            rateLimiter.checkChatLimit(userAlice)
        }
        assertThrows(AiRateLimitException::class.java) {
            rateLimiter.checkChatLimit(userAlice)
        }

        // Bob must still have full quota
        assertDoesNotThrow {
            rateLimiter.checkChatLimit(userBob)
        }
    }
}

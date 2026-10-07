package com.gatelms.ai.security

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.exception.AiRateLimitException
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class AiRateLimiter(
    private val aiProperties: AiProperties
) {
    private val chatTimestamps = ConcurrentHashMap<UUID, MutableList<Long>>()
    private val transcribeTimestamps = ConcurrentHashMap<UUID, MutableList<Long>>()
    private val statusTimestamps = ConcurrentHashMap<UUID, MutableList<Long>>()

    private val chatWindowMs = 3600_000L // 1 hour
    private val transcribeWindowMs = 600_000L // 10 minutes
    private val statusWindowMs = 3600_000L // 1 hour

    fun checkChatLimit(userId: UUID) {
        val maxAttempts = aiProperties.quota.chatPerHour
        checkLimit(
            userId = userId,
            store = chatTimestamps,
            maxAttempts = maxAttempts,
            windowMs = chatWindowMs,
            actionName = "Chat turn"
        )
    }

    fun checkTranscribeLimit(userId: UUID) {
        val maxAttempts = aiProperties.quota.transcribePer10min
        checkLimit(
            userId = userId,
            store = transcribeTimestamps,
            maxAttempts = maxAttempts,
            windowMs = transcribeWindowMs,
            actionName = "Audio transcription"
        )
    }

    fun checkStatusLimit(userId: UUID) {
        val maxAttempts = aiProperties.quota.statusPerHour
        checkLimit(
            userId = userId,
            store = statusTimestamps,
            maxAttempts = maxAttempts,
            windowMs = statusWindowMs,
            actionName = "AI Status probe"
        )
    }

    private fun checkLimit(
        userId: UUID,
        store: ConcurrentHashMap<UUID, MutableList<Long>>,
        maxAttempts: Int,
        windowMs: Long,
        actionName: String
    ) {
        val now = System.currentTimeMillis()
        val windowStart = now - windowMs

        val timestamps = store.compute(userId) { _, existing ->
            val list = existing ?: mutableListOf()
            list.removeAll { it < windowStart }
            list
        }!!

        synchronized(timestamps) {
            timestamps.removeAll { it < windowStart }
            if (timestamps.size >= maxAttempts) {
                val oldestInWindow = timestamps.first()
                val retryAfterMs = (oldestInWindow + windowMs) - now
                val retryAfterSec = maxOf(1L, (retryAfterMs + 999L) / 1000L)
                throw AiRateLimitException(
                    retryAfterSeconds = retryAfterSec,
                    message = "$actionName quota exceeded ($maxAttempts per ${windowMs / 60000}m). Please wait $retryAfterSec seconds before retrying."
                )
            }
            timestamps.add(now)
        }
    }

    fun reset() {
        chatTimestamps.clear()
        transcribeTimestamps.clear()
        statusTimestamps.clear()
    }
}

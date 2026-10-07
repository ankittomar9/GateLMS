package com.gatelms.security

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class LoginRateLimiter {
    private val failureCounts = ConcurrentHashMap<String, FailureRecord>()

    data class FailureRecord(
        var count: Int,
        var lastFailure: Instant
    )

    fun isBlocked(username: String): Boolean {
        val record = failureCounts[username] ?: return false
        val now = Instant.now()
        if (record.lastFailure.plusSeconds(300).isBefore(now)) {
            failureCounts.remove(username)
            return false
        }
        return record.count >= 5
    }

    fun recordFailure(username: String) {
        val now = Instant.now()
        failureCounts.compute(username) { _, record ->
            if (record == null || record.lastFailure.plusSeconds(300).isBefore(now)) {
                FailureRecord(1, now)
            } else {
                record.count += 1
                record.lastFailure = now
                record
            }
        }
    }

    fun reset(username: String) {
        failureCounts.remove(username)
    }

    fun getRetryAfterSeconds(username: String): Long {
        val record = failureCounts[username] ?: return 0
        val unblockAt = record.lastFailure.plusSeconds(300)
        val diff = unblockAt.epochSecond - Instant.now().epochSecond
        return if (diff > 0) diff else 0
    }
}

package com.gatelms.security;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_SECONDS = 300L;

    public static class AttemptRecord {
        public int count;
        public Instant lockedUntil;

        public AttemptRecord(int count, Instant lockedUntil) {
            this.count = count;
            this.lockedUntil = lockedUntil;
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null) return false;
        if (record.lockedUntil != null) {
            if (Instant.now().isBefore(record.lockedUntil)) {
                return true;
            } else {
                attempts.remove(key);
                return false;
            }
        }
        return false;
    }

    public long getRetryAfterSeconds(String key) {
        AttemptRecord record = attempts.get(key);
        if (record != null && record.lockedUntil != null) {
            long remaining = java.time.Duration.between(Instant.now(), record.lockedUntil).toSeconds();
            return Math.max(1L, remaining);
        }
        return 0L;
    }

    public void recordFailedAttempt(String key) {
        attempts.compute(key, (k, existing) -> {
            if (existing == null) {
                return new AttemptRecord(1, null);
            }
            int newCount = existing.count + 1;
            Instant locked = (newCount >= MAX_ATTEMPTS)
                ? Instant.now().plusSeconds(LOCKOUT_SECONDS)
                : existing.lockedUntil;
            return new AttemptRecord(newCount, locked);
        });
    }

    public void resetAttempts(String key) {
        attempts.remove(key);
    }
}

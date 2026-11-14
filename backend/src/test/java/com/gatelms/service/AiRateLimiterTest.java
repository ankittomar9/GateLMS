package com.gatelms.service;

import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.exception.AiExceptions.AiRateLimitException;
import com.gatelms.ai.security.AiRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AiRateLimiterTest {

    private AiProperties aiProperties;
    private AiRateLimiter rateLimiter;

    @BeforeEach
    void setup() {
        aiProperties = new AiProperties();
        aiProperties.getQuota().setChatPerHour(20);
        aiProperties.getQuota().setTranscribePer10min(10);
        aiProperties.getQuota().setStatusPerHour(60);
        aiProperties.getQuota().setPrimerGeneratePerHour(10);
        rateLimiter = new AiRateLimiter(aiProperties);
    }

    @Test
    void testChatLimitPermits20RequestsAndRejects21stWithRetryAfter() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 20; i++) {
            rateLimiter.checkChatLimit(userId);
        }

        AiRateLimitException ex = assertThrows(AiRateLimitException.class, () -> {
            rateLimiter.checkChatLimit(userId);
        });
        assertTrue(ex.getRetryAfterSeconds() >= 1 && ex.getRetryAfterSeconds() <= 3600);
        assertTrue(ex.getMessage().contains("quota exceeded"));
    }

    @Test
    void testTranscribeLimitPermits10RequestsAndRejects11thWithRetryAfter() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkTranscribeLimit(userId);
        }

        AiRateLimitException ex = assertThrows(AiRateLimitException.class, () -> {
            rateLimiter.checkTranscribeLimit(userId);
        });
        assertTrue(ex.getRetryAfterSeconds() >= 1 && ex.getRetryAfterSeconds() <= 600);
        assertTrue(ex.getMessage().contains("Audio transcription quota exceeded"));
    }

    @Test
    void testStatusLimitPermits60RequestsAndRejects61stWithRetryAfter() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 60; i++) {
            rateLimiter.checkStatusLimit(userId);
        }

        AiRateLimitException ex = assertThrows(AiRateLimitException.class, () -> {
            rateLimiter.checkStatusLimit(userId);
        });
        assertTrue(ex.getRetryAfterSeconds() >= 1 && ex.getRetryAfterSeconds() <= 3600);
        assertTrue(ex.getMessage().contains("Status check rate limit exceeded"));
    }

    @Test
    void testPrimerLimitPermits10RequestsAndRejects11thWithRetryAfter() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkPrimerGenerateLimit(userId);
        }

        AiRateLimitException ex = assertThrows(AiRateLimitException.class, () -> {
            rateLimiter.checkPrimerGenerateLimit(userId);
        });
        assertTrue(ex.getRetryAfterSeconds() >= 1 && ex.getRetryAfterSeconds() <= 3600);
        assertTrue(ex.getMessage().contains("quota exceeded"));
    }

    @Test
    void testRateLimiterIsolatesQuotasBetweenIndependentUsers() {
        UUID userAlice = UUID.randomUUID();
        UUID userBob = UUID.randomUUID();

        // Exhaust Alice
        for (int i = 0; i < 20; i++) {
            rateLimiter.checkChatLimit(userAlice);
        }
        assertThrows(AiRateLimitException.class, () -> {
            rateLimiter.checkChatLimit(userAlice);
        });

        // Bob must still have full quota
        assertDoesNotThrow(() -> {
            rateLimiter.checkChatLimit(userBob);
        });
    }
}

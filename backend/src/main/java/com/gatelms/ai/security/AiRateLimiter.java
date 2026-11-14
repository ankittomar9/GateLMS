package com.gatelms.ai.security;

import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.exception.AiExceptions.AiRateLimitException;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class AiRateLimiter {
    private final AiProperties aiProperties;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Instant>> callLog = new ConcurrentHashMap<>();

    public AiRateLimiter(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    public void checkChatQuota(UUID userId) {
        checkQuota("chat:" + userId, aiProperties.getQuota().getChatPerHour(), Duration.ofHours(1), "Hourly AI chat quota exceeded.");
    }

    public void checkTranscribeQuota(UUID userId) {
        checkQuota("transcribe:" + userId, aiProperties.getQuota().getTranscribePer10min(), Duration.ofMinutes(10), "Audio transcription quota exceeded. Please wait a few minutes before dictating again.");
    }

    public void checkStatusQuota(UUID userId) {
        checkQuota("status:" + userId, aiProperties.getQuota().getStatusPerHour(), Duration.ofHours(1), "Status check rate limit exceeded.");
    }

    public void checkAnatomyGenerateQuota(UUID userId) {
        checkQuota("anatomy_gen:" + userId, aiProperties.getQuota().getAnatomyGeneratePerHour(), Duration.ofHours(1), "Hourly question anatomy generation quota exceeded.");
    }

    public void checkConceptGenerateQuota(UUID userId) {
        checkQuota("concept_gen:" + userId, aiProperties.getQuota().getConceptGeneratePerHour(), Duration.ofHours(1), "Hourly concept unit generation quota exceeded.");
    }

    public void checkPrimerGenerateQuota(UUID userId) {
        checkQuota("primer_gen:" + userId, aiProperties.getQuota().getPrimerGeneratePerHour(), Duration.ofHours(1), "Hourly topic primer generation quota exceeded.");
    }

    public void checkChatLimit(UUID userId) { checkChatQuota(userId); }
    public void checkTranscribeLimit(UUID userId) { checkTranscribeQuota(userId); }
    public void checkStatusLimit(UUID userId) { checkStatusQuota(userId); }
    public void checkAnatomyGenerateLimit(UUID userId) { checkAnatomyGenerateQuota(userId); }
    public void checkConceptGenerateLimit(UUID userId) { checkConceptGenerateQuota(userId); }
    public void checkPrimerGenerateLimit(UUID userId) { checkPrimerGenerateQuota(userId); }
    public void checkLessonGenerateLimit(UUID userId) { checkPrimerGenerateQuota(userId); }

    private void checkQuota(String key, int maxAllowed, Duration window, String errorMessage) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        ConcurrentLinkedQueue<Instant> queue = callLog.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        while (!queue.isEmpty() && queue.peek().isBefore(cutoff)) {
            queue.poll();
        }

        if (queue.size() >= maxAllowed) {
            Instant oldest = queue.peek();
            long retryAfterSeconds = oldest != null
                ? Math.max(1L, Duration.between(now, oldest.plus(window)).toSeconds())
                : window.toSeconds();
            throw new AiRateLimitException(retryAfterSeconds, errorMessage);
        }

        queue.add(now);
    }
}

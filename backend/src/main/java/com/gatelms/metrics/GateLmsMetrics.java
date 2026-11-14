package com.gatelms.metrics;

import com.gatelms.entity.MockAttemptStatus;
import com.gatelms.repository.MockAttemptRepository;
import com.gatelms.repository.QuestionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GateLmsMetrics {
    private final MeterRegistry meterRegistry;
    private final QuestionRepository questionRepository;
    private final MockAttemptRepository mockAttemptRepository;
    private final ObjectProvider<Object> readinessServiceProvider;

    private final ConcurrentHashMap<String, Counter> attemptsSubmittedCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> gradingTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> curationCounters = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public GateLmsMetrics(
        MeterRegistry meterRegistry,
        QuestionRepository questionRepository,
        MockAttemptRepository mockAttemptRepository,
        ObjectProvider<?> readinessServiceProvider
    ) {
        this.meterRegistry = meterRegistry;
        this.questionRepository = questionRepository;
        this.mockAttemptRepository = mockAttemptRepository;
        this.readinessServiceProvider = (ObjectProvider<Object>) readinessServiceProvider;
    }

    @PostConstruct
    public void init() {
        // 1. gatelms_questions_loaded Gauge
        Gauge.builder("gatelms_questions_loaded", questionRepository, repo -> (double) repo.count())
            .description("Total number of questions loaded in GateLMS question bank")
            .register(meterRegistry);

        // 1b. gatelms_readiness_score Gauge
        Gauge.builder("gatelms_readiness_score", readinessServiceProvider, provider -> {
            try {
                Object svc = provider.getIfAvailable();
                if (svc != null) {
                    java.lang.reflect.Method m = svc.getClass().getMethod("getOverallReadinessScore");
                    Object res = m.invoke(svc);
                    if (res instanceof Number num) {
                        return num.doubleValue();
                    }
                }
            } catch (Exception ignored) {}
            return 0.0;
        })
            .description("Overall GateLMS readiness score (0-100)")
            .register(meterRegistry);

        // 2. gatelms_sessions_active Gauge
        Gauge.builder("gatelms_sessions_active", mockAttemptRepository, repo -> (double) repo.countByStatus(MockAttemptStatus.IN_PROGRESS))
            .description("Total number of active IN_PROGRESS practice/mock sessions")
            .register(meterRegistry);

        // 3. Counter: gatelms_attempts_submitted_total
        for (String mode : List.of("practice", "mock")) {
            attemptsSubmittedCounters.put(mode, Counter.builder("gatelms_attempts_submitted_total")
                .tag("mode", mode)
                .description("Total number of attempts submitted")
                .register(meterRegistry));
        }

        // 4. Timer: gatelms_grading_seconds
        for (String mode : List.of("practice", "mock")) {
            gradingTimers.put(mode, Timer.builder("gatelms_grading_seconds")
                .tag("mode", mode)
                .description("Time spent grading submissions in seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        }

        // 5. Counter: gatelms_curation_decisions_total
        for (String status : List.of("accepted", "rejected")) {
            curationCounters.put(status, Counter.builder("gatelms_curation_decisions_total")
                .tag("status", status)
                .description("Total number of question curation decisions")
                .register(meterRegistry));
        }
    }

    public void recordAttemptSubmitted(String mode) {
        Counter c = attemptsSubmittedCounters.get(mode.toLowerCase());
        if (c != null) c.increment();
    }

    public void recordGradingDuration(String mode, Duration duration) {
        Timer t = gradingTimers.get(mode.toLowerCase());
        if (t != null) t.record(duration);
    }

    public void recordCurationDecision(String status) {
        Counter c = curationCounters.get(status.toLowerCase());
        if (c != null) c.increment();
    }
}

package com.gatelms.metrics

import com.gatelms.entity.MockAttemptStatus
import com.gatelms.repository.MockAttemptRepository
import com.gatelms.repository.QuestionRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class GateLmsMetrics(
    private val meterRegistry: MeterRegistry,
    private val questionRepository: QuestionRepository,
    private val mockAttemptRepository: MockAttemptRepository,
    private val readinessServiceProvider: org.springframework.beans.factory.ObjectProvider<com.gatelms.service.ReadinessService>
) {

    private val attemptsSubmittedCounters = mutableMapOf<String, Counter>()
    private val gradingTimers = mutableMapOf<String, Timer>()
    private val curationCounters = mutableMapOf<String, Counter>()

    @PostConstruct
    fun init() {
        // 1. gatelms_questions_loaded Gauge
        Gauge.builder("gatelms_questions_loaded", questionRepository) { repo ->
            repo.count().toDouble()
        }
            .description("Total number of questions loaded in GateLMS question bank")
            .register(meterRegistry)

        // 1b. gatelms_readiness_score Gauge
        Gauge.builder("gatelms_readiness_score", readinessServiceProvider) { provider ->
            provider.ifAvailable?.getOverallReadinessScore()?.toDouble() ?: 0.0
        }
            .description("Overall GateLMS readiness score (0-100)")
            .register(meterRegistry)

        // 2. gatelms_sessions_active Gauge
        Gauge.builder("gatelms_sessions_active", mockAttemptRepository) { repo ->
            repo.countByStatus(MockAttemptStatus.IN_PROGRESS).toDouble()
        }
            .description("Total number of active IN_PROGRESS practice/mock sessions")
            .register(meterRegistry)

        // 3. Counter: gatelms_attempts_submitted_total (mode: practice | mock)
        for (mode in listOf("practice", "mock")) {
            attemptsSubmittedCounters[mode] = Counter.builder("gatelms_attempts_submitted_total")
                .tag("mode", mode)
                .description("Total number of attempts submitted")
                .register(meterRegistry)
        }

        // 4. Timer: gatelms_grading_seconds (mode: practice | mock)
        for (mode in listOf("practice", "mock")) {
            gradingTimers[mode] = Timer.builder("gatelms_grading_seconds")
                .tag("mode", mode)
                .description("Time spent grading submissions in seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
        }

        // 5. Counter: gatelms_curation_decisions_total (status: accepted | rejected)
        for (status in listOf("accepted", "rejected")) {
            curationCounters[status] = Counter.builder("gatelms_curation_decisions_total")
                .tag("status", status)
                .description("Total number of question curation decisions")
                .register(meterRegistry)
        }
    }

    fun recordAttemptSubmitted(mode: String) {
        attemptsSubmittedCounters[mode.lowercase()]?.increment()
    }

    fun recordGradingDuration(mode: String, duration: Duration) {
        gradingTimers[mode.lowercase()]?.record(duration)
    }

    fun recordCurationDecision(status: String) {
        curationCounters[status.lowercase()]?.increment()
    }
}

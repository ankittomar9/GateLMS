package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class AnalyticsDtos {
    public record ReadinessResponse(
        int readiness,
        List<SubjectReadinessDto> perSubject,
        Instant computedAt
    ) {
        public ReadinessResponse(int readiness, List<SubjectReadinessDto> perSubject) {
            this(readiness, perSubject, Instant.now());
        }
    }

    public record SubjectReadinessDto(
        String subjectId,
        String name,
        double weight,
        double m,
        double a,
        double c,
        double readiness
    ) {}

    public record HeatmapResponse(
        List<SubjectHeatmapDto> subjects
    ) {}

    public record SubjectHeatmapDto(
        String subjectId,
        String name,
        List<TopicHeatmapDto> topics
    ) {}

    public record TopicHeatmapDto(
        String topicId,
        String name,
        int mastery,
        boolean due,
        int answered,
        int correct
    ) {}

    public record TrendsResponse(
        List<DayRollupDto> days,
        Rolling7Stats rolling7
    ) {}

    public record DayRollupDto(
        LocalDate date,
        int questionsAnswered,
        int correct,
        int minutesPracticed,
        int minutesMocked
    ) {}

    public record Rolling7Stats(
        double accuracy,
        int volume
    ) {}

    public record ExamCountdownDto(
        String examDate,
        long daysRemaining,
        long weeksRemaining,
        String source
    ) {
        public ExamCountdownDto(String examDate, long daysRemaining, long weeksRemaining) {
            this(examDate, daysRemaining, weeksRemaining, "config");
        }
    }
}

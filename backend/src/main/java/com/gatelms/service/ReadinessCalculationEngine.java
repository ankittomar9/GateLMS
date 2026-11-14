package com.gatelms.service;

import com.gatelms.dto.StatsDtos.ReadinessResponse;
import com.gatelms.dto.StatsDtos.SubjectReadinessDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

public class ReadinessCalculationEngine {

    public record TopicData(
        String id,
        int masteryLevel,
        boolean revised
    ) {}

    public record SubjectData(
        String id,
        String name,
        double weight,
        List<TopicData> topics
    ) {}

    public record QuestionVerdict(
        UUID questionId,
        String subjectId,
        String topicId,
        boolean isCorrect
    ) {}

    public static ReadinessResponse compute(
        List<SubjectData> subjects,
        List<QuestionVerdict> verdicts
    ) {
        List<SubjectReadinessDto> subjectDtos = new ArrayList<>();
        double sumWeightedR = 0.0;
        double sumWeight = 0.0;

        for (SubjectData subj : subjects) {
            List<TopicData> sTopics = subj.topics() != null ? subj.topics() : Collections.emptyList();
            int n_s = sTopics.size();

            double m_s;
            double c_s;
            if (n_s > 0) {
                double sumMasteryLevels = sTopics.stream().mapToInt(TopicData::masteryLevel).sum();
                m_s = sumMasteryLevels / (5.0 * n_s);
                long revisedCount = sTopics.stream().filter(TopicData::revised).count();
                c_s = (double) revisedCount / (double) n_s;
            } else {
                m_s = 0.0;
                c_s = 0.0;
            }

            List<QuestionVerdict> sVerdicts = verdicts.stream()
                .filter(v -> Objects.equals(v.subjectId(), subj.id()))
                .toList();

            double a_s;
            if (!sVerdicts.isEmpty()) {
                long correctCount = sVerdicts.stream().filter(QuestionVerdict::isCorrect).count();
                a_s = (double) correctCount / (double) sVerdicts.size();
            } else {
                a_s = 0.0;
            }

            double r_s = (0.5 * m_s) + (0.3 * a_s) + (0.2 * c_s);
            double sWeight = subj.weight();

            sumWeightedR += sWeight * r_s;
            sumWeight += sWeight;

            double roundM = BigDecimal.valueOf(m_s).setScale(4, RoundingMode.HALF_UP).doubleValue();
            double roundA = BigDecimal.valueOf(a_s).setScale(4, RoundingMode.HALF_UP).doubleValue();
            double roundC = BigDecimal.valueOf(c_s).setScale(4, RoundingMode.HALF_UP).doubleValue();
            double roundR = BigDecimal.valueOf(r_s * 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue();

            subjectDtos.add(new SubjectReadinessDto(
                subj.id(),
                subj.name(),
                sWeight,
                roundM,
                roundA,
                roundC,
                roundR
            ));
        }

        int overall = sumWeight > 0.0
            ? Math.max(0, Math.min(100, (int) Math.round(100.0 * sumWeightedR / sumWeight)))
            : 0;

        return new ReadinessResponse(overall, subjectDtos, Instant.now());
    }
}

package com.gatelms.service;

import com.gatelms.dto.StatsDtos.HeatmapResponse;
import com.gatelms.dto.StatsDtos.ReadinessResponse;
import com.gatelms.dto.StatsDtos.SubjectHeatmapDto;
import com.gatelms.dto.StatsDtos.TopicHeatmapDto;
import com.gatelms.entity.Subject;
import com.gatelms.entity.Topic;
import com.gatelms.entity.TopicMastery;
import com.gatelms.repository.AttemptAnswerRepository;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.repository.TopicMasteryRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadinessService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final CurrentUserProvider currentUserProvider;

    public ReadinessService(
        SubjectRepository subjectRepository,
        TopicRepository topicRepository,
        TopicMasteryRepository topicMasteryRepository,
        AttemptAnswerRepository attemptAnswerRepository,
        CurrentUserProvider currentUserProvider
    ) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ReadinessResponse getReadiness() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<Subject> subjects = subjectRepository.findAll();
        List<Topic> topics = topicRepository.findAll();
        Map<String, TopicMastery> masteries = topicMasteryRepository.findByUserId(currentUserId).stream()
            .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm, (a, b) -> a));

        List<AttemptAnswerRepository.QuestionVerdictProjection> projections = attemptAnswerRepository.findLatestDistinctVerdicts(currentUserId);
        Map<String, List<Topic>> topicsBySubject = topics.stream()
            .collect(Collectors.groupingBy(t -> t.getSubject() != null ? t.getSubject().getId() : ""));

        List<ReadinessCalculationEngine.SubjectData> subjectDataList = subjects.stream().map(s -> {
            List<Topic> sTopics = topicsBySubject.getOrDefault(s.getId(), Collections.emptyList());
            List<ReadinessCalculationEngine.TopicData> topicDatas = sTopics.stream().map(t -> {
                TopicMastery tm = masteries.get(t.getId());
                return new ReadinessCalculationEngine.TopicData(
                    t.getId(),
                    tm != null ? tm.getMasteryLevel() : 0,
                    tm != null && tm.getLastRevisedAt() != null
                );
            }).toList();

            return new ReadinessCalculationEngine.SubjectData(s.getId(), s.getName(), s.getAvgWeightage(), topicDatas);
        }).toList();

        List<ReadinessCalculationEngine.QuestionVerdict> verdictList = projections.stream().map(p ->
            new ReadinessCalculationEngine.QuestionVerdict(p.getQuestionId(), p.getSubjectId(), p.getTopicId(), p.getIsCorrect())
        ).toList();

        return ReadinessCalculationEngine.compute(subjectDataList, verdictList);
    }

    @Transactional(readOnly = true)
    public int getOverallReadinessScore() {
        try {
            return getReadiness().readiness();
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmap() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<Subject> subjects = subjectRepository.findAll();
        List<Topic> topics = topicRepository.findAll();
        Map<String, TopicMastery> masteries = topicMasteryRepository.findByUserId(currentUserId).stream()
            .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm, (a, b) -> a));
        List<AttemptAnswerRepository.QuestionVerdictProjection> projections = attemptAnswerRepository.findLatestDistinctVerdicts(currentUserId);

        Map<String, List<Topic>> topicsBySubject = topics.stream()
            .collect(Collectors.groupingBy(t -> t.getSubject() != null ? t.getSubject().getId() : ""));
        Instant now = Instant.now();

        List<SubjectHeatmapDto> subjectDtos = subjects.stream().map(s -> {
            List<Topic> sTopics = topicsBySubject.getOrDefault(s.getId(), Collections.emptyList());
            List<TopicHeatmapDto> topicHeatmapDtos = sTopics.stream().map(t -> {
                List<AttemptAnswerRepository.QuestionVerdictProjection> tVerdicts = projections.stream()
                    .filter(p -> Objects.equals(p.getTopicId(), t.getId())).toList();
                int answered = tVerdicts.size();
                int correct = (int) tVerdicts.stream().filter(AttemptAnswerRepository.QuestionVerdictProjection::getIsCorrect).count();
                TopicMastery tm = masteries.get(t.getId());

                boolean isDue;
                if (tm != null && tm.getNextRevisionDue() != null) {
                    isDue = !tm.getNextRevisionDue().isAfter(now);
                } else {
                    isDue = tm != null && tm.getLastRevisedAt() != null;
                }

                return new TopicHeatmapDto(
                    t.getId(),
                    t.getName(),
                    tm != null ? tm.getMasteryLevel() : 0,
                    isDue,
                    answered,
                    correct
                );
            }).toList();

            return new SubjectHeatmapDto(s.getId(), s.getName(), topicHeatmapDtos);
        }).toList();

        return new HeatmapResponse(subjectDtos);
    }
}

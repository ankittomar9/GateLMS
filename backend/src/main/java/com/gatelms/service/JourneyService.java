package com.gatelms.service;

import com.gatelms.anatomy.repository.QuestionAnatomyRepository;
import com.gatelms.anatomy.repository.TopicPrimerRepository;
import com.gatelms.dto.*;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionProgress;
import com.gatelms.entity.Subject;
import com.gatelms.entity.Topic;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JourneyService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final TopicPrimerRepository topicPrimerRepository;
    private final QuestionAnatomyRepository questionAnatomyRepository;
    private final ReadinessService readinessService;
    private final CurrentUserProvider currentUserProvider;

    public JourneyService(
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            QuestionProgressRepository questionProgressRepository,
            TopicMasteryRepository topicMasteryRepository,
            TopicPrimerRepository topicPrimerRepository,
            QuestionAnatomyRepository questionAnatomyRepository,
            ReadinessService readinessService,
            CurrentUserProvider currentUserProvider
    ) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.topicPrimerRepository = topicPrimerRepository;
        this.questionAnatomyRepository = questionAnatomyRepository;
        this.readinessService = readinessService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<JourneyOverviewSubjectDto> getOverview() {
        UUID userId = currentUserProvider.getCurrentUserId();
        List<Subject> subjects = subjectRepository.findAll();
        List<Topic> topics = topicRepository.findAll();
        Map<String, List<Topic>> topicsBySubject = topics.stream()
                .collect(Collectors.groupingBy(t -> t.getSubject() != null ? t.getSubject().getId() : ""));

        Map<String, Long> visibleBySubject = questionRepository.countVisibleQuestionsGroupedBySubject().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        Map<String, Long> visibleByTopic = questionRepository.countVisibleQuestionsGroupedByTopic().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        Map<String, Long> solvedByTopic = questionProgressRepository.countSolvedByTopicForUser(userId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        Map<String, Long> solvedBySubject = questionProgressRepository.countSolvedBySubjectForUser(userId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        ReadinessResponse readinessResp = readinessService.getReadiness();
        Map<String, Double> readinessMap = readinessResp.perSubject().stream()
                .collect(Collectors.toMap(SubjectReadinessDto::subjectId, SubjectReadinessDto::readiness));

        return subjects.stream().map(s -> {
            List<Topic> sTopics = topicsBySubject.getOrDefault(s.getId(), List.of());
            int topicsTotal = sTopics.size();
            int topicsTouched = (int) sTopics.stream().filter(t -> solvedByTopic.getOrDefault(t.getId(), 0L) > 0L).count();
            int topicsCompleted = (int) sTopics.stream().filter(t -> {
                long vis = visibleByTopic.getOrDefault(t.getId(), 0L);
                long sol = solvedByTopic.getOrDefault(t.getId(), 0L);
                return vis > 0L && sol >= vis;
            }).count();

            long qVisible = visibleBySubject.getOrDefault(s.getId(), 0L);
            long qSolved = solvedBySubject.getOrDefault(s.getId(), 0L);
            double readinessPct = readinessMap.getOrDefault(s.getId(), 0.0);

            return new JourneyOverviewSubjectDto(
                    s.getId(),
                    s.getName(),
                    s.getCluster(),
                    s.getPriorityTier(),
                    s.getAvgWeightage(),
                    s.getStandardBookRef(),
                    qVisible,
                    qSolved,
                    topicsTotal,
                    topicsTouched,
                    topicsCompleted,
                    readinessPct
            );
        }).sorted(Comparator.comparing(JourneyOverviewSubjectDto::priorityTier)
                .thenComparing(JourneyOverviewSubjectDto::subjectId))
        .toList();
    }

    @Transactional(readOnly = true)
    public List<JourneyTopicSummaryDto> getSubjectTopics(String subjectId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Subject subject = subjectRepository.findById(subjectId)
                .orElseGet(() -> subjectRepository.findBySlug(subjectId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found: " + subjectId)));

        List<Topic> topics = topicRepository.findBySubjectIdOrSlugOrderBySequenceAscNameAsc(subject.getId());
        if (topics.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> visibleByTopic = questionRepository.countVisibleQuestionsGroupedByTopic().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        Map<String, Long> solvedByTopic = questionProgressRepository.countSolvedByTopicForUser(userId).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
        Map<String, com.gatelms.entity.TopicMastery> masteries = topicMasteryRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm));

        List<String> topicIds = topics.stream().map(Topic::getId).toList();
        Map<String, com.gatelms.anatomy.entity.TopicPrimer> primerMap = topicPrimerRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.toMap(
                        p -> p.getTopic() != null ? p.getTopic().getId() : "",
                        p -> p,
                        (existing, replacement) -> existing
                ));

        return topics.stream().map(t -> {
            long qVis = visibleByTopic.getOrDefault(t.getId(), 0L);
            long qSol = solvedByTopic.getOrDefault(t.getId(), 0L);
            boolean isCompleted = qVis > 0L && qSol >= qVis;

            var primer = primerMap.get(t.getId());
            String primerStatus = primer != null ? primer.getStatus().name() : null;
            var mastery = masteries.get(t.getId());
            int masteryLevel = mastery != null ? mastery.getMasteryLevel() : 0;

            return new JourneyTopicSummaryDto(
                    t.getId(),
                    t.getName(),
                    t.getSequence(),
                    primerStatus,
                    qVis,
                    qSol,
                    masteryLevel,
                    isCompleted
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public JourneyContinueDto getContinue() {
        UUID userId = currentUserProvider.getCurrentUserId();
        var latestProgressOpt = questionProgressRepository.findFirstByIdUserIdAndLastAttemptedAtIsNotNullOrderByLastAttemptedAtDesc(userId);
        if (latestProgressOpt.isEmpty()) {
            return new JourneyContinueDto(null, null, null);
        }

        QuestionProgress qp = latestProgressOpt.get();
        Question question = questionRepository.findById(qp.getId().getQuestionId()).orElse(null);
        return new JourneyContinueDto(
                question != null && question.getTopic() != null ? question.getTopic().getId() : null,
                question != null && question.getSubject() != null ? question.getSubject().getId() : null,
                question != null ? question.getId() : null
        );
    }

    @Transactional(readOnly = true)
    public SubjectNotebookDto getSubjectNotebook(String subjectId, boolean starredOnly, String topicId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        Subject subject = subjectRepository.findById(subjectId)
                .orElseGet(() -> subjectRepository.findAll().stream()
                        .filter(s -> subjectId.equals(s.getSlug()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found: " + subjectId)));

        List<Topic> allTopics = topicRepository.findBySubjectIdOrSlugOrderBySequenceAscNameAsc(subject.getId());
        List<Topic> filteredTopics = (topicId != null && !topicId.isBlank())
                ? allTopics.stream().filter(t -> topicId.equals(t.getId()) || topicId.equals(t.getSlug())).toList()
                : allTopics;

        List<Object[]> rows = questionProgressRepository.findSolvedQuestionsForSubject(userId, subject.getId(), starredOnly);
        List<UUID> questionIds = rows.stream().map(r -> ((Question) r[1]).getId()).filter(Objects::nonNull).toList();
        Set<UUID> anatomyAvailableIds = questionIds.isEmpty()
                ? Collections.emptySet()
                : questionAnatomyRepository.findQuestionIdsWithValidAnatomy(questionIds);

        Map<String, List<Object[]>> rowsByTopic = rows.stream()
                .collect(Collectors.groupingBy(r -> {
                    Question q = (Question) r[1];
                    return q.getTopic() != null ? q.getTopic().getId() : "";
                }));

        List<TopicNotebookGroupDto> topicGroups = new ArrayList<>();
        for (Topic topic : filteredTopics) {
            List<Object[]> topicRows = rowsByTopic.getOrDefault(topic.getId(), List.of());
            if (topicRows.isEmpty() && topicId == null) {
                continue;
            }

            List<NotebookEntryDto> entries = topicRows.stream().map(row -> {
                QuestionProgress qp = (QuestionProgress) row[0];
                Question q = (Question) row[1];
                String stmt = q.getStatement();
                String snippet = stmt != null && stmt.length() > 200 ? stmt.substring(0, 197) + "..." : stmt;

                return new NotebookEntryDto(
                        q.getId(),
                        q.getYear(),
                        q.getSource(),
                        snippet,
                        qp.getLastAttemptedAt(),
                        qp.isBookmarked(),
                        anatomyAvailableIds.contains(q.getId())
                );
            }).toList();

            topicGroups.add(new TopicNotebookGroupDto(
                    topic.getId(),
                    topic.getName(),
                    topic.getSequence(),
                    entries
            ));
        }

        int totalEntries = topicGroups.stream().mapToInt(tg -> tg.entries().size()).sum();

        return new SubjectNotebookDto(
                subject.getId(),
                subject.getName(),
                topicGroups,
                totalEntries
        );
    }
}

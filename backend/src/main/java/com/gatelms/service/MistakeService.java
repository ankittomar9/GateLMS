package com.gatelms.service;

import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.dto.*;
import com.gatelms.entity.*;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.repository.AttemptAnswerRepository;
import com.gatelms.repository.MistakeLogRepository;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.TopicMasteryRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MistakeService {
    private static final Logger logger = LoggerFactory.getLogger(MistakeService.class);

    private final MistakeLogRepository mistakeLogRepository;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TopicRepository topicRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PlanProgressService planProgressService;
    private final AchievementService achievementService;
    private final QuestionConceptTagRepository questionConceptTagRepository;

    private static final List<Long> REVIEW_INTERVALS = List.of(1L, 3L, 7L, 14L, 30L, 60L);

    public MistakeService(
        MistakeLogRepository mistakeLogRepository,
        QuestionRepository questionRepository,
        AttemptAnswerRepository attemptAnswerRepository,
        TopicRepository topicRepository,
        TopicMasteryRepository topicMasteryRepository,
        CurrentUserProvider currentUserProvider,
        @Autowired(required = false) PlanProgressService planProgressService,
        @Autowired(required = false) AchievementService achievementService,
        @Autowired(required = false) QuestionConceptTagRepository questionConceptTagRepository
    ) {
        this.mistakeLogRepository = mistakeLogRepository;
        this.questionRepository = questionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.topicRepository = topicRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.currentUserProvider = currentUserProvider;
        this.planProgressService = planProgressService;
        this.achievementService = achievementService;
        this.questionConceptTagRepository = questionConceptTagRepository;
    }

    public MistakeDto createMistake(MistakeCreateRequest request) {
        Question question = questionRepository.findById(request.questionId()).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + request.questionId())
        );

        MistakeErrorCategory category;
        try {
            category = MistakeErrorCategory.valueOf(request.errorCategory().trim().toUpperCase());
        } catch (Exception e) {
            throw new ValidationException(
                "Invalid errorCategory: " + request.errorCategory(),
                Map.of("errorCategory", "Must be one of CONCEPTUAL, CALCULATION, MSQ_TRAP, MISREAD")
            );
        }

        if (request.correctiveTakeaway() == null || request.correctiveTakeaway().isBlank()) {
            throw new ValidationException(
                "Corrective takeaway is required",
                Map.of("correctiveTakeaway", "Must not be blank")
            );
        }

        AttemptAnswer attemptAnswer = null;
        if (request.attemptAnswerId() != null) {
            attemptAnswer = attemptAnswerRepository.findById(request.attemptAnswerId()).orElse(null);
        }

        Instant now = Instant.now();
        Instant nextReview = now.plus(1, ChronoUnit.DAYS);
        UUID userId = currentUserProvider.getCurrentUserId();

        MistakeLog mistake = new MistakeLog();
        mistake.setUserId(userId);
        mistake.setQuestion(question);
        mistake.setAttemptAnswer(attemptAnswer);
        mistake.setErrorCategory(category);
        mistake.setDescription(request.description() != null ? request.description().trim() : null);
        mistake.setCorrectiveTakeaway(request.correctiveTakeaway().trim());
        mistake.setLoggedAt(now);
        mistake.setReviewCount(0);
        mistake.setNextReviewAt(nextReview);
        mistake.setResolved(false);

        MistakeLog saved = mistakeLogRepository.save(mistake);
        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<MistakeDto> getMistakes(Boolean resolved, boolean dueOnly, Pageable pageable) {
        Instant now = Instant.now();
        UUID userId = currentUserProvider.getCurrentUserId();
        Page<MistakeLog> page = mistakeLogRepository.findFiltered(userId, resolved, dueOnly, now, pageable);

        List<UUID> questionIds = page.getContent().stream()
            .map(m -> m.getQuestion() != null ? m.getQuestion().getId() : null)
            .filter(Objects::nonNull)
            .toList();

        Map<UUID, List<String>> tagMap = questionConceptTagRepository != null
            ? questionConceptTagRepository.findSlugsGroupedByQuestionIds(questionIds)
            : Collections.emptyMap();

        return page.map(m -> {
            UUID qId = m.getQuestion() != null ? m.getQuestion().getId() : null;
            return toDto(m, tagMap.getOrDefault(qId, Collections.emptyList()));
        });
    }

    public MistakeDto reviewMistake(UUID id, MistakeReviewRequest request) {
        MistakeLog mistake = mistakeLogRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Mistake log not found with id: " + id)
        );

        if (request.resolved() != null) {
            mistake.setResolved(request.resolved());
        }

        int newCount = mistake.getReviewCount() + 1;
        mistake.setReviewCount(newCount);

        long intervalDays = (newCount <= REVIEW_INTERVALS.size())
            ? REVIEW_INTERVALS.get(newCount - 1)
            : REVIEW_INTERVALS.get(REVIEW_INTERVALS.size() - 1);

        mistake.setNextReviewAt(Instant.now().plus(intervalDays, ChronoUnit.DAYS));
        MistakeLog saved = mistakeLogRepository.save(mistake);

        if (mistake.getQuestion() != null && mistake.getQuestion().getTopic() != null) {
            String topicId = mistake.getQuestion().getTopic().getId();
            if (topicId != null && planProgressService != null) {
                try {
                    planProgressService.onQuestionsAnswered(
                        LocalDate.now(),
                        topicId,
                        1,
                        5,
                        currentUserProvider.getCurrentUserId()
                    );
                } catch (Exception e) {
                    logger.warn("Plan progress hook failure ignored during mistake review: {}", e.getMessage(), e);
                }
            }
        }

        if (achievementService != null) {
            try {
                achievementService.checkOnMistakeReviewed();
            } catch (Exception e) {
                logger.warn("Achievement check failed in mistake review: {}", e.getMessage(), e);
            }
        }

        return toDto(saved, null);
    }

    @Transactional(readOnly = true)
    public List<TopicMasteryStatsDto> getMasteryStats() {
        List<Topic> topics = topicRepository.findAll();
        Instant now = Instant.now();
        UUID userId = currentUserProvider.getCurrentUserId();
        List<Object[]> dueCountsRaw = mistakeLogRepository.countDueReviewsByTopic(userId, now);
        Map<String, Long> dueCountMap = new HashMap<>();
        for (Object[] row : dueCountsRaw) {
            String topicId = (String) row[0];
            Long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            if (topicId != null) dueCountMap.put(topicId, cnt);
        }

        Map<String, TopicMastery> masteryMap = topicMasteryRepository.findAllByIdUserId(userId).stream()
            .collect(Collectors.toMap(tm -> tm.getId().getTopicId(), tm -> tm, (a, b) -> a));

        return topics.stream().map(t -> new TopicMasteryStatsDto(
            t.getId(),
            t.getName(),
            t.getSubject() != null ? t.getSubject().getId() : "",
            masteryMap.containsKey(t.getId()) ? masteryMap.get(t.getId()).getMasteryLevel() : 0,
            dueCountMap.getOrDefault(t.getId(), 0L)
        )).toList();
    }

    public MistakeDto toDto(MistakeLog log, List<String> conceptTags) {
        Question q = log.getQuestion();
        List<String> tags = conceptTags != null ? conceptTags
            : (questionConceptTagRepository != null && q != null && q.getId() != null
                ? questionConceptTagRepository.findSlugsByQuestionId(q.getId()) : Collections.emptyList());

        return new MistakeDto(
            log.getId(),
            q != null ? q.getId() : null,
            q != null ? q.getExternalId() : null,
            q != null ? q.getStatement() : "",
            log.getAttemptAnswer() != null ? log.getAttemptAnswer().getId() : null,
            log.getErrorCategory(),
            log.getDescription(),
            log.getCorrectiveTakeaway(),
            log.getLoggedAt(),
            log.getReviewCount(),
            log.getNextReviewAt(),
            log.isResolved(),
            tags,
            q != null && q.getTopic() != null ? q.getTopic().getId() : null
        );
    }
}

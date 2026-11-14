package com.gatelms.service;

import com.gatelms.dto.AchievementDto;
import com.gatelms.entity.Achievement;
import com.gatelms.entity.AchievementCode;
import com.gatelms.entity.NotificationType;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final MistakeLogRepository mistakeLogRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;

    public AchievementService(
            AchievementRepository achievementRepository,
            NotificationService notificationService,
            CurrentUserProvider currentUserProvider,
            @Autowired(required = false) AttemptAnswerRepository attemptAnswerRepository,
            @Autowired(required = false) MistakeLogRepository mistakeLogRepository,
            @Autowired(required = false) QuestionRepository questionRepository,
            @Autowired(required = false) TopicRepository topicRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.notificationService = notificationService;
        this.currentUserProvider = currentUserProvider;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.mistakeLogRepository = mistakeLogRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> getAllAchievements() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Map<String, Instant> earnedMap = achievementRepository.findAllByUserIdOrderByEarnedAtAsc(currentUserId).stream()
                .collect(Collectors.toMap(Achievement::getCode, Achievement::getEarnedAt, (a, b) -> a));

        return Arrays.stream(AchievementCode.values()).map(code -> new AchievementDto(
                null,
                code.name(),
                code.getTitle(),
                code.getDescription(),
                null,
                earnedMap.get(code.name()),
                null,
                null,
                null,
                earnedMap.containsKey(code.name())
        )).toList();
    }

    @Transactional
    public boolean award(AchievementCode code, UUID userId) {
        UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
        if (achievementRepository.existsByUserIdAndCode(currentUserId, code.name())) {
            return false;
        }

        try {
            Achievement achievement = new Achievement(currentUserId, code.name(), Instant.now());
            achievementRepository.save(achievement);
            logger.info("Awarded achievement for user {}: {}", currentUserId, code.name());

            try {
                notificationService.createNotification(
                        NotificationType.ACHIEVEMENT_EARNED,
                        "Achievement Unlocked: " + code.getTitle(),
                        code.getDescription(),
                        "/insights",
                        true,
                        currentUserId
                );
            } catch (Exception e) {
                logger.warn("Failed to create notification for achievement {}: {}", code.name(), e.getMessage());
            }

            return true;
        } catch (DataIntegrityViolationException e) {
            logger.debug("Achievement {} already inserted concurrently: {}", code.name(), e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean award(AchievementCode code) {
        return award(code, null);
    }

    @Transactional
    public void checkOnAnswerSubmitted(UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            award(AchievementCode.FIRST_QUESTION, currentUserId);

            long distinctCount = attemptAnswerRepository != null ? attemptAnswerRepository.countDistinctQuestionId(currentUserId) : 0L;
            if (distinctCount >= 100) award(AchievementCode.QUESTIONS_100, currentUserId);
            if (distinctCount >= 500) award(AchievementCode.QUESTIONS_500, currentUserId);
            if (distinctCount >= 1000) award(AchievementCode.QUESTIONS_1000, currentUserId);
        } catch (Exception e) {
            logger.warn("Error checking achievements on answer submitted: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnAnswerSubmitted() {
        checkOnAnswerSubmitted(null);
    }

    @Transactional
    public void checkOnMockSubmitted(double score, double maxScore, UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            award(AchievementCode.FIRST_MOCK, currentUserId);

            if (maxScore > 0.0 && (score / maxScore) >= 0.90) {
                award(AchievementCode.MOCK_90PCT, currentUserId);
            }
        } catch (Exception e) {
            logger.warn("Error checking achievements on mock submitted: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnMockSubmitted(double score, double maxScore) {
        checkOnMockSubmitted(score, maxScore, null);
    }

    @Transactional
    public void checkOnStreak(int streak, UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            if (streak >= 3) award(AchievementCode.STREAK_3, currentUserId);
            if (streak >= 7) award(AchievementCode.STREAK_7, currentUserId);
            if (streak >= 30) award(AchievementCode.STREAK_30, currentUserId);
        } catch (Exception e) {
            logger.warn("Error checking achievements on streak: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnStreak(int streak) {
        checkOnStreak(streak, null);
    }

    @Transactional
    public void checkOnTopicMastery(int masteryLevel, UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            if (masteryLevel >= 5) {
                award(AchievementCode.TOPIC_MASTERY_5, currentUserId);
            }
        } catch (Exception e) {
            logger.warn("Error checking achievements on topic mastery: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnTopicMastery(int masteryLevel) {
        checkOnTopicMastery(masteryLevel, null);
    }

    @Transactional
    public void checkOnMistakeReviewed(UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            long totalReviews = mistakeLogRepository != null ? mistakeLogRepository.sumReviewCounts(currentUserId) : 0L;
            if (totalReviews >= 50) {
                award(AchievementCode.REVISIONER_50, currentUserId);
            }
        } catch (Exception e) {
            logger.warn("Error checking achievements on mistake reviewed: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnMistakeReviewed() {
        checkOnMistakeReviewed(null);
    }

    @Transactional
    public void checkOnCurationDecision(UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            long decisions = questionRepository != null ? questionRepository.countCurationDecisions() : 0L;
            if (decisions >= 500) {
                award(AchievementCode.CURATOR_500, currentUserId);
            }
        } catch (Exception e) {
            logger.warn("Error checking achievements on curation decision: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnCurationDecision() {
        checkOnCurationDecision(null);
    }
}

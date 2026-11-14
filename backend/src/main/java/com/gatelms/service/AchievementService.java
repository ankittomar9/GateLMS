package com.gatelms.service;

import com.gatelms.dto.EngagementDtos.AchievementDto;
import com.gatelms.entity.Achievement;
import com.gatelms.entity.AchievementCode;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AchievementService {
    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;
    private final QuestionProgressRepository questionProgressRepository;
    private final MockAttemptRepository mockAttemptRepository;
    private final MistakeLogRepository mistakeLogRepository;
    private final QuestionRepository questionRepository;

    public AchievementService(
        AchievementRepository achievementRepository,
        CurrentUserProvider currentUserProvider,
        @Autowired(required = false) NotificationService notificationService,
        @Autowired(required = false) QuestionProgressRepository questionProgressRepository,
        @Autowired(required = false) MockAttemptRepository mockAttemptRepository,
        @Autowired(required = false) MistakeLogRepository mistakeLogRepository,
        @Autowired(required = false) QuestionRepository questionRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.currentUserProvider = currentUserProvider;
        this.notificationService = notificationService;
        this.questionProgressRepository = questionProgressRepository;
        this.mockAttemptRepository = mockAttemptRepository;
        this.mistakeLogRepository = mistakeLogRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> getAllAchievements() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<Achievement> unlocked = achievementRepository.findByUserId(currentUserId);
        Map<AchievementCode, Instant> unlockedMap = new HashMap<>();
        for (Achievement a : unlocked) {
            unlockedMap.put(a.getCode(), a.getUnlockedAt());
        }

        return Arrays.stream(AchievementCode.values()).map(code -> new AchievementDto(
            code.name(),
            code.getTitle(),
            code.getDescription(),
            code.getTier().name(),
            unlockedMap.containsKey(code),
            unlockedMap.get(code)
        )).toList();
    }

    @Transactional
    public boolean award(AchievementCode code, UUID userId) {
        UUID targetUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
        if (achievementRepository.existsByUserIdAndCode(targetUserId, code)) {
            return false;
        }

        Achievement a = new Achievement();
        a.setUserId(targetUserId);
        a.setCode(code);
        a.setUnlockedAt(Instant.now());
        achievementRepository.save(a);

        logger.info("Achievement unlocked for user {}: {}", targetUserId, code);

        if (notificationService != null) {
            try {
                notificationService.createNotification(
                    targetUserId,
                    com.gatelms.entity.NotificationType.ACHIEVEMENT,
                    "Achievement Unlocked: " + code.getTitle(),
                    code.getDescription(),
                    "/profile"
                );
            } catch (Exception e) {
                logger.warn("Notification dispatch failed for achievement {}: {}", code, e.getMessage());
            }
        }

        return true;
    }

    @Transactional
    public void checkOnAnswerSubmitted(UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            long count = questionProgressRepository != null ? questionProgressRepository.countAttemptedQuestions(currentUserId) : 0L;
            if (count >= 1) award(AchievementCode.FIRST_BLOOD, currentUserId);
            if (count >= 100) award(AchievementCode.CENTURION, currentUserId);
            if (count >= 500) award(AchievementCode.PROBLEM_SLAYER, currentUserId);
        } catch (Exception e) {
            logger.warn("Error checking achievements on answer submitted: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnMockSubmitted(double score, double maxScore, UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            award(AchievementCode.MOCK_INITIATE, currentUserId);

            long completedCount = mockAttemptRepository != null ? mockAttemptRepository.countCompletedMocks(currentUserId) : 0L;
            if (completedCount >= 5) award(AchievementCode.MOCK_WARRIOR, currentUserId);
            if (completedCount >= 20) award(AchievementCode.MOCK_MASTER, currentUserId);

            if (maxScore > 0) {
                double pct = (score / maxScore) * 100.0;
                if (pct >= 80.0) award(AchievementCode.SCORER_80, currentUserId);
                if (pct >= 90.0) award(AchievementCode.SCORER_90, currentUserId);
            }
        } catch (Exception e) {
            logger.warn("Error checking achievements on mock submitted: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnStreak(int streak, UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            if (streak >= 7) award(AchievementCode.STREAK_7, currentUserId);
            if (streak >= 30) award(AchievementCode.STREAK_30, currentUserId);
            if (streak >= 100) award(AchievementCode.STREAK_100, currentUserId);
        } catch (Exception e) {
            logger.warn("Error checking achievements on streak: {}", e.getMessage());
        }
    }

    @Transactional
    public void checkOnTopicMastery(int masteryLevel, UUID userId) {
        try {
            UUID currentUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
            if (masteryLevel >= 5) {
                award(AchievementCode.CONCEPT_DOMINATOR, currentUserId);
            }
        } catch (Exception e) {
            logger.warn("Error checking achievements on topic mastery: {}", e.getMessage());
        }
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
}

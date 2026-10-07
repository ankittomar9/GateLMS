package com.gatelms.service

import com.gatelms.dto.AchievementDto
import com.gatelms.entity.Achievement
import com.gatelms.entity.AchievementCode
import com.gatelms.entity.NotificationType
import com.gatelms.repository.*
import com.gatelms.security.CurrentUserProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val notificationService: NotificationService,
    private val currentUserProvider: CurrentUserProvider,
    @Autowired(required = false)
    private val attemptAnswerRepository: AttemptAnswerRepository? = null,
    @Autowired(required = false)
    private val mistakeLogRepository: MistakeLogRepository? = null,
    @Autowired(required = false)
    private val questionRepository: QuestionRepository? = null,
    @Autowired(required = false)
    private val topicRepository: TopicRepository? = null
) {
    private val logger = LoggerFactory.getLogger(AchievementService::class.java)

    @Transactional(readOnly = true)
    fun getAllAchievements(): List<AchievementDto> {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val earnedMap = achievementRepository.findAllByUserIdOrderByEarnedAtAsc(currentUserId).associate { it.code to it.earnedAt }
        return AchievementCode.values().map { code ->
            AchievementDto(
                code = code.name,
                title = code.title,
                description = code.description,
                earnedAt = earnedMap[code.name]
            )
        }
    }

    @Transactional
    fun award(code: AchievementCode, userId: UUID? = null): Boolean {
        val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
        if (achievementRepository.existsByUserIdAndCode(currentUserId, code.name)) {
            return false
        }

        return try {
            val achievement = Achievement(userId = currentUserId, code = code.name, earnedAt = Instant.now())
            achievementRepository.save(achievement)
            logger.info("Awarded achievement for user {}: {}", currentUserId, code.name)

            // Emit notification
            try {
                notificationService.createNotification(
                    type = NotificationType.ACHIEVEMENT_EARNED,
                    title = "Achievement Unlocked: ${code.title}",
                    body = code.description,
                    link = "/insights",
                    forceDuplicate = true,
                    userId = currentUserId
                )
            } catch (e: Exception) {
                logger.warn("Failed to create notification for achievement {}: {}", code.name, e.message)
            }

            true
        } catch (e: DataIntegrityViolationException) {
            // Idempotent: another thread or concurrent call inserted code
            logger.debug("Achievement {} already inserted concurrently: {}", code.name, e.message)
            false
        }
    }

    @Transactional
    fun checkOnAnswerSubmitted(userId: UUID? = null) {
        try {
            val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
            award(AchievementCode.FIRST_QUESTION, currentUserId)

            val distinctCount = attemptAnswerRepository?.countDistinctQuestionId(currentUserId) ?: 0L
            if (distinctCount >= 100) award(AchievementCode.QUESTIONS_100, currentUserId)
            if (distinctCount >= 500) award(AchievementCode.QUESTIONS_500, currentUserId)
            if (distinctCount >= 1000) award(AchievementCode.QUESTIONS_1000, currentUserId)
        } catch (e: Exception) {
            logger.warn("Error checking achievements on answer submitted: {}", e.message)
        }
    }

    @Transactional
    fun checkOnMockSubmitted(score: Double, maxScore: Double, userId: UUID? = null) {
        try {
            val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
            award(AchievementCode.FIRST_MOCK, currentUserId)

            if (maxScore > 0.0 && (score / maxScore) >= 0.90) {
                award(AchievementCode.MOCK_90PCT, currentUserId)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on mock submitted: {}", e.message)
        }
    }

    @Transactional
    fun checkOnStreak(streak: Int, userId: UUID? = null) {
        try {
            val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
            if (streak >= 3) award(AchievementCode.STREAK_3, currentUserId)
            if (streak >= 7) award(AchievementCode.STREAK_7, currentUserId)
            if (streak >= 30) award(AchievementCode.STREAK_30, currentUserId)
        } catch (e: Exception) {
            logger.warn("Error checking achievements on streak: {}", e.message)
        }
    }

    @Transactional
    fun checkOnTopicMastery(masteryLevel: Int, userId: UUID? = null) {
        try {
            val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
            if (masteryLevel >= 5) {
                award(AchievementCode.TOPIC_MASTERY_5, currentUserId)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on topic mastery: {}", e.message)
        }
    }

    @Transactional
    fun checkOnMistakeReviewed(userId: UUID? = null) {
        try {
            val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
            val totalReviews = mistakeLogRepository?.sumReviewCounts(currentUserId) ?: 0L
            if (totalReviews >= 50) {
                award(AchievementCode.REVISIONER_50, currentUserId)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on mistake reviewed: {}", e.message)
        }
    }

    @Transactional
    fun checkOnCurationDecision(userId: UUID? = null) {
        try {
            val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
            val decisions = questionRepository?.countCurationDecisions() ?: 0L
            if (decisions >= 500) {
                award(AchievementCode.CURATOR_500, currentUserId)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on curation decision: {}", e.message)
        }
    }
}

package com.gatelms.service

import com.gatelms.dto.AchievementDto
import com.gatelms.entity.Achievement
import com.gatelms.entity.AchievementCode
import com.gatelms.entity.NotificationType
import com.gatelms.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val notificationService: NotificationService,
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
        val earnedMap = achievementRepository.findAllByOrderByEarnedAtAsc().associate { it.code to it.earnedAt }
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
    fun award(code: AchievementCode): Boolean {
        if (achievementRepository.existsByCode(code.name)) {
            return false
        }

        return try {
            val achievement = Achievement(code = code.name, earnedAt = Instant.now())
            achievementRepository.save(achievement)
            logger.info("Awarded achievement: {}", code.name)

            // Emit notification
            try {
                notificationService.createNotification(
                    type = NotificationType.ACHIEVEMENT_EARNED,
                    title = "Achievement Unlocked: ${code.title}",
                    body = code.description,
                    link = "/insights",
                    forceDuplicate = true
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
    fun checkOnAnswerSubmitted() {
        try {
            award(AchievementCode.FIRST_QUESTION)

            val distinctCount = attemptAnswerRepository?.countDistinctQuestionId() ?: 0L
            if (distinctCount >= 100) award(AchievementCode.QUESTIONS_100)
            if (distinctCount >= 500) award(AchievementCode.QUESTIONS_500)
            if (distinctCount >= 1000) award(AchievementCode.QUESTIONS_1000)
        } catch (e: Exception) {
            logger.warn("Error checking achievements on answer submitted: {}", e.message)
        }
    }

    @Transactional
    fun checkOnMockSubmitted(score: Double, maxScore: Double) {
        try {
            award(AchievementCode.FIRST_MOCK)

            if (maxScore > 0.0 && (score / maxScore) >= 0.90) {
                award(AchievementCode.MOCK_90PCT)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on mock submitted: {}", e.message)
        }
    }

    @Transactional
    fun checkOnStreak(streak: Int) {
        try {
            if (streak >= 3) award(AchievementCode.STREAK_3)
            if (streak >= 7) award(AchievementCode.STREAK_7)
            if (streak >= 30) award(AchievementCode.STREAK_30)
        } catch (e: Exception) {
            logger.warn("Error checking achievements on streak: {}", e.message)
        }
    }

    @Transactional
    fun checkOnTopicMastery(masteryLevel: Int) {
        try {
            if (masteryLevel >= 5) {
                award(AchievementCode.TOPIC_MASTERY_5)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on topic mastery: {}", e.message)
        }
    }

    @Transactional
    fun checkOnMistakeReviewed() {
        try {
            val totalReviews = mistakeLogRepository?.sumReviewCounts() ?: 0L
            if (totalReviews >= 50) {
                award(AchievementCode.REVISIONER_50)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on mistake reviewed: {}", e.message)
        }
    }

    @Transactional
    fun checkOnCurationDecision() {
        try {
            val decisions = questionRepository?.countCurationDecisions() ?: 0L
            if (decisions >= 500) {
                award(AchievementCode.CURATOR_500)
            }
        } catch (e: Exception) {
            logger.warn("Error checking achievements on curation decision: {}", e.message)
        }
    }
}

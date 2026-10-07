package com.gatelms.service

import com.gatelms.dto.NotificationDto
import com.gatelms.entity.Notification
import com.gatelms.entity.NotificationType
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.repository.NotificationRepository
import com.gatelms.repository.PlanItemRepository
import com.gatelms.repository.UserGoalRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userGoalRepository: UserGoalRepository,
    private val dailyRollupRepository: DailyRollupRepository,
    @Autowired(required = false)
    private val mistakeLogRepository: com.gatelms.repository.MistakeLogRepository? = null,
    @Autowired(required = false)
    private val topicRepository: com.gatelms.repository.TopicRepository? = null,
    @Autowired(required = false)
    private val planItemRepository: PlanItemRepository? = null
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional(readOnly = true)
    fun getNotifications(unreadOnly: Boolean, pageable: Pageable): Page<NotificationDto> {
        val page = if (unreadOnly) {
            notificationRepository.findByReadAtIsNullOrderByCreatedAtDesc(pageable)
        } else {
            notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
        }
        return page.map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(): Long {
        return notificationRepository.countByReadAtIsNull()
    }

    @Transactional(readOnly = true)
    fun getTop10(): List<NotificationDto> {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc().map { it.toDto() }
    }

    @Transactional
    fun markAsRead(id: UUID): NotificationDto {
        val notification = notificationRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found")
        }
        if (notification.readAt == null) {
            notification.readAt = Instant.now()
            notificationRepository.save(notification)
        }
        return notification.toDto()
    }

    @Transactional
    fun markAllAsRead(): Int {
        return notificationRepository.markAllAsRead(Instant.now())
    }

    @Transactional
    fun createNotification(
        type: NotificationType,
        title: String,
        body: String?,
        link: String? = null,
        forceDuplicate: Boolean = false
    ): Notification? {
        val now = Instant.now()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = startOfDay.plus(1, ChronoUnit.DAYS)

        // De-dup rule: at most one notification per (type, date) for system events
        if (!forceDuplicate && type != NotificationType.ACHIEVEMENT_EARNED) {
            val exists = notificationRepository.existsByTypeAndCreatedAtBetween(type, startOfDay, endOfDay)
            if (exists) {
                logger.info("Notification of type {} already produced today. Skipping de-dup.", type)
                return null
            }
        }

        val notification = Notification(
            type = type,
            title = title,
            body = body,
            link = link,
            createdAt = now
        )
        val saved = notificationRepository.save(notification)
        logger.info("Produced notification [{}] id={}", type, saved.id)
        return saved
    }

    @Scheduled(cron = "\${app.notifications.morning-cron:0 0 8 * * *}")
    @Transactional
    fun runMorningNotificationJob() {
        logger.info("Executing morning notification job...")

        // 1. REVISION_DUE check
        try {
            val now = Instant.now()
            val dueMistakes = mistakeLogRepository?.countDueReviews(now) ?: 0L
            val dueTopics = topicRepository?.countDueTopics(now) ?: 0L
            if (dueMistakes + dueTopics > 0) {
                createNotification(
                    type = NotificationType.REVISION_DUE,
                    title = "Revisions Due Today",
                    body = "$dueMistakes mistake reviews and $dueTopics topics due for revision.",
                    link = "/revision"
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to check revision due in morning job: {}", e.message)
        }

        // 2. PLAN_DUE check
        try {
            val today = LocalDate.now()
            val openItems = planItemRepository?.findOpenItemsForDate(today) ?: emptyList()
            if (openItems.isNotEmpty()) {
                val count = openItems.size
                createNotification(
                    type = NotificationType.PLAN_DUE,
                    title = "Daily Study Plan Due",
                    body = "$count study plan items scheduled for today.",
                    link = "/planner"
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to check plan due in morning job: {}", e.message)
        }
    }

    @Scheduled(cron = "\${app.notifications.evening-cron:0 0 20 * * *}")
    @Transactional
    fun runEveningNotificationJob() {
        logger.info("Executing evening notification job...")

        // STREAK_AT_RISK check: fires only when streak > 0 and today's rollup is empty
        try {
            val rollups = dailyRollupRepository.findAllByOrderByActivityDateDesc()
            val activityMap = rollups.associate { it.activityDate to it.questionsAnswered }
            val today = LocalDate.now()
            val todayAnswered = activityMap[today] ?: 0

            val goal = userGoalRepository.findFirstByOrderByUpdatedAtDesc()
            val streakProtection = goal?.streakProtection ?: true

            val streakRes = StreakCalculator.computeStreak(
                activityMap = activityMap,
                today = today,
                streakProtection = streakProtection
            )

            if (streakRes.streak > 0 && todayAnswered == 0) {
                createNotification(
                    type = NotificationType.STREAK_AT_RISK,
                    title = "Streak At Risk!",
                    body = "You have an active streak of ${streakRes.streak} days. Complete a study session today to keep it alive!",
                    link = "/practice"
                )
            }
        } catch (e: Exception) {
            logger.warn("Failed to check streak at risk in evening job: {}", e.message)
        }
    }

    private fun Notification.toDto() = NotificationDto(
        id = id,
        type = type,
        title = title,
        body = body,
        link = link,
        readAt = readAt,
        createdAt = createdAt
    )
}

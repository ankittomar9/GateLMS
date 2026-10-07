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
    private val currentUserProvider: com.gatelms.security.CurrentUserProvider,
    @Autowired(required = false)
    private val mistakeLogRepository: com.gatelms.repository.MistakeLogRepository? = null,
    @Autowired(required = false)
    private val topicMasteryRepository: com.gatelms.repository.TopicMasteryRepository? = null,
    @Autowired(required = false)
    private val planItemRepository: PlanItemRepository? = null,
    @Autowired(required = false)
    private val userRepository: com.gatelms.repository.UserRepository? = null
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional(readOnly = true)
    fun getNotifications(unreadOnly: Boolean, pageable: Pageable): Page<NotificationDto> {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val page = if (unreadOnly) {
            notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(currentUserId, pageable)
        } else {
            notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId, pageable)
        }
        return page.map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(): Long {
        val currentUserId = currentUserProvider.getCurrentUserId()
        return notificationRepository.countByUserIdAndReadAtIsNull(currentUserId)
    }

    @Transactional(readOnly = true)
    fun getTop10(): List<NotificationDto> {
        val currentUserId = currentUserProvider.getCurrentUserId()
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUserId).map { it.toDto() }
    }

    @Transactional
    fun markAsRead(id: UUID): NotificationDto {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val notification = notificationRepository.findByIdAndUserId(id, currentUserId).orElseThrow {
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
        val currentUserId = currentUserProvider.getCurrentUserId()
        return notificationRepository.markAllAsRead(currentUserId, Instant.now())
    }

    @Transactional
    fun createNotification(
        type: NotificationType,
        title: String,
        body: String?,
        link: String? = null,
        forceDuplicate: Boolean = false,
        userId: UUID? = null
    ): Notification? {
        val targetUserId = userId ?: currentUserProvider.getCurrentUserId()
        val now = Instant.now()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = startOfDay.plus(1, ChronoUnit.DAYS)

        // De-dup rule: at most one notification per (type, date) for system events
        if (!forceDuplicate && type != NotificationType.ACHIEVEMENT_EARNED) {
            val exists = notificationRepository.existsByUserIdAndTypeAndCreatedAtBetween(targetUserId, type, startOfDay, endOfDay)
            if (exists) {
                logger.info("Notification of type {} already produced today for user {}. Skipping de-dup.", type, targetUserId)
                return null
            }
        }

        val notification = Notification(
            userId = targetUserId,
            type = type,
            title = title,
            body = body,
            link = link,
            createdAt = now
        )
        val saved = notificationRepository.save(notification)
        logger.info("Produced notification [{}] id={} for user {}", type, saved.id, targetUserId)
        return saved
    }

    @Scheduled(cron = "\${app.notifications.morning-cron:0 0 8 * * *}")
    @Transactional
    fun runMorningNotificationJob() {
        logger.info("Executing morning notification job...")

        val users = userRepository?.findAll() ?: emptyList()
        val now = Instant.now()
        val today = LocalDate.now()

        for (u in users) {
            // 1. REVISION_DUE check
            try {
                val dueMistakes = mistakeLogRepository?.countDueReviews(u.id, now) ?: 0L
                val dueTopics = topicMasteryRepository?.countDueForRevision(u.id, now) ?: 0L
                if (dueMistakes + dueTopics > 0) {
                    createNotification(
                        type = NotificationType.REVISION_DUE,
                        title = "Revisions Due Today",
                        body = "$dueMistakes mistake reviews and $dueTopics topics due for revision.",
                        link = "/revision",
                        userId = u.id
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to check revision due in morning job for user {}: {}", u.id, e.message)
            }

            // 2. PLAN_DUE check
            try {
                val openItems = planItemRepository?.findOpenItemsForDate(today)?.filter { it.plan.userId == u.id } ?: emptyList()
                if (openItems.isNotEmpty()) {
                    val count = openItems.size
                    createNotification(
                        type = NotificationType.PLAN_DUE,
                        title = "Daily Study Plan Due",
                        body = "$count study plan items scheduled for today.",
                        link = "/planner",
                        userId = u.id
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to check plan due in morning job for user {}: {}", u.id, e.message)
            }
        }
    }

    @Scheduled(cron = "\${app.notifications.evening-cron:0 0 20 * * *}")
    @Transactional
    fun runEveningNotificationJob() {
        logger.info("Executing evening notification job...")

        val users = userRepository?.findAll() ?: emptyList()
        val today = LocalDate.now()

        for (u in users) {
            // STREAK_AT_RISK check: fires only when streak > 0 and today's rollup is empty
            try {
                val rollups = dailyRollupRepository.findAllByUserIdOrderByActivityDateDesc(u.id)
                val activityMap = rollups.associate { it.activityDate to it.questionsAnswered }
                val todayAnswered = activityMap[today] ?: 0

                val goal = userGoalRepository.findFirstByUserIdOrderByUpdatedAtDesc(u.id)
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
                        link = "/practice",
                        userId = u.id
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to check streak at risk in evening job for user {}: {}", u.id, e.message)
            }
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

package com.gatelms.notification

import com.gatelms.entity.DailyRollup
import com.gatelms.entity.NotificationType
import com.gatelms.entity.User
import com.gatelms.entity.UserRole
import com.gatelms.integration.BaseIntegrationTest
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.repository.NotificationRepository
import com.gatelms.repository.UserGoalRepository
import com.gatelms.repository.UserRepository
import com.gatelms.service.NotificationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.util.UUID

class StreakAtRiskJobIT : BaseIntegrationTest() {

    @Autowired
    lateinit var notificationService: NotificationService

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var dailyRollupRepository: DailyRollupRepository

    @Autowired
    lateinit var userGoalRepository: UserGoalRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var userA: User
    private lateinit var userB: User
    private lateinit var userC: User

    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()
        dailyRollupRepository.deleteAll()
        userGoalRepository.deleteAll()

        userA = userRepository.findByUsername("streak_user_a").orElseGet {
            userRepository.save(
                User(
                    username = "streak_user_a",
                    passwordHash = passwordEncoder.encode("passA"),
                    displayName = "Streak User A",
                    role = UserRole.STUDENT
                )
            )
        }

        userB = userRepository.findByUsername("streak_user_b").orElseGet {
            userRepository.save(
                User(
                    username = "streak_user_b",
                    passwordHash = passwordEncoder.encode("passB"),
                    displayName = "Streak User B",
                    role = UserRole.STUDENT
                )
            )
        }

        userC = userRepository.findByUsername("streak_user_c").orElseGet {
            userRepository.save(
                User(
                    username = "streak_user_c",
                    passwordHash = passwordEncoder.encode("passC"),
                    displayName = "Streak User C",
                    role = UserRole.STUDENT
                )
            )
        }
    }

    @Test
    fun `1 active streak greater than 0 with no activity today triggers STREAK_AT_RISK notification`() {
        val today = LocalDate.now()
        // User A answered questions yesterday, none today
        dailyRollupRepository.save(
            DailyRollup(userId = userA.id, activityDate = today.minusDays(1), questionsAnswered = 15)
        )

        notificationService.runEveningNotificationJob()

        val notifs = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userA.id, org.springframework.data.domain.Pageable.unpaged()).content
        val streakNotifs = notifs.filter { it.type == NotificationType.STREAK_AT_RISK }

        assertEquals(1, streakNotifs.size, "Expected exactly 1 STREAK_AT_RISK notification for User A")
        val notif = streakNotifs[0]
        assertEquals("Streak At Risk!", notif.title)
        assertTrue(notif.body?.contains("active streak of 1 days") == true)
        assertEquals("/practice", notif.link)
        assertNull(notif.readAt)
    }

    @Test
    fun `2 active streak with activity today is silent`() {
        val today = LocalDate.now()
        // User A answered questions yesterday AND today
        dailyRollupRepository.save(
            DailyRollup(userId = userA.id, activityDate = today.minusDays(1), questionsAnswered = 15)
        )
        dailyRollupRepository.save(
            DailyRollup(userId = userA.id, activityDate = today, questionsAnswered = 10)
        )

        notificationService.runEveningNotificationJob()

        val notifs = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userA.id, org.springframework.data.domain.Pageable.unpaged()).content
        val streakNotifs = notifs.filter { it.type == NotificationType.STREAK_AT_RISK }

        assertEquals(0, streakNotifs.size, "Expected 0 notifications because user practiced today")
    }

    @Test
    fun `3 zero streak is silent`() {
        // User A has no rollups at all
        notificationService.runEveningNotificationJob()

        val notifs = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userA.id, org.springframework.data.domain.Pageable.unpaged()).content
        val streakNotifs = notifs.filter { it.type == NotificationType.STREAK_AT_RISK }

        assertEquals(0, streakNotifs.size, "Expected 0 notifications when streak is 0")
    }

    @Test
    fun `4 deduplication prevents duplicate dispatch on same date`() {
        val today = LocalDate.now()
        dailyRollupRepository.save(
            DailyRollup(userId = userA.id, activityDate = today.minusDays(1), questionsAnswered = 20)
        )

        // First run triggers notification
        notificationService.runEveningNotificationJob()

        // Second run on the same date should be de-duplicated
        notificationService.runEveningNotificationJob()

        val notifs = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userA.id, org.springframework.data.domain.Pageable.unpaged()).content
        val streakNotifs = notifs.filter { it.type == NotificationType.STREAK_AT_RISK }

        assertEquals(1, streakNotifs.size, "Expected de-duplication to prevent duplicate notification on same date")
    }

    @Test
    fun `5 multi-user fan out isolates streak evaluation`() {
        val today = LocalDate.now()

        // User A: streak 2, no practice today -> triggers notification
        dailyRollupRepository.save(
            DailyRollup(userId = userA.id, activityDate = today.minusDays(2), questionsAnswered = 10)
        )
        dailyRollupRepository.save(
            DailyRollup(userId = userA.id, activityDate = today.minusDays(1), questionsAnswered = 15)
        )

        // User B: streak 1, practiced today -> silent
        dailyRollupRepository.save(
            DailyRollup(userId = userB.id, activityDate = today.minusDays(1), questionsAnswered = 12)
        )
        dailyRollupRepository.save(
            DailyRollup(userId = userB.id, activityDate = today, questionsAnswered = 5)
        )

        // User C: broken streak (gap > 1 day) -> silent
        dailyRollupRepository.save(
            DailyRollup(userId = userC.id, activityDate = today.minusDays(5), questionsAnswered = 8)
        )

        notificationService.runEveningNotificationJob()

        val notifsA = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userA.id, org.springframework.data.domain.Pageable.unpaged()).content
            .filter { it.type == NotificationType.STREAK_AT_RISK }
        val notifsB = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userB.id, org.springframework.data.domain.Pageable.unpaged()).content
            .filter { it.type == NotificationType.STREAK_AT_RISK }
        val notifsC = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userC.id, org.springframework.data.domain.Pageable.unpaged()).content
            .filter { it.type == NotificationType.STREAK_AT_RISK }

        assertEquals(1, notifsA.size, "User A should receive STREAK_AT_RISK notification")
        assertTrue(notifsA[0].body?.contains("active streak of 2 days") == true)
        assertEquals(0, notifsB.size, "User B practiced today and should receive no notification")
        assertEquals(0, notifsC.size, "User C has broken streak and should receive no notification")
    }
}

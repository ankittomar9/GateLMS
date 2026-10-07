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
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.LocalDate

@TestPropertySource(properties = ["app.notifications.evening-cron=* * * * * *"])
class StreakAtRiskJobSchedulingIT : BaseIntegrationTest() {

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

    private lateinit var scheduledUser: User

    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()
        dailyRollupRepository.deleteAll()
        userGoalRepository.deleteAll()

        scheduledUser = userRepository.findByUsername("streak_scheduled_user").orElseGet {
            userRepository.save(
                User(
                    username = "streak_scheduled_user",
                    passwordHash = passwordEncoder.encode("passScheduled"),
                    displayName = "Streak Scheduled User",
                    role = UserRole.STUDENT
                )
            )
        }
    }

    @Test
    fun `evening cron triggers automatically via Spring task scheduler producing STREAK_AT_RISK notification`() {
        val today = LocalDate.now()
        // Save rollup yesterday with 10 questions answered, none today -> streak = 1 at risk
        dailyRollupRepository.save(
            DailyRollup(userId = scheduledUser.id, activityDate = today.minusDays(1), questionsAnswered = 10)
        )

        // C10: Do NOT invoke runEveningNotificationJob() manually.
        // Spring's task scheduler automatically invokes it per cron "* * * * * *".
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(200))
            .untilAsserted {
                val notifs = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(
                    scheduledUser.id,
                    org.springframework.data.domain.Pageable.unpaged()
                ).content
                val streakNotifs = notifs.filter { it.type == NotificationType.STREAK_AT_RISK }
                assertEquals(1, streakNotifs.size, "Expected scheduled cron to produce 1 STREAK_AT_RISK notification")
                val notif = streakNotifs[0]
                assertEquals("Streak At Risk!", notif.title)
                assertTrue(notif.body?.contains("active streak of 1 days") == true)
                assertEquals("/practice", notif.link)
            }
    }
}

package com.gatelms.integration

import com.gatelms.dto.MarkAllReadResponse
import com.gatelms.dto.NotificationDto
import com.gatelms.dto.UnreadCountResponse
import com.gatelms.entity.Notification
import com.gatelms.entity.NotificationType
import com.gatelms.entity.PlanItem
import com.gatelms.entity.StudyPlan
import com.gatelms.repository.*
import com.gatelms.service.NotificationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class NotificationApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var notificationService: NotificationService

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var topicMasteryRepository: com.gatelms.repository.TopicMasteryRepository

    @Autowired
    lateinit var studyPlanRepository: StudyPlanRepository

    @Autowired
    lateinit var planItemRepository: PlanItemRepository

    @Autowired
    lateinit var mistakeLogRepository: MistakeLogRepository

    private val operatorId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun cleanUp() {
        notificationRepository.deleteAll()
        planItemRepository.deleteAll()
        studyPlanRepository.deleteAll()
        mistakeLogRepository.deleteAll()
        resetTopics()
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        resetTopics()
    }

    private fun resetTopics() {
        topicMasteryRepository.deleteAll()
    }

    @Test
    fun `1 producer de-dup per type and date`() {
        // First produce REVISION_DUE
        val notif1 = notificationService.createNotification(
            type = NotificationType.REVISION_DUE,
            title = "Revisions Due",
            body = "1 mistake review due"
        )
        assertNotNull(notif1)

        // Attempt second produce REVISION_DUE on same day: should de-dup
        val notif2 = notificationService.createNotification(
            type = NotificationType.REVISION_DUE,
            title = "Revisions Due",
            body = "2 mistake reviews due"
        )
        assertNull(notif2, "Second notification of same type on same day must be de-duped")

        val total = notificationRepository.findAll().count { it.type == NotificationType.REVISION_DUE }
        assertEquals(1, total, "Database must hold exactly 1 notification of that type for today")
    }

    @Test
    fun `2 read and read-all idempotence`() {
        val n1 = notificationRepository.save(
            Notification(type = NotificationType.PLAN_DUE, title = "Plan 1", body = "B1")
        )
        val n2 = notificationRepository.save(
            Notification(type = NotificationType.PLAN_DUE, title = "Plan 2", body = "B2")
        )
        val n3 = notificationRepository.save(
            Notification(type = NotificationType.PLAN_DUE, title = "Plan 3", body = "B3")
        )

        // Mark n1 as read via PATCH
        val patchResp = restTemplate.exchange(
            "/api/v1/notifications/${n1.id}/read",
            HttpMethod.PATCH,
            HttpEntity.EMPTY,
            NotificationDto::class.java
        )
        assertEquals(HttpStatus.OK, patchResp.statusCode)
        assertNotNull(patchResp.body?.readAt)

        // Re-mark n1 as read: idempotent 200
        val patchResp2 = restTemplate.exchange(
            "/api/v1/notifications/${n1.id}/read",
            HttpMethod.PATCH,
            HttpEntity.EMPTY,
            NotificationDto::class.java
        )
        assertEquals(HttpStatus.OK, patchResp2.statusCode)

        // Mark all as read via POST
        val readAllResp = restTemplate.postForEntity(
            "/api/v1/notifications/read-all",
            null,
            MarkAllReadResponse::class.java
        )
        assertEquals(HttpStatus.OK, readAllResp.statusCode)
        assertEquals(2, readAllResp.body?.count, "Remaining 2 notifications marked read")

        // Re-trigger mark all: should return 0
        val readAllResp2 = restTemplate.postForEntity(
            "/api/v1/notifications/read-all",
            null,
            MarkAllReadResponse::class.java
        )
        assertEquals(HttpStatus.OK, readAllResp2.statusCode)
        assertEquals(0, readAllResp2.body?.count, "0 unread left to mark")
    }

    @Test
    fun `3 unread filter and unread count endpoint`() {
        // Save 2 unread, 1 read
        notificationRepository.save(
            Notification(type = NotificationType.PLAN_DUE, title = "Unread 1", body = "B1")
        )
        notificationRepository.save(
            Notification(type = NotificationType.PLAN_DUE, title = "Unread 2", body = "B2")
        )
        notificationRepository.save(
            Notification(type = NotificationType.PLAN_DUE, title = "Read 1", body = "B3", readAt = Instant.now())
        )

        // Test unread count
        val countResp = restTemplate.getForEntity(
            "/api/v1/notifications/unread-count",
            UnreadCountResponse::class.java
        )
        assertEquals(HttpStatus.OK, countResp.statusCode)
        assertEquals(2L, countResp.body?.count)

        // Test unread filter = true
        val unreadListResp = restTemplate.exchange(
            "/api/v1/notifications?unread=true",
            HttpMethod.GET,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.OK, unreadListResp.statusCode)
        val contentUnread = unreadListResp.body?.get("content") as? List<*>
        assertEquals(2, contentUnread?.size)

        // Test all notifications
        val allListResp = restTemplate.exchange(
            "/api/v1/notifications?unread=false",
            HttpMethod.GET,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.OK, allListResp.statusCode)
        val contentAll = allListResp.body?.get("content") as? List<*>
        assertEquals(3, contentAll?.size)
    }

    @Test
    fun `4 morning job emits REVISION_DUE and PLAN_DUE only when counts greater than 0`() {
        // Run morning job when no items are due -> 0 notifications
        notificationService.runMorningNotificationJob()
        assertEquals(0, notificationRepository.count())

        // Create a due topic for today
        val topic = topicRepository.findAll().first()
        try {
            topicMasteryRepository.save(
                com.gatelms.entity.TopicMastery(
                    id = com.gatelms.entity.TopicMasteryId(operatorId, topic.id),
                    nextRevisionDue = Instant.now().minusSeconds(3600)
                )
            )

            // Create a plan item for today
            val plan = studyPlanRepository.save(
                StudyPlan(
                    userId = operatorId,
                    title = "Test Morning Plan",
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now().plusDays(7)
                )
            )
            planItemRepository.save(
                PlanItem(
                    plan = plan,
                    planDate = LocalDate.now(),
                    topic = topic,
                    targetQuestions = 10
                )
            )

            // Run morning job now -> should produce REVISION_DUE and PLAN_DUE
            notificationService.runMorningNotificationJob()

            val allNotifs = notificationRepository.findAll()
            assertEquals(2, allNotifs.size)
            assertTrue(allNotifs.any { it.type == NotificationType.REVISION_DUE })
            assertTrue(allNotifs.any { it.type == NotificationType.PLAN_DUE })
        } finally {
            topicMasteryRepository.deleteAll()
        }
    }
}

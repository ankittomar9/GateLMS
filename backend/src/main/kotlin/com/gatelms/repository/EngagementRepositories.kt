package com.gatelms.repository

import com.gatelms.entity.Achievement
import com.gatelms.entity.Notification
import com.gatelms.entity.NotificationType
import com.gatelms.entity.UserGoal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface UserGoalRepository : JpaRepository<UserGoal, UUID> {
    fun findFirstByUserIdOrderByUpdatedAtDesc(userId: UUID): UserGoal?
    fun findFirstByOrderByUpdatedAtDesc(): UserGoal?
}

@Repository
interface AchievementRepository : JpaRepository<Achievement, UUID> {
    fun findByUserIdAndCode(userId: UUID, code: String): Achievement?
    fun existsByUserIdAndCode(userId: UUID, code: String): Boolean
    fun findAllByUserIdOrderByEarnedAtAsc(userId: UUID): List<Achievement>

    fun findByCode(code: String): Achievement?
    fun existsByCode(code: String): Boolean
    fun findAllByOrderByEarnedAtAsc(): List<Achievement>
}

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Notification>
    fun findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): Page<Notification>
    fun countByUserIdAndReadAtIsNull(userId: UUID): Long
    fun findTop10ByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<Notification>

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    fun markAllAsRead(@Param("userId") userId: UUID, @Param("now") now: Instant): Int

    fun existsByUserIdAndTypeAndCreatedAtBetween(
        userId: UUID,
        type: NotificationType,
        start: Instant,
        end: Instant
    ): Boolean

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Notification>
    fun findByReadAtIsNullOrderByCreatedAtDesc(pageable: Pageable): Page<Notification>
    fun countByReadAtIsNull(): Long
    fun findTop10ByOrderByCreatedAtDesc(): List<Notification>

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.readAt IS NULL")
    fun markAllAsRead(@Param("now") now: Instant): Int

    fun existsByTypeAndCreatedAtBetween(
        type: NotificationType,
        start: Instant,
        end: Instant
    ): Boolean
}

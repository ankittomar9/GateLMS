package com.gatelms.repository

import com.gatelms.entity.TopicMastery
import com.gatelms.entity.TopicMasteryId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface TopicMasteryRepository : JpaRepository<TopicMastery, TopicMasteryId> {
    fun findByIdUserIdAndIdTopicId(userId: UUID, topicId: String): Optional<TopicMastery>
    fun findAllByIdUserId(userId: UUID): List<TopicMastery>

    @Query("SELECT tm FROM TopicMastery tm WHERE tm.id.userId = :userId")
    fun findByUserId(@Param("userId") userId: UUID): List<TopicMastery>

    @Query("SELECT tm FROM TopicMastery tm WHERE tm.id.userId = :userId AND tm.nextRevisionDue IS NOT NULL AND tm.nextRevisionDue <= :cutoff")
    fun findDueForRevision(@Param("userId") userId: UUID, @Param("cutoff") cutoff: Instant): List<TopicMastery>

    @Query("SELECT COUNT(tm) FROM TopicMastery tm WHERE tm.id.userId = :userId AND tm.nextRevisionDue IS NOT NULL AND tm.nextRevisionDue <= :cutoff")
    fun countDueForRevision(@Param("userId") userId: UUID, @Param("cutoff") cutoff: Instant): Long

    @Query("SELECT COUNT(tm) FROM TopicMastery tm WHERE tm.id.userId = :userId AND tm.masteryLevel >= :level")
    fun countMasteredTopics(@Param("userId") userId: UUID, @Param("level") level: Int): Long

    @Query("SELECT AVG(tm.masteryLevel) FROM TopicMastery tm WHERE tm.id.userId = :userId")
    fun getAverageMastery(@Param("userId") userId: UUID): Double?
}

package com.gatelms.repository

import com.gatelms.entity.PlanItem
import com.gatelms.entity.PlanItemStatus
import com.gatelms.entity.StudyPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface StudyPlanRepository : JpaRepository<StudyPlan, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<StudyPlan>
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<StudyPlan>
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<StudyPlan>
    fun findFirstByUserIdOrderByCreatedAtDesc(userId: UUID): StudyPlan?

    @Query("SELECT p FROM StudyPlan p WHERE :date BETWEEN p.startDate AND p.endDate ORDER BY p.createdAt DESC")
    fun findActivePlansForDate(@Param("date") date: LocalDate): List<StudyPlan>

    @Query("SELECT p FROM StudyPlan p WHERE p.userId = :userId AND :date BETWEEN p.startDate AND p.endDate ORDER BY p.createdAt DESC")
    fun findActivePlansForDate(@Param("date") date: LocalDate, @Param("userId") userId: UUID): List<StudyPlan>

    fun findFirstByOrderByCreatedAtDesc(): StudyPlan?
}

@Repository
interface PlanItemRepository : JpaRepository<PlanItem, UUID> {
    fun findByPlanIdOrderByPlanDateAsc(planId: UUID): List<PlanItem>

    fun findByPlanDate(planDate: LocalDate): List<PlanItem>

    @Query("SELECT pi FROM PlanItem pi WHERE pi.planDate = :date AND pi.status = 'OPEN'")
    fun findOpenItemsForDate(@Param("date") date: LocalDate): List<PlanItem>

    @Query("SELECT pi FROM PlanItem pi WHERE pi.planDate = :date AND pi.topic.id = :topicId")
    fun findByPlanDateAndTopicId(@Param("date") date: LocalDate, @Param("topicId") topicId: String): List<PlanItem>

    @Query("SELECT pi FROM PlanItem pi WHERE pi.topic.id = :topicId AND pi.status = 'OPEN' ORDER BY pi.planDate ASC")
    fun findOpenItemsByTopic(@Param("topicId") topicId: String): List<PlanItem>

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.planDate >= :date")
    fun findFutureItemsByPlan(@Param("planId") planId: UUID, @Param("date") date: LocalDate): List<PlanItem>
}

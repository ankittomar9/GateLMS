package com.gatelms.repository;

import com.gatelms.entity.PlanItem;
import com.gatelms.entity.PlanItemStatus;
import com.gatelms.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, UUID> {
    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<StudyPlan> findByUserIdAndIsActiveTrue(UUID userId);
}

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, UUID> {
    List<PlanItem> findByPlanIdOrderByTargetDateAsc(UUID planId);
    List<PlanItem> findByPlanIdAndTargetDate(UUID planId, LocalDate targetDate);
    List<PlanItem> findByPlanIdAndStatus(UUID planId, PlanItemStatus status);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.targetDate <= :today AND pi.status = 'OPEN'")
    List<PlanItem> findDueItems(@Param("planId") UUID planId, @Param("today") LocalDate today);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.userId = :userId AND pi.targetDate <= :today AND pi.status = 'OPEN'")
    List<PlanItem> findDueItemsByUserId(@Param("userId") UUID userId, @Param("today") LocalDate today);
}

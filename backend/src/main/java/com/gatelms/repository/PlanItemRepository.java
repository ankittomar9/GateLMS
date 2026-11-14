package com.gatelms.repository;

import com.gatelms.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, UUID> {
    List<PlanItem> findByPlanIdOrderByPlanDateAsc(UUID planId);
    List<PlanItem> findByPlanDate(LocalDate planDate);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.planDate = :date AND pi.status = 'OPEN'")
    List<PlanItem> findOpenItemsForDate(@Param("date") LocalDate date);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.planDate = :date AND pi.topic.id = :topicId")
    List<PlanItem> findByPlanDateAndTopicId(@Param("date") LocalDate date, @Param("topicId") String topicId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.topic.id = :topicId AND pi.status = 'OPEN' ORDER BY pi.planDate ASC")
    List<PlanItem> findOpenItemsByTopic(@Param("topicId") String topicId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.planDate >= :date")
    List<PlanItem> findFutureItemsByPlan(@Param("planId") UUID planId, @Param("date") LocalDate date);
}

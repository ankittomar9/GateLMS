package com.gatelms.repository;

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
    List<StudyPlan> findAllByOrderByCreatedAtDesc();
    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<StudyPlan> findByIdAndUserId(UUID id, UUID userId);
    StudyPlan findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT p FROM StudyPlan p WHERE :date BETWEEN p.startDate AND p.endDate ORDER BY p.createdAt DESC")
    List<StudyPlan> findActivePlansForDate(@Param("date") LocalDate date);

    @Query("SELECT p FROM StudyPlan p WHERE p.userId = :userId AND :date BETWEEN p.startDate AND p.endDate ORDER BY p.createdAt DESC")
    List<StudyPlan> findActivePlansForDate(@Param("date") LocalDate date, @Param("userId") UUID userId);

    StudyPlan findFirstByOrderByCreatedAtDesc();
}

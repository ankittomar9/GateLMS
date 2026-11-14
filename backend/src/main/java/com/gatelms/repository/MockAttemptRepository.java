package com.gatelms.repository;

import com.gatelms.entity.MockAttempt;
import com.gatelms.entity.MockAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockAttemptRepository extends JpaRepository<MockAttempt, UUID> {
    List<MockAttempt> findByMockExamIdOrderByStartedAtDesc(UUID mockId);
    List<MockAttempt> findByMockExamIdAndUserIdOrderByStartedAtDesc(UUID mockId, UUID userId);
    List<MockAttempt> findByUserIdOrderByStartedAtDesc(UUID userId);
    Optional<MockAttempt> findByIdAndUserId(UUID id, UUID userId);
    List<MockAttempt> findByUserIdAndStatusOrderBySubmittedAtAsc(UUID userId, MockAttemptStatus status);
    List<MockAttempt> findByStatusOrderBySubmittedAtAsc(MockAttemptStatus status);
    List<MockAttempt> findByStatusAndStartedAtBefore(MockAttemptStatus status, Instant threshold);

    @Query("SELECT MAX(a.totalScore) FROM MockAttempt a WHERE a.mockExam.id = :mockId AND a.status = 'SUBMITTED' AND a.userId = :userId")
    Double findMaxScoreByMockExamId(@Param("mockId") UUID mockId, @Param("userId") UUID userId);

    long countByMockExamIdAndStatus(UUID mockId, MockAttemptStatus status);
    long countByMockExamIdAndUserIdAndStatus(UUID mockId, UUID userId, MockAttemptStatus status);
    long countByStatus(MockAttemptStatus status);
    long countByUserIdAndStatus(UUID userId, MockAttemptStatus status);

    @Query("SELECT COUNT(a) FROM MockAttempt a WHERE a.userId = :userId AND a.mockExam.mode = com.gatelms.entity.ExamMode.WEAKNESS_DRILL AND a.status = com.gatelms.entity.MockAttemptStatus.SUBMITTED")
    long countCompletedDrills(@Param("userId") UUID userId);
}

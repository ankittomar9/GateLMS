package com.gatelms.repository;

import com.gatelms.entity.AttemptAnswer;
import com.gatelms.entity.MockAttempt;
import com.gatelms.entity.MockAttemptStatus;
import com.gatelms.entity.MockExam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockExamRepository extends JpaRepository<MockExam, UUID> {
    List<MockExam> findByIsActiveTrue();
}

@Repository
public interface MockAttemptRepository extends JpaRepository<MockAttempt, UUID> {
    List<MockAttempt> findByUserIdAndExamIdAndStatus(UUID userId, UUID examId, MockAttemptStatus status);

    @Query("SELECT a FROM MockAttempt a WHERE a.userId = :userId AND a.status = 'SUBMITTED' ORDER BY a.submittedAt DESC")
    Page<MockAttempt> findSubmittedByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a FROM MockAttempt a WHERE a.userId = :userId AND a.status = 'SUBMITTED' ORDER BY a.submittedAt DESC")
    List<MockAttempt> findSubmittedByUserIdList(@Param("userId") UUID userId);

    long countByStatus(MockAttemptStatus status);
}

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAttemptId(UUID attemptId);
    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}

package com.gatelms.repository;

import com.gatelms.entity.MistakeErrorCategory;
import com.gatelms.entity.MistakeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MistakeLogRepository extends JpaRepository<MistakeLog, UUID> {

    Optional<MistakeLog> findByUserIdAndQuestionId(UUID userId, UUID questionId);

    @Query("""
        SELECT m FROM MistakeLog m
        JOIN m.question q
        WHERE m.userId = :userId
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
          AND (:errorCategory IS NULL OR m.errorCategory = :errorCategory)
        ORDER BY m.lastFailedAt DESC
    """)
    Page<MistakeLog> findMistakesFiltered(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        @Param("errorCategory") MistakeErrorCategory errorCategory,
        Pageable pageable
    );

    @Query("SELECT m FROM MistakeLog m WHERE m.userId = :userId AND m.nextReviewDue <= :now ORDER BY m.nextReviewDue ASC")
    List<MistakeLog> findDueForReview(@Param("userId") UUID userId, @Param("now") Instant now);

    long countByUserIdAndNextReviewDueLessThanEqual(UUID userId, Instant now);

    long countByUserId(UUID userId);
}

package com.gatelms.repository;

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

    @Query("""
        SELECT m FROM MistakeLog m
        WHERE m.userId = :userId
          AND (:resolved IS NULL OR m.resolved = :resolved)
          AND (:dueOnly = FALSE OR (m.nextReviewAt IS NOT NULL AND m.nextReviewAt <= :now))
        ORDER BY m.nextReviewAt ASC NULLS LAST, m.loggedAt DESC
    """)
    Page<MistakeLog> findFiltered(
        @Param("userId") UUID userId,
        @Param("resolved") Boolean resolved,
        @Param("dueOnly") boolean dueOnly,
        @Param("now") Instant now,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(m) FROM MistakeLog m
        WHERE m.userId = :userId
          AND m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
    """)
    long countDueReviews(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
        SELECT m.question.topic.id AS topicId, COUNT(m) AS cnt
        FROM MistakeLog m
        WHERE m.userId = :userId
          AND m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
        GROUP BY m.question.topic.id
    """)
    List<Object[]> countDueReviewsByTopic(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
        SELECT m FROM MistakeLog m
        WHERE m.userId = :userId
          AND m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
          AND (:subjectId IS NULL OR m.question.subject.id = :subjectId)
          AND (:topicId IS NULL OR m.question.topic.id = :topicId)
        ORDER BY m.nextReviewAt ASC, m.loggedAt DESC
    """)
    List<MistakeLog> findDueMistakesForDrill(
        @Param("userId") UUID userId,
        @Param("now") Instant now,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(m.reviewCount), 0) FROM MistakeLog m WHERE m.userId = :userId")
    long sumReviewCounts(@Param("userId") UUID userId);

    Optional<MistakeLog> findByIdAndUserId(UUID id, UUID userId);
}

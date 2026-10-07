package com.gatelms.repository

import com.gatelms.entity.AttemptAnswer
import com.gatelms.entity.MistakeLog
import com.gatelms.entity.MockAttempt
import com.gatelms.entity.MockExam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface MockExamRepository : JpaRepository<MockExam, UUID>

@Repository
interface MockAttemptRepository : JpaRepository<MockAttempt, UUID>

@Repository
interface AttemptAnswerRepository : JpaRepository<AttemptAnswer, UUID> {
    fun findByAttemptIdAndQuestionId(attemptId: UUID, questionId: UUID): AttemptAnswer?
    fun findByAttemptId(attemptId: UUID): List<AttemptAnswer>
}

@Repository
interface MistakeLogRepository : JpaRepository<MistakeLog, UUID> {
    @Query("""
        SELECT m FROM MistakeLog m
        WHERE (:resolved IS NULL OR m.resolved = :resolved)
          AND (:dueOnly = FALSE OR (m.nextReviewAt IS NOT NULL AND m.nextReviewAt <= :now))
        ORDER BY m.nextReviewAt ASC NULLS LAST, m.loggedAt DESC
    """)
    fun findFiltered(
        @Param("resolved") resolved: Boolean?,
        @Param("dueOnly") dueOnly: Boolean,
        @Param("now") now: Instant,
        pageable: Pageable
    ): Page<MistakeLog>

    @Query("""
        SELECT COUNT(m) FROM MistakeLog m
        WHERE m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
    """)
    fun countDueReviews(@Param("now") now: Instant): Long

    @Query("""
        SELECT m.question.topic.id AS topicId, COUNT(m) AS cnt
        FROM MistakeLog m
        WHERE m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
        GROUP BY m.question.topic.id
    """)
    fun countDueReviewsByTopic(@Param("now") now: Instant): List<Array<Any>>
}

package com.gatelms.repository

import com.gatelms.entity.AttemptAnswer
import com.gatelms.entity.ExamMode
import com.gatelms.entity.MistakeLog
import com.gatelms.entity.MockAttempt
import com.gatelms.entity.MockAttemptStatus
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
interface MockExamRepository : JpaRepository<MockExam, UUID> {
    fun findByModeOrderByCreatedAtDesc(mode: ExamMode): List<MockExam>
    fun findFirstByTitle(title: String): MockExam?
}

@Repository
interface MockAttemptRepository : JpaRepository<MockAttempt, UUID> {
    fun findByMockExamIdOrderByStartedAtDesc(mockId: UUID): List<MockAttempt>
    fun findByMockExamIdAndUserIdOrderByStartedAtDesc(mockId: UUID, userId: UUID): List<MockAttempt>
    fun findByUserIdOrderByStartedAtDesc(userId: UUID): List<MockAttempt>
    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<MockAttempt>
    fun findByUserIdAndStatusOrderBySubmittedAtAsc(userId: UUID, status: MockAttemptStatus): List<MockAttempt>
    fun findByStatusOrderBySubmittedAtAsc(status: MockAttemptStatus): List<MockAttempt>
    fun findByStatusAndStartedAtBefore(status: MockAttemptStatus, threshold: Instant): List<MockAttempt>
    @Query("SELECT MAX(a.totalScore) FROM MockAttempt a WHERE a.mockExam.id = :mockId AND a.status = 'SUBMITTED' AND a.userId = :userId")
    fun findMaxScoreByMockExamId(@Param("mockId") mockId: UUID, @Param("userId") userId: UUID): Double?
    fun countByMockExamIdAndStatus(mockId: UUID, status: MockAttemptStatus): Long
    fun countByMockExamIdAndUserIdAndStatus(mockId: UUID, userId: UUID, status: MockAttemptStatus): Long
    fun countByStatus(status: MockAttemptStatus): Long
    fun countByUserIdAndStatus(userId: UUID, status: MockAttemptStatus): Long

    @Query("SELECT COUNT(a) FROM MockAttempt a WHERE a.userId = :userId AND a.mockExam.mode = com.gatelms.entity.ExamMode.WEAKNESS_DRILL AND a.status = com.gatelms.entity.MockAttemptStatus.SUBMITTED")
    fun countCompletedDrills(@Param("userId") userId: UUID): Long
}

@Repository
interface AttemptAnswerRepository : JpaRepository<AttemptAnswer, UUID> {
    fun findByAttemptIdAndQuestionId(attemptId: UUID, questionId: UUID): AttemptAnswer?
    fun findByAttemptId(attemptId: UUID): List<AttemptAnswer>

    @Query(
        value = """
            SELECT DISTINCT ON (aa.question_id)
                cast(aa.question_id as text) as questionId,
                cast(q.subject_id as text) as subjectId,
                cast(q.topic_id as text) as topicId,
                aa.is_correct as isCorrect
            FROM attempt_answers aa
            JOIN questions q ON q.id = aa.question_id
            JOIN mock_attempts ma ON ma.id = aa.attempt_id
            WHERE ma.user_id = :userId AND aa.is_correct IS NOT NULL
            ORDER BY aa.question_id, aa.created_at DESC
        """,
        nativeQuery = true
    )
    fun findLatestDistinctVerdicts(@Param("userId") userId: UUID): List<LatestAnswerProjection>

    @Query("SELECT COUNT(DISTINCT aa.question.id) FROM AttemptAnswer aa JOIN aa.attempt a WHERE a.userId = :userId")
    fun countDistinctQuestionId(@Param("userId") userId: UUID): Long
}

interface LatestAnswerProjection {
    fun getQuestionId(): String
    fun getSubjectId(): String
    fun getTopicId(): String
    fun getIsCorrect(): Boolean
}

@Repository
interface MistakeLogRepository : JpaRepository<MistakeLog, UUID> {
    @Query("""
        SELECT m FROM MistakeLog m
        WHERE m.userId = :userId
          AND (:resolved IS NULL OR m.resolved = :resolved)
          AND (:dueOnly = FALSE OR (m.nextReviewAt IS NOT NULL AND m.nextReviewAt <= :now))
        ORDER BY m.nextReviewAt ASC NULLS LAST, m.loggedAt DESC
    """)
    fun findFiltered(
        @Param("userId") userId: UUID,
        @Param("resolved") resolved: Boolean?,
        @Param("dueOnly") dueOnly: Boolean,
        @Param("now") now: Instant,
        pageable: Pageable
    ): Page<MistakeLog>

    @Query("""
        SELECT COUNT(m) FROM MistakeLog m
        WHERE m.userId = :userId
          AND m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
    """)
    fun countDueReviews(@Param("userId") userId: UUID, @Param("now") now: Instant): Long

    @Query("""
        SELECT m.question.topic.id AS topicId, COUNT(m) AS cnt
        FROM MistakeLog m
        WHERE m.userId = :userId
          AND m.resolved = FALSE
          AND m.nextReviewAt IS NOT NULL
          AND m.nextReviewAt <= :now
        GROUP BY m.question.topic.id
    """)
    fun countDueReviewsByTopic(@Param("userId") userId: UUID, @Param("now") now: Instant): List<Array<Any>>

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
    fun findDueMistakesForDrill(
        @Param("userId") userId: UUID,
        @Param("now") now: Instant,
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<MistakeLog>

    @Query("SELECT COALESCE(SUM(m.reviewCount), 0) FROM MistakeLog m WHERE m.userId = :userId")
    fun sumReviewCounts(@Param("userId") userId: UUID): Long

    fun findByIdAndUserId(id: UUID, userId: UUID): java.util.Optional<MistakeLog>
}

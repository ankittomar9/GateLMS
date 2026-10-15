package com.gatelms.repository

import com.gatelms.entity.Question
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface QuestionRepository : JpaRepository<Question, UUID> {
    fun findByExternalId(externalId: String): Question?

    @Query(
        value = """
            SELECT q.* FROM questions q
            LEFT JOIN question_progress qp ON qp.question_id = q.id AND qp.user_id = :userId
            WHERE (:subjectId IS NULL OR q.subject_id = :subjectId)
              AND (:topicId IS NULL OR q.topic_id = :topicId)
              AND (:type IS NULL OR q.type = :type)
              AND (:year IS NULL OR q.year = :year)
              AND (:pass1Status IS NULL OR COALESCE(qp.pass1_status, 'PENDING') = :pass1Status)
              AND (:pass2Status IS NULL OR COALESCE(qp.pass2_status, 'PENDING') = :pass2Status)
              AND (:bookmarked IS NULL OR COALESCE(qp.is_bookmarked, FALSE) = :bookmarked)
              AND (:curationStatus IS NULL OR q.curation_status = :curationStatus)
              AND (:source IS NULL OR q.source = :source)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND (cast(:q as text) IS NULL OR q.search_vector @@ websearch_to_tsquery('english', cast(:q as text)))
            ORDER BY
              CASE WHEN cast(:q as text) IS NOT NULL THEN ts_rank(q.search_vector, websearch_to_tsquery('english', cast(:q as text))) END DESC NULLS LAST,
              CASE WHEN cast(:q as text) IS NULL AND :sortColumn = 'year' THEN q.year END DESC NULLS LAST,
              CASE WHEN cast(:q as text) IS NULL AND :sortColumn = 'marks' THEN q.marks END DESC NULLS LAST,
              q.created_at DESC,
              q.id DESC
        """,
        countQuery = """
            SELECT count(*) FROM questions q
            LEFT JOIN question_progress qp ON qp.question_id = q.id AND qp.user_id = :userId
            WHERE (:subjectId IS NULL OR q.subject_id = :subjectId)
              AND (:topicId IS NULL OR q.topic_id = :topicId)
              AND (:type IS NULL OR q.type = :type)
              AND (:year IS NULL OR q.year = :year)
              AND (:pass1Status IS NULL OR COALESCE(qp.pass1_status, 'PENDING') = :pass1Status)
              AND (:pass2Status IS NULL OR COALESCE(qp.pass2_status, 'PENDING') = :pass2Status)
              AND (:bookmarked IS NULL OR COALESCE(qp.is_bookmarked, FALSE) = :bookmarked)
              AND (:curationStatus IS NULL OR q.curation_status = :curationStatus)
              AND (:source IS NULL OR q.source = :source)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND (cast(:q as text) IS NULL OR q.search_vector @@ websearch_to_tsquery('english', cast(:q as text)))
        """,
        nativeQuery = true
    )
    fun findWithFilters(
        @Param("userId") userId: UUID,
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        @Param("type") type: String?,
        @Param("year") year: Int?,
        @Param("pass1Status") pass1Status: String?,
        @Param("pass2Status") pass2Status: String?,
        @Param("bookmarked") bookmarked: Boolean?,
        @Param("curationStatus") curationStatus: String? = null,
        @Param("source") source: String? = null,
        @Param("difficulty") difficulty: String? = null,
        @Param("q") q: String?,
        @Param("sortColumn") sortColumn: String = "created_at",
        pageable: Pageable
    ): Page<Question>

    @Query("SELECT q.subject.id, count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.id")
    fun countGroupedBySubject(): List<Array<Any>>

    @Query("SELECT q.type, count(q) FROM Question q GROUP BY q.type")
    fun countGroupedByType(): List<Array<Any>>

    @Query("""
        SELECT COALESCE(qp.pass1Status, com.gatelms.entity.Pass1Status.PENDING), count(q)
        FROM Question q
        LEFT JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        GROUP BY COALESCE(qp.pass1Status, com.gatelms.entity.Pass1Status.PENDING)
    """)
    fun countGroupedByPass1Status(@Param("userId") userId: UUID): List<Array<Any>>

    @Query("SELECT q.curationStatus, count(q) FROM Question q GROUP BY q.curationStatus")
    fun countGroupedByCurationStatus(): List<Array<Any>>

    @Query("SELECT count(q) FROM Question q WHERE q.curationStatus IN ('ACCEPTED', 'REJECTED')")
    fun countCurationDecisions(): Long

    @Query("SELECT q.subject.id, q.subject.name, q.curationStatus, count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.id, q.subject.name, q.curationStatus")
    fun countCurationGroupedBySubject(): List<Array<Any>>

    @Query("""
        SELECT q.topic.id, q.topic.name, q.subject.id,
               count(q),
               sum(CASE WHEN q.curationStatus != 'REJECTED' THEN 1L ELSE 0L END)
        FROM Question q
        WHERE q.topic IS NOT NULL AND q.subject IS NOT NULL
        GROUP BY q.topic.id, q.topic.name, q.subject.id
    """)
    fun countTopicQuestionInventory(): List<Array<Any>>

    @Query("SELECT q.subject.slug, q.subject.name, q.type, coalesce(q.difficulty, 'UNSPECIFIED'), count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.slug, q.subject.name, q.type, q.difficulty ORDER BY q.subject.name, q.type")
    fun countDistribution(): List<Array<Any>>

    @Query(
        value = """
            SELECT q.* FROM questions q
            WHERE q.subject_id = :subjectId
              AND q.type = :type
              AND (:marks IS NULL OR q.marks = :marks)
              AND (:year IS NULL OR q.year = :year)
              AND (:source IS NULL OR q.source = :source)
              AND (CAST(:filterCuration AS boolean) = FALSE OR q.curation_status IN (:curationStatuses))
              AND (CAST(:filterDifficulty AS boolean) = FALSE OR q.difficulty IN (:difficulties))
            ORDER BY CASE WHEN :year IS NOT NULL THEN q.marks ELSE RANDOM() END ASC, q.id ASC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findCandidatesForAssembly(
        @Param("subjectId") subjectId: String,
        @Param("type") type: String,
        @Param("marks") marks: Int?,
        @Param("year") year: Int? = null,
        @Param("source") source: String? = null,
        @Param("filterCuration") filterCuration: Boolean,
        @Param("curationStatuses") curationStatuses: List<String>,
        @Param("filterDifficulty") filterDifficulty: Boolean,
        @Param("difficulties") difficulties: List<String>,
        @Param("limit") limit: Int
    ): List<Question>

    @Query("""
        SELECT q.year, q.subject.slug, count(q)
        FROM Question q
        WHERE q.source = 'GATE_PYP' AND q.year IS NOT NULL AND q.subject IS NOT NULL
        GROUP BY q.year, q.subject.slug
        ORDER BY q.year DESC
    """)
    fun countPypGroupedByYearAndSubject(): List<Array<Any>>

    @Query("SELECT q FROM Question q WHERE q.year = :year AND q.source = 'GATE_PYP' ORDER BY q.marks ASC, q.externalId ASC")
    fun findPypByYear(@Param("year") year: Int): List<Question>

    fun findByYear(year: Int): List<Question>

    @Query("""
        SELECT COUNT(q) FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.pass1Status IN (com.gatelms.entity.Pass1Status.FAILED, com.gatelms.entity.Pass1Status.ASSISTED)
          AND qp.pass2Status = com.gatelms.entity.Pass2Status.PENDING
    """)
    fun countPass2Pending(@Param("userId") userId: UUID): Long

    @Query("""
        SELECT q FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.pass1Status IN (com.gatelms.entity.Pass1Status.FAILED, com.gatelms.entity.Pass1Status.ASSISTED)
          AND qp.pass2Status = com.gatelms.entity.Pass2Status.PENDING
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY qp.lastAttemptedAt DESC NULLS LAST, q.id ASC
    """)
    fun findPass2Candidates(
        @Param("userId") userId: UUID,
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<Question>

    @Query("""
        SELECT COUNT(q) FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.isBookmarked = TRUE
    """)
    fun countBookmarked(@Param("userId") userId: UUID): Long

    @Query("""
        SELECT q FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.isBookmarked = TRUE
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY qp.bookmarkedAt DESC NULLS LAST, q.id ASC
    """)
    fun findBookmarkedCandidates(
        @Param("userId") userId: UUID,
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<Question>

    @Query("""
        SELECT q FROM Question q
        JOIN TopicMastery tm ON tm.id.topicId = q.topic.id AND tm.id.userId = :userId
        WHERE tm.nextRevisionDue IS NOT NULL
          AND tm.nextRevisionDue <= :now
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY tm.nextRevisionDue ASC, q.id ASC
    """)
    fun findDueTopicCandidates(
        @Param("userId") userId: UUID,
        @Param("now") now: java.time.Instant,
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<Question>

    @Query("SELECT q.topic.id, COUNT(q) FROM Question q WHERE q.topic.id IS NOT NULL GROUP BY q.topic.id")
    fun countQuestionsGroupedByTopic(): List<Array<Any>>

    @Query("SELECT q.subject.id, COUNT(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' AND q.subject.id IS NOT NULL GROUP BY q.subject.id")
    fun countVisibleQuestionsGroupedBySubject(): List<Array<Any>>

    @Query("SELECT q.topic.id, COUNT(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' AND q.topic.id IS NOT NULL GROUP BY q.topic.id")
    fun countVisibleQuestionsGroupedByTopic(): List<Array<Any>>

    @Query("SELECT q FROM Question q WHERE (q.topic.id = :topicId OR q.topic.slug = :topicId) AND q.curationStatus <> 'REJECTED' ORDER BY q.year DESC")
    fun findSampleQuestionsByTopic(@Param("topicId") topicId: String, pageable: Pageable): List<Question>

    @Query("""
        SELECT q.id, q.year, q.createdAt
        FROM Question q
        WHERE (q.topic.id = :topicId OR q.topic.slug = :topicId)
          AND q.curationStatus <> 'REJECTED'
          AND q.id <> :sourceQuestionId
    """)
    fun findTopicCandidates(
        @Param("topicId") topicId: String,
        @Param("sourceQuestionId") sourceQuestionId: UUID
    ): List<Array<Any>>

    @Query("""
        SELECT q FROM Question q
        WHERE (:topicId IS NULL OR q.topic.id = :topicId OR q.topic.slug = :topicId)
          AND q.curationStatus <> 'REJECTED'
          AND q.id NOT IN (
              SELECT qct.id.questionId FROM QuestionConceptTag qct WHERE qct.confidence >= 0.8
          )
        ORDER BY q.createdAt DESC
    """)
    fun findUnmappedOrLowConfidenceQuestions(
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): Page<Question>
}


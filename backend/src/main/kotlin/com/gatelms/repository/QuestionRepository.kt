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
            WHERE (:subjectId IS NULL OR q.subject_id = :subjectId)
              AND (:topicId IS NULL OR q.topic_id = :topicId)
              AND (:type IS NULL OR q.type = :type)
              AND (:year IS NULL OR q.year = :year)
              AND (:pass1Status IS NULL OR q.pass1_status = :pass1Status)
              AND (:pass2Status IS NULL OR q.pass2_status = :pass2Status)
              AND (:bookmarked IS NULL OR q.is_bookmarked = :bookmarked)
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
            WHERE (:subjectId IS NULL OR q.subject_id = :subjectId)
              AND (:topicId IS NULL OR q.topic_id = :topicId)
              AND (:type IS NULL OR q.type = :type)
              AND (:year IS NULL OR q.year = :year)
              AND (:pass1Status IS NULL OR q.pass1_status = :pass1Status)
              AND (:pass2Status IS NULL OR q.pass2_status = :pass2Status)
              AND (:bookmarked IS NULL OR q.is_bookmarked = :bookmarked)
              AND (:curationStatus IS NULL OR q.curation_status = :curationStatus)
              AND (:source IS NULL OR q.source = :source)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND (cast(:q as text) IS NULL OR q.search_vector @@ websearch_to_tsquery('english', cast(:q as text)))
        """,
        nativeQuery = true
    )
    fun findWithFilters(
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

    @Query("SELECT q.pass1Status, count(q) FROM Question q GROUP BY q.pass1Status")
    fun countGroupedByPass1Status(): List<Array<Any>>

    @Query("SELECT q.curationStatus, count(q) FROM Question q GROUP BY q.curationStatus")
    fun countGroupedByCurationStatus(): List<Array<Any>>

    @Query("SELECT q.subject.id, q.subject.name, q.curationStatus, count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.id, q.subject.name, q.curationStatus")
    fun countCurationGroupedBySubject(): List<Array<Any>>

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
        WHERE q.pass1Status IN (com.gatelms.entity.Pass1Status.FAILED, com.gatelms.entity.Pass1Status.ASSISTED)
          AND q.pass2Status = com.gatelms.entity.Pass2Status.PENDING
    """)
    fun countPass2Pending(): Long

    @Query("""
        SELECT q FROM Question q
        WHERE q.pass1Status IN (com.gatelms.entity.Pass1Status.FAILED, com.gatelms.entity.Pass1Status.ASSISTED)
          AND q.pass2Status = com.gatelms.entity.Pass2Status.PENDING
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY q.updatedAt DESC NULLS LAST, q.id ASC
    """)
    fun findPass2Candidates(
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<Question>

    @Query("SELECT COUNT(q) FROM Question q WHERE q.isBookmarked = TRUE")
    fun countBookmarked(): Long

    @Query("""
        SELECT q FROM Question q
        WHERE q.isBookmarked = TRUE
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY q.bookmarkedAt DESC NULLS LAST, q.id ASC
    """)
    fun findBookmarkedCandidates(
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<Question>

    @Query("""
        SELECT q FROM Question q
        JOIN q.topic t
        WHERE t.nextRevisionDue IS NOT NULL
          AND t.nextRevisionDue <= :now
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR t.id = :topicId)
        ORDER BY t.nextRevisionDue ASC, q.id ASC
    """)
    fun findDueTopicCandidates(
        @Param("now") now: java.time.Instant,
        @Param("subjectId") subjectId: String?,
        @Param("topicId") topicId: String?,
        pageable: Pageable
    ): List<Question>
}

package com.gatelms.repository;

import com.gatelms.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    Optional<Question> findByExternalId(String externalId);

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
    Page<Question> findWithFilters(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        @Param("type") String type,
        @Param("year") Integer year,
        @Param("pass1Status") String pass1Status,
        @Param("pass2Status") String pass2Status,
        @Param("bookmarked") Boolean bookmarked,
        @Param("curationStatus") String curationStatus,
        @Param("source") String source,
        @Param("difficulty") String difficulty,
        @Param("q") String q,
        @Param("sortColumn") String sortColumn,
        Pageable pageable
    );

    @Query("SELECT q.subject.id, count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.id")
    List<Object[]> countGroupedBySubject();

    @Query("SELECT q.type, count(q) FROM Question q GROUP BY q.type")
    List<Object[]> countGroupedByType();

    @Query("""
        SELECT COALESCE(qp.pass1Status, com.gatelms.entity.Pass1Status.PENDING), count(q)
        FROM Question q
        LEFT JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        GROUP BY COALESCE(qp.pass1Status, com.gatelms.entity.Pass1Status.PENDING)
    """)
    List<Object[]> countGroupedByPass1Status(@Param("userId") UUID userId);

    @Query("SELECT q.curationStatus, count(q) FROM Question q GROUP BY q.curationStatus")
    List<Object[]> countGroupedByCurationStatus();

    @Query("SELECT count(q) FROM Question q WHERE q.curationStatus IN ('ACCEPTED', 'REJECTED')")
    long countCurationDecisions();

    @Query("SELECT q.subject.id, q.subject.name, q.curationStatus, count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.id, q.subject.name, q.curationStatus")
    List<Object[]> countCurationGroupedBySubject();

    @Query("""
        SELECT q.topic.id, q.topic.name, q.subject.id,
               count(q),
               sum(CASE WHEN q.curationStatus != 'REJECTED' THEN 1L ELSE 0L END)
        FROM Question q
        WHERE q.topic IS NOT NULL AND q.subject IS NOT NULL
        GROUP BY q.topic.id, q.topic.name, q.subject.id
    """)
    List<Object[]> countTopicQuestionInventory();

    @Query("SELECT q.subject.slug, q.subject.name, q.type, coalesce(q.difficulty, 'UNSPECIFIED'), count(q) FROM Question q WHERE q.subject IS NOT NULL GROUP BY q.subject.slug, q.subject.name, q.type, q.difficulty ORDER BY q.subject.name, q.type")
    List<Object[]> countDistribution();

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
    List<Question> findCandidatesForAssembly(
        @Param("subjectId") String subjectId,
        @Param("type") String type,
        @Param("marks") Integer marks,
        @Param("year") Integer year,
        @Param("source") String source,
        @Param("filterCuration") boolean filterCuration,
        @Param("curationStatuses") List<String> curationStatuses,
        @Param("filterDifficulty") boolean filterDifficulty,
        @Param("difficulties") List<String> difficulties,
        @Param("limit") int limit
    );

    @Query("""
        SELECT q.year, q.subject.slug, count(q)
        FROM Question q
        WHERE q.source = 'GATE_PYP' AND q.year IS NOT NULL AND q.subject IS NOT NULL
        GROUP BY q.year, q.subject.slug
        ORDER BY q.year DESC
    """)
    List<Object[]> countPypGroupedByYearAndSubject();

    @Query("SELECT q FROM Question q WHERE q.year = :year AND q.source = 'GATE_PYP' ORDER BY q.marks ASC, q.externalId ASC")
    List<Question> findPypByYear(@Param("year") int year);

    List<Question> findByYear(int year);

    @Query("""
        SELECT COUNT(q) FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.pass1Status IN (com.gatelms.entity.Pass1Status.FAILED, com.gatelms.entity.Pass1Status.ASSISTED)
          AND qp.pass2Status = com.gatelms.entity.Pass2Status.PENDING
    """)
    long countPass2Pending(@Param("userId") UUID userId);

    @Query("""
        SELECT q FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.pass1Status IN (com.gatelms.entity.Pass1Status.FAILED, com.gatelms.entity.Pass1Status.ASSISTED)
          AND qp.pass2Status = com.gatelms.entity.Pass2Status.PENDING
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY qp.lastAttemptedAt DESC NULLS LAST, q.id ASC
    """)
    List<Question> findPass2Candidates(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(q) FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.isBookmarked = TRUE
    """)
    long countBookmarked(@Param("userId") UUID userId);

    @Query("""
        SELECT q FROM Question q
        JOIN QuestionProgress qp ON qp.id.questionId = q.id AND qp.id.userId = :userId
        WHERE qp.isBookmarked = TRUE
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY qp.lastAttemptedAt DESC NULLS LAST, q.id ASC
    """)
    List<Question> findBookmarkedCandidates(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        Pageable pageable
    );

    @Query("""
        SELECT q FROM Question q
        JOIN TopicMastery tm ON tm.id.topicId = q.topic.id AND tm.id.userId = :userId
        WHERE tm.nextRevisionDue IS NOT NULL
          AND tm.nextRevisionDue <= :now
          AND (:subjectId IS NULL OR q.subject.id = :subjectId)
          AND (:topicId IS NULL OR q.topic.id = :topicId)
        ORDER BY tm.nextRevisionDue ASC, q.id ASC
    """)
    List<Question> findDueTopicCandidates(
        @Param("userId") UUID userId,
        @Param("now") Instant now,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        Pageable pageable
    );

    @Query("SELECT q.topic.id, COUNT(q) FROM Question q WHERE q.topic.id IS NOT NULL GROUP BY q.topic.id")
    List<Object[]> countQuestionsGroupedByTopic();

    @Query("SELECT q.subject.id, COUNT(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' AND q.subject.id IS NOT NULL GROUP BY q.subject.id")
    List<Object[]> countVisibleQuestionsGroupedBySubject();

    @Query("SELECT q.topic.id, COUNT(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' AND q.topic.id IS NOT NULL GROUP BY q.topic.id")
    List<Object[]> countVisibleQuestionsGroupedByTopic();

    @Query("SELECT q FROM Question q WHERE (q.topic.id = :topicId OR q.topic.slug = :topicId) AND q.curationStatus <> 'REJECTED' ORDER BY q.year DESC")
    List<Question> findSampleQuestionsByTopic(@Param("topicId") String topicId, Pageable pageable);

    @Query("""
        SELECT q.id, q.year, q.createdAt, q.statement
        FROM Question q
        WHERE (q.topic.id = :topicId OR q.topic.slug = :topicId)
          AND q.curationStatus <> 'REJECTED'
          AND q.id <> :sourceQuestionId
    """)
    List<Object[]> findTopicCandidates(
        @Param("topicId") String topicId,
        @Param("sourceQuestionId") UUID sourceQuestionId
    );

    @Query("""
        SELECT q FROM Question q
        WHERE (:topicId IS NULL OR q.topic.id = :topicId OR q.topic.slug = :topicId)
          AND q.curationStatus <> 'REJECTED'
          AND q.id NOT IN (
              SELECT qct.id.questionId FROM QuestionConceptTag qct WHERE qct.confidence >= 0.8
          )
        ORDER BY q.createdAt DESC
    """)
    Page<Question> findUnmappedOrLowConfidenceQuestions(
        @Param("topicId") String topicId,
        Pageable pageable
    );

    @Query(value = """
        SELECT * FROM questions q
        WHERE (:subjectId IS NULL OR q.subject_id = :subjectId)
          AND (:topicId IS NULL OR q.topic_id = :topicId)
          AND (:type IS NULL OR q.type = :type)
          AND (:difficulty IS NULL OR q.difficulty = :difficulty)
          AND q.curation_status <> 'REJECTED'
        ORDER BY RANDOM()
        LIMIT :limit
    """, nativeQuery = true)
    List<Question> findRandomPracticeQuestions(
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        @Param("type") String type,
        @Param("difficulty") String difficulty,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT * FROM questions q
        WHERE q.topic_id = :topicId
          AND q.id <> :excludeQuestionId
          AND q.curation_status <> 'REJECTED'
        ORDER BY RANDOM()
        LIMIT :limit
    """, nativeQuery = true)
    List<Question> findRandomByTopicIdExcluding(
        @Param("topicId") String topicId,
        @Param("excludeQuestionId") UUID excludeQuestionId,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT * FROM questions q
        WHERE q.subject_id = :subjectId
          AND q.id <> :excludeQuestionId
          AND q.curation_status <> 'REJECTED'
        ORDER BY RANDOM()
        LIMIT :limit
    """, nativeQuery = true)
    List<Question> findRandomBySubjectIdExcluding(
        @Param("subjectId") String subjectId,
        @Param("excludeQuestionId") UUID excludeQuestionId,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT * FROM questions q
        WHERE q.id <> :excludeQuestionId
          AND q.curation_status <> 'REJECTED'
        ORDER BY RANDOM()
        LIMIT :limit
    """, nativeQuery = true)
    List<Question> findRandomExcluding(
        @Param("excludeQuestionId") UUID excludeQuestionId,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT * FROM questions q
        WHERE q.topic_id = :topicId
          AND q.curation_status <> 'REJECTED'
        ORDER BY RANDOM()
        LIMIT :limit
    """, nativeQuery = true)
    List<Question> findRandomByTopicId(
        @Param("topicId") String topicId,
        @Param("limit") int limit
    );
}

package com.gatelms.repository;

import com.gatelms.entity.CurationStatus;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionDifficulty;
import com.gatelms.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    Optional<Question> findByExternalId(String externalId);

    @Query("SELECT q FROM Question q WHERE q.subject.id = :subjectId AND q.curationStatus <> 'REJECTED'")
    Page<Question> findBySubjectId(@Param("subjectId") String subjectId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId AND q.curationStatus <> 'REJECTED'")
    Page<Question> findByTopicId(@Param("topicId") String topicId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.topic.id IN :topicIds AND q.curationStatus <> 'REJECTED'")
    Page<Question> findByTopicIdIn(@Param("topicIds") Collection<String> topicIds, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.curationStatus <> 'REJECTED'")
    Page<Question> findAllVisible(Pageable pageable);

    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId AND q.curationStatus <> 'REJECTED'")
    List<Question> findAllVisibleByTopicId(@Param("topicId") String topicId);

    @Query("SELECT q FROM Question q WHERE q.curationStatus = :status")
    Page<Question> findByCurationStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT q.subject.id, count(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' GROUP BY q.subject.id")
    List<Object[]> countBySubjectGrouped();

    @Query("SELECT q.type, count(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' GROUP BY q.type")
    List<Object[]> countByTypeGrouped();

    @Query("SELECT q.year, q.subject.id, count(q) FROM Question q WHERE q.year IS NOT NULL AND q.curationStatus <> 'REJECTED' GROUP BY q.year, q.subject.id ORDER BY q.year DESC")
    List<Object[]> countPypByYearAndSubject();

    @Query("SELECT q.curationStatus, count(q) FROM Question q GROUP BY q.curationStatus")
    List<Object[]> countByCurationStatusGrouped();

    @Query("SELECT q.subject.id, q.curationStatus, count(q) FROM Question q GROUP BY q.subject.id, q.curationStatus")
    List<Object[]> countBySubjectAndCurationStatusGrouped();

    @Query("SELECT q.subject.id, q.type, q.difficulty, count(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' GROUP BY q.subject.id, q.type, q.difficulty")
    List<Object[]> countDistributionGrouped();

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

    @Query("SELECT q.topic.id, count(q) FROM Question q WHERE q.curationStatus <> 'REJECTED' GROUP BY q.topic.id")
    List<Object[]> countVisibleByTopicGrouped();

    @Query("SELECT q.topic.id, count(q) FROM Question q GROUP BY q.topic.id")
    List<Object[]> countTotalByTopicGrouped();

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

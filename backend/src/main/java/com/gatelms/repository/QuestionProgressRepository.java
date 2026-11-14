package com.gatelms.repository;

import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.Pass2Status;
import com.gatelms.entity.QuestionProgress;
import com.gatelms.entity.QuestionProgressId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId> {

    Optional<QuestionProgress> findByIdUserIdAndIdQuestionId(UUID userId, UUID questionId);

    @Query("SELECT qp FROM QuestionProgress qp WHERE qp.id.userId = :userId AND qp.id.questionId IN :questionIds")
    List<QuestionProgress> findAllByUserIdAndQuestionIdIn(@Param("userId") UUID userId, @Param("questionIds") Collection<UUID> questionIds);

    List<QuestionProgress> findByIdUserIdAndIdQuestionIdIn(UUID userId, Collection<UUID> questionIds);

    @Query("SELECT qp.pass1Status, count(qp) FROM QuestionProgress qp WHERE qp.id.userId = :userId GROUP BY qp.pass1Status")
    List<Object[]> countPass1StatusGrouped(@Param("userId") UUID userId);

    @Query("SELECT qp FROM QuestionProgress qp WHERE qp.id.userId = :userId AND qp.isBookmarked = true")
    Page<QuestionProgress> findBookmarkedByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT qp FROM QuestionProgress qp WHERE qp.id.userId = :userId AND qp.pass2Status = :status")
    Page<QuestionProgress> findByUserIdAndPass2Status(@Param("userId") UUID userId, @Param("status") Pass2Status status, Pageable pageable);

    long countByIdUserIdAndPass2Status(UUID userId, Pass2Status status);

    long countByIdUserIdAndIsBookmarkedTrue(UUID userId);

    @Query("""
        SELECT qp.id.questionId
        FROM QuestionProgress qp
        WHERE qp.id.userId = :userId
          AND qp.pass1Status IN ('SOLVED', 'ASSISTED')
    """)
    Set<UUID> findSolvedQuestionIdsForUser(@Param("userId") UUID userId);

    @Query("""
        SELECT q.topic.id, count(qp)
        FROM QuestionProgress qp
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId 
          AND qp.pass1Status IN ('SOLVED', 'ASSISTED')
          AND q.curationStatus <> 'REJECTED'
          AND q.topic.id IS NOT NULL
        GROUP BY q.topic.id
    """)
    List<Object[]> countSolvedByTopicForUser(@Param("userId") UUID userId);

    @Query("""
        SELECT q.subject.id, count(qp)
        FROM QuestionProgress qp
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId 
          AND qp.pass1Status IN ('SOLVED', 'ASSISTED')
          AND q.curationStatus <> 'REJECTED'
          AND q.subject.id IS NOT NULL
        GROUP BY q.subject.id
    """)
    List<Object[]> countSolvedBySubjectForUser(@Param("userId") UUID userId);

    Optional<QuestionProgress> findFirstByIdUserIdAndLastAttemptedAtIsNotNullOrderByLastAttemptedAtDesc(UUID userId);

    @Query("""
        SELECT qp, q
        FROM QuestionProgress qp
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId
          AND (q.subject.id = :subjectId OR q.subject.slug = :subjectId)
          AND qp.pass1Status IN ('SOLVED', 'ASSISTED')
          AND q.curationStatus <> 'REJECTED'
          AND (:starredOnly = false OR qp.isBookmarked = true)
        ORDER BY qp.lastAttemptedAt DESC NULLS LAST, q.createdAt DESC
    """)
    List<Object[]> findSolvedQuestionsForSubject(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("starredOnly") boolean starredOnly
    );

    @Query("""
        SELECT q.topic.id, count(qp)
        FROM QuestionProgress qp
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId AND qp.pass1Status = 'SOLVED'
        GROUP BY q.topic.id
    """)
    List<Object[]> countSolvedByTopicGrouped(@Param("userId") UUID userId);

    @Query("""
        SELECT q.subject.id, count(qp)
        FROM QuestionProgress qp
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId AND qp.pass1Status = 'SOLVED'
        GROUP BY q.subject.id
    """)
    List<Object[]> countSolvedBySubjectGrouped(@Param("userId") UUID userId);

    @Query("""
        SELECT q.topic.id, count(qp)
        FROM QuestionProgress qp
        JOIN Question q ON q.id = qp.id.questionId
        WHERE qp.id.userId = :userId
        GROUP BY q.topic.id
    """)
    List<Object[]> countTouchedByTopicGrouped(@Param("userId") UUID userId);
}

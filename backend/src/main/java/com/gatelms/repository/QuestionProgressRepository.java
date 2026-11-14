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
import java.util.UUID;

@Repository
public interface QuestionProgressRepository extends JpaRepository<QuestionProgress, QuestionProgressId> {

    Optional<QuestionProgress> findByIdUserIdAndIdQuestionId(UUID userId, UUID questionId);

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

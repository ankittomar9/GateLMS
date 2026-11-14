package com.gatelms.repository;

import com.gatelms.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    AttemptAnswer findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
    List<AttemptAnswer> findByAttemptId(UUID attemptId);

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
    List<LatestAnswerProjection> findLatestDistinctVerdicts(@Param("userId") UUID userId);

    @Query("SELECT COUNT(DISTINCT aa.question.id) FROM AttemptAnswer aa JOIN aa.attempt a WHERE a.userId = :userId")
    long countDistinctQuestionId(@Param("userId") UUID userId);
}

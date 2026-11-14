package com.gatelms.anatomy.repository;

import com.gatelms.anatomy.entity.QuestionConceptTag;
import com.gatelms.anatomy.entity.QuestionConceptTagId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public interface QuestionConceptTagRepository extends JpaRepository<QuestionConceptTag, QuestionConceptTagId> {
    List<QuestionConceptTag> findByIdQuestionId(UUID questionId);
    Page<QuestionConceptTag> findByIdConceptTagSlug(String conceptTagSlug, Pageable pageable);
    long countByIdConceptTagSlug(String conceptTagSlug);

    @Query("SELECT qct.id.conceptTagSlug FROM QuestionConceptTag qct WHERE qct.id.questionId = :questionId")
    List<String> findSlugsByQuestionId(@Param("questionId") UUID questionId);

    @Query("SELECT qct.id.questionId, qct.id.conceptTagSlug FROM QuestionConceptTag qct WHERE qct.id.questionId IN :questionIds")
    List<Object[]> findSlugPairsByQuestionIds(@Param("questionIds") Collection<UUID> questionIds);

    @Query("""
        SELECT qct.id.questionId, count(qct), q.year, q.createdAt, q.statement
        FROM QuestionConceptTag qct
        JOIN Question q ON q.id = qct.id.questionId
        WHERE qct.id.conceptTagSlug IN :slugs
          AND q.curationStatus <> 'REJECTED'
          AND q.id <> :sourceQuestionId
        GROUP BY qct.id.questionId, q.year, q.createdAt, q.statement
    """)
    List<Object[]> findConceptCandidatesWithSharedSlugCount(
        @Param("slugs") Collection<String> slugs,
        @Param("sourceQuestionId") UUID sourceQuestionId
    );

    @Query("SELECT COUNT(DISTINCT qct.id.questionId) FROM QuestionConceptTag qct")
    long countDistinctQuestions();

    @Query("""
        SELECT qct.id.conceptTagSlug, q.id, q.statement
        FROM QuestionConceptTag qct
        JOIN Question q ON q.id = qct.id.questionId
        WHERE qct.id.conceptTagSlug IN :slugs
          AND q.curationStatus <> 'REJECTED'
    """)
    List<Object[]> findTaggedQuestionInfos(@Param("slugs") Collection<String> slugs);

    default Map<UUID, List<String>> findSlugsGroupedByQuestionIds(Collection<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return Collections.emptyMap();
        List<Object[]> pairs = findSlugPairsByQuestionIds(questionIds);
        Map<UUID, List<String>> result = new HashMap<>();
        for (Object[] pair : pairs) {
            UUID qId = (UUID) pair[0];
            String slug = (String) pair[1];
            result.computeIfAbsent(qId, k -> new ArrayList<>()).add(slug);
        }
        return result;
    }
}

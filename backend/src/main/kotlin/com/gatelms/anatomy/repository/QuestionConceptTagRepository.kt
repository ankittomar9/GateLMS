package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface QuestionConceptTagRepository : JpaRepository<QuestionConceptTag, QuestionConceptTagId> {
    fun findByIdQuestionId(questionId: UUID): List<QuestionConceptTag>
    fun findByIdConceptTagSlug(conceptTagSlug: String, pageable: Pageable): Page<QuestionConceptTag>
    fun countByIdConceptTagSlug(conceptTagSlug: String): Long

    @Query("SELECT qct.id.conceptTagSlug FROM QuestionConceptTag qct WHERE qct.id.questionId = :questionId")
    fun findSlugsByQuestionId(@Param("questionId") questionId: UUID): List<String>

    @Query("SELECT qct.id.questionId, qct.id.conceptTagSlug FROM QuestionConceptTag qct WHERE qct.id.questionId IN :questionIds")
    fun findSlugPairsByQuestionIds(@Param("questionIds") questionIds: Collection<UUID>): List<Array<Any>>

    @Query("""
        SELECT qct.id.questionId, count(qct), q.year, q.createdAt, q.statement
        FROM QuestionConceptTag qct
        JOIN Question q ON q.id = qct.id.questionId
        WHERE qct.id.conceptTagSlug IN :slugs
          AND q.curationStatus <> 'REJECTED'
          AND q.id <> :sourceQuestionId
        GROUP BY qct.id.questionId, q.year, q.createdAt, q.statement
    """)
    fun findConceptCandidatesWithSharedSlugCount(
        @Param("slugs") slugs: Collection<String>,
        @Param("sourceQuestionId") sourceQuestionId: UUID
    ): List<Array<Any>>

    @Query("SELECT COUNT(DISTINCT qct.id.questionId) FROM QuestionConceptTag qct")
    fun countDistinctQuestions(): Long

    @Query("""
        SELECT qct.id.conceptTagSlug, q.id, q.statement
        FROM QuestionConceptTag qct
        JOIN Question q ON q.id = qct.id.questionId
        WHERE qct.id.conceptTagSlug IN :slugs
          AND q.curationStatus <> 'REJECTED'
    """)
    fun findTaggedQuestionInfos(@Param("slugs") slugs: Collection<String>): List<Array<Any>>
}

fun QuestionConceptTagRepository.findSlugsGroupedByQuestionIds(questionIds: Collection<UUID>): Map<UUID, List<String>> {
    if (questionIds.isEmpty()) return emptyMap()
    val pairs = this.findSlugPairsByQuestionIds(questionIds)
    return pairs.groupBy({ it[0] as UUID }, { it[1] as String })
}


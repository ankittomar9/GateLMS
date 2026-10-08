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
}

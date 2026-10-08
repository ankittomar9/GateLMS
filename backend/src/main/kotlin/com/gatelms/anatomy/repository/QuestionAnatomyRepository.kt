package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.AnatomyStatus
import com.gatelms.anatomy.entity.QuestionAnatomy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface QuestionAnatomyRepository : JpaRepository<QuestionAnatomy, UUID> {
    fun findByQuestionId(questionId: UUID): Optional<QuestionAnatomy>
    fun findByStatus(status: AnatomyStatus, pageable: Pageable): Page<QuestionAnatomy>
    fun countByStatus(status: AnatomyStatus): Long
}

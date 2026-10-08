package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.ConceptUnit
import com.gatelms.anatomy.entity.ConceptUnitStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ConceptUnitRepository : JpaRepository<ConceptUnit, UUID> {
    @Query("SELECT cu FROM ConceptUnit cu WHERE cu.conceptTag.slug = :slug")
    fun findByConceptTagSlug(@Param("slug") slug: String): Optional<ConceptUnit>

    fun findByStatus(status: ConceptUnitStatus, pageable: Pageable): Page<ConceptUnit>
}

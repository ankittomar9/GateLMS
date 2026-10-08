package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.RetrievalCard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RetrievalCardRepository : JpaRepository<RetrievalCard, UUID> {
    fun findByConceptUnitIdOrderByCardOrderAsc(conceptUnitId: UUID): List<RetrievalCard>
}

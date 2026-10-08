package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.RetrievalCardState
import com.gatelms.anatomy.entity.RetrievalCardStateId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface RetrievalCardStateRepository : JpaRepository<RetrievalCardState, RetrievalCardStateId> {
    @Query("SELECT rcs FROM RetrievalCardState rcs WHERE rcs.id.userId = :userId AND rcs.dueAt <= :now ORDER BY rcs.dueAt ASC")
    fun findDueCards(@Param("userId") userId: UUID, @Param("now") now: Instant): List<RetrievalCardState>

    fun countByIdUserIdAndDueAtLessThanEqual(userId: UUID, now: Instant): Long

    fun findByIdUserId(userId: UUID): List<RetrievalCardState>
}

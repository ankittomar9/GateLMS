package com.gatelms.anatomy.repository;

import com.gatelms.anatomy.entity.RetrievalCardState;
import com.gatelms.anatomy.entity.RetrievalCardStateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RetrievalCardStateRepository extends JpaRepository<RetrievalCardState, RetrievalCardStateId> {
    @Query("SELECT rcs FROM RetrievalCardState rcs WHERE rcs.id.userId = :userId AND rcs.dueAt <= :now ORDER BY rcs.dueAt ASC")
    List<RetrievalCardState> findDueCards(@Param("userId") UUID userId, @Param("now") Instant now);

    long countByIdUserIdAndDueAtLessThanEqual(UUID userId, Instant now);

    List<RetrievalCardState> findByIdUserId(UUID userId);
}

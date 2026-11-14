package com.gatelms.anatomy.repository;

import com.gatelms.anatomy.entity.RetrievalCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RetrievalCardRepository extends JpaRepository<RetrievalCard, UUID> {
    List<RetrievalCard> findByConceptUnitIdOrderByCardOrderAsc(UUID conceptUnitId);
}

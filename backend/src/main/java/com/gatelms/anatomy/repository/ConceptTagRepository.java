package com.gatelms.anatomy.repository;

import com.gatelms.anatomy.entity.ConceptTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptTagRepository extends JpaRepository<ConceptTag, String> {
    List<ConceptTag> findByTopicId(String topicId);
    List<ConceptTag> findByTopicIdOrderByLabelAsc(String topicId);
    Optional<ConceptTag> findBySlug(String slug);

    @Query("SELECT ct FROM ConceptTag ct WHERE ct.topic.id IN :topicIds ORDER BY ct.label ASC")
    List<ConceptTag> findByTopicIdIn(@Param("topicIds") Collection<String> topicIds);
}

package com.gatelms.anatomy.repository;

import com.gatelms.anatomy.entity.TopicPrimer;
import com.gatelms.anatomy.entity.TopicPrimerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicPrimerRepository extends JpaRepository<TopicPrimer, UUID> {
    @Query("SELECT tp FROM TopicPrimer tp WHERE tp.topic.id = :topicId")
    Optional<TopicPrimer> findByTopicId(@Param("topicId") String topicId);

    @Query("SELECT tp FROM TopicPrimer tp WHERE tp.topic.id IN :topicIds")
    List<TopicPrimer> findByTopicIdIn(@Param("topicIds") Collection<String> topicIds);

    Page<TopicPrimer> findByStatus(TopicPrimerStatus status, Pageable pageable);
}

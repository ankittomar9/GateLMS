package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.TopicPrimer
import com.gatelms.anatomy.entity.TopicPrimerStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface TopicPrimerRepository : JpaRepository<TopicPrimer, UUID> {
    @Query("SELECT tp FROM TopicPrimer tp WHERE tp.topic.id = :topicId")
    fun findByTopicId(@Param("topicId") topicId: String): Optional<TopicPrimer>

    @Query("SELECT tp FROM TopicPrimer tp WHERE tp.topic.id IN :topicIds")
    fun findByTopicIdIn(@Param("topicIds") topicIds: Collection<String>): List<TopicPrimer>

    fun findByStatus(status: TopicPrimerStatus, pageable: Pageable): Page<TopicPrimer>
}

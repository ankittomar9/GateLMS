package com.gatelms.anatomy.repository

import com.gatelms.anatomy.entity.ConceptTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConceptTagRepository : JpaRepository<ConceptTag, String> {
    fun findByTopicId(topicId: String): List<ConceptTag>
}

package com.gatelms.repository

import com.gatelms.entity.Topic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TopicRepository : JpaRepository<Topic, String> {
    fun findBySubjectId(subjectId: String): List<Topic>

    @Query("SELECT t FROM Topic t JOIN t.subject s WHERE s.slug = :subjectSlug")
    fun findBySubjectSlug(subjectSlug: String): List<Topic>

    fun findBySlug(slug: String): Topic?

    @Query("SELECT t FROM Topic t WHERE (t.subject.id = :subjectIdOrSlug OR t.subject.slug = :subjectIdOrSlug) AND t.slug = :topicSlug")
    fun findBySubjectAndTopicSlug(subjectIdOrSlug: String, topicSlug: String): Topic?

    @Query("SELECT t FROM Topic t WHERE t.subject.id = :subjectIdOrSlug OR t.subject.slug = :subjectIdOrSlug ORDER BY t.sequence ASC, t.name ASC")
    fun findBySubjectIdOrSlugOrderBySequenceAscNameAsc(subjectIdOrSlug: String): List<Topic>
}

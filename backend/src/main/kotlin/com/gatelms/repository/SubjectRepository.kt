package com.gatelms.repository

import com.gatelms.entity.Subject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SubjectRepository : JpaRepository<Subject, String> {
    fun findBySlug(slug: String): Subject?

    @Query("SELECT s FROM Subject s WHERE s.id = :idOrSlug OR s.slug = :idOrSlug")
    fun findByIdOrSlug(idOrSlug: String): Subject?
}

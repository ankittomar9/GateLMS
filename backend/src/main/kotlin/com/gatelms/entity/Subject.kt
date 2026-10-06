package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "subjects")
class Subject(
    @Id
    @Column(name = "id", length = 50, nullable = false)
    var id: String = "",

    @Column(name = "name", length = 150, nullable = false)
    var name: String = "",

    @Column(name = "slug", length = 100, nullable = false, unique = true)
    var slug: String = "",

    @Column(name = "cluster", length = 50, nullable = false)
    var cluster: String = "",

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "avg_weightage", nullable = false)
    var avgWeightage: Double = 0.0,

    @Column(name = "priority_tier", nullable = false)
    var priorityTier: Int = 1,

    @Column(name = "standard_book_ref", length = 255)
    var standardBookRef: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "subject", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var topics: MutableList<Topic> = mutableListOf()
)

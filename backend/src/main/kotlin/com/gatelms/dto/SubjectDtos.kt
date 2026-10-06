package com.gatelms.dto

data class SubjectSummaryDto(
    val id: String,
    val name: String,
    val slug: String,
    val cluster: String,
    val avgWeightage: Double,
    val priorityTier: Int,
    val standardBookRef: String?,
    val topicCount: Int
)

data class SubjectDetailDto(
    val id: String,
    val name: String,
    val slug: String,
    val cluster: String,
    val avgWeightage: Double,
    val priorityTier: Int,
    val standardBookRef: String?,
    val topicCount: Int,
    val topics: List<TopicDto>
)

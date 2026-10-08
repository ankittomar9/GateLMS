package com.gatelms.dto

data class SeedInventoryResponse(
    val totalQuestions: Long,
    val totalVisibleQuestions: Long,
    val subjects: List<SubjectInventoryDto>
)

data class SubjectInventoryDto(
    val subjectId: String,
    val subjectName: String,
    val visibleCount: Long,
    val curationBreakdown: Map<String, Long>,
    val topics: List<TopicInventoryDto>
)

data class TopicInventoryDto(
    val topicId: String,
    val topicName: String,
    val visibleCount: Long,
    val totalCount: Long
)

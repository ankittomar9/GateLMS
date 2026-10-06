package com.gatelms.dto

import java.time.Instant

data class TopicDto(
    val id: String,
    val subjectId: String,
    val name: String,
    val slug: String,
    val bookChapterRef: String?,
    val masteryLevel: Int,
    val lastRevisedAt: Instant?,
    val nextRevisionDue: Instant?,
    val version: Int
)

data class TopicMasteryUpdateDto(
    val masteryLevel: Int? = null,
    val lastRevisedAt: Instant? = null,
    val nextRevisionDue: Instant? = null,
    val version: Int
)

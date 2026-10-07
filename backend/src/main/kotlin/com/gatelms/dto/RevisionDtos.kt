package com.gatelms.dto

import java.time.Instant

data class RevisionOverviewDto(
    val dueMistakes: Long,
    val dueTopics: Long,
    val pass2Pending: Long,
    val bookmarked: Long,
    val drillsCompleted: Long,
    val generatedAt: Instant
)

enum class RevisionDrillSource {
    DUE_MISTAKES,
    PASS2,
    BOOKMARKED,
    TOPIC
}

data class RevisionDrillRequest(
    val source: RevisionDrillSource,
    val subjectId: String? = null,
    val topicId: String? = null,
    val count: Int = 15
)

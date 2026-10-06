package com.gatelms.dto

data class StatsSummaryDto(
    val totalQuestions: Long,
    val bySubject: List<SubjectCountDto>,
    val byType: ByTypeCountDto,
    val pass1: Pass1CountDto
)

data class SubjectCountDto(
    val subjectId: String,
    val subjectName: String,
    val count: Long
)

data class ByTypeCountDto(
    val mcq: Long,
    val msq: Long,
    val nat: Long
)

data class Pass1CountDto(
    val solved: Long,
    val assisted: Long,
    val failed: Long,
    val pending: Long
)

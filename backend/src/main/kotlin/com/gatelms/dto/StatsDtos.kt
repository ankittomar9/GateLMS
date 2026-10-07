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

data class SubjectCurationCountDto(
    val subjectId: String,
    val name: String,
    val unreviewed: Long,
    val accepted: Long,
    val rejected: Long
)

data class CurationStatsDto(
    val totalQuestions: Long,
    val byStatus: Map<String, Long>,
    val bySubject: List<SubjectCurationCountDto>
)

data class DistributionItemDto(
    val subjectSlug: String,
    val subjectName: String,
    val type: String,
    val difficulty: String,
    val count: Long
)

data class DistributionStatsDto(
    val total: Long,
    val distribution: List<DistributionItemDto>
)

data class PypYearStatsDto(
    val year: Int,
    val total: Long,
    val bySubject: Map<String, Long>
)

data class PypCoverageDto(
    val fullPaperYears: List<Int>,
    val partialYears: List<Int>
)

data class PypStatsResponse(
    val years: List<PypYearStatsDto>,
    val coverage: PypCoverageDto
)


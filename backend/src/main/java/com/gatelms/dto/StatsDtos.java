package com.gatelms.dto;

import java.util.List;
import java.util.Map;

public class StatsDtos {
    public record StatsSummaryDto(
        long totalQuestions,
        List<SubjectCountDto> bySubject,
        ByTypeCountDto byType,
        Pass1CountDto pass1
    ) {}

    public record SubjectCountDto(
        String subjectId,
        String subjectName,
        long count
    ) {}

    public record ByTypeCountDto(
        long mcq,
        long msq,
        long nat
    ) {}

    public record Pass1CountDto(
        long solved,
        long assisted,
        long failed,
        long pending
    ) {}

    public record SubjectCurationCountDto(
        String subjectId,
        String name,
        long unreviewed,
        long accepted,
        long rejected
    ) {}

    public record CurationStatsDto(
        long totalQuestions,
        Map<String, Long> byStatus,
        List<SubjectCurationCountDto> bySubject
    ) {}

    public record DistributionItemDto(
        String subjectSlug,
        String subjectName,
        String type,
        String difficulty,
        long count
    ) {}

    public record DistributionStatsDto(
        long total,
        List<DistributionItemDto> distribution
    ) {}

    public record PypYearStatsDto(
        int year,
        long total,
        Map<String, Long> bySubject
    ) {}

    public record PypCoverageDto(
        List<Integer> fullPaperYears,
        List<Integer> partialYears
    ) {}

    public record PypStatsResponse(
        List<PypYearStatsDto> years,
        PypCoverageDto coverage
    ) {}
}

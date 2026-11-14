package com.gatelms.service;

import java.util.*;

public class SimilarQuestionRanker {

    public record CandidateInfo(
        UUID questionId,
        int sharedSlugCount,
        Integer year,
        long createdAtEpochMs,
        String normalizedStatement
    ) {
        public CandidateInfo(UUID questionId, int sharedSlugCount, Integer year, long createdAtEpochMs) {
            this(questionId, sharedSlugCount, year, createdAtEpochMs, null);
        }
    }

    public record RankedResult(
        List<UUID> questionIds,
        String similarityBasis
    ) {}

    public static RankedResult rankCandidates(
        UUID sourceQuestionId,
        List<CandidateInfo> conceptCandidates,
        List<CandidateInfo> topicCandidates,
        Set<UUID> solvedQuestionIds,
        int limit,
        String sourceNormalizedStatement
    ) {
        Set<UUID> excluded = new HashSet<>(solvedQuestionIds);
        excluded.add(sourceQuestionId);
        int effectiveLimit = Math.max(1, limit);

        // 1. Evaluate concept candidates with dedup pre-filter
        List<CandidateInfo> validConcept = conceptCandidates.stream()
            .filter(c -> !excluded.contains(c.questionId()))
            .filter(c -> !isDedupSibling(c, sourceNormalizedStatement))
            .filter(c -> c.sharedSlugCount() > 0)
            .sorted(Comparator
                .comparingInt(CandidateInfo::sharedSlugCount).reversed()
                .thenComparing(Comparator.comparing((CandidateInfo c) -> c.year() != null ? c.year() : -1).reversed())
                .thenComparing(Comparator.comparingLong(CandidateInfo::createdAtEpochMs).reversed())
                .thenComparing(CandidateInfo::questionId))
            .toList();

        if (validConcept.size() >= 2) {
            List<UUID> chosen = validConcept.stream().limit(effectiveLimit).map(CandidateInfo::questionId).toList();
            return new RankedResult(chosen, "CONCEPT");
        }

        // 2. Fallback to topic candidates with dedup pre-filter
        List<CandidateInfo> validTopic = topicCandidates.stream()
            .filter(c -> !excluded.contains(c.questionId()))
            .filter(c -> !isDedupSibling(c, sourceNormalizedStatement))
            .sorted(Comparator
                .comparing((CandidateInfo c) -> c.year() != null ? c.year() : -1).reversed()
                .thenComparing(Comparator.comparingLong(CandidateInfo::createdAtEpochMs).reversed())
                .thenComparing(CandidateInfo::questionId))
            .toList();

        List<UUID> chosen = validTopic.stream().limit(effectiveLimit).map(CandidateInfo::questionId).toList();
        return new RankedResult(chosen, "TOPIC");
    }

    private static boolean isDedupSibling(CandidateInfo c, String sourceNormalizedStatement) {
        return sourceNormalizedStatement != null &&
            c.normalizedStatement() != null &&
            c.normalizedStatement().equals(sourceNormalizedStatement);
    }
}

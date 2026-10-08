package com.gatelms.service

import java.util.UUID

object SimilarQuestionRanker {

    data class CandidateInfo(
        val questionId: UUID,
        val sharedSlugCount: Int = 0,
        val year: Int? = null,
        val createdAtEpochMs: Long = 0L
    )

    /**
     * Pure ranking function implementing Spec 7.1 (PC-5):
     * 1. If >= 2 candidate questions share a concept slug with the source question:
     *    rank by shared-slug count desc, year desc, createdAt desc -> basis: "CONCEPT"
     * 2. Else fallback to same-topic unsolved, newest year first -> basis: "TOPIC"
     *
     * In all cases:
     * - The source question itself is excluded (self-exclusion)
     * - Requester's solved questions are excluded (solved-exclusion)
     */
    fun rankCandidates(
        sourceQuestionId: UUID,
        conceptCandidates: List<CandidateInfo>,
        topicCandidates: List<CandidateInfo>,
        solvedQuestionIds: Set<UUID>,
        limit: Int = 5
    ): Pair<List<UUID>, String> {
        val excluded = solvedQuestionIds + sourceQuestionId
        val effectiveLimit = limit.coerceAtLeast(1)

        // 1. Evaluate concept candidates
        val validConcept = conceptCandidates
            .filter { it.questionId !in excluded && it.sharedSlugCount > 0 }
            .sortedWith(
                compareByDescending<CandidateInfo> { it.sharedSlugCount }
                    .thenByDescending { it.year ?: -1 }
                    .thenByDescending { it.createdAtEpochMs }
                    .thenBy { it.questionId }
            )

        if (validConcept.size >= 2) {
            val chosen = validConcept.take(effectiveLimit).map { it.questionId }
            return Pair(chosen, "CONCEPT")
        }

        // 2. Fallback to topic candidates
        val validTopic = topicCandidates
            .filter { it.questionId !in excluded }
            .sortedWith(
                compareByDescending<CandidateInfo> { it.year ?: -1 }
                    .thenByDescending { it.createdAtEpochMs }
                    .thenBy { it.questionId }
            )

        val chosen = validTopic.take(effectiveLimit).map { it.questionId }
        return Pair(chosen, "TOPIC")
    }
}

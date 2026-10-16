package com.gatelms.service

import java.util.UUID

object SimilarQuestionRanker {

    data class CandidateInfo(
        val questionId: UUID,
        val sharedSlugCount: Int = 0,
        val year: Int? = null,
        val createdAtEpochMs: Long = 0L,
        val normalizedStatement: String? = null
    )

    /**
     * Pure ranking function implementing Spec 7.1 (PC-5) & Phase 8 WP3:
     * 1. Dedup pre-filter: exclude candidates whose normalized statement matches
     *    the source question's normalized statement (excluding identical or prefix-rotated duplicates).
     * 2. Self-exclusion & Solved-exclusion: exclude source question itself and requester's solved questions.
     * 3. If >= 2 candidate questions share a concept slug with the source question:
     *    rank by shared-slug count desc, year desc, createdAt desc -> basis: "CONCEPT"
     * 4. Else fallback to same-topic unsolved, newest year first -> basis: "TOPIC"
     */
    fun rankCandidates(
        sourceQuestionId: UUID,
        conceptCandidates: List<CandidateInfo>,
        topicCandidates: List<CandidateInfo>,
        solvedQuestionIds: Set<UUID>,
        limit: Int = 5,
        sourceNormalizedStatement: String? = null
    ): Pair<List<UUID>, String> {
        val excluded = solvedQuestionIds + sourceQuestionId
        val effectiveLimit = limit.coerceAtLeast(1)

        val isDedupSibling = { c: CandidateInfo ->
            sourceNormalizedStatement != null &&
            c.normalizedStatement != null &&
            c.normalizedStatement == sourceNormalizedStatement
        }

        // 1. Evaluate concept candidates with dedup pre-filter
        val validConcept = conceptCandidates
            .filter { it.questionId !in excluded && !isDedupSibling(it) && it.sharedSlugCount > 0 }
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

        // 2. Fallback to topic candidates with dedup pre-filter
        val validTopic = topicCandidates
            .filter { it.questionId !in excluded && !isDedupSibling(it) }
            .sortedWith(
                compareByDescending<CandidateInfo> { it.year ?: -1 }
                    .thenByDescending { it.createdAtEpochMs }
                    .thenBy { it.questionId }
            )

        val chosen = validTopic.take(effectiveLimit).map { it.questionId }
        return Pair(chosen, "TOPIC")
    }
}

package com.gatelms.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class SimilarQuestionRankerTest {

    private val sourceId = UUID.randomUUID()
    private val q1 = UUID.randomUUID()
    private val q2 = UUID.randomUUID()
    private val q3 = UUID.randomUUID()
    private val q4 = UUID.randomUUID()
    private val q5 = UUID.randomUUID()

    @Test
    @DisplayName("honest-judge PASS - activates CONCEPT basis when >= 2 candidates share concept slugs and ranks by count desc")
    fun testConceptBasisOrdering() {
        val conceptCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q1, sharedSlugCount = 1, year = 2022),
            SimilarQuestionRanker.CandidateInfo(questionId = q2, sharedSlugCount = 3, year = 2020),
            SimilarQuestionRanker.CandidateInfo(questionId = q3, sharedSlugCount = 2, year = 2021)
        )
        val topicCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q4, year = 2024)
        )

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = conceptCandidates,
            topicCandidates = topicCandidates,
            solvedQuestionIds = emptySet(),
            limit = 5
        )

        assertEquals("CONCEPT", basis)
        assertEquals(3, rankedIds.size)
        assertEquals(listOf(q2, q3, q1), rankedIds, "Must order by sharedSlugCount DESC (3 -> 2 -> 1)")
    }

    @Test
    @DisplayName("honest-judge PASS - breaks ties in concept basis by year desc")
    fun testConceptBasisTieBreakingByYear() {
        val conceptCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q1, sharedSlugCount = 2, year = 2018),
            SimilarQuestionRanker.CandidateInfo(questionId = q2, sharedSlugCount = 2, year = 2023),
            SimilarQuestionRanker.CandidateInfo(questionId = q3, sharedSlugCount = 2, year = 2020)
        )

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = conceptCandidates,
            topicCandidates = emptyList(),
            solvedQuestionIds = emptySet(),
            limit = 5
        )

        assertEquals("CONCEPT", basis)
        assertEquals(listOf(q2, q3, q1), rankedIds, "Must break tie with year DESC (2023 -> 2020 -> 2018)")
    }

    @Test
    @DisplayName("honest-judge PASS - solved-exclusion removes solved questions from concept candidates and may trigger topic fallback")
    fun testSolvedExclusion() {
        val conceptCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q1, sharedSlugCount = 2, year = 2022),
            SimilarQuestionRanker.CandidateInfo(questionId = q2, sharedSlugCount = 2, year = 2021)
        )
        val topicCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q3, year = 2024),
            SimilarQuestionRanker.CandidateInfo(questionId = q4, year = 2023)
        )

        // q2 is solved by requester, leaving only 1 valid concept candidate (q1)
        val solvedIds = setOf(q2)

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = conceptCandidates,
            topicCandidates = topicCandidates,
            solvedQuestionIds = solvedIds,
            limit = 5
        )

        // Since only 1 valid concept candidate remains (< 2 threshold), fallback to topic
        assertEquals("TOPIC", basis)
        assertFalse(rankedIds.contains(q2), "Solved question q2 must be excluded")
        assertEquals(listOf(q3, q4), rankedIds)
    }

    @Test
    @DisplayName("honest-judge PASS - self-exclusion strictly excludes the source question ID")
    fun testSelfExclusion() {
        val conceptCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = sourceId, sharedSlugCount = 5, year = 2024),
            SimilarQuestionRanker.CandidateInfo(questionId = q1, sharedSlugCount = 2, year = 2022),
            SimilarQuestionRanker.CandidateInfo(questionId = q2, sharedSlugCount = 1, year = 2021)
        )

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = conceptCandidates,
            topicCandidates = emptyList(),
            solvedQuestionIds = emptySet(),
            limit = 5
        )

        assertEquals("CONCEPT", basis)
        assertFalse(rankedIds.contains(sourceId), "Source question must never appear in similar results")
        assertEquals(listOf(q1, q2), rankedIds)
    }

    @Test
    @DisplayName("honest-judge PASS - falls back to TOPIC when fewer than 2 concept candidates exist")
    fun testFallbackWhenSingleConceptCandidate() {
        val conceptCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q1, sharedSlugCount = 1, year = 2020)
        )
        val topicCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q3, year = 2024),
            SimilarQuestionRanker.CandidateInfo(questionId = q4, year = 2023)
        )

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = conceptCandidates,
            topicCandidates = topicCandidates,
            solvedQuestionIds = emptySet(),
            limit = 5
        )

        assertEquals("TOPIC", basis)
        assertEquals(listOf(q3, q4), rankedIds)
    }

    @Test
    @DisplayName("honest-judge PASS - falls back to TOPIC with newest year first when tags are absent")
    fun testFallbackWhenTagsAbsent() {
        val topicCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q1, year = 2018),
            SimilarQuestionRanker.CandidateInfo(questionId = q2, year = 2024),
            SimilarQuestionRanker.CandidateInfo(questionId = q3, year = 2021)
        )

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = emptyList(),
            topicCandidates = topicCandidates,
            solvedQuestionIds = emptySet(),
            limit = 5
        )

        assertEquals("TOPIC", basis)
        assertEquals(listOf(q2, q3, q1), rankedIds, "Topic candidates must be ordered by year DESC")
    }

    @Test
    @DisplayName("honest-judge PASS - limits results to requested limit and handles empty topic pool gracefully")
    fun testLimitAndEmptyCandidates() {
        val topicCandidates = listOf(
            SimilarQuestionRanker.CandidateInfo(questionId = q1, year = 2020),
            SimilarQuestionRanker.CandidateInfo(questionId = q2, year = 2021),
            SimilarQuestionRanker.CandidateInfo(questionId = q3, year = 2022)
        )

        val (rankedIds, basis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = emptyList(),
            topicCandidates = topicCandidates,
            solvedQuestionIds = emptySet(),
            limit = 2
        )

        assertEquals("TOPIC", basis)
        assertEquals(2, rankedIds.size)
        assertEquals(listOf(q3, q2), rankedIds)

        // When both candidates are empty
        val (emptyIds, emptyBasis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = sourceId,
            conceptCandidates = emptyList(),
            topicCandidates = emptyList(),
            solvedQuestionIds = emptySet(),
            limit = 5
        )
        assertEquals("TOPIC", emptyBasis)
        assertTrue(emptyIds.isEmpty())
    }
}

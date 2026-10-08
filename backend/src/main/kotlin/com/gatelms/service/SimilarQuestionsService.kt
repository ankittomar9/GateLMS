package com.gatelms.service

import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.dto.SimilarQuestionsResponseDto
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class SimilarQuestionsService(
    private val questionRepository: QuestionRepository,
    private val questionProgressRepository: QuestionProgressRepository,
    private val questionConceptTagRepository: QuestionConceptTagRepository,
    private val questionService: QuestionService,
    private val currentUserProvider: CurrentUserProvider
) {
    @Transactional(readOnly = true)
    fun getSimilarQuestions(questionId: UUID, limit: Int = 5): SimilarQuestionsResponseDto {
        val sourceQuestion = questionRepository.findById(questionId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: $questionId") }

        val userId = currentUserProvider.getCurrentUserId()
        val solvedQuestionIds = questionProgressRepository.findSolvedQuestionIdsForUser(userId)

        // 1. Fetch concept candidates sharing slugs from ANATOMY tags
        val sourceSlugs = questionConceptTagRepository.findSlugsByQuestionId(questionId)
        val conceptCandidates = if (sourceSlugs.isNotEmpty()) {
            val rows = questionConceptTagRepository.findConceptCandidatesWithSharedSlugCount(sourceSlugs, questionId)
            rows.map { row ->
                val qId = row[0] as UUID
                val sharedCount = (row[1] as Number).toInt()
                val year = (row[2] as? Number)?.toInt()
                val createdAt = (row[3] as? Instant)?.toEpochMilli() ?: 0L
                SimilarQuestionRanker.CandidateInfo(
                    questionId = qId,
                    sharedSlugCount = sharedCount,
                    year = year,
                    createdAtEpochMs = createdAt
                )
            }
        } else {
            emptyList()
        }

        // 2. Fetch topic candidates
        val topicId = sourceQuestion.topic?.id
        val topicCandidates = if (!topicId.isNullOrBlank()) {
            val rows = questionRepository.findTopicCandidates(topicId, questionId)
            rows.map { row ->
                val qId = row[0] as UUID
                val year = (row[1] as? Number)?.toInt()
                val createdAt = (row[2] as? Instant)?.toEpochMilli() ?: 0L
                SimilarQuestionRanker.CandidateInfo(
                    questionId = qId,
                    sharedSlugCount = 0,
                    year = year,
                    createdAtEpochMs = createdAt
                )
            }
        } else {
            emptyList()
        }

        // 3. Execute pure ranker (PC-5)
        val (rankedIds, similarityBasis) = SimilarQuestionRanker.rankCandidates(
            sourceQuestionId = questionId,
            conceptCandidates = conceptCandidates,
            topicCandidates = topicCandidates,
            solvedQuestionIds = solvedQuestionIds,
            limit = limit
        )

        if (rankedIds.isEmpty()) {
            return SimilarQuestionsResponseDto(questions = emptyList(), similarityBasis = similarityBasis)
        }

        val questionsMap = questionRepository.findAllById(rankedIds).associateBy { it.id!! }
        val progressMap = questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, rankedIds)
            .associateBy { it.id.questionId }

        val questionDtos = rankedIds.mapNotNull { id ->
            val q = questionsMap[id] ?: return@mapNotNull null
            val progress = progressMap[id]
            with(questionService) {
                q.toSummaryDto(progress)
            }
        }

        return SimilarQuestionsResponseDto(
            questions = questionDtos,
            similarityBasis = similarityBasis
        )
    }
}

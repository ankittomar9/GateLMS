package com.gatelms.service

import com.gatelms.dto.MistakeCreateRequest
import com.gatelms.dto.MistakeDto
import com.gatelms.dto.MistakeReviewRequest
import com.gatelms.dto.TopicMasteryStatsDto
import com.gatelms.entity.MistakeErrorCategory
import com.gatelms.entity.MistakeLog
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.AttemptAnswerRepository
import com.gatelms.repository.MistakeLogRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class MistakeService(
    private val mistakeLogRepository: MistakeLogRepository,
    private val questionRepository: QuestionRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val topicRepository: TopicRepository,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val planProgressService: PlanProgressService? = null
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(MistakeService::class.java)

    private val reviewIntervals = listOf(1L, 3L, 7L, 14L, 30L, 60L)

    @Transactional
    fun createMistake(request: MistakeCreateRequest): MistakeDto {
        val question = questionRepository.findById(request.questionId).orElseThrow {
            EntityNotFoundException("Question not found with id: ${request.questionId}")
        }

        val category = runCatching {
            MistakeErrorCategory.valueOf(request.errorCategory.trim().uppercase())
        }.getOrElse {
            throw ValidationException(
                "Invalid errorCategory: ${request.errorCategory}",
                mapOf("errorCategory" to "Must be one of CONCEPTUAL, CALCULATION, MSQ_TRAP, MISREAD")
            )
        }

        if (request.correctiveTakeaway.isBlank()) {
            throw ValidationException(
                "Corrective takeaway is required",
                mapOf("correctiveTakeaway" to "Must not be blank")
            )
        }

        val attemptAnswer = request.attemptAnswerId?.let {
            attemptAnswerRepository.findById(it).orElse(null)
        }

        val now = Instant.now()
        val nextReview = now.plus(1, ChronoUnit.DAYS)

        val mistake = MistakeLog(
            question = question,
            attemptAnswer = attemptAnswer,
            errorCategory = category,
            description = request.description?.trim(),
            correctiveTakeaway = request.correctiveTakeaway.trim(),
            loggedAt = now,
            reviewCount = 0,
            nextReviewAt = nextReview,
            resolved = false
        )

        val saved = mistakeLogRepository.save(mistake)
        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun getMistakes(resolved: Boolean?, dueOnly: Boolean, pageable: Pageable): Page<MistakeDto> {
        val now = Instant.now()
        val page = mistakeLogRepository.findFiltered(
            resolved = resolved,
            dueOnly = dueOnly,
            now = now,
            pageable = pageable
        )
        return page.map { it.toDto() }
    }

    @Transactional
    fun reviewMistake(id: UUID, request: MistakeReviewRequest): MistakeDto {
        val mistake = mistakeLogRepository.findById(id).orElseThrow {
            EntityNotFoundException("Mistake log not found with id: $id")
        }

        if (request.resolved != null) {
            mistake.resolved = request.resolved
        }

        val newCount = mistake.reviewCount + 1
        mistake.reviewCount = newCount

        val intervalDays = if (newCount <= reviewIntervals.size) {
            reviewIntervals[newCount - 1]
        } else {
            reviewIntervals.last()
        }

        mistake.nextReviewAt = Instant.now().plus(intervalDays, ChronoUnit.DAYS)
        val saved = mistakeLogRepository.save(mistake)

        // Plan auto-progress hook (SPEC §2.3: Mistake reviews add 1 to the matching topic item)
        val topicId = mistake.question?.topic?.id
        if (topicId != null) {
            try {
                planProgressService?.onQuestionsAnswered(
                    date = java.time.LocalDate.now(),
                    topicId = topicId,
                    count = 1,
                    minutes = 5
                )
            } catch (e: Exception) {
                logger.warn("Plan progress hook failure ignored during mistake review: ${e.message}", e)
            }
        }

        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun getMasteryStats(): List<TopicMasteryStatsDto> {
        val topics = topicRepository.findAll()
        val now = Instant.now()
        val dueCountsRaw = mistakeLogRepository.countDueReviewsByTopic(now)
        val dueCountMap = dueCountsRaw.mapNotNull { row ->
            val topicId = row[0] as? String
            val cnt = (row[1] as? Number)?.toLong() ?: 0L
            if (topicId != null) topicId to cnt else null
        }.toMap()

        return topics.map { t ->
            TopicMasteryStatsDto(
                topicId = t.id,
                topicName = t.name,
                subjectId = t.subject?.id ?: "",
                masteryLevel = t.masteryLevel,
                dueReviewsCount = dueCountMap[t.id] ?: 0L
            )
        }
    }

    private fun MistakeLog.toDto(): MistakeDto = MistakeDto(
        id = id,
        questionId = question?.id ?: UUID.randomUUID(),
        questionExternalId = question?.externalId,
        questionStatement = question?.statement ?: "",
        attemptAnswerId = attemptAnswer?.id,
        errorCategory = errorCategory,
        description = description,
        correctiveTakeaway = correctiveTakeaway,
        loggedAt = loggedAt,
        reviewCount = reviewCount,
        nextReviewAt = nextReviewAt,
        resolved = resolved
    )
}

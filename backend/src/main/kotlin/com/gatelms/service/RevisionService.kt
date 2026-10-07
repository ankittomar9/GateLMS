package com.gatelms.service

import com.gatelms.dto.PracticeSessionResponse
import com.gatelms.dto.RevisionDrillRequest
import com.gatelms.dto.RevisionDrillSource
import com.gatelms.dto.RevisionOverviewDto
import com.gatelms.entity.Question
import com.gatelms.repository.MistakeLogRepository
import com.gatelms.repository.MockAttemptRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class RevisionService(
    private val mistakeLogRepository: MistakeLogRepository,
    private val topicRepository: TopicRepository,
    private val questionRepository: QuestionRepository,
    private val mockAttemptRepository: MockAttemptRepository,
    private val practiceService: PracticeService
) {

    fun getOverview(): RevisionOverviewDto {
        val now = Instant.now()
        val dueMistakes = mistakeLogRepository.countDueReviews(now)
        val dueTopics = topicRepository.countDueTopics(now)
        val pass2Pending = questionRepository.countPass2Pending()
        val bookmarked = questionRepository.countBookmarked()
        val drillsCompleted = mockAttemptRepository.countCompletedDrills()

        return RevisionOverviewDto(
            dueMistakes = dueMistakes,
            dueTopics = dueTopics,
            pass2Pending = pass2Pending,
            bookmarked = bookmarked,
            drillsCompleted = drillsCompleted,
            generatedAt = now
        )
    }

    @Transactional
    fun createDrill(request: RevisionDrillRequest): PracticeSessionResponse {
        val now = Instant.now()
        val targetCount = request.count.coerceIn(1, 50)
        val pageable = PageRequest.of(0, targetCount * 2)

        val questions: List<Question> = when (request.source) {
            RevisionDrillSource.DUE_MISTAKES -> {
                val dueMistakes = mistakeLogRepository.findDueMistakesForDrill(
                    now = now,
                    subjectId = request.subjectId?.ifBlank { null },
                    topicId = request.topicId?.ifBlank { null },
                    pageable = pageable
                )
                dueMistakes.mapNotNull { it.question }
                    .distinctBy { it.id }
                    .take(targetCount)
            }
            RevisionDrillSource.PASS2 -> {
                questionRepository.findPass2Candidates(
                    subjectId = request.subjectId?.ifBlank { null },
                    topicId = request.topicId?.ifBlank { null },
                    pageable = PageRequest.of(0, targetCount)
                )
            }
            RevisionDrillSource.BOOKMARKED -> {
                questionRepository.findBookmarkedCandidates(
                    subjectId = request.subjectId?.ifBlank { null },
                    topicId = request.topicId?.ifBlank { null },
                    pageable = PageRequest.of(0, targetCount)
                )
            }
            RevisionDrillSource.TOPIC -> {
                questionRepository.findDueTopicCandidates(
                    now = now,
                    subjectId = request.subjectId?.ifBlank { null },
                    topicId = request.topicId?.ifBlank { null },
                    pageable = PageRequest.of(0, targetCount)
                )
            }
        }

        if (questions.isEmpty()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No candidates for this drill source")
        }

        return practiceService.createDrillSession(
            questions = questions,
            title = "Weakness Drill · ${request.source.name}",
            source = request.source.name
        )
    }

    @Transactional
    fun redrillMistake(mistakeId: UUID): PracticeSessionResponse {
        val mistake = mistakeLogRepository.findById(mistakeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Mistake log not found with id: $mistakeId")
        }
        val question = mistake.question ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Question associated with mistake not found"
        )

        return practiceService.createDrillSession(
            questions = listOf(question),
            title = "Mistake Redrill - ${question.externalId ?: question.id}",
            source = "MISTAKE_REDRILL"
        )
    }
}

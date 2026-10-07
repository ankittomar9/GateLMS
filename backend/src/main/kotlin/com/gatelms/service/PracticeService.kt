package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.*
import com.gatelms.entity.*
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PracticeService(
    private val questionRepository: QuestionRepository,
    private val mockExamRepository: MockExamRepository,
    private val mockAttemptRepository: MockAttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val topicRepository: TopicRepository,
    private val gradingService: GradingService,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val gateLmsMetrics: com.gatelms.metrics.GateLmsMetrics? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val rollupService: RollupService? = null
) {

    @Transactional
    fun createSession(request: PracticeSessionCreateRequest): PracticeSessionResponse {
        val count = request.count.coerceIn(1, 50)

        val pageable = PageRequest.of(0, count)
        val questionsPage = questionRepository.findWithFilters(
            subjectId = request.subjectId?.ifBlank { null },
            topicId = request.topicId?.ifBlank { null },
            type = request.questionType?.name,
            year = null,
            pass1Status = null,
            pass2Status = null,
            bookmarked = null,
            curationStatus = request.curationStatus?.ifBlank { null },
            source = null,
            difficulty = null,
            q = null,
            pageable = pageable
        )

        val initialList = if (request.marks != null) {
            questionsPage.content.filter { it.marks == request.marks }
        } else {
            questionsPage.content
        }

        val selectedQuestions: List<Question> = if (initialList.isNotEmpty()) {
            initialList
        } else {
            val fallbackPage = questionRepository.findWithFilters(
                subjectId = request.subjectId?.ifBlank { null },
                topicId = request.topicId?.ifBlank { null },
                type = request.questionType?.name,
                year = null,
                pass1Status = null,
                pass2Status = null,
                bookmarked = null,
                curationStatus = null,
                source = null,
                difficulty = null,
                q = null,
                pageable = pageable
            )
            if (request.marks != null) {
                fallbackPage.content.filter { it.marks == request.marks }.ifEmpty { fallbackPage.content }
            } else {
                fallbackPage.content
            }
        }

        if (selectedQuestions.isEmpty()) {
            throw ValidationException("No questions available for the specified practice criteria", emptyMap())
        }

        val totalMarks = selectedQuestions.sumOf { it.marks }.toDouble()
        val mockExam = MockExam(
            title = "Practice Session - ${request.topicId ?: request.subjectId ?: "General"}",
            durationMinutes = 0,
            totalMarks = totalMarks,
            blueprintJson = objectMapper.writeValueAsString(request),
            mode = ExamMode.PRACTICE
        )
        val savedExam = mockExamRepository.save(mockExam)

        val attempt = MockAttempt(
            mockExam = savedExam,
            status = MockAttemptStatus.IN_PROGRESS
        )
        val savedAttempt = mockAttemptRepository.save(attempt)

        // Pre-create attempt answers in NOT_VISITED state
        selectedQuestions.forEach { q ->
            val ans = AttemptAnswer(
                attempt = savedAttempt,
                question = q,
                state = AttemptAnswerState.NOT_VISITED
            )
            attemptAnswerRepository.save(ans)
        }

        val safeQuestionDtos = selectedQuestions.map { q ->
            PracticeQuestionDto(
                id = q.id ?: UUID.randomUUID(),
                externalId = q.externalId,
                type = q.type,
                marks = q.marks,
                statement = q.statement,
                optionsJson = q.optionsJson,
                topicId = q.topic?.id,
                subjectId = q.subject?.id,
                year = q.year
            )
        }

        return PracticeSessionResponse(
            sessionId = savedExam.id,
            attemptId = savedAttempt.id,
            questions = safeQuestionDtos,
            totalQuestions = safeQuestionDtos.size
        )
    }

    @Transactional
    fun submitAnswer(attemptId: UUID, submission: PracticeAnswerSubmission): PracticeVerdictResponse {
        val attempt = mockAttemptRepository.findById(attemptId).orElseThrow {
            EntityNotFoundException("Practice session attempt not found with id: $attemptId")
        }
        val question = questionRepository.findById(submission.questionId).orElseThrow {
            EntityNotFoundException("Question not found with id: ${submission.questionId}")
        }

        val qId = question.id ?: submission.questionId
        val startNano = System.nanoTime()
        val grading = gradingService.gradeQuestion(
            question = question,
            selectedOptions = submission.selectedOptions,
            numericAnswer = submission.numericAnswer
        )
        gateLmsMetrics?.recordGradingDuration("practice", java.time.Duration.ofNanos(System.nanoTime() - startNano))

        val ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, qId)
            ?: AttemptAnswer(attempt = attempt, question = question)

        ans.selectedOptions = submission.selectedOptions?.trim()
        ans.numericAnswer = submission.numericAnswer
        ans.state = AttemptAnswerState.ANSWERED
        ans.timeSpentSeconds = submission.timeSpentSeconds
        ans.marksAwarded = grading.marksAwarded
        ans.isCorrect = grading.isCorrect
        attemptAnswerRepository.save(ans)

        rollupService?.recordAnswer(
            activityDate = java.time.LocalDate.now(),
            correct = grading.isCorrect,
            seconds = submission.timeSpentSeconds ?: 0,
            mode = "practice"
        )

        // Auto-update Question pass1_status
        question.pass1Status = if (grading.isCorrect) Pass1Status.SOLVED else Pass1Status.FAILED
        questionRepository.save(question)

        // Mastery auto-flow (§2.4)
        val topic = question.topic
        val updatedMastery = if (topic != null) {
            if (grading.isCorrect) {
                topic.masteryLevel = minOf(5, topic.masteryLevel + 1)
            } else {
                topic.masteryLevel = maxOf(0, topic.masteryLevel - 1)
            }
            topic.lastRevisedAt = Instant.now()
            val savedTopic = topicRepository.save(topic)
            savedTopic.masteryLevel
        } else {
            0
        }

        return PracticeVerdictResponse(
            correct = grading.isCorrect,
            marksAwarded = grading.marksAwarded,
            correctOptions = question.correctOptions,
            numericMin = question.numericMin,
            numericMax = question.numericMax,
            explanation = question.explanation,
            topicMastery = updatedMastery,
            mistakeLogPrompt = !grading.isCorrect
        )
    }

    @Transactional
    fun skipQuestion(attemptId: UUID, questionId: UUID) {
        val attempt = mockAttemptRepository.findById(attemptId).orElseThrow {
            EntityNotFoundException("Practice session attempt not found with id: $attemptId")
        }
        val question = questionRepository.findById(questionId).orElseThrow {
            EntityNotFoundException("Question not found with id: $questionId")
        }

        val qId = question.id ?: questionId
        val ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, qId)
            ?: AttemptAnswer(attempt = attempt, question = question)

        ans.state = AttemptAnswerState.NOT_ANSWERED
        attemptAnswerRepository.save(ans)
    }

    @Transactional
    fun revealQuestion(attemptId: UUID, questionId: UUID): PracticeVerdictResponse {
        val attempt = mockAttemptRepository.findById(attemptId).orElseThrow {
            EntityNotFoundException("Practice session attempt not found with id: $attemptId")
        }
        val question = questionRepository.findById(questionId).orElseThrow {
            EntityNotFoundException("Question not found with id: $questionId")
        }

        val qId = question.id ?: questionId
        val ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, qId)
            ?: AttemptAnswer(attempt = attempt, question = question)

        ans.state = AttemptAnswerState.NOT_ANSWERED
        ans.isCorrect = false
        ans.marksAwarded = 0.0
        attemptAnswerRepository.save(ans)

        // Reveal marks pass1 as ASSISTED
        question.pass1Status = Pass1Status.ASSISTED
        questionRepository.save(question)

        return PracticeVerdictResponse(
            correct = false,
            marksAwarded = 0.0,
            correctOptions = question.correctOptions,
            numericMin = question.numericMin,
            numericMax = question.numericMax,
            explanation = question.explanation,
            topicMastery = question.topic?.masteryLevel ?: 0,
            mistakeLogPrompt = false
        )
    }

    @Transactional
    fun finishSession(attemptId: UUID): PracticeFinishResponse {
        val attempt = mockAttemptRepository.findById(attemptId).orElseThrow {
            EntityNotFoundException("Practice session attempt not found with id: $attemptId")
        }

        attempt.submittedAt = Instant.now()
        attempt.status = MockAttemptStatus.SUBMITTED
        mockAttemptRepository.save(attempt)

        gateLmsMetrics?.recordAttemptSubmitted("practice")

        val answers = attemptAnswerRepository.findByAttemptId(attemptId)

        val correct = answers.count { it.isCorrect == true }
        val wrong = answers.count { it.state == AttemptAnswerState.ANSWERED && it.isCorrect == false }
        val skipped = answers.count { it.state == AttemptAnswerState.NOT_ANSWERED || it.state == AttemptAnswerState.NOT_VISITED }
        val totalTime = answers.sumOf { it.timeSpentSeconds }

        val topicGroups = answers.groupBy { it.question?.topic }
        val topicSummaries = topicGroups.mapNotNull { (topic, ansList) ->
            if (topic == null) null
            else PracticeTopicSummary(
                topicId = topic.id,
                name = topic.name,
                correct = ansList.count { it.isCorrect == true },
                total = ansList.size
            )
        }

        return PracticeFinishResponse(
            correct = correct,
            wrong = wrong,
            skipped = skipped,
            totalQuestions = answers.size,
            timeSpentSeconds = totalTime,
            byTopic = topicSummaries
        )
    }
}

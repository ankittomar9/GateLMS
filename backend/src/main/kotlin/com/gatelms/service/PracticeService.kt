package com.gatelms.service

import com.gatelms.anatomy.repository.findSlugsGroupedByQuestionIds

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.anatomy.service.CardSchedulingService
import com.gatelms.dto.*
import com.gatelms.entity.*
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.*
import com.gatelms.security.CurrentUserProvider
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
    private val questionProgressRepository: QuestionProgressRepository,
    private val topicMasteryRepository: TopicMasteryRepository,
    private val topicMasteryService: TopicMasteryService,
    private val currentUserProvider: CurrentUserProvider,
    private val gradingService: GradingService,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val gateLmsMetrics: com.gatelms.metrics.GateLmsMetrics? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val rollupService: RollupService? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val planProgressService: PlanProgressService? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private val achievementService: AchievementService? = null,
    private val cardSchedulingService: CardSchedulingService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(PracticeService::class.java)

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    var questionConceptTagRepository: com.gatelms.anatomy.repository.QuestionConceptTagRepository? = null

    private fun getConceptTagsForQuestion(questionId: UUID?): List<String> {
        return questionId?.let { questionConceptTagRepository?.findSlugsByQuestionId(it) } ?: emptyList()
    }

    @Transactional
    fun createSession(request: PracticeSessionCreateRequest): PracticeSessionResponse {
        val count = request.count.coerceIn(1, 50)
        val userId = currentUserProvider.getCurrentUserId()

        val pageable = PageRequest.of(0, count)
        val questionsPage = questionRepository.findWithFilters(
            userId = userId,
            subjectId = request.subjectId?.ifBlank { null },
            topicId = request.topicId?.ifBlank { null },
            type = request.questionType?.name,
            year = request.year,
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
                userId = userId,
                subjectId = request.subjectId?.ifBlank { null },
                topicId = request.topicId?.ifBlank { null },
                type = request.questionType?.name,
                year = request.year,
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
            userId = userId,
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

        val qIds = selectedQuestions.mapNotNull { it.id }
        val tagMap = questionConceptTagRepository?.findSlugsGroupedByQuestionIds(qIds) ?: emptyMap()

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
                year = q.year,
                conceptTags = tagMap[q.id] ?: emptyList()
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
    fun createDrillSession(
        questions: List<Question>,
        title: String,
        source: String
    ): PracticeSessionResponse {
        val totalMarks = questions.sumOf { it.marks }.toDouble()
        val mockExam = MockExam(
            title = title,
            durationMinutes = 0,
            totalMarks = totalMarks,
            blueprintJson = """{"source":"$source","count":${questions.size}}""",
            mode = ExamMode.WEAKNESS_DRILL
        )
        val savedExam = mockExamRepository.save(mockExam)

        val userId = currentUserProvider.getCurrentUserId()
        val attempt = MockAttempt(
            userId = userId,
            mockExam = savedExam,
            status = MockAttemptStatus.IN_PROGRESS
        )
        val savedAttempt = mockAttemptRepository.save(attempt)

        questions.forEach { q ->
            val ans = AttemptAnswer(
                attempt = savedAttempt,
                question = q,
                state = AttemptAnswerState.NOT_VISITED
            )
            attemptAnswerRepository.save(ans)
        }

        val qIds = questions.mapNotNull { it.id }
        val tagMap = questionConceptTagRepository?.findSlugsGroupedByQuestionIds(qIds) ?: emptyMap()

        val safeQuestionDtos = questions.map { q ->
            PracticeQuestionDto(
                id = q.id ?: UUID.randomUUID(),
                externalId = q.externalId,
                type = q.type,
                marks = q.marks,
                statement = q.statement,
                optionsJson = q.optionsJson,
                topicId = q.topic?.id,
                subjectId = q.subject?.id,
                year = q.year,
                conceptTags = tagMap[q.id] ?: emptyList()
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
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
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
            mode = "practice",
            userId = currentUserId
        )

        // Plan auto-progress hook (SPEC §2.3)
        val topicId = question.topic?.id
        if (topicId != null) {
            val secondsSpent = submission.timeSpentSeconds ?: 0
            val minutesSpent = maxOf(1, (secondsSpent + 59) / 60)
            try {
                planProgressService?.onQuestionsAnswered(
                    date = java.time.LocalDate.now(),
                    topicId = topicId,
                    count = 1,
                    minutes = minutesSpent,
                    userId = currentUserId
                )
            } catch (e: Exception) {
                logger.warn("Plan progress hook failure ignored during practice grading: ${e.message}", e)
            }
        }

        // Auto-update QuestionProgress pass1_status & pass2_status for this user
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(currentUserId, qId).orElseGet {
            QuestionProgress(id = QuestionProgressId(userId = currentUserId, questionId = qId))
        }
        progress.pass1Status = if (grading.isCorrect) Pass1Status.SOLVED else Pass1Status.FAILED
        progress.lastAttemptedAt = Instant.now()
        if (attempt.mockExam?.mode == ExamMode.WEAKNESS_DRILL) {
            progress.pass2Status = if (grading.isCorrect) Pass2Status.SOLVED else Pass2Status.FAILED
        } else if (progress.pass1Status in listOf(Pass1Status.FAILED, Pass1Status.ASSISTED)) {
            progress.pass2Status = Pass2Status.PENDING
        }
        questionProgressRepository.save(progress)

        // PHASE7-M1 WP2 (SPEC §6.3, PC-2): wrong attempt -> dossier diagnosis -> prerequisite
        // retrieval cards enrolled (due tomorrow). Non-fatal by contract, idempotent by row.
        if (!grading.isCorrect) {
            try {
                cardSchedulingService.enrollPrerequisiteCards(currentUserId, qId)
            } catch (e: Exception) {
                logger.warn("Card auto-enrollment hook failure ignored for question {}: {}", qId, e.message, e)
            }
        }

        // Mastery auto-flow (§2.4) via TopicMastery
        val topic = question.topic
        val updatedMastery = if (topic != null) {
            val mastery = topicMasteryService.getMastery(topic.id, currentUserId)
            val newLevel = if (grading.isCorrect) {
                minOf(5, mastery.masteryLevel + 1)
            } else {
                maxOf(0, mastery.masteryLevel - 1)
            }
            mastery.masteryLevel = newLevel
            if (!grading.isCorrect) {
                mastery.nextRevisionDue = Instant.now().plus(java.time.Duration.ofDays(3))
            }
            mastery.lastRevisedAt = Instant.now()
            topicMasteryRepository.save(mastery)
            newLevel
        } else {
            0
        }

        // Engagement achievements hook (SPEC §2.3)
        try {
            achievementService?.checkOnAnswerSubmitted(currentUserId)
            if (updatedMastery >= 5) {
                achievementService?.checkOnTopicMastery(updatedMastery, currentUserId)
            }
        } catch (e: Exception) {
            logger.warn("Achievement check failed in practice grading: ${e.message}", e)
        }

        val qTags = getConceptTagsForQuestion(question.id)
        return PracticeVerdictResponse(
            correct = grading.isCorrect,
            marksAwarded = grading.marksAwarded,
            correctOptions = question.correctOptions,
            numericMin = question.numericMin,
            numericMax = question.numericMax,
            explanation = question.explanation,
            topicMastery = updatedMastery,
            mistakeLogPrompt = !grading.isCorrect,
            conceptTags = qTags
        )
    }

    @Transactional
    fun skipQuestion(attemptId: UUID, questionId: UUID) {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
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
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
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

        // Reveal marks pass1 as ASSISTED and enters pass2 as PENDING for this user
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(currentUserId, qId).orElseGet {
            QuestionProgress(id = QuestionProgressId(userId = currentUserId, questionId = qId))
        }
        progress.pass1Status = Pass1Status.ASSISTED
        progress.pass2Status = Pass2Status.PENDING
        progress.lastAttemptedAt = Instant.now()
        questionProgressRepository.save(progress)

        val topicMastery = question.topic?.id?.let { tId ->
            topicMasteryRepository.findByIdUserIdAndIdTopicId(currentUserId, tId).orElse(null)?.masteryLevel
        } ?: 0

        val qTags = getConceptTagsForQuestion(question.id)
        return PracticeVerdictResponse(
            correct = false,
            marksAwarded = 0.0,
            correctOptions = question.correctOptions,
            numericMin = question.numericMin,
            numericMax = question.numericMax,
            explanation = question.explanation,
            topicMastery = topicMastery,
            mistakeLogPrompt = false,
            conceptTags = qTags
        )
    }

    @Transactional
    fun finishSession(attemptId: UUID): PracticeFinishResponse {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
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

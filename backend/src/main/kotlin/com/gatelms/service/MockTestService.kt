package com.gatelms.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.anatomy.service.CardSchedulingService
import com.gatelms.dto.*
import com.gatelms.entity.*
import com.gatelms.repository.*
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class MockTestService(
    private val mockExamRepository: MockExamRepository,
    private val mockAttemptRepository: MockAttemptRepository,
    private val attemptAnswerRepository: AttemptAnswerRepository,
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository,
    private val mockAssemblyService: MockAssemblyService,
    private val gradingService: GradingService,
    private val objectMapper: ObjectMapper,
    private val currentUserProvider: com.gatelms.security.CurrentUserProvider,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val gateLmsMetrics: com.gatelms.metrics.GateLmsMetrics? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val rollupService: RollupService? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val planProgressService: PlanProgressService? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val achievementService: AchievementService? = null,
    private val cardSchedulingService: CardSchedulingService
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(MockTestService::class.java)

    @Transactional
    fun createTemplate(req: CreateMockTemplateRequest): MockTemplateSummaryDto {
        if (req.title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required")
        }

        // Validate section subjectIds
        for (section in req.sections) {
            if (!subjectRepository.existsById(section.subjectId)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject not found: ${section.subjectId}")
            }
        }

        val totalMarksCalculated = req.totalMarks ?: req.sections.sumOf {
            val qCount = it.mcq + it.msq + it.nat
            val marksPerQ = it.marks ?: 1
            qCount * marksPerQ
        }.toDouble()

        val blueprint = MockBlueprint(
            title = req.title,
            sections = req.sections,
            questionPool = req.questionPool,
            shuffle = req.shuffle
        )
        val blueprintJson = objectMapper.writeValueAsString(blueprint)

        val exam = MockExam(
            title = req.title,
            durationMinutes = req.durationMinutes,
            totalMarks = totalMarksCalculated,
            blueprintJson = blueprintJson,
            mode = ExamMode.MOCK
        )
        val saved = mockExamRepository.save(exam)

        val totalQuestions = req.sections.sumOf { it.mcq + it.msq + it.nat }

        return MockTemplateSummaryDto(
            id = saved.id,
            title = saved.title,
            durationMinutes = saved.durationMinutes ?: 180,
            totalMarks = saved.totalMarks ?: 100.0,
            totalQuestions = totalQuestions,
            mode = saved.mode.name,
            createdAt = saved.createdAt,
            sections = req.sections,
            bestScore = null,
            attemptCount = 0
        )
    }

    @Transactional
    fun generatePypMock(year: Int): MockTemplateSummaryDto {
        val pypQuestions = questionRepository.findPypByYear(year).ifEmpty {
            questionRepository.findByYear(year)
        }
        if (pypQuestions.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No questions found for GATE year $year")
        }

        val shortfallWarnings = mutableListOf<String>()

        val gaSubject = subjectRepository.findBySlug("general-aptitude")
        val gaSubjectId = gaSubject?.id ?: "subj_aptitude"

        val gaQuestions = pypQuestions.filter { it.subject?.id == gaSubjectId || it.subject?.slug == "general-aptitude" }
        val coreQuestions = pypQuestions.filter { it.subject?.id != gaSubjectId && it.subject?.slug != "general-aptitude" }

        if (gaQuestions.size < 10) {
            shortfallWarnings.add("General Aptitude: requested 10 questions but only ${gaQuestions.size} available in pool")
        }
        if (coreQuestions.size < 55) {
            shortfallWarnings.add("CSE Core: requested 55 questions but only ${coreQuestions.size} available in pool")
        }

        val sections = mutableListOf<BlueprintSection>()
        val bySubject = pypQuestions.groupBy { it.subject ?: subjectRepository.findById(gaSubjectId).orElse(null) }
            .filterKeys { it != null }

        for ((subj, qList) in bySubject) {
            val mcqCount = qList.count { it.type == QuestionType.MCQ }
            val msqCount = qList.count { it.type == QuestionType.MSQ }
            val natCount = qList.count { it.type == QuestionType.NAT }

            sections.add(
                BlueprintSection(
                    subjectId = subj!!.id,
                    mcq = mcqCount,
                    msq = msqCount,
                    nat = natCount,
                    marks = null
                )
            )
        }

        val totalQuestions = pypQuestions.size
        val totalMarks = pypQuestions.sumOf { it.marks }.toDouble()

        val blueprint = MockBlueprint(
            title = "GATE $year Official Paper",
            year = year,
            source = "GATE_PYP",
            sections = sections,
            shuffle = false
        )
        val blueprintJson = objectMapper.writeValueAsString(blueprint)

        val title = "GATE $year Official Paper"
        val existing = mockExamRepository.findFirstByTitle(title)
        val saved = if (existing != null) {
            existing.blueprintJson = blueprintJson
            existing.totalMarks = totalMarks
            existing.durationMinutes = 180
            mockExamRepository.save(existing)
        } else {
            val exam = MockExam(
                title = title,
                durationMinutes = 180,
                totalMarks = totalMarks,
                blueprintJson = blueprintJson,
                mode = ExamMode.MOCK
            )
            mockExamRepository.save(exam)
        }

        val currentUserId = currentUserProvider.getCurrentUserId()
        val bestScore = mockAttemptRepository.findMaxScoreByMockExamId(saved.id, currentUserId)
        val attemptCount = mockAttemptRepository.countByMockExamIdAndUserIdAndStatus(saved.id, currentUserId, MockAttemptStatus.SUBMITTED).toInt()

        return MockTemplateSummaryDto(
            id = saved.id,
            title = saved.title,
            durationMinutes = saved.durationMinutes ?: 180,
            totalMarks = saved.totalMarks ?: 100.0,
            totalQuestions = totalQuestions,
            mode = saved.mode.name,
            createdAt = saved.createdAt,
            sections = sections,
            bestScore = bestScore,
            attemptCount = attemptCount,
            shortfallWarnings = shortfallWarnings
        )
    }

    @Transactional(readOnly = true)
    fun listTemplates(): List<MockTemplateSummaryDto> {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val exams = mockExamRepository.findByModeOrderByCreatedAtDesc(ExamMode.MOCK)
        return exams.map { exam ->
            val blueprint = mockAssemblyService.parseBlueprint(exam.blueprintJson)
            val totalQuestions = blueprint.sections.sumOf { it.mcq + it.msq + it.nat }
            val bestScore = mockAttemptRepository.findMaxScoreByMockExamId(exam.id, currentUserId)
            val attemptCount = mockAttemptRepository.countByMockExamIdAndUserIdAndStatus(exam.id, currentUserId, MockAttemptStatus.SUBMITTED).toInt()

            MockTemplateSummaryDto(
                id = exam.id,
                title = exam.title,
                durationMinutes = exam.durationMinutes ?: 180,
                totalMarks = exam.totalMarks ?: 100.0,
                totalQuestions = totalQuestions,
                mode = exam.mode.name,
                createdAt = exam.createdAt,
                sections = blueprint.sections,
                bestScore = bestScore,
                attemptCount = attemptCount
            )
        }
    }

    @Transactional(readOnly = true)
    fun getTemplate(id: UUID): MockTemplateSummaryDto {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val exam = mockExamRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Mock template not found")
        }
        val blueprint = mockAssemblyService.parseBlueprint(exam.blueprintJson)
        val totalQuestions = blueprint.sections.sumOf { it.mcq + it.msq + it.nat }
        val bestScore = mockAttemptRepository.findMaxScoreByMockExamId(exam.id, currentUserId)
        val attemptCount = mockAttemptRepository.countByMockExamIdAndUserIdAndStatus(exam.id, currentUserId, MockAttemptStatus.SUBMITTED).toInt()

        return MockTemplateSummaryDto(
            id = exam.id,
            title = exam.title,
            durationMinutes = exam.durationMinutes ?: 180,
            totalMarks = exam.totalMarks ?: 100.0,
            totalQuestions = totalQuestions,
            mode = exam.mode.name,
            createdAt = exam.createdAt,
            sections = blueprint.sections,
            bestScore = bestScore,
            attemptCount = attemptCount
        )
    }

    @Transactional
    fun startAttempt(mockId: UUID): StartMockAttemptResponse {
        val exam = mockExamRepository.findById(mockId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Mock template not found")
        }

        val blueprint = mockAssemblyService.parseBlueprint(exam.blueprintJson)
        val assembled = mockAssemblyService.assembleMock(blueprint)

        val durationSeconds = (exam.durationMinutes ?: 180) * 60
        val questionIds = assembled.questions.map { it.id.toString() }
        val questionIdsJson = objectMapper.writeValueAsString(questionIds)

        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = MockAttempt(
            userId = currentUserId,
            mockExam = exam,
            startedAt = Instant.now(),
            durationSeconds = durationSeconds,
            questionIdsJson = questionIdsJson,
            status = MockAttemptStatus.IN_PROGRESS
        )
        val savedAttempt = mockAttemptRepository.save(attempt)

        // Pre-create attempt_answers with NOT_VISITED state
        for (q in assembled.questions) {
            val answer = AttemptAnswer(
                attempt = savedAttempt,
                question = q,
                state = AttemptAnswerState.NOT_VISITED,
                timeSpentSeconds = 0,
                marksAwarded = 0.0
            )
            attemptAnswerRepository.save(answer)
        }

        // Safe DTO projection — strictly anti-leak! Zero correctOptions, numericMin, numericMax, explanation
        val safeQuestions = assembled.questions.map { q ->
            PracticeQuestionDto(
                id = q.id!!,
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

        val now = Instant.now()

        return StartMockAttemptResponse(
            attemptId = savedAttempt.id,
            mockId = exam.id,
            mockTitle = exam.title,
            durationSeconds = durationSeconds,
            sections = assembled.sections,
            questions = safeQuestions,
            startedAt = savedAttempt.startedAt,
            serverTime = now,
            shortfallWarnings = assembled.shortfallWarnings
        )
    }

    @Transactional
    fun upsertAnswer(attemptId: UUID, questionId: UUID, req: UpsertAnswerRequest): UpsertAnswerResponse {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")
        }

        if (attempt.status != MockAttemptStatus.IN_PROGRESS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is not in progress")
        }

        val deadline = attempt.startedAt.plusSeconds((attempt.durationSeconds ?: 10800).toLong() + 60)
        if (Instant.now().isAfter(deadline)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt time has expired")
        }

        val frozenIds: List<String> = try {
            objectMapper.readValue(attempt.questionIdsJson ?: "[]", object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            emptyList()
        }

        if (!frozenIds.contains(questionId.toString())) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Question $questionId is not part of this attempt")
        }

        val parsedState = if (req.state != null) {
            try {
                if (req.state == "ANSWERED_AND_MARKED") {
                    AttemptAnswerState.ANSWERED_AND_MARKED_FOR_REVIEW
                } else {
                    AttemptAnswerState.valueOf(req.state)
                }
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AttemptAnswerState: ${req.state}")
            }
        } else null

        val answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
            ?: AttemptAnswer(
                attempt = attempt,
                question = questionRepository.findById(questionId).orElseThrow {
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found")
                }
            )

        if (req.selectedOptions != null) {
            answer.selectedOptions = req.selectedOptions
        }
        if (req.numericAnswer != null) {
            answer.numericAnswer = req.numericAnswer
        }
        if (parsedState != null) {
            answer.state = parsedState
        }
        if (req.timeSpentSeconds != null && req.timeSpentSeconds > 0) {
            answer.timeSpentSeconds += req.timeSpentSeconds
        }

        val savedAnswer = attemptAnswerRepository.save(answer)

        return UpsertAnswerResponse(
            saved = true,
            state = savedAnswer.state.name
        )
    }

    @Transactional
    fun submitAttempt(attemptId: UUID, req: SubmitMockAttemptRequest?): SubmitMockAttemptResponse {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")
        }

        if (attempt.status != MockAttemptStatus.IN_PROGRESS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt already submitted or finalized")
        }

        val deadline = attempt.startedAt.plusSeconds((attempt.durationSeconds ?: 10800).toLong() + 60)
        val finishedReason = if (Instant.now().isAfter(deadline)) {
            FinishedReason.AUTO_SUBMITTED
        } else {
            FinishedReason.SUBMITTED
        }

        val frozenIds: List<String> = try {
            objectMapper.readValue(attempt.questionIdsJson ?: "[]", object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            emptyList()
        }

        val existingAnswers = attemptAnswerRepository.findByAttemptId(attemptId).associateBy { it.question?.id }

        var positiveScoreSum = 0.0
        var negativeScoreSum = 0.0
        var correctCount = 0
        var wrongCount = 0
        var unattemptedCount = 0
        var maxMarks = 0.0

        val startNano = System.nanoTime()
        for (qidStr in frozenIds) {
            val qid = UUID.fromString(qidStr)
            val question = questionRepository.findById(qid).orElse(null) ?: continue
            maxMarks += question.marks

            val answer = existingAnswers[qid] ?: AttemptAnswer(
                attempt = attempt,
                question = question,
                state = AttemptAnswerState.NOT_VISITED
            )

            val grading = gradingService.gradeMockQuestion(
                question = question,
                selectedOptions = answer.selectedOptions,
                numericAnswer = answer.numericAnswer,
                state = answer.state
            )

            answer.isCorrect = grading.isCorrect
            answer.marksAwarded = grading.marksAwarded
            attemptAnswerRepository.save(answer)

            if (!grading.isAttempted) {
                unattemptedCount++
            } else if (grading.isCorrect == true) {
                correctCount++
                positiveScoreSum += grading.marksAwarded
            } else {
                wrongCount++
                negativeScoreSum += grading.penalty
                // PHASE7-M1 WP2 (SPEC §6.3, PC-2): wrong mock answer -> prerequisite card
                // enrollment. Non-fatal, idempotent.
                try {
                    attempt.userId?.let { cardSchedulingService.enrollPrerequisiteCards(it, qid) }
                } catch (e: Exception) {
                    logger.warn("Card auto-enrollment hook failure ignored for question {}: {}", qid, e.message)
                }
            }
        }
        gateLmsMetrics?.recordGradingDuration("mock", java.time.Duration.ofNanos(System.nanoTime() - startNano))

        val posScore = BigDecimal.valueOf(positiveScoreSum).setScale(2, RoundingMode.HALF_UP).toDouble()
        val negScore = BigDecimal.valueOf(negativeScoreSum).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totScore = maxOf(0.0, BigDecimal.valueOf(positiveScoreSum - negativeScoreSum).setScale(2, RoundingMode.HALF_UP).toDouble())

        attempt.status = MockAttemptStatus.SUBMITTED
        attempt.submittedAt = Instant.now()
        attempt.positiveScore = posScore
        attempt.negativeScore = negScore
        attempt.totalScore = totScore
        attempt.finishedReason = finishedReason

        mockAttemptRepository.save(attempt)

        gateLmsMetrics?.recordAttemptSubmitted("mock")

        val durationSec = ((attempt.submittedAt ?: Instant.now()).epochSecond - attempt.startedAt.epochSecond).toInt()
        val minutesMocked = maxOf(1, (durationSec + 59) / 60)
        rollupService?.recordMockSubmit(
            activityDate = java.time.LocalDate.now(),
            questionsAnswered = correctCount + wrongCount,
            correctAnswers = correctCount,
            minutesMocked = minutesMocked
        )

        // Plan auto-progress hook (SPEC §2.3)
        try {
            val answeredByTopic = frozenIds.mapNotNull { qidStr ->
                questionRepository.findById(UUID.fromString(qidStr)).orElse(null)
            }.filter { q ->
                val ans = existingAnswers[q.id]
                ans != null && ans.state in listOf(
                    AttemptAnswerState.ANSWERED,
                    AttemptAnswerState.ANSWERED_AND_MARKED,
                    AttemptAnswerState.ANSWERED_AND_MARKED_FOR_REVIEW
                )
            }.groupBy { it.topic?.id }

            val totalQuestionsAnswered = correctCount + wrongCount
            answeredByTopic.forEach { (topicId, qList) ->
                if (topicId != null && qList.isNotEmpty()) {
                    val topicMinutes = if (totalQuestionsAnswered > 0) {
                        maxOf(1, (minutesMocked * qList.size) / totalQuestionsAnswered)
                    } else {
                        1
                    }
                    planProgressService?.onQuestionsAnswered(
                        date = java.time.LocalDate.now(),
                        topicId = topicId,
                        count = qList.size,
                        minutes = topicMinutes
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Plan progress hook failure ignored during mock attempt submit: ${e.message}", e)
        }

        // Engagement achievements hook (SPEC §2.3)
        try {
            achievementService?.checkOnMockSubmitted(score = totScore, maxScore = maxMarks)
        } catch (e: Exception) {
            logger.warn("Achievement check failed in mock attempt submit: ${e.message}", e)
        }

        return SubmitMockAttemptResponse(
            attemptId = attempt.id,
            status = attempt.status.name,
            finishedReason = finishedReason.name,
            positiveScore = posScore,
            negativeScore = negScore,
            totalScore = totScore,
            maxMarks = maxMarks,
            correctCount = correctCount,
            wrongCount = wrongCount,
            unattemptedCount = unattemptedCount,
            totalQuestions = frozenIds.size
        )
    }

    @Transactional(readOnly = true)
    fun getResult(attemptId: UUID): MockResultResponse {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")
        }

        if (attempt.status != MockAttemptStatus.SUBMITTED) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt has not been submitted yet")
        }

        val exam = attempt.mockExam ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Mock template missing")

        val frozenIds: List<String> = try {
            objectMapper.readValue(attempt.questionIdsJson ?: "[]", object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            emptyList()
        }

        val answersMap = attemptAnswerRepository.findByAttemptId(attemptId).associateBy { it.question?.id }

        val questionReviews = mutableListOf<MockQuestionReviewDto>()
        val subjectScoresMap = mutableMapOf<String, MutableList<Pair<Question, AttemptAnswer?>>>()

        var totalTimeSpent = 0
        var totalMaxMarks = 0.0

        for (qidStr in frozenIds) {
            val qid = UUID.fromString(qidStr)
            val question = questionRepository.findById(qid).orElse(null) ?: continue
            val answer = answersMap[qid]

            totalMaxMarks += question.marks
            if (answer != null) {
                totalTimeSpent += answer.timeSpentSeconds
            }

            val subjectId = question.subject?.id ?: "unknown"
            subjectScoresMap.computeIfAbsent(subjectId) { mutableListOf() }.add(Pair(question, answer))

            questionReviews.add(
                MockQuestionReviewDto(
                    questionId = question.id!!,
                    externalId = question.externalId ?: "",
                    type = question.type.name,
                    marks = question.marks,
                    statement = question.statement,
                    optionsJson = question.optionsJson,
                    subjectId = question.subject?.id,
                    subjectName = question.subject?.name,
                    topicId = question.topic?.id,
                    yourAnswer = answer?.selectedOptions,
                    numericAnswer = answer?.numericAnswer,
                    correctAnswer = question.correctOptions,
                    numericMin = question.numericMin,
                    numericMax = question.numericMax,
                    isCorrect = answer?.isCorrect,
                    marksAwarded = answer?.marksAwarded ?: 0.0,
                    state = answer?.state?.name ?: AttemptAnswerState.NOT_VISITED.name,
                    timeSpentSeconds = answer?.timeSpentSeconds ?: 0,
                    explanation = question.explanation
                )
            )
        }

        val sectionScores = subjectScoresMap.map { (subjId, pairs) ->
            val subj = pairs.first().first.subject
            val totalQuestions = pairs.size
            val attemptedCount = pairs.count { (_, a) -> a != null && a.state != AttemptAnswerState.NOT_VISITED && a.state != AttemptAnswerState.NOT_ANSWERED }
            val correctCount = pairs.count { (_, a) -> a?.isCorrect == true }
            val wrongCount = pairs.count { (_, a) -> a?.isCorrect == false }
            val unattemptedCount = totalQuestions - attemptedCount
            val marksAwarded = pairs.sumOf { (_, a) -> a?.marksAwarded ?: 0.0 }
            val maxMarks = pairs.sumOf { (q, _) -> q.marks }.toDouble()
            val accuracy = if (attemptedCount > 0) (correctCount.toDouble() / attemptedCount.toDouble()) * 100.0 else 0.0
            val timeSpent = pairs.sumOf { (_, a) -> a?.timeSpentSeconds ?: 0 }

            MockSectionScoreDto(
                subjectId = subjId,
                subjectName = subj?.name ?: subjId,
                totalQuestions = totalQuestions,
                attemptedCount = attemptedCount,
                correctCount = correctCount,
                wrongCount = wrongCount,
                unattemptedCount = unattemptedCount,
                marksAwarded = BigDecimal.valueOf(marksAwarded).setScale(2, RoundingMode.HALF_UP).toDouble(),
                maxMarks = maxMarks,
                accuracy = BigDecimal.valueOf(accuracy).setScale(1, RoundingMode.HALF_UP).toDouble(),
                timeSpentSeconds = timeSpent
            )
        }

        return MockResultResponse(
            attemptId = attempt.id,
            mockId = exam.id,
            mockTitle = exam.title,
            startedAt = attempt.startedAt,
            submittedAt = attempt.submittedAt,
            durationSeconds = attempt.durationSeconds ?: 10800,
            finishedReason = attempt.finishedReason?.name ?: FinishedReason.SUBMITTED.name,
            totalScore = attempt.totalScore ?: 0.0,
            positiveScore = attempt.positiveScore ?: 0.0,
            negativeScore = attempt.negativeScore ?: 0.0,
            maxMarks = totalMaxMarks,
            timeSpentSeconds = totalTimeSpent,
            questions = questionReviews,
            sectionScores = sectionScores
        )
    }

    @Transactional(readOnly = true)
    fun getAttemptHistory(mockId: UUID): List<MockAttemptHistoryDto> {
        val currentUserId = currentUserProvider.getCurrentUserId()
        return mockAttemptRepository.findByMockExamIdAndUserIdOrderByStartedAtDesc(mockId, currentUserId).map { a ->
            MockAttemptHistoryDto(
                attemptId = a.id,
                startedAt = a.startedAt,
                submittedAt = a.submittedAt,
                durationSeconds = a.durationSeconds,
                totalScore = a.totalScore,
                positiveScore = a.positiveScore,
                negativeScore = a.negativeScore,
                status = a.status.name,
                finishedReason = a.finishedReason?.name
            )
        }
    }

    @Transactional(readOnly = true)
    fun getMockTrend(): List<MockTrendDto> {
        val currentUserId = currentUserProvider.getCurrentUserId()
        val attempts = mockAttemptRepository.findByUserIdAndStatusOrderBySubmittedAtAsc(currentUserId, MockAttemptStatus.SUBMITTED)
        return attempts.mapNotNull { a ->
            val exam = a.mockExam ?: return@mapNotNull null
            val subAt = a.submittedAt ?: a.startedAt
            val totalScore = a.totalScore ?: 0.0
            val maxScore = exam.totalMarks ?: 100.0
            val percentage = if (maxScore > 0) (totalScore / maxScore) * 100.0 else 0.0

            MockTrendDto(
                attemptId = a.id,
                mockId = exam.id,
                mockTitle = exam.title,
                date = subAt,
                totalScore = totalScore,
                maxScore = maxScore,
                percentage = BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP).toDouble(),
                percentileLike = null
            )
        }
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    fun cleanupAbandonedAttempts() {
        // In-flight attempts older than duration + 1 hour are auto-finalized as ABANDONED
        val now = Instant.now()
        val inProgress = mockAttemptRepository.findByStatusOrderBySubmittedAtAsc(MockAttemptStatus.IN_PROGRESS)
        for (attempt in inProgress) {
            val timeoutSeconds = (attempt.durationSeconds ?: 10800) + 3600
            val deadline = attempt.startedAt.plusSeconds(timeoutSeconds.toLong())
            if (now.isAfter(deadline)) {
                attempt.status = MockAttemptStatus.ABANDONED
                attempt.finishedReason = FinishedReason.ABANDONED
                mockAttemptRepository.save(attempt)
            }
        }
    }
}

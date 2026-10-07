package com.gatelms.integration

import com.gatelms.dto.AchievementDto
import com.gatelms.dto.PracticeAnswerSubmission
import com.gatelms.dto.PracticeSessionCreateRequest
import com.gatelms.entity.AchievementCode
import com.gatelms.entity.AttemptAnswer
import com.gatelms.entity.MockAttempt
import com.gatelms.entity.NotificationType
import com.gatelms.repository.*
import com.gatelms.service.AchievementService
import com.gatelms.service.PracticeService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class AchievementAwardIT : BaseIntegrationTest() {

    @Autowired
    lateinit var achievementRepository: AchievementRepository

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var attemptAnswerRepository: AttemptAnswerRepository

    @Autowired
    lateinit var mockAttemptRepository: MockAttemptRepository

    @Autowired
    lateinit var mockExamRepository: MockExamRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var achievementService: AchievementService

    @Autowired
    lateinit var practiceService: PracticeService

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    @BeforeEach
    fun cleanUp() {
        notificationRepository.deleteAll()
        achievementRepository.deleteAll()
        attemptAnswerRepository.deleteAll()
        if (questionRepository.count() < 10) {
            val runner = com.gatelms.seed.QuestionSeedRunner(
                questionRepository = questionRepository,
                subjectRepository = subjectRepository,
                topicRepository = topicRepository,
                objectMapper = objectMapper,
                seedPath = "seed-data/questions.jsonl"
            )
            runner.importQuestions("seed-data/questions.jsonl")
        }
    }

    @Test
    fun `1 first-question award once and idempotent re-trigger`() {
        // Prepare a question
        val question = questionRepository.findAll().first()

        // Create practice session
        val session = practiceService.createSession(
            PracticeSessionCreateRequest(count = 1)
        )
        val targetQuestion = session.questions.first()

        // Submit answer
        val verdict = practiceService.submitAnswer(
            attemptId = session.attemptId,
            submission = PracticeAnswerSubmission(
                questionId = targetQuestion.id,
                selectedOptions = "A",
                timeSpentSeconds = 30
            )
        )
        assertNotNull(verdict)

        // Verify FIRST_QUESTION achievement earned
        val firstQuestionAch = achievementRepository.findByCode(AchievementCode.FIRST_QUESTION.name)
        assertNotNull(firstQuestionAch, "FIRST_QUESTION achievement must be earned after first graded answer")

        // Re-trigger award: should be idempotent and return false
        val retrigger = achievementService.award(AchievementCode.FIRST_QUESTION)
        assertFalse(retrigger, "Re-triggering an already earned achievement must return false")

        val totalFirstQuestionRows = achievementRepository.findAll().count { it.code == AchievementCode.FIRST_QUESTION.name }
        assertEquals(1, totalFirstQuestionRows, "Achievement table must contain exactly one row per code")
    }

    @Test
    fun `2 QUESTIONS_100 at exact boundary`() {
        val topic = topicRepository.findAll().first()

        // Create a dummy attempt for answers without pre-creating answers
        val mockExam = mockExamRepository.save(
            com.gatelms.entity.MockExam(
                title = "Dummy Test Exam",
                durationMinutes = 10,
                totalMarks = 100.0,
                mode = com.gatelms.entity.ExamMode.PRACTICE
            )
        )
        val attempt = mockAttemptRepository.save(
            MockAttempt(
                mockExam = mockExam,
                status = com.gatelms.entity.MockAttemptStatus.IN_PROGRESS
            )
        )

        // Create 100 distinct questions
        val dummyQuestions = (1..100).map { i ->
            com.gatelms.entity.Question(
                statement = "Boundary Question $i",
                type = com.gatelms.entity.QuestionType.MCQ,
                marks = 1,
                topic = topic,
                subject = topic.subject
            )
        }
        val savedQuestions = questionRepository.saveAll(dummyQuestions)

        // Insert 99 distinct dummy answers
        for (i in 0 until 99) {
            val q = savedQuestions[i]
            val answer = AttemptAnswer(
                id = UUID.randomUUID(),
                attempt = attempt,
                question = q
            )
            attemptAnswerRepository.save(answer)
        }

        achievementService.checkOnAnswerSubmitted()
        assertFalse(
            achievementRepository.existsByCode(AchievementCode.QUESTIONS_100.name),
            "QUESTIONS_100 must NOT be awarded at 99 questions"
        )

        // Insert 100th answer
        val q100 = savedQuestions[99]
        val answer100 = AttemptAnswer(
            id = UUID.randomUUID(),
            attempt = attempt,
            question = q100
        )
        attemptAnswerRepository.save(answer100)

        achievementService.checkOnAnswerSubmitted()
        assertTrue(
            achievementRepository.existsByCode(AchievementCode.QUESTIONS_100.name),
            "QUESTIONS_100 MUST be awarded at exact boundary of 100 questions"
        )
    }

    @Test
    fun `3 notification created with award`() {
        // Award STREAK_7
        val awarded = achievementService.award(AchievementCode.STREAK_7)
        assertTrue(awarded)

        // Verify ACHIEVEMENT_EARNED notification exists
        val notifications = notificationRepository.findAll()
        val achNotif = notifications.find { it.type == NotificationType.ACHIEVEMENT_EARNED }
        assertNotNull(achNotif, "ACHIEVEMENT_EARNED notification must be generated")
        assertTrue(achNotif!!.title.contains("Weekly Discipline"))
        assertEquals("/insights", achNotif.link)
        assertNull(achNotif.readAt, "New notification must be unread")
    }

    @Test
    fun `4 locked list shows null earnedAt in achievements endpoint`() {
        // Award only FIRST_MOCK
        achievementService.award(AchievementCode.FIRST_MOCK)

        val response = restTemplate.exchange(
            "/api/v1/engagement/achievements",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<AchievementDto>>() {}
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        val list = response.body
        assertNotNull(list)
        assertEquals(12, list!!.size, "All 12 achievement codes must be returned")

        val firstMock = list.find { it.code == AchievementCode.FIRST_MOCK.name }
        assertNotNull(firstMock)
        assertNotNull(firstMock!!.earnedAt, "Earned achievement must have non-null earnedAt")

        val curator = list.find { it.code == AchievementCode.CURATOR_500.name }
        assertNotNull(curator)
        assertNull(curator!!.earnedAt, "Locked achievement must have null earnedAt")
    }
}

package com.gatelms.integration

import com.gatelms.dto.RevisionOverviewDto
import com.gatelms.entity.*
import com.gatelms.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RevisionOverviewIT : BaseIntegrationTest() {

    @Autowired
    lateinit var mistakeLogRepository: MistakeLogRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var mockExamRepository: MockExamRepository

    @Autowired
    lateinit var mockAttemptRepository: MockAttemptRepository

    @Autowired
    lateinit var attemptAnswerRepository: AttemptAnswerRepository

    private lateinit var subject: Subject
    private lateinit var topic1: Topic
    private lateinit var topic2: Topic

    @BeforeEach
    fun setup() {
        mistakeLogRepository.deleteAll()
        attemptAnswerRepository.deleteAll()
        mockAttemptRepository.deleteAll()
        mockExamRepository.deleteAll()
        questionRepository.deleteAll()

        subject = subjectRepository.findById("subj_algo").orElseGet {
            subjectRepository.save(Subject(id = "subj_algo", name = "Algorithms", slug = "algo", cluster = "core", avgWeightage = 8.0))
        }

        topic1 = topicRepository.findById("top_algo_dp").orElseGet {
            topicRepository.save(Topic(id = "top_algo_dp", subject = subject, name = "Dynamic Programming", slug = "dp"))
        }

        topic2 = topicRepository.findById("top_algo_graphs").orElseGet {
            topicRepository.save(Topic(id = "top_algo_graphs", subject = subject, name = "Graph Algorithms", slug = "graphs"))
        }
    }

    @Test
    fun `1 Overview counts match known fixture with honest exact numbers`() {
        val now = Instant.now()

        // 1. Seed Questions
        val q1 = questionRepository.save(Question(
            statement = "Q1 DP problem",
            subject = subject,
            topic = topic1,
            type = QuestionType.MCQ,
            correctOptions = "A",
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING,
            isBookmarked = true,
            bookmarkedAt = now
        ))

        val q2 = questionRepository.save(Question(
            statement = "Q2 Graph problem",
            subject = subject,
            topic = topic2,
            type = QuestionType.MCQ,
            correctOptions = "B",
            pass1Status = Pass1Status.ASSISTED,
            pass2Status = Pass2Status.PENDING,
            isBookmarked = true,
            bookmarkedAt = now
        ))

        val q3 = questionRepository.save(Question(
            statement = "Q3 Normal problem",
            subject = subject,
            topic = topic1,
            type = QuestionType.MCQ,
            correctOptions = "C",
            pass1Status = Pass1Status.SOLVED,
            pass2Status = Pass2Status.SOLVED,
            isBookmarked = false
        ))

        // 2. Seed Mistake logs (2 due, 1 not due)
        mistakeLogRepository.save(MistakeLog(
            question = q1,
            errorCategory = MistakeErrorCategory.CONCEPTUAL,
            correctiveTakeaway = "Remember DP optimal substructure",
            loggedAt = now.minus(5, ChronoUnit.DAYS),
            nextReviewAt = now.minus(1, ChronoUnit.DAYS), // DUE
            resolved = false
        ))

        mistakeLogRepository.save(MistakeLog(
            question = q2,
            errorCategory = MistakeErrorCategory.CALCULATION,
            correctiveTakeaway = "Double check adjacency matrix index",
            loggedAt = now.minus(3, ChronoUnit.DAYS),
            nextReviewAt = now.minus(2, ChronoUnit.HOURS), // DUE
            resolved = false
        ))

        mistakeLogRepository.save(MistakeLog(
            question = q3,
            errorCategory = MistakeErrorCategory.MISREAD,
            correctiveTakeaway = "Read question carefully",
            loggedAt = now,
            nextReviewAt = now.plus(3, ChronoUnit.DAYS), // NOT DUE
            resolved = false
        ))

        // 3. Seed Topic due dates (1 due, 1 not due)
        topic1.nextRevisionDue = now.minus(1, ChronoUnit.DAYS)
        topicRepository.save(topic1)

        topic2.nextRevisionDue = now.plus(7, ChronoUnit.DAYS)
        topicRepository.save(topic2)

        // 4. Seed completed WEAKNESS_DRILL
        val drillExam = mockExamRepository.save(MockExam(
            title = "Weakness Drill 1",
            durationMinutes = 0,
            totalMarks = 2.0,
            mode = ExamMode.WEAKNESS_DRILL
        ))
        mockAttemptRepository.save(MockAttempt(
            mockExam = drillExam,
            status = MockAttemptStatus.SUBMITTED
        ))

        // Call GET /api/v1/revision/overview
        val response = restTemplate.getForEntity("/api/v1/revision/overview", RevisionOverviewDto::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        val overview = response.body!!

        assertEquals(2L, overview.dueMistakes, "Should have exactly 2 due mistakes")
        assertEquals(1L, overview.dueTopics, "Should have exactly 1 due topic")
        assertEquals(2L, overview.pass2Pending, "Should have exactly 2 pass2 pending questions")
        assertEquals(2L, overview.bookmarked, "Should have exactly 2 bookmarked questions")
        assertEquals(1L, overview.drillsCompleted, "Should have exactly 1 completed drill")
        assertNotNull(overview.generatedAt)
    }

    @Test
    fun `2 Overview reports honest zero counts when queue is clean`() {
        topic1.nextRevisionDue = null
        topicRepository.save(topic1)
        topic2.nextRevisionDue = null
        topicRepository.save(topic2)

        val response = restTemplate.getForEntity("/api/v1/revision/overview", RevisionOverviewDto::class.java)
        assertEquals(HttpStatus.OK, response.statusCode)
        val overview = response.body!!

        assertEquals(0L, overview.dueMistakes)
        assertEquals(0L, overview.dueTopics)
        assertEquals(0L, overview.pass2Pending)
        assertEquals(0L, overview.bookmarked)
        assertEquals(0L, overview.drillsCompleted)
        assertNotNull(overview.generatedAt)
    }
}

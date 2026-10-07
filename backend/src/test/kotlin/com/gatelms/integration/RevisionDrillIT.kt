package com.gatelms.integration

import com.gatelms.dto.*
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

class RevisionDrillIT : BaseIntegrationTest() {

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

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    @Autowired
    lateinit var topicMasteryRepository: TopicMasteryRepository

    private val operatorId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private lateinit var subject: Subject
    private lateinit var topic: Topic

    @BeforeEach
    fun setup() {
        mistakeLogRepository.deleteAll()
        attemptAnswerRepository.deleteAll()
        mockAttemptRepository.deleteAll()
        mockExamRepository.deleteAll()
        questionProgressRepository.deleteAll()
        topicMasteryRepository.deleteAll()
        questionRepository.deleteAll()

        subject = subjectRepository.findById("subj_cn").orElseGet {
            subjectRepository.save(Subject(id = "subj_cn", name = "Computer Networks", slug = "cn", cluster = "applied", avgWeightage = 7.0))
        }

        topic = topicRepository.findById("top_cn_tcp").orElseGet {
            topicRepository.save(Topic(id = "top_cn_tcp", subject = subject, name = "TCP and Congestion", slug = "tcp"))
        }
    }

    @Test
    fun `1 DUE_MISTAKES drill selects due-mistake questions in next_review_at order`() {
        val now = Instant.now()

        val q1 = questionRepository.save(Question(
            statement = "TCP Flow Control",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "A"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, q1.id!!),
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING
        ))

        val q2 = questionRepository.save(Question(
            statement = "TCP Congestion Control",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "B"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, q2.id!!),
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING
        ))

        // Mistake 2 was due earlier than Mistake 1
        mistakeLogRepository.save(MistakeLog(
            userId = operatorId,
            question = q1,
            errorCategory = MistakeErrorCategory.CONCEPTUAL,
            correctiveTakeaway = "Review sliding window",
            loggedAt = now.minus(3, ChronoUnit.DAYS),
            nextReviewAt = now.minus(1, ChronoUnit.DAYS),
            resolved = false
        ))

        mistakeLogRepository.save(MistakeLog(
            userId = operatorId,
            question = q2,
            errorCategory = MistakeErrorCategory.CONCEPTUAL,
            correctiveTakeaway = "Review slow start threshold",
            loggedAt = now.minus(5, ChronoUnit.DAYS),
            nextReviewAt = now.minus(2, ChronoUnit.DAYS), // Due EARLIER
            resolved = false
        ))

        val req = RevisionDrillRequest(
            source = RevisionDrillSource.DUE_MISTAKES,
            count = 10
        )

        val resp = restTemplate.postForEntity("/api/v1/revision/drills", req, PracticeSessionResponse::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val session = resp.body!!
        assertEquals(2, session.totalQuestions)
        // First question should be q2 because its nextReviewAt was earlier
        assertEquals(q2.id, session.questions[0].id)
        assertEquals(q1.id, session.questions[1].id)
    }

    @Test
    fun `2 PASS2 pool honors pass1 FAILED or ASSISTED and pass2 PENDING`() {
        val qFailed = questionRepository.save(Question(
            statement = "Failed Q",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "A"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, qFailed.id!!),
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING
        ))

        val qAssisted = questionRepository.save(Question(
            statement = "Assisted Q",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "B"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, qAssisted.id!!),
            pass1Status = Pass1Status.ASSISTED,
            pass2Status = Pass2Status.PENDING
        ))

        val qSolved = questionRepository.save(Question(
            statement = "Solved Q",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "C"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, qSolved.id!!),
            pass1Status = Pass1Status.SOLVED,
            pass2Status = Pass2Status.PENDING // should NOT be included because pass1 is SOLVED
        ))

        val req = RevisionDrillRequest(
            source = RevisionDrillSource.PASS2,
            count = 10
        )

        val resp = restTemplate.postForEntity("/api/v1/revision/drills", req, PracticeSessionResponse::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val session = resp.body!!
        assertEquals(2, session.totalQuestions)
        val ids = session.questions.map { it.id }.toSet()
        assertTrue(ids.contains(qFailed.id))
        assertTrue(ids.contains(qAssisted.id))
        assertFalse(ids.contains(qSolved.id))
    }

    @Test
    fun `3 Empty drill source returns 409 Conflict with actionable detail`() {
        val req = RevisionDrillRequest(
            source = RevisionDrillSource.DUE_MISTAKES,
            count = 5
        )

        val resp = restTemplate.postForEntity("/api/v1/revision/drills", req, String::class.java)
        assertEquals(HttpStatus.CONFLICT, resp.statusCode)
        assertTrue(resp.body!!.contains("No candidates for this drill source"))
    }

    @Test
    fun `4 Drill correct answer moves pass2 PENDING to SOLVED`() {
        val q = questionRepository.save(Question(
            statement = "TCP Three-way Handshake",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "A"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, q.id!!),
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING
        ))

        val createResp = restTemplate.postForEntity(
            "/api/v1/revision/drills",
            RevisionDrillRequest(source = RevisionDrillSource.PASS2, count = 1),
            PracticeSessionResponse::class.java
        )
        assertEquals(HttpStatus.OK, createResp.statusCode)
        val session = createResp.body!!

        // Submit correct answer
        val answerSubmission = PracticeAnswerSubmission(
            questionId = q.id!!,
            selectedOptions = "A",
            timeSpentSeconds = 30
        )

        val verdictResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/answers",
            answerSubmission,
            PracticeVerdictResponse::class.java
        )
        assertEquals(HttpStatus.OK, verdictResp.statusCode)
        assertTrue(verdictResp.body!!.correct)

        // Verify Question pass2_status is now SOLVED
        val updatedQp = questionProgressRepository.findByIdUserIdAndIdQuestionId(operatorId, q.id!!).get()
        assertEquals(Pass2Status.SOLVED, updatedQp.pass2Status)
        assertNotNull(updatedQp.lastAttemptedAt)
    }

    @Test
    fun `5 Drill wrong answer moves pass2 PENDING to FAILED and updates next_revision_due`() {
        val q = questionRepository.save(Question(
            statement = "TCP Timers",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "B"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, q.id!!),
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING
        ))

        val createResp = restTemplate.postForEntity(
            "/api/v1/revision/drills",
            RevisionDrillRequest(source = RevisionDrillSource.PASS2, count = 1),
            PracticeSessionResponse::class.java
        )
        val session = createResp.body!!

        // Submit wrong answer
        val answerSubmission = PracticeAnswerSubmission(
            questionId = q.id!!,
            selectedOptions = "C",
            timeSpentSeconds = 45
        )

        val verdictResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/answers",
            answerSubmission,
            PracticeVerdictResponse::class.java
        )
        assertEquals(HttpStatus.OK, verdictResp.statusCode)
        assertFalse(verdictResp.body!!.correct)

        // Verify Question pass2_status is now FAILED
        val updatedQp = questionProgressRepository.findByIdUserIdAndIdQuestionId(operatorId, q.id!!).get()
        assertEquals(Pass2Status.FAILED, updatedQp.pass2Status)
        assertNotNull(updatedQp.lastAttemptedAt)

        // Verify topic next_revision_due is set to approx now + 3 days
        val updatedMastery = topicMasteryRepository.findByIdUserIdAndIdTopicId(operatorId, topic.id).get()
        assertNotNull(updatedMastery.nextRevisionDue)
        val expectedDueMin = Instant.now().plus(2, ChronoUnit.DAYS)
        val expectedDueMax = Instant.now().plus(4, ChronoUnit.DAYS)
        assertTrue(updatedMastery.nextRevisionDue!!.isAfter(expectedDueMin))
        assertTrue(updatedMastery.nextRevisionDue!!.isBefore(expectedDueMax))
    }

    @Test
    fun `6 Skip question leaves pass2 PENDING`() {
        val q = questionRepository.save(Question(
            statement = "TCP Silly Window Syndrome",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "D"
        ))
        questionProgressRepository.save(QuestionProgress(
            id = QuestionProgressId(operatorId, q.id!!),
            pass1Status = Pass1Status.FAILED,
            pass2Status = Pass2Status.PENDING
        ))

        val createResp = restTemplate.postForEntity(
            "/api/v1/revision/drills",
            RevisionDrillRequest(source = RevisionDrillSource.PASS2, count = 1),
            PracticeSessionResponse::class.java
        )
        val session = createResp.body!!

        // Skip question
        val skipResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/skip",
            PracticeQuestionActionRequest(questionId = q.id!!),
            Void::class.java
        )
        assertEquals(HttpStatus.OK, skipResp.statusCode)

        val updatedQp = questionProgressRepository.findByIdUserIdAndIdQuestionId(operatorId, q.id!!).get()
        assertEquals(Pass2Status.PENDING, updatedQp.pass2Status, "Skipped question should remain PENDING")
    }

    @Test
    fun `7 Redrill mistake creates 1-question WEAKNESS_DRILL session`() {
        val q = questionRepository.save(Question(
            statement = "TCP Header length",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "A"
        ))

        val mistake = mistakeLogRepository.save(MistakeLog(
            userId = operatorId,
            question = q,
            errorCategory = MistakeErrorCategory.MISREAD,
            correctiveTakeaway = "Remember scaling by 4 bytes",
            loggedAt = Instant.now(),
            resolved = false
        ))

        val resp = restTemplate.postForEntity(
            "/api/v1/revision/mistakes/${mistake.id}/redrill",
            null,
            PracticeSessionResponse::class.java
        )
        assertEquals(HttpStatus.OK, resp.statusCode)
        val session = resp.body!!
        assertEquals(1, session.totalQuestions)
        assertEquals(q.id, session.questions[0].id)
    }
}

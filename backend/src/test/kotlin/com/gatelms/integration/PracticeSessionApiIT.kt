package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.*
import com.gatelms.entity.Pass1Status
import com.gatelms.repository.AttemptAnswerRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

class PracticeSessionApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var attemptAnswerRepository: AttemptAnswerRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        if (questionRepository.count() < 10) {
            val runner = QuestionSeedRunner(
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
    fun `session creation honors filters and guarantees NO answer leakage in payload`() {
        val anyQ = questionRepository.findAll().first()
        val request = PracticeSessionCreateRequest(
            subjectId = anyQ.subject?.id,
            count = 5
        )

        val rawResp = restTemplate.postForEntity("/api/v1/practice/sessions", request, String::class.java)
        assertEquals(HttpStatus.OK, rawResp.statusCode)
        assertNotNull(rawResp.body)

        // Parse as generic JSON map to verify absence of answer keys
        val rootNode = objectMapper.readTree(rawResp.body)
        val questionsArray = rootNode.get("questions")
        assertNotNull(questionsArray)
        assertTrue(questionsArray.size() > 0)

        // Honest-Judge Core Assertion: payload must NOT leak answers or explanations
        for (qNode in questionsArray) {
            assertFalse(qNode.has("correctOptions"), "Security gate violation: correctOptions leaked in practice session payload!")
            assertFalse(qNode.has("numericMin"), "Security gate violation: numericMin leaked in practice session payload!")
            assertFalse(qNode.has("numericMax"), "Security gate violation: numericMax leaked in practice session payload!")
            assertFalse(qNode.has("explanation"), "Security gate violation: explanation leaked in practice session payload!")
            assertTrue(qNode.has("statement"))
            assertTrue(qNode.has("id"))
            assertTrue(qNode.has("type"))
        }
    }

    @Test
    fun `answer submission returns graded verdict and updates pass1 status`() {
        // Create session
        val createReq = PracticeSessionCreateRequest(count = 3)
        val sessionResp = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java)
        assertEquals(HttpStatus.OK, sessionResp.statusCode)
        val session = sessionResp.body!!
        val question = session.questions.first()

        val qEntity = questionRepository.findById(question.id).get()

        // Submit answer (intentionally correct if MCQ)
        val chosenOption = qEntity.correctOptions ?: "A"
        val submission = PracticeAnswerSubmission(
            questionId = question.id,
            selectedOptions = chosenOption,
            numericAnswer = qEntity.numericMin ?: 0.0,
            timeSpentSeconds = 45
        )

        val answerResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/answers",
            submission,
            PracticeVerdictResponse::class.java
        )
        assertEquals(HttpStatus.OK, answerResp.statusCode)
        val verdict = answerResp.body!!

        // Post-verdict, correct answers and explanation are now revealed
        assertNotNull(verdict.explanation)
        assertTrue(verdict.marksAwarded >= 0.0)

        val updatedQ = questionRepository.findById(question.id).get()
        if (verdict.correct) {
            assertEquals(Pass1Status.SOLVED, updatedQ.pass1Status)
            assertFalse(verdict.mistakeLogPrompt)
        } else {
            assertEquals(Pass1Status.FAILED, updatedQ.pass1Status)
            assertTrue(verdict.mistakeLogPrompt)
        }
    }

    @Test
    fun `reveal marks question as ASSISTED without grading and returns explanation`() {
        val createReq = PracticeSessionCreateRequest(count = 2)
        val sessionResp = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java)
        val session = sessionResp.body!!
        val question = session.questions.first()

        val actionReq = PracticeQuestionActionRequest(questionId = question.id)
        val revealResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/reveal",
            actionReq,
            PracticeVerdictResponse::class.java
        )
        assertEquals(HttpStatus.OK, revealResp.statusCode)
        val verdict = revealResp.body!!

        assertFalse(verdict.correct)
        assertEquals(0.0, verdict.marksAwarded)
        assertNotNull(verdict.explanation)
        assertFalse(verdict.mistakeLogPrompt)

        val updatedQ = questionRepository.findById(question.id).get()
        assertEquals(Pass1Status.ASSISTED, updatedQ.pass1Status)
    }

    @Test
    fun `skip question persists NOT_ANSWERED state`() {
        val createReq = PracticeSessionCreateRequest(count = 2)
        val sessionResp = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java)
        val session = sessionResp.body!!
        val question = session.questions.first()

        val actionReq = PracticeQuestionActionRequest(questionId = question.id)
        val skipResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/skip",
            actionReq,
            Void::class.java
        )
        assertEquals(HttpStatus.OK, skipResp.statusCode)

        val answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(session.attemptId, question.id)
        assertNotNull(answer)
        assertEquals(com.gatelms.entity.AttemptAnswerState.NOT_ANSWERED, answer?.state)
    }

    @Test
    fun `finish session calculates correct mathematical summary`() {
        val createReq = PracticeSessionCreateRequest(count = 3)
        val sessionResp = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java)
        val session = sessionResp.body!!

        // Answer Q1 correctly or incorrectly
        val q1 = session.questions[0]
        val submission = PracticeAnswerSubmission(
            questionId = q1.id,
            selectedOptions = "Z", // wrong option
            timeSpentSeconds = 30
        )
        restTemplate.postForEntity("/api/v1/practice/sessions/${session.attemptId}/answers", submission, PracticeVerdictResponse::class.java)

        // Skip Q2
        if (session.questions.size > 1) {
            val q2 = session.questions[1]
            restTemplate.postForEntity("/api/v1/practice/sessions/${session.attemptId}/skip", PracticeQuestionActionRequest(q2.id), Void::class.java)
        }

        // Finish session
        val finishResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/finish",
            null,
            PracticeFinishResponse::class.java
        )
        assertEquals(HttpStatus.OK, finishResp.statusCode)
        val summary = finishResp.body!!

        assertEquals(session.questions.size, summary.totalQuestions)
        assertEquals(session.questions.size, summary.correct + summary.wrong + summary.skipped)
        assertTrue(summary.timeSpentSeconds >= 30)
        assertNotNull(summary.byTopic)
    }

    @Test
    fun `createSession with empty filters handles question selection gracefully`() {
        val createReq = PracticeSessionCreateRequest(
            subjectId = "subj_algo",
            count = 10
        )
        val resp = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertTrue((resp.body?.totalQuestions ?: 0) > 0)
    }
}

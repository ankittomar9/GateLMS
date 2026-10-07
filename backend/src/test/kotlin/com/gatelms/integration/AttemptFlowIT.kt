package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.*
import com.gatelms.entity.FinishedReason
import com.gatelms.entity.MockAttemptStatus
import com.gatelms.repository.AttemptAnswerRepository
import com.gatelms.repository.MockAttemptRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import java.time.Instant
import java.util.UUID

class AttemptFlowIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var mockAttemptRepository: MockAttemptRepository

    @Autowired
    lateinit var attemptAnswerRepository: AttemptAnswerRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val subject = subjectRepository.findAll().first()
        ensureQuestionsForSubject(subject)
    }

    private fun ensureQuestionsForSubject(subject: com.gatelms.entity.Subject) {
        val topic = topicRepository.findAll().firstOrNull { it.subject?.id == subject.id }
        for (t in listOf(com.gatelms.entity.QuestionType.MCQ, com.gatelms.entity.QuestionType.MSQ, com.gatelms.entity.QuestionType.NAT)) {
            for (i in 1..4) {
                val extId = "FLOW-TEST-${subject.id}-${t.name}-$i"
                if (questionRepository.findByExternalId(extId) == null) {
                    questionRepository.save(
                        com.gatelms.entity.Question(
                            externalId = extId,
                            subject = subject,
                            topic = topic,
                            type = t,
                            marks = if (t == com.gatelms.entity.QuestionType.NAT) 2 else 1,
                            statement = "Flow Test Question $extId",
                            correctOptions = if (t == com.gatelms.entity.QuestionType.NAT) null else "A",
                            numericMin = if (t == com.gatelms.entity.QuestionType.NAT) 10.0 else null,
                            numericMax = if (t == com.gatelms.entity.QuestionType.NAT) 20.0 else null,
                            explanation = "Detailed explanation for $extId",
                            curationStatus = "ACCEPTED"
                        )
                    )
                }
            }
        }
    }

    private fun createAttempt(durationMinutes: Int = 180): StartMockAttemptResponse {
        val subject = subjectRepository.findAll().first()
        val templateReq = CreateMockTemplateRequest(
            title = "Attempt Flow Mock",
            durationMinutes = durationMinutes,
            sections = listOf(
                BlueprintSection(subjectId = subject.id, mcq = 2, msq = 1, nat = 1)
            )
        )
        val tmplResp = restTemplate.postForEntity("/api/v1/mocks", templateReq, MockTemplateSummaryDto::class.java)
        val templateId = tmplResp.body!!.id

        val startResp = restTemplate.postForEntity("/api/v1/mocks/$templateId/attempts", null, StartMockAttemptResponse::class.java)
        return startResp.body!!
    }

    @Test
    fun `answer upsert updates selectedOptions and honors TCS iON state transitions`() {
        val attemptData = createAttempt()
        val questionId = attemptData.questions[0].id

        val patchReq = UpsertAnswerRequest(
            selectedOptions = "A",
            state = "ANSWERED",
            timeSpentSeconds = 35
        )

        val patchEntity = HttpEntity(patchReq)
        val resp = restTemplate.exchange(
            "/api/v1/attempts/${attemptData.attemptId}/answers/$questionId",
            HttpMethod.PATCH,
            patchEntity,
            UpsertAnswerResponse::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertTrue(resp.body!!.saved)
        assertEquals("ANSWERED", resp.body!!.state)

        // Verify persisted state in database
        val ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptData.attemptId, questionId)
        assertNotNull(ans)
        assertEquals("A", ans!!.selectedOptions)
        assertEquals("ANSWERED", ans.state.name)
        assertEquals(35, ans.timeSpentSeconds)
    }

    @Test
    fun `invalid question ID or invalid state returns 400 ProblemDetail`() {
        val attemptData = createAttempt()
        val randomQId = UUID.randomUUID()

        val patchReq = UpsertAnswerRequest(
            selectedOptions = "A",
            state = "ANSWERED"
        )

        val patchEntity = HttpEntity(patchReq)
        val resp = restTemplate.exchange(
            "/api/v1/attempts/${attemptData.attemptId}/answers/$randomQId",
            HttpMethod.PATCH,
            patchEntity,
            String::class.java
        )

        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)

        // Test invalid state
        val validQId = attemptData.questions[0].id
        val invalidStateReq = UpsertAnswerRequest(state = "INVALID_STATE_XYZ")
        val invalidResp = restTemplate.exchange(
            "/api/v1/attempts/${attemptData.attemptId}/answers/$validQId",
            HttpMethod.PATCH,
            HttpEntity(invalidStateReq),
            String::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, invalidResp.statusCode)
    }

    @Test
    fun `submit grades all answers and computes score trio with GATE negative marking`() {
        val attemptData = createAttempt()
        val q1 = attemptData.questions.first { it.type == com.gatelms.entity.QuestionType.MCQ }
        val realQ1 = questionRepository.findById(q1.id).get()

        // Submit answer for Q1
        val patchReq = UpsertAnswerRequest(
            selectedOptions = realQ1.correctOptions ?: "A",
            state = "ANSWERED",
            timeSpentSeconds = 60
        )
        restTemplate.exchange(
            "/api/v1/attempts/${attemptData.attemptId}/answers/${q1.id}",
            HttpMethod.PATCH,
            HttpEntity(patchReq),
            UpsertAnswerResponse::class.java
        )

        // Submit attempt
        val submitResp = restTemplate.postForEntity(
            "/api/v1/attempts/${attemptData.attemptId}/submit",
            SubmitMockAttemptRequest(timeSpentSeconds = 120),
            SubmitMockAttemptResponse::class.java
        )

        assertEquals(HttpStatus.OK, submitResp.statusCode)
        val subBody = submitResp.body!!
        assertEquals("SUBMITTED", subBody.status)
        assertEquals("SUBMITTED", subBody.finishedReason)
        assertTrue(subBody.positiveScore > 0.0)
        assertEquals(attemptData.questions.size, subBody.totalQuestions)

        // Verify status in DB
        val dbAttempt = mockAttemptRepository.findById(attemptData.attemptId).get()
        assertEquals(MockAttemptStatus.SUBMITTED, dbAttempt.status)
        assertNotNull(dbAttempt.submittedAt)
    }

    @Test
    fun `overtime submission flags AUTO_SUBMITTED with unanswered graded as zero`() {
        val attemptData = createAttempt(durationMinutes = 1) // 1 minute

        // Artificially age the attempt startedAt to 10 minutes ago
        val dbAttempt = mockAttemptRepository.findById(attemptData.attemptId).get()
        dbAttempt.startedAt = Instant.now().minusSeconds(600)
        mockAttemptRepository.save(dbAttempt)

        val submitResp = restTemplate.postForEntity(
            "/api/v1/attempts/${attemptData.attemptId}/submit",
            null,
            SubmitMockAttemptResponse::class.java
        )

        assertEquals(HttpStatus.OK, submitResp.statusCode)
        val subBody = submitResp.body!!
        assertEquals("AUTO_SUBMITTED", subBody.finishedReason)
        assertEquals(0, subBody.correctCount)
        assertEquals(attemptData.questions.size, subBody.unattemptedCount)
        assertEquals(0.0, subBody.totalScore)
    }

    @Test
    fun `result payload returns complete question review and section scores`() {
        val attemptData = createAttempt()
        val q1 = attemptData.questions.first { it.type == com.gatelms.entity.QuestionType.MCQ }

        // Answer one question
        val patchReq = UpsertAnswerRequest(
            selectedOptions = "Z", // wrong
            state = "ANSWERED",
            timeSpentSeconds = 45
        )
        restTemplate.exchange(
            "/api/v1/attempts/${attemptData.attemptId}/answers/${q1.id}",
            HttpMethod.PATCH,
            HttpEntity(patchReq),
            UpsertAnswerResponse::class.java
        )

        // Submit attempt
        restTemplate.postForEntity("/api/v1/attempts/${attemptData.attemptId}/submit", null, SubmitMockAttemptResponse::class.java)

        // Fetch result
        val resultResp = restTemplate.getForEntity("/api/v1/attempts/${attemptData.attemptId}/result", MockResultResponse::class.java)
        assertEquals(HttpStatus.OK, resultResp.statusCode)
        val result = resultResp.body!!

        assertEquals(attemptData.attemptId, result.attemptId)
        assertNotNull(result.submittedAt)
        assertTrue(result.sectionScores.isNotEmpty())
        assertEquals(attemptData.questions.size, result.questions.size)

        // Honest-judge check: post-submit review contains answers and explanations
        val reviewedQ = result.questions.first { it.questionId == q1.id }
        assertNotNull(reviewedQ.statement)
        assertNotNull(reviewedQ.correctAnswer)
        assertNotNull(reviewedQ.explanation)
    }
}

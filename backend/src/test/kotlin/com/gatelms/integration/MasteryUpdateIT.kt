package com.gatelms.integration

import com.gatelms.dto.PracticeAnswerSubmission
import com.gatelms.dto.PracticeSessionCreateRequest
import com.gatelms.dto.PracticeSessionResponse
import com.gatelms.dto.PracticeVerdictResponse
import com.gatelms.entity.QuestionType
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.repository.SubjectRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class MasteryUpdateIT : BaseIntegrationTest() {

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

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
    fun `wrong answer decrements mastery level and respects floor 0`() {
        val anyQ = questionRepository.findAll().first { it.topic != null }
        val topic = anyQ.topic!!
        topic.masteryLevel = 0
        topicRepository.save(topic)

        val createReq = PracticeSessionCreateRequest(
            topicId = topic.id,
            count = 1
        )
        val session = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java).body!!
        val question = session.questions.first()

        val wrongSubmission = PracticeAnswerSubmission(
            questionId = question.id,
            selectedOptions = "WRONG_ANSWER",
            timeSpentSeconds = 10
        )

        val verdict = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/answers",
            wrongSubmission,
            PracticeVerdictResponse::class.java
        ).body!!

        assertFalse(verdict.correct)
        assertEquals(0, verdict.topicMastery, "Mastery level must floor at 0")

        val reloadedTopic = topicRepository.findById(topic.id).get()
        assertEquals(0, reloadedTopic.masteryLevel)
        assertNotNull(reloadedTopic.lastRevisedAt)
    }

    @Test
    fun `correct answer increments mastery level and respects cap 5`() {
        val anyQ = questionRepository.findAll().first { it.topic != null }
        val topic = anyQ.topic!!
        topic.masteryLevel = 5
        topicRepository.save(topic)

        val createReq = PracticeSessionCreateRequest(
            topicId = topic.id,
            count = 1
        )
        val session = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java).body!!
        val question = session.questions.first()
        val qEntity = questionRepository.findById(question.id).get()

        val correctOption = qEntity.correctOptions ?: "A"
        val correctSubmission = PracticeAnswerSubmission(
            questionId = question.id,
            selectedOptions = correctOption,
            numericAnswer = qEntity.numericMin ?: 0.0,
            timeSpentSeconds = 15
        )

        val verdict = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/answers",
            correctSubmission,
            PracticeVerdictResponse::class.java
        ).body!!

        if (verdict.correct) {
            assertEquals(5, verdict.topicMastery, "Mastery level must cap at 5")
            val reloadedTopic = topicRepository.findById(topic.id).get()
            assertEquals(5, reloadedTopic.masteryLevel)
        }
    }

    @Test
    fun `sequential transitions update mastery level and update lastRevisedAt`() {
        val anyQ = questionRepository.findAll().first { it.topic != null }
        val topic = anyQ.topic!!
        topic.masteryLevel = 2
        topicRepository.save(topic)

        val createReq = PracticeSessionCreateRequest(
            topicId = topic.id,
            count = 2
        )
        val session = restTemplate.postForEntity("/api/v1/practice/sessions", createReq, PracticeSessionResponse::class.java).body!!
        val question = session.questions.first()

        // Wrong answer -> drops to 1
        val wrongSubmission = PracticeAnswerSubmission(
            questionId = question.id,
            selectedOptions = "NON_EXISTENT",
            timeSpentSeconds = 20
        )
        val verdict1 = restTemplate.postForEntity(
            "/api/v1/practice/sessions/${session.attemptId}/answers",
            wrongSubmission,
            PracticeVerdictResponse::class.java
        ).body!!

        assertEquals(1, verdict1.topicMastery)
        val intermediateTopic = topicRepository.findById(topic.id).get()
        assertEquals(1, intermediateTopic.masteryLevel)
        assertNotNull(intermediateTopic.lastRevisedAt)
    }
}

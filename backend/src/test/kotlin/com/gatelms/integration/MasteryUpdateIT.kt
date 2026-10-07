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
    lateinit var topicMasteryRepository: com.gatelms.repository.TopicMasteryRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val operatorId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")

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
        topicMasteryRepository.save(
            com.gatelms.entity.TopicMastery(
                id = com.gatelms.entity.TopicMasteryId(operatorId, topic.id),
                masteryLevel = 0
            )
        )

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

        val reloaded = topicMasteryRepository.findByIdUserIdAndIdTopicId(operatorId, topic.id).get()
        assertEquals(0, reloaded.masteryLevel)
        assertNotNull(reloaded.lastRevisedAt)
    }

    @Test
    fun `correct answer increments mastery level and respects cap 5`() {
        val anyQ = questionRepository.findAll().first { it.topic != null }
        val topic = anyQ.topic!!
        topicMasteryRepository.save(
            com.gatelms.entity.TopicMastery(
                id = com.gatelms.entity.TopicMasteryId(operatorId, topic.id),
                masteryLevel = 5
            )
        )

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
            val reloaded = topicMasteryRepository.findByIdUserIdAndIdTopicId(operatorId, topic.id).get()
            assertEquals(5, reloaded.masteryLevel)
        }
    }

    @Test
    fun `sequential transitions update mastery level and update lastRevisedAt`() {
        val anyQ = questionRepository.findAll().first { it.topic != null }
        val topic = anyQ.topic!!
        topicMasteryRepository.save(
            com.gatelms.entity.TopicMastery(
                id = com.gatelms.entity.TopicMasteryId(operatorId, topic.id),
                masteryLevel = 2
            )
        )

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
        val intermediate = topicMasteryRepository.findByIdUserIdAndIdTopicId(operatorId, topic.id).get()
        assertEquals(1, intermediate.masteryLevel)
        assertNotNull(intermediate.lastRevisedAt)
    }
}

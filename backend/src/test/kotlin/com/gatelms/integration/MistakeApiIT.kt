package com.gatelms.integration

import com.gatelms.anatomy.entity.ConceptTag
import com.gatelms.anatomy.entity.ConceptTagSource
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.dto.MistakeCreateRequest
import com.gatelms.dto.MistakeDto
import com.gatelms.dto.MistakeReviewRequest
import com.gatelms.entity.MistakeErrorCategory
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Topic
import com.gatelms.repository.MistakeLogRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class MistakeApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var mistakeLogRepository: MistakeLogRepository

    @Autowired
    lateinit var questionConceptTagRepository: QuestionConceptTagRepository

    @Autowired
    lateinit var conceptTagRepository: ConceptTagRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        if (questionRepository.count() == 0L) {
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
    fun `create mistake log with valid categories succeeds and schedules review in 1 day`() {
        val question = questionRepository.findAll().first()
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "CONCEPTUAL",
            description = "Misunderstood master theorem case 2",
            correctiveTakeaway = "Check polynomial difference between log_b(a) and f(n)"
        )

        val resp = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val created = resp.body!!

        assertEquals(MistakeErrorCategory.CONCEPTUAL, created.errorCategory)
        assertEquals("Check polynomial difference between log_b(a) and f(n)", created.correctiveTakeaway)
        assertEquals(0, created.reviewCount)
        assertFalse(created.resolved)
        assertNotNull(created.nextReviewAt)

        val expectedNextReviewMin = Instant.now().plus(23, ChronoUnit.HOURS)
        val expectedNextReviewMax = Instant.now().plus(25, ChronoUnit.HOURS)
        assertTrue(created.nextReviewAt!!.isAfter(expectedNextReviewMin))
        assertTrue(created.nextReviewAt!!.isBefore(expectedNextReviewMax))
    }

    @Test
    fun `create mistake log with invalid error category returns 400 bad request`() {
        val question = questionRepository.findAll().first()
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "RANDOM_INVALID_CATEGORY",
            correctiveTakeaway = "Some takeaway"
        )

        val resp = restTemplate.postForEntity("/api/v1/mistakes", request, String::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
    }

    @Test
    fun `get mistakes filters by due and resolved flags`() {
        val question = questionRepository.findAll().first()

        // Create a mistake due in the past
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "CALCULATION",
            correctiveTakeaway = "Arithmetic error on 2^10"
        )
        val createdResp = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java)
        val mistake = createdResp.body!!

        // Force next_review_at to the past in the DB
        val entity = mistakeLogRepository.findById(mistake.id).get()
        entity.nextReviewAt = Instant.now().minus(2, ChronoUnit.HOURS)
        mistakeLogRepository.save(entity)

        // Query with due=true
        val dueResp = restTemplate.getForEntity("/api/v1/mistakes?due=true&resolved=false", String::class.java)
        assertEquals(HttpStatus.OK, dueResp.statusCode)
        assertTrue(dueResp.body!!.contains("CALCULATION"))
    }

    @Test
    fun `review mistake schedules intervals in sequence 1 to 3 to 7 days and supports resolving`() {
        val question = questionRepository.findAll().first()
        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "MSQ_TRAP",
            correctiveTakeaway = "Remember multiple choices can all be false"
        )
        val created = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java).body!!
        val mId = created.id

        // First review (reviewCount: 0 -> 1): interval should be reviewIntervals[0] = 1 day (or reviewIntervals[1] = 3 days)
        val review1 = restTemplate.exchange(
            "/api/v1/mistakes/$mId/review",
            HttpMethod.PATCH,
            HttpEntity(MistakeReviewRequest()),
            MistakeDto::class.java
        ).body!!
        assertEquals(1, review1.reviewCount)
        assertFalse(review1.resolved)

        // Second review (reviewCount: 1 -> 2): interval is reviewIntervals[1] = 3 days
        val review2 = restTemplate.exchange(
            "/api/v1/mistakes/$mId/review",
            HttpMethod.PATCH,
            HttpEntity(MistakeReviewRequest()),
            MistakeDto::class.java
        ).body!!
        assertEquals(2, review2.reviewCount)
        val daysUntilReview2 = ChronoUnit.DAYS.between(Instant.now(), review2.nextReviewAt)
        assertTrue(daysUntilReview2 in 2..4, "Next review should be scheduled ~3 days out")

        // Third review (reviewCount: 2 -> 3) with resolved=true: interval is reviewIntervals[2] = 7 days
        val review3 = restTemplate.exchange(
            "/api/v1/mistakes/$mId/review",
            HttpMethod.PATCH,
            HttpEntity(MistakeReviewRequest(resolved = true)),
            MistakeDto::class.java
        ).body!!
        assertEquals(3, review3.reviewCount)
        assertTrue(review3.resolved)
        val daysUntilReview3 = ChronoUnit.DAYS.between(Instant.now(), review3.nextReviewAt)
        assertTrue(daysUntilReview3 in 6..8, "Next review should be scheduled ~7 days out")
    }

    @Test
    fun `honest-judge PASS - mistake log surfaces concept tags and topicId for wrong answer review (PC-7)`() {
        val topic = topicRepository.save(
            Topic(
                id = "mistake_concept_top_${UUID.randomUUID().toString().take(6)}",
                subject = subjectRepository.findAll().first(),
                name = "Mistake Concept Topic",
                slug = "mistake-concept-top-${UUID.randomUUID().toString().take(6)}"
            )
        )
        val question = questionRepository.save(
            Question(
                statement = "Semaphore wait operation question for mistake tracking",
                explanation = "Wait decrements the semaphore counter.",
                topic = topic,
                subject = topic.subject,
                type = QuestionType.MCQ,
                correctOptions = "A",
                curationStatus = "ACCEPTED"
            )
        )
        val slug = "semaphore-operations"
        conceptTagRepository.save(ConceptTag(slug = slug, label = "Semaphore Operations", topic = topic))
        questionConceptTagRepository.save(QuestionConceptTag(QuestionConceptTagId(question.id!!, slug), source = ConceptTagSource.MANUAL))

        val request = MistakeCreateRequest(
            questionId = question.id!!,
            errorCategory = "CONCEPTUAL",
            description = "Confused wait and signal semantics",
            correctiveTakeaway = "P(S) decrements, V(S) increments"
        )

        val resp = restTemplate.postForEntity("/api/v1/mistakes", request, MistakeDto::class.java)
        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val created = resp.body!!

        assertEquals(listOf(slug), created.conceptTags)
        assertEquals(topic.id, created.topicId)

        // Also verify GET /api/v1/mistakes returns the concept tags
        val getResp = restTemplate.getForEntity("/api/v1/mistakes?due=false&resolved=false", String::class.java)
        assertEquals(HttpStatus.OK, getResp.statusCode)
        val root = objectMapper.readTree(getResp.body)
        val content = root.get("content")
        assertNotNull(content)
        val foundNode = content.firstOrNull { it.get("id").asText() == created.id.toString() }
        assertNotNull(foundNode)
        assertEquals(slug, foundNode!!.get("conceptTags").first().asText())
        assertEquals(topic.id, foundNode.get("topicId").asText())
    }
}

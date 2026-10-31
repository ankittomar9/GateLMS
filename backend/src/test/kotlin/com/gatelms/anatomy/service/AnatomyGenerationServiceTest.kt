package com.gatelms.anatomy.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gatelms.ai.dto.AiChatResponse
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.dto.AnatomyJsonPayload
import com.gatelms.anatomy.dto.PrerequisiteItem
import com.gatelms.anatomy.dto.TrapAnalysisItem
import com.gatelms.anatomy.entity.AnatomyStatus
import com.gatelms.anatomy.entity.QuestionAnatomy
import com.gatelms.anatomy.exception.AnatomyValidationException
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.QuestionAnatomyRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.entity.Question
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.*

class AnatomyGenerationServiceTest {

    private val questionRepository = mockk<QuestionRepository>()
    private val questionProgressRepository = mockk<QuestionProgressRepository>()
    private val questionAnatomyRepository = mockk<QuestionAnatomyRepository>()
    private val conceptTagRepository = mockk<ConceptTagRepository>()
    private val questionConceptTagRepository = mockk<QuestionConceptTagRepository>()
    private val anatomyValidationService = mockk<AnatomyValidationService>()
    private val aiChatService = mockk<AiChatService>()
    private val aiRateLimiter = mockk<AiRateLimiter>(relaxed = true)
    private val objectMapper = jacksonObjectMapper()

    private val service = AnatomyGenerationService(
        questionRepository = questionRepository,
        questionProgressRepository = questionProgressRepository,
        questionAnatomyRepository = questionAnatomyRepository,
        conceptTagRepository = conceptTagRepository,
        questionConceptTagRepository = questionConceptTagRepository,
        anatomyValidationService = anatomyValidationService,
        aiChatService = aiChatService,
        aiRateLimiter = aiRateLimiter,
        objectMapper = objectMapper
    )

    private val questionId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val testTopic = Topic(id = "top_dbms", name = "Relational Model")
    private val testQuestion = Question(
        id = questionId,
        topic = testTopic,
        statement = "Which normal form prohibits transitive dependency?"
    )

    private val validPayload = AnatomyJsonPayload(
        testedConcept = "Third Normal Form transitive functional dependency elimination",
        invariantRule = "Every non-prime attribute must depend directly on candidate keys",
        trapAnalysis = listOf(
            TrapAnalysisItem(option = "A", verdict = "INCORRECT", whyPlausible = "Confuses 2NF with 3NF", misconception = "Partial vs transitive FD")
        ),
        prerequisites = listOf(
            PrerequisiteItem(conceptSlug = "second-normal-form", conceptLabel = "Second Normal Form", topicSlug = "normalization")
        ),
        conceptTags = listOf("third-normal-form"),
        similarPatternHint = "Standard normalization question"
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        every { questionRepository.findById(questionId) } returns Optional.of(testQuestion)
        every { conceptTagRepository.existsById(any()) } returns true
        every { questionConceptTagRepository.existsById(any()) } returns true
    }

    @Test
    fun `generateOrGetCached returns existing anatomy when cached, valid, and not forced`() {
        val cached = QuestionAnatomy(
            question = testQuestion,
            testedConcept = "Existing Valid Concept",
            status = AnatomyStatus.DRAFT,
            parseError = null
        )
        every { questionAnatomyRepository.findByQuestionId(questionId) } returns Optional.of(cached)

        val result = service.generateOrGetCached(questionId, userId, force = false)

        assertEquals("Existing Valid Concept", result.testedConcept)
        verify(exactly = 0) { aiChatService.chat(any(), any()) }
    }

    @Test
    fun `generateOrGetCached bypasses cache and regenerates when force is true`() {
        val cached = QuestionAnatomy(
            question = testQuestion,
            testedConcept = "Old Concept",
            status = AnatomyStatus.DRAFT,
            parseError = null
        )
        every { questionAnatomyRepository.findByQuestionId(questionId) } returns Optional.of(cached)
        every { aiChatService.chat(any(), any()) } returns AiChatResponse(reply = "{}", model = "gemini-3.5-flash", provider = "GEMINI", latencyMs = 120L)
        every { anatomyValidationService.parseAndValidate(any()) } returns validPayload
        every { questionAnatomyRepository.save(any()) } answers { firstArg() }

        val result = service.generateOrGetCached(questionId, userId, force = true)

        assertEquals(validPayload.testedConcept, result.testedConcept)
        verify(exactly = 1) { aiChatService.chat(any(), any()) }
        verify(exactly = 1) { questionAnatomyRepository.save(any()) }
    }

    @Test
    fun `generateOrGetCached bypasses cache when existing row has parseError or incomplete concept`() {
        val corruptCached = QuestionAnatomy(
            question = testQuestion,
            testedConcept = "Generation parsing incomplete",
            status = AnatomyStatus.DRAFT,
            parseError = "Malformed or unparseable JSON: Unexpected character ('v' (code 118))"
        )
        every { questionAnatomyRepository.findByQuestionId(questionId) } returns Optional.of(corruptCached)
        every { aiChatService.chat(any(), any()) } returns AiChatResponse(reply = "{}", model = "gemini-3.5-flash", provider = "GEMINI", latencyMs = 120L)
        every { anatomyValidationService.parseAndValidate(any()) } returns validPayload
        every { questionAnatomyRepository.save(any()) } answers { firstArg() }

        val result = service.generateOrGetCached(questionId, userId, force = false)

        assertEquals(validPayload.testedConcept, result.testedConcept)
        assertNull(result.parseError)
        verify(exactly = 1) { aiChatService.chat(any(), any()) }
        verify(exactly = 1) { questionAnatomyRepository.save(match { it.parseError == null && it.testedConcept == validPayload.testedConcept }) }
    }

    @Test
    fun `generateOrGetCached throws 502 BAD_GATEWAY and DOES NOT persist corrupt row when AI fails`() {
        every { questionAnatomyRepository.findByQuestionId(questionId) } returns Optional.empty()
        every { aiChatService.chat(any(), any()) } throws RuntimeException("AI provider timed out: 503 Service Unavailable")

        val ex = assertThrows<ResponseStatusException> {
            service.generateOrGetCached(questionId, userId, force = false)
        }

        assertEquals(HttpStatus.BAD_GATEWAY, ex.statusCode)
        assertTrue(ex.reason?.contains("Question anatomy generation failed") == true)
        verify(exactly = 0) { questionAnatomyRepository.save(any()) }
    }
}

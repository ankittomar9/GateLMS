package com.gatelms.anatomy.service

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.dto.AiChatResponse
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.dto.ConceptLessonPayload
import com.gatelms.anatomy.dto.CurationLessonUpdateRequest
import com.gatelms.anatomy.entity.*
import com.gatelms.anatomy.exception.AnatomyValidationException
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.ConceptUnitRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.*

class ConceptLessonServiceTest {

    private val conceptTagRepository = mockk<ConceptTagRepository>()
    private val conceptUnitRepository = mockk<ConceptUnitRepository>()
    private val questionConceptTagRepository = mockk<QuestionConceptTagRepository>()
    private val questionRepository = mockk<QuestionRepository>()
    private val topicPrimerRepository = mockk<TopicPrimerRepository>()
    private val aiChatService = mockk<AiChatService>()
    private val aiRateLimiter = mockk<AiRateLimiter>()
    private val aiProperties = AiProperties()
    private val sampler = TopicGroundingSampler()

    private val lessonService = ConceptLessonService(
        conceptTagRepository = conceptTagRepository,
        conceptUnitRepository = conceptUnitRepository,
        questionConceptTagRepository = questionConceptTagRepository,
        questionRepository = questionRepository,
        topicPrimerRepository = topicPrimerRepository,
        aiChatService = aiChatService,
        aiRateLimiter = aiRateLimiter,
        aiProperties = aiProperties,
        topicGroundingSampler = sampler
    )

    private val testSubject = Subject(id = "sub_os", name = "Operating Systems", slug = "operating-systems")
    private val testTopic = Topic(id = "top_psync", name = "Process Synchronization", slug = "process-synchronization", subject = testSubject)
    private val testTag = ConceptTag(slug = "peterson-software-solutions", label = "Peterson's Software Solution", topic = testTopic)

    // A valid 350-word lesson payload
    private val validMotivation = "Peterson's algorithm is a classical software-based concurrent programming solution to the mutual exclusion problem for two cooperative processes. It eliminates the strict alternation flaw of earlier two-process attempts while satisfying mutual exclusion, progress, and bounded waiting under standard memory sequential consistency semantics without requiring atomic hardware instructions."
    private val validCoreIdea = "The protocol employs two state variables: a boolean flag array flag[2] indicating each process's intent to enter the critical section, and a shared scalar turn indicating whose turn it is to yield priority. When process P_i intends to enter, it asserts flag[i] = true and yields turn = j. It busy-waits in a while loop (flag[j] && turn == j) until process P_j either lowers its flag or resets turn."
    private val validInvariant = "Mutual exclusion invariant: at most one process can have (flag[0] == true && flag[1] == true && turn == i) while in the critical section, because the variable 'turn' cannot simultaneously hold two distinct values."
    private val validWorkedExample = """
        Let us trace a canonical execution interleaving for two processes P0 and P1 competing for the critical section.
        Problem: Both P0 and P1 execute entry code concurrently. We determine the final value of turn and verify mutual exclusion.
        
        Step 1: P0 sets flag[0] = true and executes turn = 1.
        Step 2: Concurrently, P1 sets flag[1] = true and executes turn = 0.
        Step 3: Because memory writes to turn are sequential, the last assignment overwrites the previous value. Suppose P1's write occurs last, so turn = 0.
        Step 4: Now evaluate the entry while conditions:
        - For P0: flag[1] is true, but turn == 1 is false (turn is 0). Hence, the while condition (flag[1] && turn == 1) evaluates to false. P0 enters the critical section immediately.
        - For P1: flag[0] is true and turn == 0 is true. The while condition (flag[0] && turn == 0) evaluates to true. P1 busy-waits in the loop.
        Step 5: When P0 exits, it sets flag[0] = false. This immediately unblocks P1, allowing it to enter the critical section. Thus progress and bounded waiting are guaranteed.
    """.trimIndent()
    private val validTraps = "1. Modern superscalar out-of-order CPUs can reorder the store instructions to flag and turn, violating safety unless explicit memory barrier fences (mfence) are issued. 2. Peterson's algorithm strictly supports two processes; generalized N-process mutual exclusion requires the Bakery algorithm or hardware primitives."

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `honest-judge PASS - valid concept lesson passes validation and word count is in 300 to 600 range`() {
        val payload = ConceptLessonPayload(
            title = "Peterson's Software Solution",
            motivation = validMotivation,
            coreIdea = validCoreIdea,
            invariant = validInvariant,
            workedExample = validWorkedExample,
            workedExampleExternalId = "SO-OS-0001",
            traps = validTraps
        )

        assertDoesNotThrow {
            lessonService.validateLessonPayload(payload, setOf("SO-OS-0001", "SO-OS-0002"))
        }
    }

    @Test
    fun `honest-judge FAIL - worked_example_external_id not in mapped set fails PC-2`() {
        val payload = ConceptLessonPayload(
            title = "Peterson's Software Solution",
            motivation = validMotivation,
            coreIdea = validCoreIdea,
            invariant = validInvariant,
            workedExample = validWorkedExample,
            workedExampleExternalId = "UNMAPPED-EXTERNAL-999",
            traps = validTraps
        )

        val ex = assertThrows<AnatomyValidationException> {
            lessonService.validateLessonPayload(payload, setOf("SO-OS-0001", "SO-OS-0002"))
        }
        assertTrue(ex.message!!.contains("is not mapped to this concept"))
    }

    @Test
    fun `honest-judge FAIL - blank worked_example_external_id fails PC-2`() {
        val payload = ConceptLessonPayload(
            title = "Peterson's Software Solution",
            motivation = validMotivation,
            coreIdea = validCoreIdea,
            invariant = validInvariant,
            workedExample = validWorkedExample,
            workedExampleExternalId = "   ",
            traps = validTraps
        )

        val ex = assertThrows<AnatomyValidationException> {
            lessonService.validateLessonPayload(payload, setOf("SO-OS-0001"))
        }
        assertTrue(ex.message!!.contains("worked_example_external_id must not be blank"))
    }

    @Test
    fun `honest-judge FAIL - lesson word count under 300 words fails PC-2`() {
        val shortPayload = ConceptLessonPayload(
            title = "Short lesson",
            motivation = "Too short motivation.",
            coreIdea = "Short core idea.",
            workedExample = "Step 1: Do something.",
            workedExampleExternalId = "SO-OS-0001"
        )

        val ex = assertThrows<AnatomyValidationException> {
            lessonService.validateLessonPayload(shortPayload, setOf("SO-OS-0001"))
        }
        assertTrue(ex.message!!.contains("total lesson word count must be between 300 and 600 words"))
    }

    @Test
    fun `honest-judge FAIL - lesson word count over 600 words fails PC-2`() {
        val hugeMotivation = "word ".repeat(500)
        val payload = ConceptLessonPayload(
            title = "Overly long lesson",
            motivation = hugeMotivation,
            coreIdea = validCoreIdea,
            workedExample = validWorkedExample,
            workedExampleExternalId = "SO-OS-0001",
            traps = validTraps
        )

        val ex = assertThrows<AnatomyValidationException> {
            lessonService.validateLessonPayload(payload, setOf("SO-OS-0001"))
        }
        assertTrue(ex.message!!.contains("total lesson word count must be between 300 and 600 words"))
    }

    @Test
    fun `honest-judge FAIL - fabricated student statistics fails AF-1`() {
        val badMotivation = "$validMotivation Notice that 72% of candidates fail this question in exam halls."
        val payload = ConceptLessonPayload(
            title = "Peterson's Software Solution",
            motivation = badMotivation,
            coreIdea = validCoreIdea,
            invariant = validInvariant,
            workedExample = validWorkedExample,
            workedExampleExternalId = "SO-OS-0001",
            traps = validTraps
        )

        val ex = assertThrows<AnatomyValidationException> {
            lessonService.validateLessonPayload(payload, setOf("SO-OS-0001"))
        }
        assertTrue(ex.message!!.contains("AF-1 violation"))
    }

    @Test
    fun `honest-judge PASS - generateOrGetCached returns existing lesson when cached and not forced`() {
        val userId = UUID.randomUUID()
        val unit = ConceptUnit(
            id = UUID.randomUUID(),
            conceptTag = testTag,
            title = "Peterson's Software Solution",
            motivation = validMotivation,
            coreIdea = validCoreIdea,
            invariant = validInvariant,
            workedExample = validWorkedExample,
            workedExampleExternalId = "SO-OS-0001",
            traps = validTraps,
            status = ConceptUnitStatus.ACCEPTED
        )

        every { conceptTagRepository.findById(testTag.slug) } returns Optional.of(testTag)
        every { conceptUnitRepository.findByConceptTagSlug(testTag.slug) } returns Optional.of(unit)
        every { questionConceptTagRepository.findByIdConceptTagSlug(testTag.slug, any()) } returns PageImpl(emptyList())

        val res = lessonService.generateOrGetCached(testTag.slug, userId, force = false)

        assertNotNull(res.lesson)
        assertEquals("ACCEPTED", res.lesson!!.status)
        assertEquals("SO-OS-0001", res.lesson!!.workedExampleExternalId)
        verify(exactly = 0) { aiChatService.chat(any(), any()) }
    }

    @Test
    fun `honest-judge PASS - generateOrGetCached force=true regenerates lesson and persists DRAFT`() {
        val userId = UUID.randomUUID()
        val existingUnit = ConceptUnit(
            id = UUID.randomUUID(),
            conceptTag = testTag,
            status = ConceptUnitStatus.DRAFT
        )

        val q = Question(
            id = UUID.randomUUID(),
            externalId = "SO-OS-0001",
            statement = "Sample Peterson question",
            type = QuestionType.MCQ,
            difficulty = "MEDIUM",
            topic = testTopic
        )

        every { conceptTagRepository.findById(testTag.slug) } returns Optional.of(testTag)
        every { conceptUnitRepository.findByConceptTagSlug(testTag.slug) } returns Optional.of(existingUnit)
        every { aiRateLimiter.checkPrimerGenerateLimit(userId) } just Runs

        val link = QuestionConceptTag(
            id = QuestionConceptTagId(q.id!!, testTag.slug),
            source = ConceptTagSource.MANUAL,
            confidence = 1.0f
        )
        every { questionConceptTagRepository.findByIdConceptTagSlug(testTag.slug, any()) } returns PageImpl(listOf(link))
        every { questionRepository.findAllById(listOf(q.id!!)) } returns listOf(q)
        every { topicPrimerRepository.findByTopicId(testTopic.id) } returns Optional.empty()

        val jsonResponse = """
            {
              "title": "Peterson's Software Solution",
              "motivation": "$validMotivation",
              "core_idea": "$validCoreIdea",
              "invariant": "$validInvariant",
              "worked_example": "${validWorkedExample.replace("\n", "\\n")}",
              "worked_example_external_id": "SO-OS-0001",
              "traps": "${validTraps.replace("\n", "\\n")}"
            }
        """.trimIndent()

        every { aiChatService.chat(any(), any()) } returns AiChatResponse(
            reply = jsonResponse,
            provider = "GEMINI",
            model = "gemini-3.8-flash",
            latencyMs = 200
        )
        every { conceptUnitRepository.save(any()) } answers { firstArg() }

        val res = lessonService.generateOrGetCached(testTag.slug, userId, force = true)

        assertNotNull(res.lesson)
        assertEquals("DRAFT", res.lesson!!.status)
        assertEquals("SO-OS-0001", res.lesson!!.workedExampleExternalId)
        verify(exactly = 1) { aiChatService.chat(any(), any()) }
        verify(exactly = 1) { conceptUnitRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - generation failure after retries throws 502 and does not persist corrupt entity`() {
        val userId = UUID.randomUUID()
        val q = Question(
            id = UUID.randomUUID(),
            externalId = "SO-OS-0001",
            statement = "Sample Peterson question",
            type = QuestionType.MCQ,
            difficulty = "MEDIUM",
            topic = testTopic
        )

        every { conceptTagRepository.findById(testTag.slug) } returns Optional.of(testTag)
        every { conceptUnitRepository.findByConceptTagSlug(testTag.slug) } returns Optional.empty()
        every { aiRateLimiter.checkPrimerGenerateLimit(userId) } just Runs

        val link = QuestionConceptTag(
            id = QuestionConceptTagId(q.id!!, testTag.slug),
            source = ConceptTagSource.MANUAL,
            confidence = 1.0f
        )
        every { questionConceptTagRepository.findByIdConceptTagSlug(testTag.slug, any()) } returns PageImpl(listOf(link))
        every { questionRepository.findAllById(listOf(q.id!!)) } returns listOf(q)
        every { topicPrimerRepository.findByTopicId(testTopic.id) } returns Optional.empty()

        // Both attempts return unparseable garbage
        every { aiChatService.chat(any(), any()) } returns AiChatResponse(
            reply = "Server Error 500: upstream failed",
            provider = "GEMINI",
            model = "gemini-3.8-flash",
            latencyMs = 50
        )

        val ex = assertThrows<ResponseStatusException> {
            lessonService.generateOrGetCached(testTag.slug, userId, force = true)
        }

        assertEquals(HttpStatus.BAD_GATEWAY, ex.statusCode)
        verify(exactly = 2) { aiChatService.chat(any(), any()) }
        verify(exactly = 0) { conceptUnitRepository.save(any()) }
    }

    @Test
    fun `honest-judge PASS - admin update and delete curation functions correctly modify ConceptUnit`() {
        val unit = ConceptUnit(
            id = UUID.randomUUID(),
            conceptTag = testTag,
            status = ConceptUnitStatus.DRAFT,
            motivation = "Draft motivation",
            coreIdea = "Draft core idea",
            workedExample = "Draft example"
        )

        every { conceptTagRepository.findById(testTag.slug) } returns Optional.of(testTag)
        every { conceptUnitRepository.findByConceptTagSlug(testTag.slug) } returns Optional.of(unit)
        every { conceptUnitRepository.save(any()) } answers { firstArg() }
        every { conceptUnitRepository.delete(unit) } just Runs

        // 1. Update to ACCEPTED
        val updated = lessonService.updateLesson(
            testTag.slug,
            CurationLessonUpdateRequest(
                status = ConceptUnitStatus.ACCEPTED,
                motivation = "Curated motivation"
            )
        )
        assertEquals("ACCEPTED", updated.status)
        assertEquals("Curated motivation", updated.motivation)
        verify(exactly = 1) { conceptUnitRepository.save(any()) }

        // 2. Delete
        lessonService.deleteLesson(testTag.slug)
        verify(exactly = 1) { conceptUnitRepository.delete(unit) }
    }
}

package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.error.ValidationException
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QuestionServiceTest {

    private val questionRepository = mockk<QuestionRepository>()
    private val subjectRepository = mockk<SubjectRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private lateinit var questionService: QuestionService

    private val testSubject = Subject(id = "subj_os", name = "Operating Systems", slug = "operating-systems")
    private val testTopic = Topic(id = "topic_paging", subject = testSubject, name = "Paging", slug = "paging")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { questionRepository.findByExternalId(any()) } returns null
        questionService = QuestionService(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `honest-judge PASS - valid MCQ create succeeds and persists`() {
        val dto = QuestionUpsertDto(
            externalId = "GATE-CS-2023-Q01",
            subjectId = "subj_os",
            topicId = "topic_paging",
            year = 2023,
            type = QuestionType.MCQ,
            marks = 1,
            statement = "What is the page table base address?",
            optionsJson = """[{"label":"A","text":"1000"},{"label":"B","text":"2000"},{"label":"C","text":"3000"},{"label":"D","text":"4000"}]""",
            correctOptions = "B"
        )

        every { subjectRepository.findById("subj_os") } returns Optional.of(testSubject)
        every { topicRepository.findById("topic_paging") } returns Optional.of(testTopic)
        every { questionRepository.findByExternalId("GATE-CS-2023-Q01") } returns null
        every { questionRepository.save(any()) } answers {
            val q = firstArg<Question>()
            q.id = UUID.randomUUID()
            q
        }

        val result = questionService.createQuestion(dto)

        assertNotNull(result.id)
        assertEquals("GATE-CS-2023-Q01", result.externalId)
        assertEquals(QuestionType.MCQ, result.type)
        assertEquals(1, result.marks)
        verify(exactly = 1) { questionRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - deliberate-wrong NAT with min greater than max is rejected`() {
        val dto = QuestionUpsertDto(
            externalId = "GATE-CS-2023-NAT-ERR",
            subjectId = "subj_os",
            topicId = "topic_paging",
            year = 2023,
            type = QuestionType.NAT,
            marks = 2,
            statement = "Calculate speedup",
            numericMin = 5.0,
            numericMax = 4.0 // Deliberate wrong: min > max
        )

        val ex = assertThrows<ValidationException> {
            questionService.createQuestion(dto)
        }

        assertEquals(true, ex.fieldErrors.containsKey("numericMin"))
        verify(exactly = 0) { questionRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - deliberate-wrong MSQ with zero correct options is rejected`() {
        val dto = QuestionUpsertDto(
            externalId = "GATE-CS-2023-MSQ-ERR",
            subjectId = "subj_os",
            topicId = "topic_paging",
            year = 2023,
            type = QuestionType.MSQ,
            marks = 2,
            statement = "Which of the following are valid page replacement policies?",
            optionsJson = """[{"label":"A","text":"FIFO"},{"label":"B","text":"LRU"},{"label":"C","text":"OPT"},{"label":"D","text":"RAND"}]""",
            correctOptions = "" // Deliberate wrong: 0 correct options
        )

        val ex = assertThrows<ValidationException> {
            questionService.createQuestion(dto)
        }

        assertEquals(true, ex.fieldErrors.containsKey("correctOptions"))
        verify(exactly = 0) { questionRepository.save(any()) }
    }
}

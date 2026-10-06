package com.gatelms.seed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionSeedRunnerTest {

    private val questionRepository = mockk<QuestionRepository>()
    private val subjectRepository = mockk<SubjectRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private lateinit var seedRunner: QuestionSeedRunner

    private val mockSubject = Subject(id = "subj_os", name = "Operating Systems", slug = "operating-systems")
    private val mockTopic = Topic(id = "topic_paging", subject = mockSubject, name = "Paging", slug = "virtual-memory-paging")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { subjectRepository.findBySlug("operating-systems") } returns mockSubject
        every { subjectRepository.findBySlug(any()) } answers {
            val slug = firstArg<String>()
            Subject(id = "subj_$slug", name = slug, slug = slug)
        }
        every { topicRepository.findBySubjectAndTopicSlug(any(), any()) } answers {
            val topicSlug = secondArg<String>()
            Topic(id = "topic_$topicSlug", subject = mockSubject, name = topicSlug, slug = topicSlug)
        }
        every { topicRepository.findBySlug(any()) } answers {
            val slug = firstArg<String>()
            Topic(id = "topic_$slug", subject = mockSubject, name = slug, slug = slug)
        }

        seedRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            seedPath = "seed-data/questions.jsonl"
        )
    }

    @Test
    fun `honest-judge PASS - import sample pack inserts all 26 questions with zero rejections`() {
        every { questionRepository.findByExternalId(any()) } returns null
        every { questionRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importQuestions("seed-data/questions.jsonl")

        assertEquals(26, report.total)
        assertEquals(26, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(0, report.rejected)
        verify(exactly = 26) { questionRepository.save(any()) }
    }

    @Test
    fun `honest-judge PASS - re-running seed import updates all 26 existing questions idempotently`() {
        val existingMap = mutableMapOf<String, Question>()
        every { questionRepository.findByExternalId(any()) } answers {
            val extId = firstArg<String>()
            existingMap.computeIfAbsent(extId) {
                Question(
                    id = UUID.randomUUID(),
                    externalId = extId,
                    subject = mockSubject,
                    topic = mockTopic,
                    year = 2024,
                    type = QuestionType.MCQ,
                    marks = 1,
                    statement = "Existing question"
                )
            }
        }
        every { questionRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importQuestions("seed-data/questions.jsonl")

        assertEquals(26, report.total)
        assertEquals(0, report.inserted)
        assertEquals(26, report.updated)
        assertEquals(0, report.rejected)
        verify(exactly = 26) { questionRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - deliberate-corrupt fixture with 3 valid and 2 invalid questions rejects invalid with reasons`(@TempDir tempDir: Path) {
        val fixtureFile = File(tempDir.toFile(), "corrupt_questions.jsonl")
        fixtureFile.writeText(
            """
            {"externalId":"VALID-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Valid 1","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            {"externalId":"INVALID-NAT","year":2024,"type":"NAT","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Invalid NAT","numericMin":10.0,"numericMax":5.0}
            {"externalId":"VALID-2","year":2024,"type":"NAT","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Valid 2","numericMin":5.0,"numericMax":10.0}
            {"externalId":"INVALID-MCQ","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Invalid MCQ 0 correct","options":{"A":"1","B":"2"},"correctOptions":[]}
            {"externalId":"VALID-3","year":2024,"type":"MSQ","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Valid 3","options":{"A":"1","B":"2"},"correctOptions":["A","B"]}
            """.trimIndent()
        )

        every { questionRepository.findByExternalId(any()) } returns null
        every { questionRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importQuestions(fixtureFile.absolutePath)

        assertEquals(5, report.total)
        assertEquals(3, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(2, report.rejected)
        assertEquals(2, report.rejectionReasons.size)
        assertTrue(report.rejectionReasons.any { it.contains("cannot exceed numericMax") })
        assertTrue(report.rejectionReasons.any { it.contains("MCQ requires exactly 1 correct option") })
        verify(exactly = 3) { questionRepository.save(any()) }
    }
}

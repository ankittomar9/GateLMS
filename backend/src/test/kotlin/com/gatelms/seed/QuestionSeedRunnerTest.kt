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

    @Test
    fun `honest-judge PASS - resolvePaths honors multi-path ordering and fallback`() {
        // Default paths
        val defaultRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            seedPathsStr = "seed-data/questions.jsonl,seed-data/pyp-bank.jsonl,seed-data/so-gate-bank.jsonl",
            seedPath = ""
        )
        val defaultPaths = defaultRunner.resolvePaths()
        assertEquals(3, defaultPaths.size)
        assertEquals("seed-data/questions.jsonl", defaultPaths[0])
        assertEquals("seed-data/pyp-bank.jsonl", defaultPaths[1])
        assertEquals("seed-data/so-gate-bank.jsonl", defaultPaths[2])

        // Backward compatibility with single seedPath
        val legacyRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            seedPathsStr = "",
            seedPath = "custom/legacy-path.jsonl"
        )
        val legacyPaths = legacyRunner.resolvePaths()
        assertEquals(listOf("custom/legacy-path.jsonl"), legacyPaths)
    }

    @Test
    fun `honest-judge PASS - duplicate externalId across multi-path files updates idempotently`(@TempDir tempDir: Path) {
        val file1 = File(tempDir.toFile(), "pack1.jsonl").apply {
            writeText("""{"externalId":"DUP-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Original statement","options":{"A":"1","B":"2"},"correctOptions":["A"]}""")
        }
        val file2 = File(tempDir.toFile(), "pack2.jsonl").apply {
            writeText("""{"externalId":"DUP-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Updated statement","options":{"A":"1","B":"2"},"correctOptions":["A"]}""")
        }

        val store = mutableMapOf<String, Question>()
        every { questionRepository.findByExternalId(any()) } answers { store[firstArg()] }
        every { questionRepository.save(any()) } answers {
            val q = firstArg<Question>()
            store[q.externalId!!] = q
            q
        }

        // File 1 imports new
        val report1 = seedRunner.importQuestions(file1.absolutePath)
        assertEquals(1, report1.inserted)
        assertEquals(0, report1.updated)
        assertEquals("Original statement", store["DUP-1"]?.statement)

        // File 2 encounters duplicate -> updates
        val report2 = seedRunner.importQuestions(file2.absolutePath)
        assertEquals(0, report2.inserted)
        assertEquals(1, report2.updated)
        assertEquals(0, report2.rejected)
        assertEquals("Updated statement", store["DUP-1"]?.statement)
    }

    @Test
    fun `honest-judge PASS - exact duplicate question statement rejected in deduplicated topic`(@TempDir tempDir: Path) {
        val fixtureFile = File(tempDir.toFile(), "dup_statement.jsonl")
        fixtureFile.writeText(
            """
            {"externalId":"DUP-EXACT-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"process-synchronization","statement":"Consider two processes sharing a variable.","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            {"externalId":"DUP-EXACT-2","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"process-synchronization","statement":"Consider two processes sharing a variable.","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            """.trimIndent()
        )

        every { questionRepository.findByExternalId(any()) } returns null
        every { questionRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importQuestions(fixtureFile.absolutePath)

        assertEquals(2, report.total)
        assertEquals(1, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(1, report.rejected)
        assertTrue(report.rejectionReasons.any { it.contains("Duplicate normalized statement detected") })
    }

    @Test
    fun `honest-judge PASS - prefix-rotated duplicate statement rejected in deduplicated topic`(@TempDir tempDir: Path) {
        val fixtureFile = File(tempDir.toFile(), "prefix_rotated_dup.jsonl")
        fixtureFile.writeText(
            """
            {"externalId":"BASE-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"process-synchronization","statement":"Consider two processes sharing a variable.","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            {"externalId":"PREFIX-DUP-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"process-synchronization","statement":"For Process Synchronization: Consider two processes sharing a variable.","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            """.trimIndent()
        )

        every { questionRepository.findByExternalId(any()) } returns null
        every { questionRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importQuestions(fixtureFile.absolutePath)

        assertEquals(2, report.total)
        assertEquals(1, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(1, report.rejected)
        assertTrue(report.rejectionReasons.any { it.contains("Duplicate normalized statement detected") })
    }

    @Test
    fun `honest-judge PASS - parameter variants with different numbers are accepted and distinct patterns tracked`(@TempDir tempDir: Path) {
        val fixtureFile = File(tempDir.toFile(), "param_variants.jsonl")
        fixtureFile.writeText(
            """
            {"externalId":"PARAM-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"process-synchronization","statement":"There are 3 processes sharing 4 resources of the same type.","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            {"externalId":"PARAM-2","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"process-synchronization","statement":"There are 5 processes sharing 7 resources of the same type.","options":{"A":"1","B":"2"},"correctOptions":["A"]}
            """.trimIndent()
        )

        every { questionRepository.findByExternalId(any()) } returns null
        every { questionRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importQuestions(fixtureFile.absolutePath)

        assertEquals(2, report.total)
        assertEquals(2, report.inserted)
        assertEquals(0, report.rejected)
        assertEquals(2, report.uniqueStatements)
        assertEquals(1, report.distinctPatterns)
        val topicStat = report.topicStats["process-synchronization"]
        assertTrue(topicStat != null)
        assertEquals(2, topicStat!!.uniqueStatements)
        assertEquals(1, topicStat.distinctPatterns)
    }

    @Test
    fun `honest-judge PASS - applyRemovals deletes matching questions and is idempotent`(@TempDir tempDir: Path) {
        val removalsFile = File(tempDir.toFile(), "removals.jsonl")
        removalsFile.writeText(
            """
            {"externalId":"REM-1"}
            {"externalId":"REM-2"}
            {"externalId":"REM-ABSENT"}
            """.trimIndent()
        )

        val q1 = Question(id = UUID.randomUUID(), externalId = "REM-1", subject = mockSubject, topic = mockTopic, year = 2024, type = QuestionType.MCQ, marks = 1, statement = "To delete 1")
        val q2 = Question(id = UUID.randomUUID(), externalId = "REM-2", subject = mockSubject, topic = mockTopic, year = 2024, type = QuestionType.MCQ, marks = 1, statement = "To delete 2")

        val store = mutableMapOf("REM-1" to q1, "REM-2" to q2)
        every { questionRepository.findByExternalId(any()) } answers { store[firstArg()] }
        every { questionRepository.delete(any()) } answers {
            val q = firstArg<Question>()
            store.remove(q.externalId)
        }

        // Run 1: 2 items deleted
        val deletedFirst = seedRunner.applyRemovals(removalsFile.absolutePath)
        assertEquals(2, deletedFirst)
        verify(exactly = 2) { questionRepository.delete(any()) }

        // Run 2: idempotent, 0 items deleted
        val deletedSecond = seedRunner.applyRemovals(removalsFile.absolutePath)
        assertEquals(0, deletedSecond)
        verify(exactly = 2) { questionRepository.delete(any()) }
    }

    @Test
    fun `honest-judge PASS - statement normalization strips known prefixes and masks digits in pattern`() {
        val prefixExample = "For Process Synchronization:   Consider  3  processes and 5 semaphores.  "
        val normalized = QuestionSeedRunner.normalizeStatement(prefixExample)
        assertEquals("consider 3 processes and 5 semaphores.", normalized)

        val pattern = QuestionSeedRunner.statementPattern(normalized)
        assertEquals("consider # processes and # semaphores.", pattern)

        val evalExample = "Evaluate for Readers-Writers: Consider 2 readers and 1 writer."
        val normEval = QuestionSeedRunner.normalizeStatement(evalExample)
        assertEquals("consider 2 readers and 1 writer.", normEval)

        val patEval = QuestionSeedRunner.statementPattern(normEval)
        assertEquals("consider # readers and # writer.", patEval)
    }
}

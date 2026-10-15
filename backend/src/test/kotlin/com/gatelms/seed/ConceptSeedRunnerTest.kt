package com.gatelms.seed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.gatelms.anatomy.entity.ConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionRepository
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

class ConceptSeedRunnerTest {

    private val conceptTagRepository = mockk<ConceptTagRepository>()
    private val questionConceptTagRepository = mockk<QuestionConceptTagRepository>()
    private val questionRepository = mockk<QuestionRepository>()
    private val topicRepository = mockk<TopicRepository>()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private lateinit var seedRunner: ConceptSeedRunner

    private val mockSubject = Subject(id = "subj_os", name = "Operating Systems", slug = "operating-systems")
    private val mockTopic = Topic(id = "topic_os_process_sync", subject = mockSubject, name = "Process Synchronization", slug = "process-synchronization")

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { topicRepository.findBySlug("process-synchronization") } returns mockTopic
        every { topicRepository.findById("topic_os_process_sync") } returns Optional.of(mockTopic)
        every { topicRepository.findBySlug(any()) } answers {
            val s = firstArg<String>()
            Topic(id = "topic_$s", subject = mockSubject, name = s, slug = s)
        }

        seedRunner = ConceptSeedRunner(
            conceptTagRepository = conceptTagRepository,
            questionConceptTagRepository = questionConceptTagRepository,
            questionRepository = questionRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            taxonomyPath = "seed-data/concept-taxonomy-pilot.jsonl",
            mappingPath = "seed-data/concept-mapping-pilot.jsonl"
        )
    }

    @Test
    fun `honest-judge PASS - import taxonomy pilot inserts all 10 concepts with zero rejections`() {
        every { conceptTagRepository.findById(any()) } returns Optional.empty()
        every { conceptTagRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importTaxonomy("seed-data/concept-taxonomy-pilot.jsonl")

        assertEquals(10, report.total)
        assertEquals(10, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(0, report.rejected)
        verify(exactly = 10) { conceptTagRepository.save(any()) }
    }

    @Test
    fun `honest-judge PASS - re-running taxonomy import updates all 10 concepts idempotently`() {
        val existingTags = mutableMapOf<String, ConceptTag>()
        every { conceptTagRepository.findById(any()) } answers {
            val slug = firstArg<String>()
            val tag = existingTags.computeIfAbsent(slug) {
                ConceptTag(slug = slug, label = "Old label", topic = mockTopic)
            }
            Optional.of(tag)
        }
        every { conceptTagRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importTaxonomy("seed-data/concept-taxonomy-pilot.jsonl")

        assertEquals(10, report.total)
        assertEquals(0, report.inserted)
        assertEquals(10, report.updated)
        assertEquals(0, report.rejected)
        verify(exactly = 10) { conceptTagRepository.save(any()) }
    }

    @Test
    fun `honest-judge PASS - taxonomy rejection on missing required fields or unknown topic`(@TempDir tempDir: Path) {
        val badFile = File(tempDir.toFile(), "bad-taxonomy.jsonl")
        badFile.writeText(
            """
            {"slug": "", "label": "No slug", "topicSlug": "process-synchronization"}
            {"slug": "missing-label", "label": "", "topicSlug": "process-synchronization"}
            {"slug": "unknown-topic", "label": "Valid", "topicSlug": "non-existent-topic"}
            """.trimIndent()
        )

        every { topicRepository.findBySlug("non-existent-topic") } returns null
        every { topicRepository.findById("non-existent-topic") } returns Optional.empty()

        val report = seedRunner.importTaxonomy(badFile.absolutePath)

        assertEquals(3, report.total)
        assertEquals(0, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(3, report.rejected)
        assertEquals(3, report.rejectionReasons.size)
        assertTrue(report.rejectionReasons[0].contains("slug, label, and topicSlug are required"))
        assertTrue(report.rejectionReasons[1].contains("slug, label, and topicSlug are required"))
        assertTrue(report.rejectionReasons[2].contains("Topic not found"))
    }

    @Test
    fun `honest-judge PASS - import mappings pilot inserts all 143 mappings with zero rejections`() {
        every { questionRepository.findByExternalId(any()) } answers {
            val extId = firstArg<String>()
            Question(
                id = UUID.nameUUIDFromBytes(extId.toByteArray()),
                externalId = extId,
                statement = "Test question statement for $extId",
                type = QuestionType.MCQ
            )
        }
        every { conceptTagRepository.findById(any()) } answers {
            val slug = firstArg<String>()
            Optional.of(ConceptTag(slug = slug, label = slug, topic = mockTopic))
        }
        every { questionConceptTagRepository.findById(any()) } returns Optional.empty()
        every { questionConceptTagRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importMappings("seed-data/concept-mapping-pilot.jsonl")

        assertEquals(143, report.total)
        assertEquals(0, report.rejected)
        assertTrue(report.inserted >= 143, "Expected >= 143 links created (multiple tags for Peterson/Semaphores)")
    }

    @Test
    fun `honest-judge PASS - re-running mappings import updates all links idempotently`() {
        val existingLinks = mutableMapOf<QuestionConceptTagId, QuestionConceptTag>()
        every { questionRepository.findByExternalId(any()) } answers {
            val extId = firstArg<String>()
            Question(
                id = UUID.nameUUIDFromBytes(extId.toByteArray()),
                externalId = extId,
                statement = "Test question statement for $extId",
                type = QuestionType.MCQ
            )
        }
        every { conceptTagRepository.findById(any()) } answers {
            val slug = firstArg<String>()
            Optional.of(ConceptTag(slug = slug, label = slug, topic = mockTopic))
        }
        every { questionConceptTagRepository.findById(any()) } answers {
            val id = firstArg<QuestionConceptTagId>()
            val link = existingLinks.computeIfAbsent(id) {
                QuestionConceptTag(id = id)
            }
            Optional.of(link)
        }
        every { questionConceptTagRepository.save(any()) } answers { firstArg() }

        val report = seedRunner.importMappings("seed-data/concept-mapping-pilot.jsonl")

        assertEquals(143, report.total)
        assertEquals(0, report.inserted)
        assertTrue(report.updated >= 143)
        assertEquals(0, report.rejected)
    }

    @Test
    fun `honest-judge PASS - mapping rejection on unknown question externalId or unknown concept slug`(@TempDir tempDir: Path) {
        val badFile = File(tempDir.toFile(), "bad-mapping.jsonl")
        badFile.writeText(
            """
            {"externalId": "", "conceptSlugs": ["critical-section-requirements"]}
            {"externalId": "NON-EXISTENT-Q", "conceptSlugs": ["critical-section-requirements"]}
            {"externalId": "VALID-Q", "conceptSlugs": []}
            {"externalId": "VALID-Q", "conceptSlugs": ["unknown-concept-slug"]}
            """.trimIndent()
        )

        every { questionRepository.findByExternalId("NON-EXISTENT-Q") } returns null
        every { questionRepository.findByExternalId("VALID-Q") } returns Question(
            id = UUID.randomUUID(),
            externalId = "VALID-Q",
            statement = "Valid question",
            type = QuestionType.MCQ
        )
        every { conceptTagRepository.findById("unknown-concept-slug") } returns Optional.empty()

        val report = seedRunner.importMappings(badFile.absolutePath)

        assertEquals(4, report.total)
        assertEquals(0, report.inserted)
        assertEquals(0, report.updated)
        assertEquals(4, report.rejected)
        assertTrue(report.rejectionReasons[0].contains("externalId is required"))
        assertTrue(report.rejectionReasons[1].contains("Question not found"))
        assertTrue(report.rejectionReasons[2].contains("At least one concept slug is required"))
        assertTrue(report.rejectionReasons[3].contains("Concept tag 'unknown-concept-slug' not registered"))
    }

    @Test
    fun `honest-judge PASS - seedAll executes both imports and counts concepts`() {
        every { conceptTagRepository.findById(any()) } answers {
            val slug = firstArg<String>()
            Optional.of(ConceptTag(slug = slug, label = slug, topic = mockTopic))
        }
        every { conceptTagRepository.save(any()) } answers { firstArg() }
        every { questionRepository.findByExternalId(any()) } answers {
            val extId = firstArg<String>()
            Question(
                id = UUID.nameUUIDFromBytes(extId.toByteArray()),
                externalId = extId,
                statement = "Test question statement",
                type = QuestionType.MCQ
            )
        }
        every { questionConceptTagRepository.findById(any()) } returns Optional.empty()
        every { questionConceptTagRepository.save(any()) } answers { firstArg() }

        val tags = (1..10).map { ConceptTag(slug = "slug-$it", label = "Label $it", topic = mockTopic) }
        every { conceptTagRepository.findAll() } returns tags
        every { questionConceptTagRepository.countByIdConceptTagSlug(any()) } returns 15L
        every { questionConceptTagRepository.countDistinctQuestions() } returns 143L
        every { questionConceptTagRepository.count() } returns 150L

        seedRunner.seedAll(
            taxonomyFile = "seed-data/concept-taxonomy-pilot.jsonl",
            mappingFile = "seed-data/concept-mapping-pilot.jsonl"
        )

        verify(atLeast = 10) { conceptTagRepository.save(any()) }
        verify(atLeast = 143) { questionConceptTagRepository.save(any()) }
        verify(exactly = 10) { questionConceptTagRepository.countByIdConceptTagSlug(any()) }
    }
}

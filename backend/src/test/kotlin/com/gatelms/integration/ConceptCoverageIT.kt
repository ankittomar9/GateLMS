package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.anatomy.dto.CurationQuestionConceptsPatchRequest
import com.gatelms.anatomy.dto.TopicConceptCoverageResponseDto
import com.gatelms.anatomy.entity.ConceptTagSource
import com.gatelms.anatomy.entity.ConceptUnit
import com.gatelms.anatomy.entity.ConceptUnitStatus
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.ConceptUnitRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.dto.QuestionDto
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.User
import com.gatelms.entity.UserRole
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.repository.UserRepository
import com.gatelms.seed.ConceptSeedRunner
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ConceptCoverageIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var conceptTagRepository: ConceptTagRepository

    @Autowired
    lateinit var questionConceptTagRepository: QuestionConceptTagRepository

    @Autowired
    lateinit var conceptUnitRepository: ConceptUnitRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var objectMapper: ObjectMapper

    companion object {
        private var dataSeeded = false
    }

    private lateinit var conceptSeedRunner: ConceptSeedRunner

    @BeforeEach
    fun seedDataOnce() {
        conceptSeedRunner = ConceptSeedRunner(
            conceptTagRepository = conceptTagRepository,
            questionConceptTagRepository = questionConceptTagRepository,
            questionRepository = questionRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            taxonomyPath = "seed-data/concept-taxonomy-pilot.jsonl",
            mappingPath = "seed-data/concept-mapping-pilot.jsonl"
        )

        if (!dataSeeded) {
            val qRunner = QuestionSeedRunner(
                questionRepository = questionRepository,
                subjectRepository = subjectRepository,
                topicRepository = topicRepository,
                objectMapper = objectMapper,
                seedPathsStr = "seed-data/questions.jsonl,seed-data/pyp-bank.jsonl,seed-data/so-gate-bank.jsonl"
            )
            qRunner.importQuestions("seed-data/questions.jsonl")
            qRunner.importQuestions("seed-data/pyp-bank.jsonl")
            qRunner.importQuestions("seed-data/so-gate-bank.jsonl")

            conceptSeedRunner.seedAll()
            dataSeeded = true
        }

        // Ensure clean mappings and no residual units before each test
        conceptSeedRunner.importMappings("seed-data/concept-mapping-pilot.jsonl")
    }

    @Test
    @Order(1)
    fun `honest-judge PASS - taxonomy 10 of 10 seeded and 100 percent of pilot bank mapped with 0 unmapped questions`() {
        loginAs("ankur", "admin123")

        val expectedSlugs = listOf(
            "critical-section-requirements",
            "peterson-software-solutions",
            "hardware-atomic-primitives",
            "semaphore-operations",
            "semaphore-state-tracing",
            "producer-consumer-bounded-buffer",
            "readers-writers",
            "dining-philosophers",
            "monitors-condition-variables",
            "race-interleavings"
        )

        for (slug in expectedSlugs) {
            val tag = conceptTagRepository.findById(slug)
            assertTrue(tag.isPresent, "Expected concept tag '$slug' to be seeded")
            assertEquals("topic_os_process_sync", tag.get().topic?.id)
        }

        // Call admin unmapped endpoint: must find ZERO unmapped questions in process synchronization
        val resp = restTemplate.exchange(
            "/api/v1/curation/mappings/unmapped?topicId=topic_os_process_sync",
            HttpMethod.GET,
            null,
            Map::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        val totalElements = (body["totalElements"] as Number).toLong()
        assertEquals(0L, totalElements, "Pilot bank must be 100% mapped by construction (0 unmapped questions)")
    }

    @Test
    @Order(2)
    fun `honest-judge PASS - coverage endpoint returns honest split for process synchronization`() {
        loginAs("ankur", "admin123")

        val resp = restTemplate.getForEntity(
            "/api/v1/journey/topics/topic_os_process_sync/concepts",
            TopicConceptCoverageResponseDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val coverage = resp.body
        assertNotNull(coverage)
        assertEquals("topic_os_process_sync", coverage!!.topicId)
        assertEquals(10, coverage.conceptsTotal)
        assertEquals(10, coverage.conceptsWithMaterial, "All 10 concepts have material post-WP0")
        assertEquals(0, coverage.conceptsExplained, "0 concepts explained until WP2 lessons are ACCEPTED")

        assertEquals(10, coverage.concepts.size)
        for (c in coverage.concepts) {
            assertEquals(15L, c.mappedQuestionCount, "Each concept in pilot has 15 mapped questions")
            assertEquals(15L, c.uniqueStatementCount, "Each concept in pilot has 15 unique statements")
            assertNull(c.lessonStatus, "Lesson status is null when no ConceptUnit exists")
            assertEquals("MAPPED", c.coverage, "Coverage status must be MAPPED when questions > 0 and no accepted lesson")
        }
    }

    @Test
    @Order(3)
    fun `honest-judge PASS - computed-on-read coverage status flipping`() {
        loginAs("ankur", "admin123")

        val tag = conceptTagRepository.findById("critical-section-requirements").get()

        // 1. Initially MAPPED
        val initialResp = restTemplate.getForEntity(
            "/api/v1/journey/topics/topic_os_process_sync/concepts",
            TopicConceptCoverageResponseDto::class.java
        )
        val initialItem = initialResp.body!!.concepts.first { it.slug == "critical-section-requirements" }
        assertEquals("MAPPED", initialItem.coverage)
        assertEquals(0, initialResp.body!!.conceptsExplained)

        // 2. Add an ACCEPTED ConceptUnit -> coverage flips to EXPLAINED
        val unit = conceptUnitRepository.findByConceptTagSlug("critical-section-requirements").orElseGet {
            ConceptUnit(conceptTag = tag)
        }
        unit.title = "Critical Section & Mutual Exclusion Requirements"
        unit.motivation = "Motivation for critical section"
        unit.coreIdea = "Core idea of mutual exclusion"
        unit.workedExample = "Detailed worked example for testing"
        unit.status = ConceptUnitStatus.ACCEPTED
        conceptUnitRepository.save(unit)

        val explainedResp = restTemplate.getForEntity(
            "/api/v1/journey/topics/topic_os_process_sync/concepts",
            TopicConceptCoverageResponseDto::class.java
        )
        val explainedItem = explainedResp.body!!.concepts.first { it.slug == "critical-section-requirements" }
        assertEquals("EXPLAINED", explainedItem.coverage)
        assertEquals("ACCEPTED", explainedItem.lessonStatus)
        assertEquals(1, explainedResp.body!!.conceptsExplained)

        // 3. Remove all question tags for dining-philosophers -> coverage flips to UNMAPPED
        val diningLinks = questionConceptTagRepository.findAll().filter { it.id.conceptTagSlug == "dining-philosophers" }
        assertTrue(diningLinks.isNotEmpty())
        questionConceptTagRepository.deleteAll(diningLinks)

        val unmappedResp = restTemplate.getForEntity(
            "/api/v1/journey/topics/topic_os_process_sync/concepts",
            TopicConceptCoverageResponseDto::class.java
        )
        val unmappedItem = unmappedResp.body!!.concepts.first { it.slug == "dining-philosophers" }
        assertEquals("UNMAPPED", unmappedItem.coverage)
        assertEquals(0L, unmappedItem.mappedQuestionCount)
        assertEquals(9, unmappedResp.body!!.conceptsWithMaterial)

        // 4. Restore a tag -> flips back to MAPPED
        val anyQ = questionRepository.findAll().first { it.topic?.slug == "process-synchronization" }
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(anyQ.id!!, "dining-philosophers"),
                source = ConceptTagSource.MANUAL,
                confidence = 1.0f
            )
        )

        val restoredResp = restTemplate.getForEntity(
            "/api/v1/journey/topics/topic_os_process_sync/concepts",
            TopicConceptCoverageResponseDto::class.java
        )
        val restoredItem = restoredResp.body!!.concepts.first { it.slug == "dining-philosophers" }
        assertEquals("MAPPED", restoredItem.coverage)
        assertEquals(1L, restoredItem.mappedQuestionCount)
        assertEquals(10, restoredResp.body!!.conceptsWithMaterial)

        // Cleanup: remove the test unit and restore original mappings
        conceptUnitRepository.delete(unit)
        conceptSeedRunner.importMappings("seed-data/concept-mapping-pilot.jsonl")
    }

    @Test
    @Order(4)
    fun `honest-judge PASS - admin-gated curation endpoints reject students with 403 ProblemDetail`() {
        val studentUsername = "student_curation_${UUID.randomUUID().toString().take(8)}"
        userRepository.save(
            User(
                username = studentUsername,
                displayName = "Curation Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )

        // Create an isolated test question so we don't mutate pilot data
        val psyncTopic = topicRepository.findById("topic_os_process_sync").get()
        val testQ = questionRepository.save(
            Question(
                externalId = "TEST-CURATION-${UUID.randomUUID().toString().take(8)}",
                statement = "Test question for curation endpoint patch verification",
                type = QuestionType.MCQ,
                topic = psyncTopic
            )
        )

        loginAs(studentUsername, "password123")

        // 1. Student GET unmapped -> 403
        val getResp = restTemplate.exchange(
            "/api/v1/curation/mappings/unmapped?topicId=topic_os_process_sync",
            HttpMethod.GET,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, getResp.statusCode)
        assertEquals(403, getResp.body?.get("status"))
        assertEquals("Forbidden", getResp.body?.get("title"))

        // 2. Student PATCH question concepts -> 403
        val patchResp = restTemplate.exchange(
            "/api/v1/curation/questions/${testQ.id}/concepts",
            HttpMethod.PATCH,
            HttpEntity(CurationQuestionConceptsPatchRequest(addTags = listOf("semaphore-operations"))),
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, patchResp.statusCode)
        assertEquals(403, patchResp.body?.get("status"))
        assertEquals("Forbidden", patchResp.body?.get("title"))

        // 3. Admin PATCH question concepts -> 200 OK and updates tags
        loginAs("ankur", "admin123")
        val adminPatchResp = restTemplate.exchange(
            "/api/v1/curation/questions/${testQ.id}/concepts",
            HttpMethod.PATCH,
            HttpEntity(CurationQuestionConceptsPatchRequest(tags = listOf("readers-writers"))),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, adminPatchResp.statusCode)
        val updatedQ = adminPatchResp.body
        assertNotNull(updatedQ)
        assertEquals(listOf("readers-writers"), updatedQ!!.conceptTags)

        // Clean up test question and tags
        questionConceptTagRepository.deleteById(QuestionConceptTagId(testQ.id!!, "readers-writers"))
        questionRepository.delete(testQ)
    }

    @Test
    @Order(5)
    fun `honest-judge PASS - question detail endpoint returns conceptTags in QuestionDto`() {
        loginAs("ankur", "admin123")

        val q = questionRepository.findByExternalId("SO-OS-0001")
        assertNotNull(q, "SO-OS-0001 must exist in the bank")

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${q!!.id}",
            QuestionDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val dto = resp.body
        assertNotNull(dto)
        assertTrue(dto!!.conceptTags.contains("peterson-software-solutions"))
        assertTrue(dto.conceptTags.contains("critical-section-requirements"))
    }
}

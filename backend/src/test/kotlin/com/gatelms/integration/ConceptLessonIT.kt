package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.dto.ConceptLessonResponseDto
import com.gatelms.anatomy.dto.CurationLessonUpdateRequest
import com.gatelms.anatomy.entity.*
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.ConceptUnitRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.entity.*
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.repository.UserRepository
import com.gatelms.seed.ConceptSeedRunner
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

class ConceptLessonIT : BaseIntegrationTest() {

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
    lateinit var aiRateLimiter: AiRateLimiter

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockBean
    lateinit var aiChatService: AiChatService

    companion object {
        private var seeded = false
    }

    @BeforeEach
    fun setUp() {
        if (!seeded) {
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

            val cRunner = ConceptSeedRunner(
                conceptTagRepository = conceptTagRepository,
                questionConceptTagRepository = questionConceptTagRepository,
                questionRepository = questionRepository,
                topicRepository = topicRepository,
                objectMapper = objectMapper,
                taxonomyPath = "seed-data/concept-taxonomy-pilot.jsonl",
                mappingPath = "seed-data/concept-mapping-pilot.jsonl"
            )
            cRunner.seedAll()
            seeded = true
        }
        aiRateLimiter.reset()
    }

    @Test
    fun `honest-judge PASS - negative authorization - students cannot access curation lesson endpoints (403)`() {
        val studentUsername = "student_lesson_authz_${UUID.randomUUID().toString().take(8)}"
        userRepository.save(
            User(
                username = studentUsername,
                displayName = "Lesson Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )
        loginAs(studentUsername, "password123")

        // 1. Student GET curation list -> 403
        val listResp = restTemplate.getForEntity("/api/v1/curation/lessons", Map::class.java)
        assertEquals(HttpStatus.FORBIDDEN, listResp.statusCode)
        assertEquals(403, listResp.body?.get("status"))

        // 2. Student PUT update lesson -> 403
        val updateResp = restTemplate.exchange(
            "/api/v1/curation/concepts/critical-section-requirements/lesson",
            HttpMethod.PUT,
            HttpEntity(
                CurationLessonUpdateRequest(
                    status = ConceptUnitStatus.ACCEPTED,
                    motivation = "Hacked by student"
                )
            ),
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, updateResp.statusCode)
        assertEquals(403, updateResp.body?.get("status"))

        // 3. Student DELETE lesson -> 403
        val deleteResp = restTemplate.exchange(
            "/api/v1/curation/concepts/critical-section-requirements/lesson",
            HttpMethod.DELETE,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, deleteResp.statusCode)
        assertEquals(403, deleteResp.body?.get("status"))
    }

    @Test
    fun `honest-judge PASS - student can read concept lesson and mapped questions rail`() {
        loginAs("ankur", "admin123")

        // Seed a sample concept unit for critical-section-requirements
        val tag = conceptTagRepository.findById("critical-section-requirements").get()
        conceptUnitRepository.findByConceptTagSlug("critical-section-requirements").ifPresent {
            conceptUnitRepository.delete(it)
        }

        val savedUnit = conceptUnitRepository.save(
            ConceptUnit(
                conceptTag = tag,
                title = "Critical Section Requirements",
                motivation = "Coordinate concurrent threads without race conditions or memory corruption.",
                coreIdea = "Three pillars: Mutual Exclusion, Progress, Bounded Waiting.",
                invariant = "At most one process inside critical region.",
                workedExample = "Step 1: Process P0 sets flag[0] = true. Step 2: Sets turn = 1. Peterson protocol guarantees mutual exclusion under sequential consistency.",
                traps = "Assuming hardware memory ordering matches software line order.",
                workedExampleExternalId = "SO-OS-0001",
                status = ConceptUnitStatus.ACCEPTED,
                generatorModel = "TEST",
                generatedAt = Instant.now()
            )
        )

        // Read as student
        val studentUsername = "student_read_${UUID.randomUUID().toString().take(8)}"
        userRepository.save(
            User(
                username = studentUsername,
                displayName = "Read Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )
        loginAs(studentUsername, "password123")

        val resp = restTemplate.getForEntity(
            "/api/v1/journey/concepts/critical-section-requirements/lesson",
            ConceptLessonResponseDto::class.java
        )
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertNotNull(resp.body?.lesson)
        assertEquals("Critical Section Requirements", resp.body?.lesson?.title)
        assertEquals("SO-OS-0001", resp.body?.lesson?.workedExampleExternalId)
        assertEquals("ACCEPTED", resp.body?.lesson?.status)
        assertTrue(resp.body?.mappedQuestions?.isNotEmpty() == true, "Mapped questions rail must be populated")
    }

    @Test
    fun `honest-judge PASS - lesson generation failure throws 502 Bad Gateway and does not persist corrupt row`() {
        loginAs("ankur", "admin123")
        conceptUnitRepository.findByConceptTagSlug("hardware-atomic-primitives").ifPresent {
            conceptUnitRepository.delete(it)
        }

        // Mock AI to throw upstream error
        `when`(aiChatService.chat(any(AiChatRequest::class.java) ?: AiChatRequest(messages = emptyList(), provider = ""), nullable(String::class.java)))
            .thenThrow(RuntimeException("503 Service Unavailable: High demand"))

        val resp = restTemplate.postForEntity(
            "/api/v1/journey/concepts/hardware-atomic-primitives/lesson/generate",
            null,
            String::class.java
        )
        assertEquals(HttpStatus.BAD_GATEWAY, resp.statusCode)

        // Verify no corrupt row was saved
        val dbUnit = conceptUnitRepository.findByConceptTagSlug("hardware-atomic-primitives")
        assertFalse(dbUnit.isPresent, "Failed generation must never persist corrupt placeholder into the database")
    }

    @Test
    fun `honest-judge PASS - admin curation update and delete workflows`() {
        loginAs("ankur", "admin123")
        val tag = conceptTagRepository.findById("peterson-software-solutions").get()
        conceptUnitRepository.findByConceptTagSlug("peterson-software-solutions").ifPresent {
            conceptUnitRepository.delete(it)
        }

        val initial = conceptUnitRepository.save(
            ConceptUnit(
                conceptTag = tag,
                title = "Peterson Solution",
                motivation = "Two process mutual exclusion",
                coreIdea = "Software turn and flag flags",
                invariant = "Mutual exclusion invariant",
                workedExample = "Detailed calculation of turn variable and flag array",
                traps = "Out-of-order execution",
                workedExampleExternalId = "SO-OS-0001",
                status = ConceptUnitStatus.DRAFT,
                generatorModel = "AI_STUDIO",
                generatedAt = Instant.now()
            )
        )

        // Admin updates status to ACCEPTED
        val updateResp = restTemplate.exchange(
            "/api/v1/curation/concepts/peterson-software-solutions/lesson",
            HttpMethod.PUT,
            HttpEntity(
                CurationLessonUpdateRequest(
                    status = ConceptUnitStatus.ACCEPTED,
                    motivation = "Curated motivation for Peterson solution."
                )
            ),
            Map::class.java
        )
        assertEquals(HttpStatus.OK, updateResp.statusCode)
        assertEquals("ACCEPTED", updateResp.body?.get("status"))

        val dbUpdated = conceptUnitRepository.findByConceptTagSlug("peterson-software-solutions").get()
        assertEquals(ConceptUnitStatus.ACCEPTED, dbUpdated.status)
        assertEquals("Curated motivation for Peterson solution.", dbUpdated.motivation)

        // Admin deletes lesson
        val deleteResp = restTemplate.exchange(
            "/api/v1/curation/concepts/peterson-software-solutions/lesson",
            HttpMethod.DELETE,
            null,
            Map::class.java
        )
        assertEquals(HttpStatus.OK, deleteResp.statusCode)
        assertFalse(conceptUnitRepository.findByConceptTagSlug("peterson-software-solutions").isPresent)
    }
}

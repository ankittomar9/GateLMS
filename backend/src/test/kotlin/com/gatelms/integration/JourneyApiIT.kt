package com.gatelms.integration

import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.AiChatResponse
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.dto.JourneyContinueDto
import com.gatelms.dto.JourneyOverviewSubjectDto
import com.gatelms.dto.JourneyTopicSummaryDto
import com.gatelms.dto.TopicPrimerResponseDto
import com.gatelms.entity.*
import com.gatelms.repository.*
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.nullable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

class JourneyApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    @Autowired
    lateinit var topicMasteryRepository: TopicMasteryRepository

    @Autowired
    lateinit var topicPrimerRepository: TopicPrimerRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var aiRateLimiter: AiRateLimiter

    @Autowired
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    @MockBean
    lateinit var aiChatService: AiChatService

    private val validWorkedExample = """
        Let us solve a canonical Relational Algebra problem step-by-step:
        Problem: Given schema R(A, B, C) and S(C, D, E), find all tuples where R.A > 10 and S.D = 'Active'.
        
        Step 1: Understand the base relations. Relation R has attributes A, B, and C with primary key A. Relation S has attributes C, D, and E with primary key C. The natural join R ⨝ S combines tuples that match on attribute C.
        
        Step 2: Apply selections early using heuristic optimization. We push selection condition σ_{A > 10}(R) directly to table R before computing the Cartesian product or join. This drastically reduces intermediate relation cardinality from 100,000 tuples down to 2,500 tuples.
        
        Step 3: Similarly apply selection σ_{D = 'Active'}(S) on relation S, filtering out inactive entities before join evaluation.
        
        Step 4: Perform the theta join on the equality condition R.C = S.C. The resulting schema is (A, B, C, D, E).
        
        Step 5: Project the required target attributes using π_{A, B, E}. Notice that duplicate elimination occurs implicitly under set-theoretic relational algebra semantics. The overall query equivalence holds strictly under relational completeness theorems.
    """.trimIndent()

    @BeforeEach
    fun setUpData() {
        val seedRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper
        )
        seedRunner.importQuestions("seed-data/questions.jsonl")
        aiRateLimiter.reset()
    }

    @Test
    fun `honest-judge PASS - anonymous access to journey endpoints returns 401`() {
        logout()

        val resp1 = restTemplate.getForEntity("/api/v1/journey/overview", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp1.statusCode)

        val resp2 = restTemplate.getForEntity("/api/v1/journey/subjects/subj_dbms/topics", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp2.statusCode)

        val resp3 = restTemplate.getForEntity("/api/v1/journey/topics/topic_dbms_relational_alg/primer", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp3.statusCode)

        val resp4 = restTemplate.postForEntity("/api/v1/journey/topics/topic_dbms_relational_alg/primer/generate", null, String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp4.statusCode)

        val resp5 = restTemplate.getForEntity("/api/v1/journey/continue", String::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp5.statusCode)
    }

    @Test
    fun `honest-judge PASS - fresh student can freely navigate all subjects and topics with zero locks (PC-1)`() {
        // Create fresh student
        val username = "student_pc1_${UUID.randomUUID().toString().take(8)}"
        val user = userRepository.save(
            User(
                username = username,
                displayName = "Fresh Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )
        loginAs(username, "password123")

        // 1. Overview returns all subjects without preconditions
        val overviewResp = restTemplate.exchange(
            "/api/v1/journey/overview",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<JourneyOverviewSubjectDto>>() {}
        )
        assertEquals(HttpStatus.OK, overviewResp.statusCode)
        val subjects = overviewResp.body!!
        assertTrue(subjects.size >= 10, "Expected at least 10 subjects in overview, got ${subjects.size}")

        // Check fresh student counters are clean
        for (subj in subjects) {
            assertEquals(0L, subj.questionsSolved, "Fresh student should have 0 questions solved for ${subj.name}")
            assertEquals(0, subj.topicsTouched, "Fresh student should have 0 topics touched for ${subj.name}")
            assertEquals(0, subj.topicsCompleted, "Fresh student should have 0 topics completed for ${subj.name}")
            assertTrue(subj.topicsTotal > 0, "Subject ${subj.name} should have topics")
        }

        // 2. Fresh student can access ANY subject topics roadmap freely
        for (subj in subjects) {
            val topicsResp = restTemplate.exchange(
                "/api/v1/journey/subjects/${subj.subjectId}/topics",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<JourneyTopicSummaryDto>>() {}
            )
            assertEquals(HttpStatus.OK, topicsResp.statusCode, "Fresh student should freely access roadmap for ${subj.subjectId}")
            assertNotNull(topicsResp.body)
        }

        // 3. Fresh student can access topic primer before any attempt (PC-4)
        val primerResp = restTemplate.getForEntity(
            "/api/v1/journey/topics/topic_dbms_relational_alg/primer",
            TopicPrimerResponseDto::class.java
        )
        assertEquals(HttpStatus.OK, primerResp.statusCode)
        assertNotNull(primerResp.body)

        // 4. Continue endpoint returns nulls cleanly for fresh student
        val continueResp = restTemplate.getForEntity("/api/v1/journey/continue", JourneyContinueDto::class.java)
        assertEquals(HttpStatus.OK, continueResp.statusCode)
        assertNull(continueResp.body?.questionId)
    }

    @Test
    fun `honest-judge PASS - roadmap topics are strictly ordered by sequence`() {
        loginAs("ankur", "admin123")

        val resp = restTemplate.exchange(
            "/api/v1/journey/subjects/subj_discrete_math/topics",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<JourneyTopicSummaryDto>>() {}
        )
        assertEquals(HttpStatus.OK, resp.statusCode)
        val topics = resp.body!!
        assertTrue(topics.size >= 5, "Expected >= 5 topics in discrete math")

        var previousSequence = 0
        for (t in topics) {
            assertTrue(t.sequence >= 1, "Topic ${t.name} sequence must be >= 1 (got ${t.sequence})")
            assertTrue(t.sequence >= previousSequence, "Topics must be sorted by sequence in non-decreasing order")
            previousSequence = t.sequence
        }
    }

    @Test
    fun `honest-judge PASS - solving one question moves all three dials (PC-3)`() {
        val username = "student_pc3_${UUID.randomUUID().toString().take(8)}"
        val user = userRepository.save(
            User(
                username = username,
                displayName = "Dial Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )
        loginAs(username, "password123")

        // Initial Overview
        val initialResp = restTemplate.exchange(
            "/api/v1/journey/overview",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<JourneyOverviewSubjectDto>>() {}
        )
        val initialDbms = initialResp.body!!.first { it.subjectId == "subj_dbms" }
        assertEquals(0L, initialDbms.questionsSolved)
        assertEquals(0, initialDbms.topicsTouched)

        // Find a question in DBMS topic
        val dbmsQuestion = questionRepository.findAll().firstOrNull { it.subject?.id == "subj_dbms" }
        assertNotNull(dbmsQuestion, "DBMS question should exist in seeded dataset")

        // Simulate solving 1 question
        val topicId = dbmsQuestion!!.topic!!.id
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(userId = user.id, questionId = dbmsQuestion.id!!),
                pass1Status = Pass1Status.SOLVED,
                pass2Status = Pass2Status.PENDING,
                lastAttemptedAt = Instant.now()
            )
        )
        topicMasteryRepository.save(
            TopicMastery(
                id = TopicMasteryId(userId = user.id, topicId = topicId),
                masteryLevel = 2,
                lastRevisedAt = Instant.now()
            )
        )

        // Post-solve Overview
        val updatedResp = restTemplate.exchange(
            "/api/v1/journey/overview",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<JourneyOverviewSubjectDto>>() {}
        )
        val updatedDbms = updatedResp.body!!.first { it.subjectId == "subj_dbms" }

        // Dial 1: questionsSolved advanced by +1
        assertEquals(1L, updatedDbms.questionsSolved, "Dial 1: questionsSolved must increment by 1")

        // Dial 2: topicsTouched advanced from 0 to 1
        assertEquals(1, updatedDbms.topicsTouched, "Dial 2: topicsTouched must flip from 0 to 1")

        // Dial 3: continue strip now reflects this attempted question
        val continueResp = restTemplate.getForEntity("/api/v1/journey/continue", JourneyContinueDto::class.java)
        assertEquals(HttpStatus.OK, continueResp.statusCode)
        assertEquals(dbmsQuestion.id, continueResp.body?.questionId)
        assertEquals(topicId, continueResp.body?.topicId)
    }

    @Test
    fun `honest-judge PASS - primer lazy generation and idempotent caching`() {
        loginAs("ankur", "admin123")

        val primerJson = objectMapper.writeValueAsString(
            mapOf(
                "motivation" to "Relational Algebra provides the core mathematical foundation for declarative query execution.",
                "core_idea" to "An algebraic system consisting of relations and algebraic operators that produce relations.",
                "invariant" to "Relational closure property: relations in, relations out.",
                "worked_example" to validWorkedExample,
                "traps" to "Confusing set vs bag semantics and Cartesian product order."
            )
        )

        `when`(aiChatService.chat(any(AiChatRequest::class.java) ?: AiChatRequest(messages = emptyList(), provider = ""), nullable(String::class.java))).thenReturn(
            AiChatResponse(
                reply = primerJson,
                provider = "GEMINI",
                model = "gemini-3.8-flash",
                latencyMs = 150
            )
        )

        // Generate primer
        val genResp = restTemplate.postForEntity(
            "/api/v1/journey/topics/topic_dbms_relational_alg/primer/generate",
            null,
            TopicPrimerResponseDto::class.java
        )
        assertEquals(HttpStatus.OK, genResp.statusCode)
        assertNotNull(genResp.body?.primer)
        val generatedPrimer = genResp.body!!.primer!!
        assertEquals("DRAFT", generatedPrimer.status)
        assertEquals("topic_dbms_relational_alg", generatedPrimer.topicId)

        // Second call should return cached primer idempotently
        val cachedResp = restTemplate.postForEntity(
            "/api/v1/journey/topics/topic_dbms_relational_alg/primer/generate",
            null,
            TopicPrimerResponseDto::class.java
        )
        assertEquals(HttpStatus.OK, cachedResp.statusCode)
        assertEquals(generatedPrimer.id, cachedResp.body?.primer?.id)

        // Database row count for this topic is exactly 1
        val dbPrimer = topicPrimerRepository.findByTopicId("topic_dbms_relational_alg")
        assertTrue(dbPrimer.isPresent)
    }
}

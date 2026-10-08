package com.gatelms.integration

import com.gatelms.anatomy.entity.ConceptTag
import com.gatelms.anatomy.entity.ConceptTagSource
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.dto.SimilarQuestionsResponseDto
import com.gatelms.entity.*
import com.gatelms.repository.*
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

class SimilarApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    @Autowired
    lateinit var questionConceptTagRepository: QuestionConceptTagRepository

    @Autowired
    lateinit var conceptTagRepository: ConceptTagRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    private lateinit var student: User
    private lateinit var testSubject: Subject
    private lateinit var testTopic: Topic

    @BeforeEach
    fun setUp() {
        val seedRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper
        )
        seedRunner.importQuestions("seed-data/questions.jsonl")

        testSubject = subjectRepository.findAll().first()
        testTopic = topicRepository.findBySubjectIdOrSlugOrderBySequenceAscNameAsc(testSubject.id).first()

        val username = "student_sim_${UUID.randomUUID().toString().take(8)}"
        student = userRepository.save(
            User(
                username = username,
                displayName = "Similar Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )
        loginAs(username, "password123")
    }

    @Test
    fun `honest-judge PASS - same-topic unsolved set returned with basis TOPIC when concept tags absent (PC-5)`() {
        val topic = topicRepository.save(
            Topic(
                id = "fresh_topic_${UUID.randomUUID().toString().take(6)}",
                subject = testSubject,
                name = "Fresh Topic",
                slug = "fresh-topic-${UUID.randomUUID().toString().take(6)}",
                sequence = 99
            )
        )

        // Create 3 questions in the topic with distinct years
        val q1 = questionRepository.save(
            Question(
                statement = "Topic question 1",
                year = 2021,
                marks = 1,
                topic = topic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val q2 = questionRepository.save(
            Question(
                statement = "Topic question 2",
                year = 2023,
                marks = 2,
                topic = topic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val q3 = questionRepository.save(
            Question(
                statement = "Topic question 3",
                year = 2022,
                marks = 2,
                topic = topic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${q1.id}/similar?limit=5",
            SimilarQuestionsResponseDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertEquals("TOPIC", body.similarityBasis)
        assertFalse(body.questions.any { it.id == q1.id }, "Source question must never be returned in results")

        // Should return q2 (2023) then q3 (2022)
        val returnedIds = body.questions.map { it.id }
        assertTrue(returnedIds.contains(q2.id))
        assertTrue(returnedIds.contains(q3.id))
        val idxQ2 = returnedIds.indexOf(q2.id)
        val idxQ3 = returnedIds.indexOf(q3.id)
        assertTrue(idxQ2 < idxQ3, "Newer year (2023) must rank before older year (2022)")
    }

    @Test
    fun `honest-judge PASS - activates CONCEPT basis when at least 2 candidates share concept slugs (PC-5)`() {
        val sourceQ = questionRepository.save(
            Question(
                statement = "Concept source question",
                year = 2020,
                marks = 1,
                topic = testTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val candidate1 = questionRepository.save(
            Question(
                statement = "Candidate 1 shares two concepts",
                year = 2021,
                marks = 2,
                topic = testTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val candidate2 = questionRepository.save(
            Question(
                statement = "Candidate 2 shares one concept",
                year = 2023,
                marks = 1,
                topic = testTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        val slugA = "concept-slug-a-${UUID.randomUUID().toString().take(6)}"
        val slugB = "concept-slug-b-${UUID.randomUUID().toString().take(6)}"

        // Save ConceptTags first to satisfy foreign key
        conceptTagRepository.save(ConceptTag(slug = slugA, label = "Concept A", topic = testTopic))
        conceptTagRepository.save(ConceptTag(slug = slugB, label = "Concept B", topic = testTopic))

        // Tag source with slugA and slugB
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(sourceQ.id!!, slugA),
                source = ConceptTagSource.ANATOMY
            )
        )
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(sourceQ.id!!, slugB),
                source = ConceptTagSource.ANATOMY
            )
        )

        // candidate1 shares BOTH slugs (count = 2)
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(candidate1.id!!, slugA),
                source = ConceptTagSource.ANATOMY
            )
        )
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(candidate1.id!!, slugB),
                source = ConceptTagSource.ANATOMY
            )
        )

        // candidate2 shares slugA only (count = 1)
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(candidate2.id!!, slugA),
                source = ConceptTagSource.ANATOMY
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${sourceQ.id}/similar?limit=5",
            SimilarQuestionsResponseDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertEquals("CONCEPT", body.similarityBasis, "Basis must flip to CONCEPT with >= 2 candidates")
        assertTrue(body.questions.size >= 2)
        assertEquals(candidate1.id, body.questions[0].id, "Candidate with 2 shared slugs must rank first")
        assertEquals(candidate2.id, body.questions[1].id, "Candidate with 1 shared slug must rank second")
    }

    @Test
    fun `honest-judge PASS - solved questions are excluded from similar results (PC-5 solved-exclusion)`() {
        val sourceQ = questionRepository.save(
            Question(
                statement = "Source question for solved test",
                year = 2020,
                marks = 1,
                topic = testTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val solvedQ = questionRepository.save(
            Question(
                statement = "Solved question to be excluded",
                year = 2024,
                marks = 2,
                topic = testTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val unsolvedQ = questionRepository.save(
            Question(
                statement = "Unsolved question should appear",
                year = 2021,
                marks = 1,
                topic = testTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        // Mark solvedQ as SOLVED for current student
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, solvedQ.id!!),
                pass1Status = Pass1Status.SOLVED,
                pass2Status = Pass2Status.SOLVED,
                lastAttemptedAt = Instant.now()
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${sourceQ.id}/similar?limit=10",
            SimilarQuestionsResponseDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertFalse(body.questions.any { it.id == solvedQ.id }, "Solved question must be excluded from similar list")
        assertTrue(body.questions.any { it.id == unsolvedQ.id }, "Unsolved question should be included")
    }

    @Test
    fun `honest-judge PASS - isolated topic returns 200 with empty list and TOPIC basis`() {
        // Create an isolated topic with a single question
        val isolatedTopic = topicRepository.save(
            Topic(
                id = "isolated_topic_${UUID.randomUUID().toString().take(6)}",
                subject = testSubject,
                name = "Isolated Topic",
                slug = "isolated-topic-${UUID.randomUUID().toString().take(6)}",
                sequence = 999
            )
        )

        val isolatedQ = questionRepository.save(
            Question(
                statement = "Isolated single question statement",
                type = QuestionType.NAT,
                marks = 2,
                topic = isolatedTopic,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${isolatedQ.id}/similar?limit=5",
            SimilarQuestionsResponseDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertEquals("TOPIC", body.similarityBasis)
        assertTrue(body.questions.isEmpty(), "Should return empty list since no other questions exist in topic")
    }
}

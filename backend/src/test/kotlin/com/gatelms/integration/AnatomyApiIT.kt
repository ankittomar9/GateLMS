package com.gatelms.integration

import com.gatelms.anatomy.dto.CurationAnatomyUpdateRequest
import com.gatelms.anatomy.dto.QuestionAnatomyDto
import com.gatelms.anatomy.entity.AnatomyStatus
import com.gatelms.anatomy.entity.QuestionAnatomy
import com.gatelms.anatomy.repository.QuestionAnatomyRepository
import com.gatelms.entity.*
import com.gatelms.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

class AnatomyApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    @Autowired
    lateinit var questionAnatomyRepository: QuestionAnatomyRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testQuestion: Question
    private lateinit var aliceUser: User

    @BeforeEach
    fun setup() {
        questionAnatomyRepository.deleteAll()
        questionProgressRepository.deleteAll()
        questionRepository.deleteAll()

        val subject = subjectRepository.findById("subj_dbms").orElseGet {
            subjectRepository.save(Subject(id = "subj_dbms", name = "Databases", slug = "dbms", cluster = "core", avgWeightage = 7.0))
        }

        val topic = topicRepository.findById("top_dbms_concurrency").orElseGet {
            topicRepository.save(Topic(id = "top_dbms_concurrency", subject = subject, name = "Concurrency Control", slug = "concurrency"))
        }

        testQuestion = questionRepository.save(
            Question(
                subject = subject,
                topic = topic,
                type = QuestionType.MCQ,
                marks = 2,
                statement = "Which locking protocol guarantees conflict serializability without cascading aborts?",
                optionsJson = """[{"key":"A","text":"Basic 2PL"},{"key":"B","text":"Strict 2PL"},{"key":"C","text":"Timestamp ordering"},{"key":"D","text":"None"}]""",
                correctOptions = "B"
            )
        )

        aliceUser = userRepository.findByUsername("alice_student").orElseGet {
            userRepository.save(
                User(
                    username = "alice_student",
                    passwordHash = passwordEncoder.encode("studentPass"),
                    displayName = "Alice Student",
                    role = UserRole.STUDENT
                )
            )
        }
    }

    @Test
    fun `1 Gating rule - Student without attempt receives 403 ANATOMY_LOCKED_UNTIL_ATTEMPT`() {
        // Create an existing anatomy row
        val anatomy = questionAnatomyRepository.save(
            QuestionAnatomy(
                question = testQuestion,
                testedConcept = "Strict 2PL concurrency guarantee",
                invariantRule = "Hold exclusive locks until transaction commit",
                trapAnalysisJson = """[{"option":"A","verdict":"INCORRECT","why_plausible":"Basic 2PL allows cascading aborts","misconception":"Confusing serializability with recoverability"}]""",
                prerequisitesJson = "[]",
                status = AnatomyStatus.DRAFT
            )
        )

        // Log in as student Alice
        loginAs("alice_student", "studentPass")

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${testQuestion.id}/anatomy",
            Map::class.java
        )

        assertEquals(HttpStatus.FORBIDDEN, resp.statusCode)
        val body = resp.body!!
        assertEquals("ANATOMY_LOCKED_UNTIL_ATTEMPT", body["code"])
    }

    @Test
    fun `2 Gating rule - Admin can access anatomy without prior attempt`() {
        questionAnatomyRepository.save(
            QuestionAnatomy(
                question = testQuestion,
                testedConcept = "Strict 2PL concurrency guarantee",
                invariantRule = "Hold exclusive locks until transaction commit",
                status = AnatomyStatus.DRAFT
            )
        )

        // Log in as admin
        loginAs("ankur", "admin123")

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${testQuestion.id}/anatomy",
            QuestionAnatomyDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertNotNull(resp.body)
        assertEquals("Strict 2PL concurrency guarantee", resp.body!!.testedConcept)
    }

    @Test
    fun `3 Gating rule - Student with recorded attempt can access anatomy dossier`() {
        questionAnatomyRepository.save(
            QuestionAnatomy(
                question = testQuestion,
                testedConcept = "Strict 2PL concurrency guarantee",
                status = AnatomyStatus.DRAFT
            )
        )

        // Record attempt for student Alice
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(userId = aliceUser.id, questionId = testQuestion.id!!),
                lastAttemptedAt = Instant.now(),
                pass1Status = Pass1Status.SOLVED
            )
        )

        loginAs("alice_student", "studentPass")

        val resp = restTemplate.getForEntity(
            "/api/v1/questions/${testQuestion.id}/anatomy",
            QuestionAnatomyDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals("Strict 2PL concurrency guarantee", resp.body!!.testedConcept)
    }

    @Test
    fun `4 Admin soft delete sets status REJECTED and hides from student`() {
        val anatomy = questionAnatomyRepository.save(
            QuestionAnatomy(
                question = testQuestion,
                testedConcept = "Strict 2PL concurrency guarantee",
                status = AnatomyStatus.DRAFT
            )
        )

        // Record attempt for student Alice
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(userId = aliceUser.id, questionId = testQuestion.id!!),
                lastAttemptedAt = Instant.now()
            )
        )

        // Admin deletes
        loginAs("ankur", "admin123")
        val deleteResp = restTemplate.exchange(
            "/api/v1/questions/${testQuestion.id}/anatomy",
            HttpMethod.DELETE,
            null,
            QuestionAnatomyDto::class.java
        )
        assertEquals(HttpStatus.OK, deleteResp.statusCode)
        assertEquals(AnatomyStatus.REJECTED, deleteResp.body!!.status)

        // Student tries to view -> gets 404 Not Found
        loginAs("alice_student", "studentPass")
        val studentResp = restTemplate.getForEntity(
            "/api/v1/questions/${testQuestion.id}/anatomy",
            Map::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, studentResp.statusCode)

        // Admin can still view rejected dossier
        loginAs("ankur", "admin123")
        val adminResp = restTemplate.getForEntity(
            "/api/v1/questions/${testQuestion.id}/anatomy",
            QuestionAnatomyDto::class.java
        )
        assertEquals(HttpStatus.OK, adminResp.statusCode)
        assertEquals(AnatomyStatus.REJECTED, adminResp.body!!.status)
    }

    @Test
    fun `5 Admin curation review flow updates status to ACCEPTED`() {
        val anatomy = questionAnatomyRepository.save(
            QuestionAnatomy(
                question = testQuestion,
                testedConcept = "Draft concept",
                status = AnatomyStatus.DRAFT
            )
        )

        loginAs("ankur", "admin123")

        val patchReq = CurationAnatomyUpdateRequest(
            status = AnatomyStatus.ACCEPTED,
            testedConcept = "Curated Strict 2PL concept"
        )
        val patchResp = restTemplate.exchange(
            "/api/v1/curation/anatomy/${anatomy.id}",
            HttpMethod.PATCH,
            HttpEntity(patchReq),
            QuestionAnatomyDto::class.java
        )

        assertEquals(HttpStatus.OK, patchResp.statusCode)
        assertEquals(AnatomyStatus.ACCEPTED, patchResp.body!!.status)
        assertEquals("Curated Strict 2PL concept", patchResp.body!!.testedConcept)
    }
}

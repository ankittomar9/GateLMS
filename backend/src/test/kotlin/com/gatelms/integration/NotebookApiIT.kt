package com.gatelms.integration

import com.gatelms.anatomy.entity.AnatomyStatus
import com.gatelms.anatomy.entity.QuestionAnatomy
import com.gatelms.anatomy.repository.QuestionAnatomyRepository
import com.gatelms.dto.SubjectNotebookDto
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

class NotebookApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    @Autowired
    lateinit var questionAnatomyRepository: QuestionAnatomyRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var objectMapper: com.fasterxml.jackson.databind.ObjectMapper

    private lateinit var student: User
    private lateinit var testSubject: Subject
    private lateinit var topic1: Topic
    private lateinit var topic2: Topic

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
        val subjectTopics = topicRepository.findBySubjectIdOrSlugOrderBySequenceAscNameAsc(testSubject.id)
        topic1 = subjectTopics[0]
        topic2 = if (subjectTopics.size > 1) subjectTopics[1] else topic1

        val username = "student_note_${UUID.randomUUID().toString().take(8)}"
        student = userRepository.save(
            User(
                username = username,
                displayName = "Notebook Student",
                passwordHash = passwordEncoder.encode("password123"),
                role = UserRole.STUDENT
            )
        )
        loginAs(username, "password123")
    }

    @Test
    fun `honest-judge PASS - anonymous access to subject notebook returns 401`() {
        logout()
        val resp = restTemplate.getForEntity(
            "/api/v1/journey/subjects/${testSubject.id}/notebook",
            String::class.java
        )
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `honest-judge PASS - solved questions are grouped by topic with snippet and exclude unsolved (PC-6)`() {
        val q1 = questionRepository.save(
            Question(
                statement = "Solved Question in Topic 1 with a reasonably long statement to verify snippet truncation behavior properly works under all conditions.",
                year = 2023,
                marks = 2,
                topic = topic1,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val q2 = questionRepository.save(
            Question(
                statement = "Solved Question in Topic 2",
                year = 2022,
                marks = 1,
                topic = topic2,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val qUnsolved = questionRepository.save(
            Question(
                statement = "Unsolved Question in Topic 1",
                year = 2021,
                marks = 1,
                topic = topic1,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        // Solve q1 and q2 for student
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, q1.id!!),
                pass1Status = Pass1Status.SOLVED,
                pass2Status = Pass2Status.SOLVED,
                lastAttemptedAt = Instant.now()
            )
        )
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, q2.id!!),
                pass1Status = Pass1Status.ASSISTED,
                pass2Status = Pass2Status.SOLVED,
                lastAttemptedAt = Instant.now().minusSeconds(100)
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/journey/subjects/${testSubject.id}/notebook",
            SubjectNotebookDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertEquals(testSubject.id, body.subjectId)
        assertTrue(body.totalEntries >= 2)

        // Verify unsolved question is NOT present
        val allReturnedQuestionIds = body.topics.flatMap { it.entries }.map { it.questionId }
        assertTrue(allReturnedQuestionIds.contains(q1.id))
        assertTrue(allReturnedQuestionIds.contains(q2.id))
        assertFalse(allReturnedQuestionIds.contains(qUnsolved.id), "Unsolved question must not appear in notebook")

        // Verify snippet length <= 200 chars
        for (topicGroup in body.topics) {
            for (entry in topicGroup.entries) {
                assertTrue(entry.statementSnippet.length <= 200, "Snippet length must be <= 200 chars")
                assertNotNull(entry.solvedAt)
            }
        }
    }

    @Test
    fun `honest-judge PASS - star filter isolates only bookmarked entries`() {
        val qStarred = questionRepository.save(
            Question(
                statement = "Starred question statement",
                year = 2024,
                marks = 2,
                topic = topic1,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val qUnstarred = questionRepository.save(
            Question(
                statement = "Unstarred question statement",
                year = 2023,
                marks = 1,
                topic = topic1,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        // Solve both, but star only one
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, qStarred.id!!),
                pass1Status = Pass1Status.SOLVED,
                isBookmarked = true,
                lastAttemptedAt = Instant.now()
            )
        )
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, qUnstarred.id!!),
                pass1Status = Pass1Status.SOLVED,
                isBookmarked = false,
                lastAttemptedAt = Instant.now()
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/journey/subjects/${testSubject.id}/notebook?starredOnly=true",
            SubjectNotebookDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        val entries = body.topics.flatMap { it.entries }
        assertTrue(entries.any { it.questionId == qStarred.id })
        assertFalse(entries.any { it.questionId == qUnstarred.id }, "Unstarred question must be excluded when starredOnly=true")
        assertTrue(entries.all { it.starred }, "All returned entries must be starred")
    }

    @Test
    fun `honest-judge PASS - anatomyAvailable is true when anatomy dossier exists`() {
        val q = questionRepository.save(
            Question(
                statement = "Question with anatomy dossier",
                year = 2024,
                marks = 2,
                topic = topic1,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        // Solve question
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, q.id!!),
                pass1Status = Pass1Status.SOLVED,
                lastAttemptedAt = Instant.now()
            )
        )

        // Create anatomy dossier for this question
        questionAnatomyRepository.save(
            QuestionAnatomy(
                question = q,
                testedConcept = "Relational Algebra Equivalence",
                invariantRule = "σ_{c1}(σ_{c2}(R)) ≡ σ_{c1 ∧ c2}(R)",
                status = AnatomyStatus.ACCEPTED
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/journey/subjects/${testSubject.id}/notebook",
            SubjectNotebookDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val entry = resp.body!!.topics.flatMap { it.entries }.first { it.questionId == q.id }
        assertTrue(entry.anatomyAvailable, "anatomyAvailable must be true when dossier is accepted")
    }

    @Test
    fun `honest-judge PASS - topicId filter scopes notebook entries to a single topic`() {
        val q1 = questionRepository.save(
            Question(
                statement = "Question in topic 1",
                year = 2023,
                marks = 2,
                topic = topic1,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )
        val q2 = questionRepository.save(
            Question(
                statement = "Question in topic 2",
                year = 2022,
                marks = 1,
                topic = topic2,
                subject = testSubject,
                curationStatus = "ACCEPTED"
            )
        )

        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, q1.id!!),
                pass1Status = Pass1Status.SOLVED,
                lastAttemptedAt = Instant.now()
            )
        )
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(student.id!!, q2.id!!),
                pass1Status = Pass1Status.SOLVED,
                lastAttemptedAt = Instant.now()
            )
        )

        val resp = restTemplate.getForEntity(
            "/api/v1/journey/subjects/${testSubject.id}/notebook?topicId=${topic1.id}",
            SubjectNotebookDto::class.java
        )

        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertEquals(1, body.topics.size)
        assertEquals(topic1.id, body.topics[0].topicId)
        assertTrue(body.topics[0].entries.any { it.questionId == q1.id })
    }
}

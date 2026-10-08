package com.gatelms.integration

import com.gatelms.anatomy.entity.*
import com.gatelms.anatomy.repository.*
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
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * CardFlowIT — PHASE7-M1-ANATOMY-SPEC §6.4 integration gate (Docker machine required):
 * due list empty -> wrong attempt auto-enrolls prerequisite cards (due tomorrow) ->
 * cards surface in overview + due list once due -> review grade 2 reschedules +1d ->
 * per-user isolation.
 */
class CardFlowIT : BaseIntegrationTest() {

    @Autowired lateinit var questionRepository: QuestionRepository
    @Autowired lateinit var questionProgressRepository: QuestionProgressRepository
    @Autowired lateinit var questionAnatomyRepository: QuestionAnatomyRepository
    @Autowired lateinit var conceptTagRepository: ConceptTagRepository
    @Autowired lateinit var conceptUnitRepository: ConceptUnitRepository
    @Autowired lateinit var questionConceptTagRepository: QuestionConceptTagRepository
    @Autowired lateinit var retrievalCardRepository: RetrievalCardRepository
    @Autowired lateinit var retrievalCardStateRepository: RetrievalCardStateRepository
    @Autowired lateinit var subjectRepository: SubjectRepository
    @Autowired lateinit var topicRepository: TopicRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testQuestion: Question
    private lateinit var aliceUser: User
    private lateinit var bobUser: User
    private lateinit var firstCardId: UUID

    @BeforeEach
    fun setup() {
        // FK-safe teardown order (all V1/V15 FKs are ON DELETE CASCADE)
        retrievalCardStateRepository.deleteAll()
        retrievalCardRepository.deleteAll()
        conceptUnitRepository.deleteAll()
        questionConceptTagRepository.deleteAll()
        questionAnatomyRepository.deleteAll()
        questionProgressRepository.deleteAll()
        questionRepository.deleteAll()
        conceptTagRepository.deleteAll()

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

        // Flywheel fixture: registered concept tag + question link
        val tag = conceptTagRepository.save(
            ConceptTag(slug = "strict-2pl", label = "Strict Two-Phase Locking", topic = topic)
        )
        questionConceptTagRepository.save(
            QuestionConceptTag(
                id = QuestionConceptTagId(testQuestion.id!!, "strict-2pl"),
                source = ConceptTagSource.ANATOMY,
                confidence = 1.0f
            )
        )

        // Dossier with a prerequisite pointing at the concept
        questionAnatomyRepository.save(
            QuestionAnatomy(
                question = testQuestion,
                testedConcept = "Strict 2PL concurrency guarantee",
                invariantRule = "Hold exclusive locks until transaction commit",
                prerequisitesJson = """[{"concept_slug":"strict-2pl","concept_label":"Strict Two-Phase Locking","topic_slug":"concurrency","must_read":true}]""",
                status = AnatomyStatus.DRAFT
            )
        )

        // Concept unit + its retrieval cards (2)
        val unit = conceptUnitRepository.save(
            ConceptUnit(
                conceptTag = tag,
                title = "Strict 2PL: The Locking Discipline",
                motivation = "Why strictness prevents cascading aborts.",
                coreIdea = "Exclusive locks are held until commit.",
                invariant = "X-locks release only at transaction end.",
                workedExample = "Consider T1 writing X then committing at t=5 while T2 reads X at t=3 under basic 2PL. " +
                    "Step 1: T1 acquires X-lock on X at t=1. Step 2: T2 requests a shared lock on X at t=2 and blocks. " +
                    "Step 3: T1 commits at t=5. Under basic 2PL T1 could have released the lock at t=4 after its last write, " +
                    "letting T2 read uncommitted data — if T1 then aborts, T2 must abort too (cascading abort). " +
                    "Step 4: under strict 2PL the lock is held to commit at t=5, so T2 only reads committed values.",
                traps = "Confusing serializability with recoverability.",
                status = ConceptUnitStatus.DRAFT
            )
        )
        val card1 = retrievalCardRepository.save(
            RetrievalCard(conceptUnit = unit, prompt = "State the lock-release rule that distinguishes strict 2PL from basic 2PL.", answer = "Exclusive locks are released only at transaction commit or abort, never mid-transaction.", cardOrder = 0)
        )
        retrievalCardRepository.save(
            RetrievalCard(conceptUnit = unit, prompt = "Explain how strict 2PL prevents cascading aborts.", answer = "No transaction can read uncommitted data, so no transaction ever depends on an aborted one.", cardOrder = 1)
        )
        firstCardId = card1.id!!

        aliceUser = userRepository.findByUsername("alice_student").orElseGet {
            userRepository.save(User(username = "alice_student", passwordHash = passwordEncoder.encode("studentPass"), displayName = "Alice Student", role = UserRole.STUDENT))
        }
        bobUser = userRepository.findByUsername("bob_student").orElseGet {
            userRepository.save(User(username = "bob_student", passwordHash = passwordEncoder.encode("studentPass"), displayName = "Bob Student", role = UserRole.STUDENT))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createPracticeSessionAndAnswerWrong(): Map<String, Any> {
        val sessionResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions",
            HttpEntity(mapOf("topicId" to "top_dbms_concurrency", "count" to 1)),
            Map::class.java
        )
        assertEquals(HttpStatus.OK, sessionResp.statusCode)
        val attemptId = sessionResp.body!!["attemptId"] as String
        val questions = sessionResp.body!!["questions"] as List<Map<String, Any>>
        assertEquals(1, questions.size)
        assertEquals(testQuestion.id.toString(), questions[0]["id"])

        val answerResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions/$attemptId/answers",
            HttpEntity(mapOf("questionId" to testQuestion.id.toString(), "selectedOptions" to "A", "timeSpentSeconds" to 30)),
            Map::class.java
        )
        assertEquals(HttpStatus.OK, answerResp.statusCode)
        assertEquals(false, answerResp.body!!["correct"])
        return answerResp.body as Map<String, Any>
    }

    @Test
    fun `1 Fresh student sees an empty due list and zero dueCards in overview`() {
        loginAs("alice_student", "studentPass")

        val dueResp = restTemplate.getForEntity("/api/v1/revision/cards/due", Map::class.java)
        assertEquals(HttpStatus.OK, dueResp.statusCode)
        assertEquals(0, dueResp.body!!["dueCount"])
        assertEquals(0, (dueResp.body!!["cards"] as List<*>).size)

        val overviewResp = restTemplate.getForEntity("/api/v1/revision/overview", Map::class.java)
        assertEquals(HttpStatus.OK, overviewResp.statusCode)
        assertEquals(0, overviewResp.body!!["dueCards"])
    }

    @Test
    fun `2 Wrong practice attempt auto-enrolls prerequisite cards due tomorrow, idempotently`() {
        loginAs("alice_student", "studentPass")
        createPracticeSessionAndAnswerWrong()

        val states = retrievalCardStateRepository.findByIdUserId(aliceUser.id!!)
        assertEquals(2, states.size)

        val now = Instant.now()
        for (state in states) {
            assertEquals(0f, state.intervalDays)
            assertEquals(2.5f, state.ease)
            assertEquals(0, state.consecutiveCorrect)
            assertNull(state.lastReviewedAt)
            val hoursUntilDue = Duration.between(now, state.dueAt).toHours()
            assertTrue(hoursUntilDue >= 23 && hoursUntilDue <= 25, "enrolled card must be due tomorrow (+1d), was +${hoursUntilDue}h")
        }

        // Second wrong submission of the same question must not duplicate enrollment rows
        createPracticeSessionAndAnswerWrong()
        assertEquals(2, retrievalCardStateRepository.findByIdUserId(aliceUser.id!!).size)

        // Cards are due tomorrow, so nothing is due yet: overview must stay at 0
        val overviewResp = restTemplate.getForEntity("/api/v1/revision/overview", Map::class.java)
        assertEquals(0, overviewResp.body!!["dueCards"])
    }

    @Test
    fun `3 Enrolled cards surface once due and a SOLID review reschedules exactly plus 1 day`() {
        loginAs("alice_student", "studentPass")
        createPracticeSessionAndAnswerWrong()

        // Simulate the passage of one day (enrollment dueAt = now + 1d)
        retrievalCardStateRepository.findByIdUserId(aliceUser.id!!).forEach {
            it.dueAt = Instant.now().minusSeconds(3600)
            retrievalCardStateRepository.save(it)
        }

        val overviewResp = restTemplate.getForEntity("/api/v1/revision/overview", Map::class.java)
        assertEquals(2, (overviewResp.body!!["dueCards"] as Number).toInt())

        val dueResp = restTemplate.getForEntity("/api/v1/revision/cards/due", Map::class.java)
        assertEquals(HttpStatus.OK, dueResp.statusCode)
        assertEquals(2, dueResp.body!!["dueCount"])
        val cards = dueResp.body!!["cards"] as List<Map<String, Any>>
        val first = cards.first { it["cardId"] == firstCardId.toString() }
        assertEquals("strict-2pl", first["conceptSlug"])
        assertEquals("Strict Two-Phase Locking", first["conceptLabel"])
        assertEquals("Strict 2PL: The Locking Discipline", first["unitTitle"])
        assertTrue((first["prompt"] as String).isNotBlank())

        // Review with grade 2 (SOLID) — first review -> +1 day exactly
        val reviewResp = restTemplate.postForEntity(
            "/api/v1/revision/cards/$firstCardId/review",
            HttpEntity(mapOf("grade" to 2)),
            Map::class.java
        )
        assertEquals(HttpStatus.OK, reviewResp.statusCode)
        val nextDue = Instant.parse(reviewResp.body!!["nextDueAt"] as String)
        val hours = Duration.between(Instant.now(), nextDue).toHours()
        assertTrue(hours >= 23 && hours <= 25, "nextDueAt must be +1d, was +${hours}h")
        assertEquals(1.0, (reviewResp.body!!["intervalDays"] as Number).toDouble(), 0.0001)
        assertEquals(2.5, (reviewResp.body!!["ease"] as Number).toDouble(), 0.0001)

        // The reviewed card leaves the due list
        val afterResp = restTemplate.getForEntity("/api/v1/revision/cards/due", Map::class.java)
        assertEquals(1, afterResp.body!!["dueCount"])
    }

    @Test
    fun `4 Per-user isolation - Bob never sees Alice's cards and his reviews never move her state`() {
        loginAs("alice_student", "studentPass")
        createPracticeSessionAndAnswerWrong()

        val aliceStatesBefore = retrievalCardStateRepository.findByIdUserId(aliceUser.id!!)
        assertEquals(2, aliceStatesBefore.size)
        val aliceDueAtBefore = aliceStatesBefore.first { it.id.cardId == firstCardId }.dueAt

        // Bob: no enrollment, empty due list
        loginAs("bob_student", "studentPass")
        val bobDueResp = restTemplate.getForEntity("/api/v1/revision/cards/due", Map::class.java)
        assertEquals(0, bobDueResp.body!!["dueCount"])

        // Bob reviews the same global card: he gets his OWN state row (self-enrollment),
        // and Alice's row must remain untouched.
        val bobReviewResp = restTemplate.postForEntity(
            "/api/v1/revision/cards/$firstCardId/review",
            HttpEntity(mapOf("grade" to 0)),
            Map::class.java
        )
        assertEquals(HttpStatus.OK, bobReviewResp.statusCode)
        assertEquals(1, retrievalCardStateRepository.findByIdUserId(bobUser.id!!).size)

        val aliceStateAfter = retrievalCardStateRepository.findById(
            RetrievalCardStateId(aliceUser.id!!, firstCardId)
        ).orElseThrow()
        assertEquals(0f, aliceStateAfter.intervalDays)      // still enrolled, never reviewed
        assertEquals(2.5f, aliceStateAfter.ease)
        assertNull(aliceStateAfter.lastReviewedAt)
        assertEquals(aliceDueAtBefore, aliceStateAfter.dueAt)
        assertEquals(2, retrievalCardStateRepository.findByIdUserId(aliceUser.id!!).size)
    }

    @Test
    fun `5 Validation - grade outside 0-2 returns 400 and unknown card returns 404`() {
        loginAs("alice_student", "studentPass")

        val badGradeResp = restTemplate.postForEntity(
            "/api/v1/revision/cards/$firstCardId/review",
            HttpEntity(mapOf("grade" to 3)),
            Map::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, badGradeResp.statusCode)

        val unknownCardResp = restTemplate.postForEntity(
            "/api/v1/revision/cards/${UUID.randomUUID()}/review",
            HttpEntity(mapOf("grade" to 1)),
            Map::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, unknownCardResp.statusCode)
    }
}

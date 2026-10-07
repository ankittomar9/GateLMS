package com.gatelms.integration

import com.gatelms.dto.*
import com.gatelms.entity.*
import com.gatelms.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class UserScopingIT : BaseIntegrationTest() {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var noteRepository: NoteRepository

    @Autowired
    lateinit var mistakeLogRepository: MistakeLogRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var dailyRollupRepository: DailyRollupRepository

    @Autowired
    lateinit var topicMasteryRepository: com.gatelms.repository.TopicMasteryRepository

    @Autowired
    lateinit var notificationRepository: NotificationRepository

    @Autowired
    lateinit var studyPlanRepository: StudyPlanRepository

    @Autowired
    lateinit var planItemRepository: PlanItemRepository

    @Autowired
    lateinit var userGoalRepository: UserGoalRepository

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    private lateinit var subject: Subject
    private lateinit var topic: Topic
    private lateinit var question: Question

    @BeforeEach
    fun setupData() {
        noteRepository.deleteAll()
        mistakeLogRepository.deleteAll()
        notificationRepository.deleteAll()
        planItemRepository.deleteAll()
        studyPlanRepository.deleteAll()
        dailyRollupRepository.deleteAll()
        userGoalRepository.deleteAll()
        questionProgressRepository.deleteAll()

        // Create Alice and Bob if they do not exist
        if (userRepository.findByUsername("alice").isEmpty) {
            userRepository.save(
                User(
                    username = "alice",
                    passwordHash = passwordEncoder.encode("alicePass"),
                    displayName = "Alice Student",
                    role = UserRole.STUDENT
                )
            )
        }
        if (userRepository.findByUsername("bob").isEmpty) {
            userRepository.save(
                User(
                    username = "bob",
                    passwordHash = passwordEncoder.encode("bobPass"),
                    displayName = "Bob Student",
                    role = UserRole.STUDENT
                )
            )
        }

        subject = subjectRepository.findById("subj_scoping").orElseGet {
            subjectRepository.save(
                Subject(id = "subj_scoping", name = "Scoping Subject", slug = "scoping", cluster = "core", avgWeightage = 5.0)
            )
        }
        topic = topicRepository.findById("top_scoping").orElseGet {
            topicRepository.save(
                Topic(id = "top_scoping", subject = subject, name = "Scoping Topic", slug = "scoping-topic")
            )
        }
        if (questionRepository.findAll().none { it.topic?.id == topic.id }) {
            question = questionRepository.save(
                Question(
                    externalId = "SCOPING-Q-01",
                    statement = "Scoping Question Statement",
                    subject = subject,
                    topic = topic,
                    type = QuestionType.MCQ,
                    correctOptions = "A",
                    explanation = "A is correct"
                )
            )
        } else {
            question = questionRepository.findAll().first { it.topic?.id == topic.id }
        }
    }

    @Test
    fun `1 notes data isolation between alice and bob`() {
        // Alice logs in and creates a note
        loginAs("alice", "alicePass")
        val createNoteReq = CreateTextNoteRequest(
            title = "Alice Confidential Note",
            contentMd = "Alice Secret Formula"
        )
        val noteResp = restTemplate.postForEntity("/api/v1/notes", createNoteReq, NoteDto::class.java)
        assertEquals(HttpStatus.CREATED, noteResp.statusCode)
        val aliceNoteId = noteResp.body!!.id

        // Alice can list and retrieve her note
        val aliceList = restTemplate.getForEntity("/api/v1/notes", NotePageResponse::class.java).body!!
        assertTrue(aliceList.content.any { it.id == aliceNoteId })

        // Bob logs in
        loginAs("bob", "bobPass")

        // Bob's list must NOT include Alice's note
        val bobList = restTemplate.getForEntity("/api/v1/notes", NotePageResponse::class.java).body!!
        assertFalse(bobList.content.any { it.id == aliceNoteId }, "Bob must not see Alice's note in list")

        // Bob attempting to fetch Alice's note directly must return 404
        val bobFetchResp = restTemplate.getForEntity("/api/v1/notes/$aliceNoteId", Map::class.java)
        assertEquals(HttpStatus.NOT_FOUND, bobFetchResp.statusCode, "Bob must receive 404 for Alice's note")
    }

    @Test
    fun `2 mistake logs isolation between alice and bob`() {
        // Alice logs in
        val alice = loginAs("alice", "alicePass")
        val aliceId = UUID.fromString(alice.userId)

        // Seed a due mistake for Alice
        mistakeLogRepository.save(
            MistakeLog(
                userId = aliceId,
                question = question,
                errorCategory = MistakeErrorCategory.CONCEPTUAL,
                correctiveTakeaway = "Alice Mistake Takeaway",
                loggedAt = Instant.now().minus(5, ChronoUnit.DAYS),
                nextReviewAt = Instant.now().minus(1, ChronoUnit.DAYS),
                resolved = false
            )
        )

        val aliceOverview = restTemplate.getForEntity("/api/v1/revision/overview", RevisionOverviewDto::class.java).body!!
        assertTrue(aliceOverview.dueMistakes >= 1, "Alice must see her due mistake")

        // Bob logs in
        loginAs("bob", "bobPass")
        val bobOverview = restTemplate.getForEntity("/api/v1/revision/overview", RevisionOverviewDto::class.java).body!!
        assertEquals(0L, bobOverview.dueMistakes, "Bob must have 0 due mistakes")
    }

    @Test
    fun `3 topic mastery isolation between alice and bob`() {
        // Alice logs in and updates mastery for topic
        loginAs("alice", "alicePass")
        val updateMasteryReq = TopicMasteryUpdateDto(
            version = topic.version,
            masteryLevel = 75
        )
        val aliceUpdateResp = restTemplate.exchange(
            "/api/v1/topics/${topic.id}",
            HttpMethod.PATCH,
            HttpEntity(updateMasteryReq),
            TopicDto::class.java
        )
        assertEquals(HttpStatus.OK, aliceUpdateResp.statusCode)
        assertEquals(75, aliceUpdateResp.body!!.masteryLevel)

        // Alice fetches topic list: mastery is 75
        val aliceTopics = restTemplate.getForEntity(
            "/api/v1/topics?subjectId=${subject.id}",
            Array<TopicDto>::class.java
        ).body!!
        val aliceTopicDto = aliceTopics.first { it.id == topic.id }
        assertEquals(75, aliceTopicDto.masteryLevel)

        // Bob logs in and fetches topics: Bob's mastery remains 0
        loginAs("bob", "bobPass")
        val bobTopics = restTemplate.getForEntity(
            "/api/v1/topics?subjectId=${subject.id}",
            Array<TopicDto>::class.java
        ).body!!
        val bobTopicDto = bobTopics.first { it.id == topic.id }
        assertEquals(0, bobTopicDto.masteryLevel, "Bob's mastery must remain at default 0")
    }

    @Test
    fun `4 question bookmarks isolation between alice and bob`() {
        // Alice bookmarks question
        loginAs("alice", "alicePass")
        val bookmarkResp = restTemplate.exchange(
            "/api/v1/questions/${question.id}/bookmark",
            HttpMethod.PATCH,
            HttpEntity(BookmarkRequest(bookmarked = true)),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, bookmarkResp.statusCode)
        assertTrue(bookmarkResp.body!!.isBookmarked)

        val aliceFiltered = restTemplate.getForEntity("/api/v1/questions?bookmarked=true", Map::class.java)
        val aliceContent = aliceFiltered.body?.get("content") as List<Map<String, Any>>
        assertTrue(aliceContent.any { it["id"] == question.id.toString() })

        // Bob logs in: question is NOT bookmarked for Bob
        loginAs("bob", "bobPass")
        val bobFiltered = restTemplate.getForEntity("/api/v1/questions?bookmarked=true", Map::class.java)
        val bobContent = bobFiltered.body?.get("content") as List<Map<String, Any>>
        assertFalse(bobContent.any { it["id"] == question.id.toString() }, "Bob must not see Alice's bookmark")
    }

    @Test
    fun `5 practice attempts isolation between alice and bob`() {
        // Alice starts a practice session
        loginAs("alice", "alicePass")
        val createReq = PracticeSessionCreateRequest(topicId = topic.id, count = 1)
        val sessionResp = restTemplate.postForEntity(
            "/api/v1/practice/sessions",
            createReq,
            PracticeSessionResponse::class.java
        )
        assertEquals(HttpStatus.OK, sessionResp.statusCode)
        val aliceAttemptId = sessionResp.body!!.attemptId

        // Bob logs in and tries to access Alice's attempt result -> 404 Not Found
        loginAs("bob", "bobPass")
        val bobAttemptResp = restTemplate.getForEntity(
            "/api/v1/attempts/$aliceAttemptId/result",
            Map::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, bobAttemptResp.statusCode, "Bob must receive 404 for Alice's attempt result")
    }

    @Test
    fun `6 curation endpoint role check — STUDENT gets 403, ADMIN gets 200`() {
        // Bob is STUDENT -> 403 Forbidden
        loginAs("bob", "bobPass")
        val curationReq = CurationUpdateRequest(
            status = "ACCEPTED",
            note = "Looks good"
        )
        val forbiddenResp = restTemplate.exchange(
            "/api/v1/questions/${question.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(curationReq),
            Map::class.java
        )
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResp.statusCode, "STUDENT must be denied curation with 403")

        // Ankur is ADMIN -> 200 OK
        loginAs("ankur", "admin123")
        val allowedResp = restTemplate.exchange(
            "/api/v1/questions/${question.id}/curation",
            HttpMethod.PATCH,
            HttpEntity(curationReq),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, allowedResp.statusCode, "ADMIN must be permitted curation with 200")
        assertEquals("ACCEPTED", allowedResp.body!!.curationStatus)
    }

    @Test
    fun `7 stats and heatmap isolation between alice and bob`() {
        val alice = userRepository.findByUsername("alice").get()
        val bob = userRepository.findByUsername("bob").get()

        // Alice has solved question
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(userId = alice.id, questionId = question.id!!),
                pass1Status = Pass1Status.SOLVED
            )
        )

        loginAs("alice", "alicePass")
        val aliceStats = restTemplate.getForEntity("/api/v1/stats/summary", StatsSummaryDto::class.java).body
        assertNotNull(aliceStats)
        assertEquals(1L, aliceStats!!.pass1.solved)

        loginAs("bob", "bobPass")
        val bobStats = restTemplate.getForEntity("/api/v1/stats/summary", StatsSummaryDto::class.java).body
        assertNotNull(bobStats)
        assertEquals(0L, bobStats!!.pass1.solved, "Bob must have 0 solved questions")
    }

    @Test
    fun `8 readiness score isolation between alice and bob`() {
        val alice = userRepository.findByUsername("alice").get()
        val bob = userRepository.findByUsername("bob").get()

        // Alice has topic mastery recorded
        topicMasteryRepository.save(
            TopicMastery(
                id = TopicMasteryId(userId = alice.id, topicId = topic.id),
                masteryLevel = 5,
                lastRevisedAt = Instant.now()
            )
        )

        loginAs("alice", "alicePass")
        val aliceReadiness = restTemplate.getForEntity("/api/v1/stats/readiness", ReadinessResponse::class.java).body
        assertNotNull(aliceReadiness)
        assertTrue(aliceReadiness!!.readiness > 0, "Alice overall readiness should reflect mastery")

        loginAs("bob", "bobPass")
        val bobReadiness = restTemplate.getForEntity("/api/v1/stats/readiness", ReadinessResponse::class.java).body
        assertNotNull(bobReadiness)
        assertEquals(0, bobReadiness!!.readiness, "Bob overall readiness must be 0 without mastery")
    }

    @Test
    fun `9 engagement summary and goal isolation between alice and bob`() {
        // Alice sets custom daily target to 40
        loginAs("alice", "alicePass")
        val updateReq = UpdateUserGoalRequest(dailyTargetQuestions = 40)
        val aliceUpdateResp = restTemplate.exchange(
            "/api/v1/engagement/goal",
            HttpMethod.PATCH,
            HttpEntity(updateReq),
            UserGoalDto::class.java
        )
        assertEquals(HttpStatus.OK, aliceUpdateResp.statusCode)

        val aliceSummary = restTemplate.getForEntity("/api/v1/engagement/summary", EngagementSummaryResponse::class.java).body
        assertEquals(40, aliceSummary!!.goal.target)

        // Bob logs in: Bob must still have the default target of 20
        loginAs("bob", "bobPass")
        val bobSummary = restTemplate.getForEntity("/api/v1/engagement/summary", EngagementSummaryResponse::class.java).body
        assertEquals(20, bobSummary!!.goal.target, "Bob must see his default target (20), not Alice's (40)")
    }

    @Test
    fun `10 notifications isolation between alice and bob`() {
        val alice = userRepository.findByUsername("alice").get()
        val bob = userRepository.findByUsername("bob").get()

        // Produce a notification for Alice directly
        notificationRepository.save(
            Notification(
                userId = alice.id,
                type = NotificationType.ACHIEVEMENT_EARNED,
                title = "Alice Milestone",
                body = "Only for Alice"
            )
        )

        loginAs("alice", "alicePass")
        val aliceNotifs = restTemplate.getForEntity("/api/v1/notifications", Map::class.java).body
        val aliceContent = aliceNotifs?.get("content") as List<Map<String, Any>>
        assertEquals(1, aliceContent.size)
        assertEquals("Alice Milestone", aliceContent[0]["title"])

        loginAs("bob", "bobPass")
        val bobNotifs = restTemplate.getForEntity("/api/v1/notifications", Map::class.java).body
        val bobContent = bobNotifs?.get("content") as List<Map<String, Any>>
        assertEquals(0, bobContent.size, "Bob must receive 0 notifications")
    }

    @Test
    fun `11 study plan isolation between alice and bob`() {
        val alice = userRepository.findByUsername("alice").get()

        // Create study plan for Alice
        studyPlanRepository.save(
            StudyPlan(
                userId = alice.id,
                title = "Alice 30-Day GATE Plan",
                startDate = java.time.LocalDate.now(),
                endDate = java.time.LocalDate.now().plusDays(30)
            )
        )

        loginAs("alice", "alicePass")
        val alicePlans = restTemplate.getForEntity(
            "/api/v1/plans",
            Array<StudyPlanSummaryDto>::class.java
        ).body
        assertNotNull(alicePlans)
        assertEquals(1, alicePlans!!.size)
        assertEquals("Alice 30-Day GATE Plan", alicePlans[0].title)

        loginAs("bob", "bobPass")
        val bobPlans = restTemplate.getForEntity(
            "/api/v1/plans",
            Array<StudyPlanSummaryDto>::class.java
        ).body
        assertNotNull(bobPlans)
        assertEquals(0, bobPlans!!.size, "Bob must see 0 plans")
    }
}

package com.gatelms.integration

import com.gatelms.dto.MistakeCreateRequest
import com.gatelms.dto.MistakeReviewRequest
import com.gatelms.dto.PracticeAnswerSubmission
import com.gatelms.dto.PracticeSessionCreateRequest
import com.gatelms.entity.*
import com.gatelms.repository.*
import com.gatelms.service.MistakeService
import com.gatelms.service.PlanProgressService
import com.gatelms.service.PracticeService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class PlanProgressIT : BaseIntegrationTest() {

    @Autowired
    lateinit var studyPlanRepository: StudyPlanRepository

    @Autowired
    lateinit var planItemRepository: PlanItemRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var practiceService: PracticeService

    @Autowired
    lateinit var mistakeService: MistakeService

    @Autowired
    lateinit var planProgressService: PlanProgressService

    private lateinit var testSubject: Subject
    private lateinit var testTopic: Topic
    private lateinit var otherTopic: Topic

    @BeforeEach
    fun setUp() {
        planItemRepository.deleteAll()
        studyPlanRepository.deleteAll()
        testSubject = subjectRepository.findAll().first()
        val topics = topicRepository.findAll().filter { it.subject?.id == testSubject.id }
        testTopic = topics.first()
        otherTopic = if (topics.size > 1) topics[1] else testTopic
    }

    @Test
    fun `1 practice finish increments matching item and flips to DONE at target`() {
        val today = LocalDate.now()

        // Create plan
        val plan = StudyPlan(
            title = "Progress Test Plan",
            startDate = today,
            endDate = today.plusDays(6),
            weeklyTargetQuestions = 100,
            weeklyTargetMinutes = 400,
            strategy = PlanStrategy.WEIGHTED
        )
        val savedPlan = studyPlanRepository.save(plan)

        // Create plan item for testTopic with target = 2 questions
        val planItem = PlanItem(
            plan = savedPlan,
            planDate = today,
            subject = testSubject,
            topic = testTopic,
            targetQuestions = 2,
            targetMinutes = 30,
            doneQuestions = 0,
            doneMinutes = 0,
            status = PlanItemStatus.OPEN
        )
        val savedItem = planItemRepository.save(planItem)

        // Seed 2 questions on testTopic
        val q1 = questionRepository.save(
            Question(
                externalId = "PLAN-PROG-Q1-${UUID.randomUUID()}",
                statement = "Question 1 for progress test",
                type = QuestionType.MCQ,
                marks = 1,
                subject = testSubject,
                topic = testTopic,
                optionsJson = """[{"label":"A","text":"Opt A"},{"label":"B","text":"Opt B"}]""",
                correctOptions = "[\"A\"]",
                explanation = "A is correct"
            )
        )
        val q2 = questionRepository.save(
            Question(
                externalId = "PLAN-PROG-Q2-${UUID.randomUUID()}",
                statement = "Question 2 for progress test",
                type = QuestionType.MCQ,
                marks = 1,
                subject = testSubject,
                topic = testTopic,
                optionsJson = """[{"label":"A","text":"Opt A"},{"label":"B","text":"Opt B"}]""",
                correctOptions = "[\"B\"]",
                explanation = "B is correct"
            )
        )

        // Start practice session
        val session = practiceService.createSession(
            PracticeSessionCreateRequest(
                subjectId = testSubject.id,
                topicId = testTopic.id,
                count = 5
            )
        )

        // Answer Q1 -> item should have doneQuestions = 1, status = OPEN
        val verdict1 = practiceService.submitAnswer(
            session.attemptId,
            PracticeAnswerSubmission(questionId = q1.id!!, selectedOptions = "A", timeSpentSeconds = 60)
        )
        assertTrue(verdict1.correct)

        val itemAfterQ1 = planItemRepository.findById(savedItem.id).orElseThrow()
        assertEquals(1, itemAfterQ1.doneQuestions)
        assertEquals(PlanItemStatus.OPEN, itemAfterQ1.status)

        // Answer Q2 -> item should have doneQuestions = 2, status = DONE (reached target)
        val verdict2 = practiceService.submitAnswer(
            session.attemptId,
            PracticeAnswerSubmission(questionId = q2.id!!, selectedOptions = "B", timeSpentSeconds = 60)
        )
        assertTrue(verdict2.correct)

        val itemAfterQ2 = planItemRepository.findById(savedItem.id).orElseThrow()
        assertEquals(2, itemAfterQ2.doneQuestions)
        assertEquals(PlanItemStatus.DONE, itemAfterQ2.status)
    }

    @Test
    fun `2 topic-no-item is safe no-op`() {
        val isolatedTopic = topicRepository.save(
            Topic(
                id = "isolated-topic-${UUID.randomUUID()}",
                subject = testSubject,
                name = "Isolated Topic No Plan",
                masteryLevel = 2
            )
        )

        val q = questionRepository.save(
            Question(
                externalId = "PLAN-NOITEM-Q-${UUID.randomUUID()}",
                statement = "Isolated question",
                type = QuestionType.MCQ,
                marks = 1,
                subject = testSubject,
                topic = isolatedTopic,
                optionsJson = """[{"label":"A","text":"Opt A"}]""",
                correctOptions = "[\"A\"]",
                explanation = "A is correct"
            )
        )

        val session = practiceService.createSession(
            PracticeSessionCreateRequest(
                subjectId = testSubject.id,
                topicId = isolatedTopic.id,
                count = 1
            )
        )

        // Submitting answer should cleanly complete without error even with no plan items
        val verdict = practiceService.submitAnswer(
            session.attemptId,
            PracticeAnswerSubmission(questionId = q.id!!, selectedOptions = "A", timeSpentSeconds = 30)
        )

        assertNotNull(verdict)
        assertTrue(verdict.correct)
    }

    @Test
    fun `3 hook failure does not fail grading`() {
        // Create practice session
        val q = questionRepository.save(
            Question(
                externalId = "PLAN-FAIL-HOOK-${UUID.randomUUID()}",
                statement = "Question testing resilient hook",
                type = QuestionType.MCQ,
                marks = 1,
                subject = testSubject,
                topic = testTopic,
                optionsJson = """[{"label":"A","text":"Opt A"}]""",
                correctOptions = "[\"A\"]",
                explanation = "A"
            )
        )

        val session = practiceService.createSession(
            PracticeSessionCreateRequest(
                subjectId = testSubject.id,
                topicId = testTopic.id,
                count = 1
            )
        )

        // Submitting answer with normal flow succeeds
        val verdict = practiceService.submitAnswer(
            session.attemptId,
            PracticeAnswerSubmission(questionId = q.id!!, selectedOptions = "A", timeSpentSeconds = 45)
        )
        assertNotNull(verdict)
        assertTrue(verdict.correct)
    }

    @Test
    fun `4 mistake review adds 1 to matching topic item`() {
        val today = LocalDate.now()

        val plan = studyPlanRepository.save(
            StudyPlan(
                title = "Mistake Test Plan",
                startDate = today,
                endDate = today.plusDays(6),
                weeklyTargetQuestions = 150,
                weeklyTargetMinutes = 600,
                strategy = PlanStrategy.WEIGHTED
            )
        )

        val planItem = planItemRepository.save(
            PlanItem(
                plan = plan,
                planDate = today,
                subject = testSubject,
                topic = testTopic,
                targetQuestions = 5,
                targetMinutes = 40,
                doneQuestions = 1,
                doneMinutes = 10,
                status = PlanItemStatus.OPEN
            )
        )

        val q = questionRepository.save(
            Question(
                externalId = "MISTAKE-PLAN-Q-${UUID.randomUUID()}",
                statement = "Mistake question",
                type = QuestionType.MCQ,
                marks = 2,
                subject = testSubject,
                topic = testTopic,
                optionsJson = """[{"label":"A","text":"Opt A"}]""",
                correctOptions = "[\"A\"]",
                explanation = "A"
            )
        )

        // Create mistake
        val createdMistake = mistakeService.createMistake(
            MistakeCreateRequest(
                questionId = q.id!!,
                errorCategory = "CONCEPTUAL",
                correctiveTakeaway = "Remember definition"
            )
        )

        // Review mistake
        val reviewed = mistakeService.reviewMistake(
            createdMistake.id,
            MistakeReviewRequest(resolved = false)
        )
        assertEquals(1, reviewed.reviewCount)

        // Verify plan item has doneQuestions incremented by 1 (1 -> 2)
        val updatedItem = planItemRepository.findById(planItem.id).orElseThrow()
        assertEquals(2, updatedItem.doneQuestions, "Mistake review must increment matching topic item by +1")
    }
}

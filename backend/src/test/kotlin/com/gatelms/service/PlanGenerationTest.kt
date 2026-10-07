package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.GeneratePlanRequest
import com.gatelms.dto.SubjectReadinessDto
import com.gatelms.entity.*
import com.gatelms.error.ValidationException
import com.gatelms.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.LocalDate

private fun <T> anyNonNull(): T {
    org.mockito.ArgumentMatchers.any<T>()
    @Suppress("UNCHECKED_CAST")
    return null as T
}

class PlanGenerationTest {

    private lateinit var studyPlanRepository: StudyPlanRepository
    private lateinit var planItemRepository: PlanItemRepository
    private lateinit var subjectRepository: SubjectRepository
    private lateinit var topicRepository: TopicRepository
    private lateinit var questionRepository: QuestionRepository
    private lateinit var attemptAnswerRepository: AttemptAnswerRepository
    private lateinit var topicMasteryRepository: TopicMasteryRepository
    private lateinit var currentUserProvider: com.gatelms.security.CurrentUserProvider
    private lateinit var readinessService: ReadinessService
    private val objectMapper = ObjectMapper()

    private lateinit var planService: PlanService

    private val subjectAlgo = Subject(id = "CS-ALGO", name = "Algorithms", slug = "cs-algo", cluster = "CS", avgWeightage = 10.0)
    private val subjectOs = Subject(id = "CS-OS", name = "Operating Systems", slug = "cs-os", cluster = "CS", avgWeightage = 8.0)
    private val subjectTOC = Subject(id = "CS-TOC", name = "Theory of Computation", slug = "cs-toc", cluster = "CS", avgWeightage = 6.0)

    private val topicAlgo1 = Topic(id = "top-algo-1", subject = subjectAlgo, name = "Sorting")
    private val topicAlgo2 = Topic(id = "top-algo-2", subject = subjectAlgo, name = "Dynamic Programming")
    private val topicOs1 = Topic(id = "top-os-1", subject = subjectOs, name = "Scheduling")
    private val topicTOC1 = Topic(id = "top-toc-1", subject = subjectTOC, name = "Regular Languages")

    @BeforeEach
    fun setup() {
        studyPlanRepository = mock(StudyPlanRepository::class.java)
        planItemRepository = mock(PlanItemRepository::class.java)
        subjectRepository = mock(SubjectRepository::class.java)
        topicRepository = mock(TopicRepository::class.java)
        questionRepository = mock(QuestionRepository::class.java)
        attemptAnswerRepository = mock(AttemptAnswerRepository::class.java)
        topicMasteryRepository = mock(TopicMasteryRepository::class.java)
        currentUserProvider = mock(com.gatelms.security.CurrentUserProvider::class.java)
        readinessService = mock(ReadinessService::class.java)

        `when`(currentUserProvider.getCurrentUserId()).thenReturn(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
        `when`(topicMasteryRepository.findByUserId(anyNonNull())).thenReturn(emptyList())
        `when`(studyPlanRepository.save(anyNonNull())).thenAnswer { it.arguments[0] }
        `when`(planItemRepository.saveAll(anyNonNull<List<PlanItem>>())).thenAnswer { it.arguments[0] }

        planService = PlanService(
            studyPlanRepository = studyPlanRepository,
            planItemRepository = planItemRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            questionRepository = questionRepository,
            attemptAnswerRepository = attemptAnswerRepository,
            topicMasteryRepository = topicMasteryRepository,
            readinessService = readinessService,
            currentUserProvider = currentUserProvider,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `1 WEIGHTED shares on hand-computable fixture`() {
        // Hand-computable fixture:
        // Subject 1 (Algo): weight = 10.0, readiness = 0.60
        // S_1 = 10.0 * (1.0 + 0.5 * (1.0 - 0.60)) = 10.0 * 1.20 = 12.0
        // Subject 2 (OS): weight = 8.0, readiness = 0.20
        // S_2 = 8.0 * (1.0 + 0.5 * (1.0 - 0.20)) = 8.0 * 1.40 = 11.2
        val subjects = listOf(subjectAlgo, subjectOs)
        val readinessMap = mapOf(
            "CS-ALGO" to 0.60,
            "CS-OS" to 0.20
        )

        val shares = planService.computeSubjectShares(
            subjects = subjects,
            readinessMap = readinessMap,
            hasActivity = true,
            strategy = PlanStrategy.WEIGHTED
        )

        val algoShare = shares.find { it.subject.id == "CS-ALGO" }!!
        val osShare = shares.find { it.subject.id == "CS-OS" }!!

        assertEquals(12.0, algoShare.share, 0.001, "Algo share must be exactly 10.0 * (1 + 0.5 * 0.4) = 12.0")
        assertEquals(11.2, osShare.share, 0.001, "OS share must be exactly 8.0 * (1 + 0.5 * 0.8) = 11.2")
    }

    @Test
    fun `2 WEAKNESS_FIRST front-loads weakest subject`() {
        // Algo readiness = 0.8 (strong), OS readiness = 0.1 (weakest), TOC readiness = 0.5 (medium)
        `when`(subjectRepository.findAll()).thenReturn(listOf(subjectAlgo, subjectOs, subjectTOC))
        `when`(topicRepository.findAll()).thenReturn(listOf(topicAlgo1, topicOs1, topicTOC1))

        // All topics have questions
        val counts: List<Array<Any>> = listOf(
            arrayOf("top-algo-1", 10L),
            arrayOf("top-os-1", 10L),
            arrayOf("top-toc-1", 10L)
        )
        `when`(questionRepository.countQuestionsGroupedByTopic()).thenReturn(counts)
        `when`(attemptAnswerRepository.countDistinctQuestionId(anyNonNull())).thenReturn(25L)

        val readinessResp = com.gatelms.dto.ReadinessResponse(
            readiness = 46,
            perSubject = listOf(
                SubjectReadinessDto("CS-ALGO", "Algorithms", 10.0, 0.8, 0.8, 0.8, 80.0),
                SubjectReadinessDto("CS-OS", "Operating Systems", 8.0, 0.1, 0.1, 0.1, 10.0),
                SubjectReadinessDto("CS-TOC", "Theory of Computation", 6.0, 0.5, 0.5, 0.5, 50.0)
            )
        )
        `when`(readinessService.getReadiness()).thenReturn(readinessResp)

        val plan = planService.generatePlan(
            GeneratePlanRequest(
                weeks = 4,
                strategy = PlanStrategy.WEAKNESS_FIRST,
                startDate = LocalDate.of(2026, 10, 1)
            )
        )

        assertNotNull(plan)
        assertTrue(plan.items.isNotEmpty())

        // The very first study day item must be from the weakest subject (CS-OS)
        val firstStudyItem = plan.items.first()
        assertEquals("CS-OS", firstStudyItem.subjectId, "WEAKNESS_FIRST must schedule the weakest subject first")
    }

    @Test
    fun `3 fresh-system degenerates to weightage_only + reasoning recorded`() {
        `when`(subjectRepository.findAll()).thenReturn(listOf(subjectAlgo, subjectOs))
        `when`(topicRepository.findAll()).thenReturn(listOf(topicAlgo1, topicOs1))
        `when`(attemptAnswerRepository.countDistinctQuestionId(anyNonNull())).thenReturn(0L)
        val counts: List<Array<Any>> = listOf(
            arrayOf("top-algo-1", 5L),
            arrayOf("top-os-1", 5L)
        )
        `when`(questionRepository.countQuestionsGroupedByTopic()).thenReturn(counts)

        val plan = planService.generatePlan(
            GeneratePlanRequest(
                weeks = 2,
                strategy = PlanStrategy.WEIGHTED,
                startDate = LocalDate.of(2026, 10, 1)
            )
        )

        assertNotNull(plan)
        val reasoning = plan.generatedReasoning
        assertNotNull(reasoning, "Reasoning must be recorded")
        assertEquals("weightage_only", reasoning?.get("mode"))
        assertEquals("no_activity", reasoning?.get("reason"))

        // Subject shares equal weightage
        val sharesMap = reasoning?.get("subjectShares") as? Map<*, *>
        assertNotNull(sharesMap)
        val algoShare = sharesMap?.get("CS-ALGO") as? Map<*, *>
        assertEquals(10.0, algoShare?.get("share"))
    }

    @Test
    fun `4 empty-bank topic excluded + listed`() {
        `when`(subjectRepository.findAll()).thenReturn(listOf(subjectAlgo))
        // topicAlgo1 has questions, topicAlgo2 has ZERO questions in bank
        `when`(topicRepository.findAll()).thenReturn(listOf(topicAlgo1, topicAlgo2))
        `when`(attemptAnswerRepository.countDistinctQuestionId(anyNonNull())).thenReturn(5L)
        val counts: List<Array<Any>> = listOf(
            arrayOf("top-algo-1", 10L)
            // top-algo-2 omitted => 0 questions
        )
        `when`(questionRepository.countQuestionsGroupedByTopic()).thenReturn(counts)

        val plan = planService.generatePlan(
            GeneratePlanRequest(
                weeks = 1,
                strategy = PlanStrategy.LINEAR,
                startDate = LocalDate.of(2026, 10, 1)
            )
        )

        // Check scheduled items: only top-algo-1 scheduled
        val scheduledTopicIds = plan.items.mapNotNull { it.topicId }.distinct()
        assertTrue(scheduledTopicIds.contains("top-algo-1"))
        assertFalse(scheduledTopicIds.contains("top-algo-2"), "Empty bank topic must not be scheduled")

        // Check reasoning lists excluded topic
        val reasoning = plan.generatedReasoning
        val excluded = reasoning?.get("excludedTopics") as? List<*>
        assertNotNull(excluded)
        assertEquals(1, excluded?.size)
        val exItem = excluded?.first() as? Map<*, *>
        assertEquals("top-algo-2", exItem?.get("id"))
        assertEquals("zero_questions", exItem?.get("reason"))
    }

    @Test
    fun `5 deterministic — two calls, same output`() {
        `when`(subjectRepository.findAll()).thenReturn(listOf(subjectAlgo, subjectOs))
        `when`(topicRepository.findAll()).thenReturn(listOf(topicAlgo1, topicOs1))
        `when`(attemptAnswerRepository.countDistinctQuestionId(anyNonNull())).thenReturn(10L)
        val counts: List<Array<Any>> = listOf(
            arrayOf("top-algo-1", 15L),
            arrayOf("top-os-1", 15L)
        )
        `when`(questionRepository.countQuestionsGroupedByTopic()).thenReturn(counts)

        val req = GeneratePlanRequest(
            weeks = 3,
            strategy = PlanStrategy.WEIGHTED,
            weeklyTargetQuestions = 120,
            startDate = LocalDate.of(2026, 10, 1)
        )

        val plan1 = planService.generatePlan(req)
        val plan2 = planService.generatePlan(req)

        assertEquals(plan1.items.size, plan2.items.size)
        for (i in plan1.items.indices) {
            val item1 = plan1.items[i]
            val item2 = plan2.items[i]
            assertEquals(item1.planDate, item2.planDate)
            assertEquals(item1.subjectId, item2.subjectId)
            assertEquals(item1.topicId, item2.topicId)
            assertEquals(item1.targetQuestions, item2.targetQuestions)
            assertEquals(item1.targetMinutes, item2.targetMinutes)
        }
    }

    @Test
    fun `6 validation throws when weeks out of bounds`() {
        assertThrows(ValidationException::class.java) {
            planService.generatePlan(GeneratePlanRequest(weeks = 0))
        }

        assertThrows(ValidationException::class.java) {
            planService.generatePlan(GeneratePlanRequest(weeks = 53))
        }
    }
}

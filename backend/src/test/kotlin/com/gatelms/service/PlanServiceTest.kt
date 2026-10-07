package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.GeneratePlanRequest
import com.gatelms.dto.UpdatePlanItemStatusRequest
import com.gatelms.entity.*
import com.gatelms.error.EntityNotFoundException
import com.gatelms.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.LocalDate
import java.util.*

class PlanServiceTest {

    private lateinit var studyPlanRepository: StudyPlanRepository
    private lateinit var planItemRepository: PlanItemRepository
    private lateinit var subjectRepository: SubjectRepository
    private lateinit var topicRepository: TopicRepository
    private lateinit var questionRepository: QuestionRepository
    private lateinit var attemptAnswerRepository: AttemptAnswerRepository
    private lateinit var readinessService: ReadinessService
    private val objectMapper = ObjectMapper()

    private lateinit var planService: PlanService

    @BeforeEach
    fun setup() {
        studyPlanRepository = mock(StudyPlanRepository::class.java)
        planItemRepository = mock(PlanItemRepository::class.java)
        subjectRepository = mock(SubjectRepository::class.java)
        topicRepository = mock(TopicRepository::class.java)
        questionRepository = mock(QuestionRepository::class.java)
        attemptAnswerRepository = mock(AttemptAnswerRepository::class.java)
        readinessService = mock(ReadinessService::class.java)

        planService = PlanService(
            studyPlanRepository = studyPlanRepository,
            planItemRepository = planItemRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            questionRepository = questionRepository,
            attemptAnswerRepository = attemptAnswerRepository,
            readinessService = readinessService,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `today plan returns 404-free empty response when no active plan`() {
        `when`(studyPlanRepository.findActivePlansForDate(any(LocalDate::class.java) ?: LocalDate.now())).thenReturn(emptyList())

        val todayResp = planService.getTodayPlan()

        assertNotNull(todayResp)
        assertTrue(todayResp.items.isEmpty())
        assertEquals(0, todayResp.doneQuestions)
        assertEquals(0, todayResp.targetQuestions)
        assertEquals(0, todayResp.minutesDone)
        assertEquals(0, todayResp.minutesTarget)
    }

    @Test
    fun `getPlanById throws EntityNotFoundException for unknown id`() {
        val unknownId = UUID.randomUUID()
        `when`(studyPlanRepository.findById(unknownId)).thenReturn(Optional.empty())

        assertThrows(EntityNotFoundException::class.java) {
            planService.getPlanById(unknownId)
        }
    }

    @Test
    fun `updateItemStatus updates to DONE and sets done metrics to target`() {
        val itemId = UUID.randomUUID()
        val plan = StudyPlan(title = "Test")
        val item = PlanItem(
            id = itemId,
            plan = plan,
            planDate = LocalDate.now(),
            targetQuestions = 10,
            targetMinutes = 30,
            doneQuestions = 4,
            doneMinutes = 12,
            status = PlanItemStatus.OPEN
        )

        `when`(planItemRepository.findById(itemId)).thenReturn(Optional.of(item))
        `when`(planItemRepository.save(any())).thenAnswer { it.arguments[0] }

        val res = planService.updateItemStatus(itemId, UpdatePlanItemStatusRequest(status = PlanItemStatus.DONE))

        assertEquals(PlanItemStatus.DONE, res.status)
        assertEquals(10, res.doneQuestions)
        assertEquals(30, res.doneMinutes)
    }
}

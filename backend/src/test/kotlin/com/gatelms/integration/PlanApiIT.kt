package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.*
import com.gatelms.entity.PlanItemStatus
import com.gatelms.entity.PlanStrategy
import com.gatelms.repository.PlanItemRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.StudyPlanRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import java.time.LocalDate
import java.util.UUID

class PlanApiIT : BaseIntegrationTest() {

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
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        planItemRepository.deleteAll()
        studyPlanRepository.deleteAll()
        if (questionRepository.count() < 10) {
            val runner = QuestionSeedRunner(
                questionRepository = questionRepository,
                subjectRepository = subjectRepository,
                topicRepository = topicRepository,
                objectMapper = objectMapper,
                seedPath = "seed-data/questions.jsonl"
            )
            runner.importQuestions("seed-data/questions.jsonl")
        }
    }

    @Test
    fun `1 generate plan, list plans, and get today plan shapes`() {
        val generateReq = GeneratePlanRequest(
            title = "API Test Plan",
            weeks = 2,
            strategy = PlanStrategy.WEIGHTED,
            weeklyTargetQuestions = 120,
            weeklyTargetMinutes = 480
        )

        val genResp = restTemplate.postForEntity(
            "/api/v1/plans/generate",
            generateReq,
            StudyPlanDetailDto::class.java
        )

        assertEquals(HttpStatus.CREATED, genResp.statusCode)
        val plan = genResp.body!!
        assertNotNull(plan.id)
        assertEquals("API Test Plan", plan.title)
        assertEquals(2, plan.weeks)
        assertTrue(plan.items.isNotEmpty())
        assertNotNull(plan.generatedReasoning)

        // 2. List plans
        val listResp = restTemplate.exchange(
            "/api/v1/plans",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<StudyPlanSummaryDto>>() {}
        )
        assertEquals(HttpStatus.OK, listResp.statusCode)
        val plansList = listResp.body!!
        assertTrue(plansList.any { it.id == plan.id })

        // 3. Get plan by ID
        val getResp = restTemplate.getForEntity(
            "/api/v1/plans/${plan.id}",
            StudyPlanDetailDto::class.java
        )
        assertEquals(HttpStatus.OK, getResp.statusCode)
        assertEquals(plan.id, getResp.body!!.id)

        // 4. Get today's plan
        val todayResp = restTemplate.getForEntity(
            "/api/v1/plans/today",
            TodayPlanResponse::class.java
        )
        assertEquals(HttpStatus.OK, todayResp.statusCode)
        assertNotNull(todayResp.body)
    }

    @Test
    fun `2 validation returns 400 with fieldErrors for invalid weeks`() {
        val invalidReqZero = GeneratePlanRequest(weeks = 0)
        val respZero = restTemplate.postForEntity(
            "/api/v1/plans/generate",
            invalidReqZero,
            Map::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, respZero.statusCode)
        val bodyZero = respZero.body as? Map<*, *>
        val fieldErrors = bodyZero?.get("fieldErrors") as? Map<*, *>
        assertNotNull(fieldErrors, "Expected fieldErrors in 400 problem detail")
        assertTrue(fieldErrors?.containsKey("weeks") == true, "Expected fieldErrors.weeks")

        val invalidReqTooLarge = GeneratePlanRequest(weeks = 53)
        val respLarge = restTemplate.postForEntity(
            "/api/v1/plans/generate",
            invalidReqTooLarge,
            Map::class.java
        )
        assertEquals(HttpStatus.BAD_REQUEST, respLarge.statusCode)
    }

    @Test
    fun `3 PATCH plan-item transitions status and returns 404 for unknown`() {
        // Generate plan first
        val genResp = restTemplate.postForEntity(
            "/api/v1/plans/generate",
            GeneratePlanRequest(weeks = 1, strategy = PlanStrategy.LINEAR),
            StudyPlanDetailDto::class.java
        )
        val plan = genResp.body!!
        val firstItem = plan.items.first()

        // PATCH item status to DONE
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val patchReq = HttpEntity(UpdatePlanItemStatusRequest(status = PlanItemStatus.DONE), headers)

        val patchResp = restTemplate.exchange(
            "/api/v1/plan-items/${firstItem.id}",
            HttpMethod.PATCH,
            patchReq,
            PlanItemDto::class.java
        )
        assertEquals(HttpStatus.OK, patchResp.statusCode)
        assertEquals(PlanItemStatus.DONE, patchResp.body!!.status)

        // Unknown ID returns 404
        val unknownId = UUID.randomUUID()
        val notFoundResp = restTemplate.exchange(
            "/api/v1/plan-items/$unknownId",
            HttpMethod.PATCH,
            patchReq,
            Map::class.java
        )
        assertEquals(HttpStatus.NOT_FOUND, notFoundResp.statusCode)
    }

    @Test
    fun `4 regenerate preserves past DONE items`() {
        val today = LocalDate.now()
        val genResp = restTemplate.postForEntity(
            "/api/v1/plans/generate",
            GeneratePlanRequest(weeks = 2, startDate = today.minusDays(3)),
            StudyPlanDetailDto::class.java
        )
        val plan = genResp.body!!
        assertTrue(plan.items.isNotEmpty())

        // Mark first item as DONE
        val itemToDone = plan.items.first()
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        restTemplate.exchange(
            "/api/v1/plan-items/${itemToDone.id}",
            HttpMethod.PATCH,
            HttpEntity(UpdatePlanItemStatusRequest(status = PlanItemStatus.DONE), headers),
            PlanItemDto::class.java
        )

        // Regenerate plan
        val regenResp = restTemplate.postForEntity(
            "/api/v1/plans/${plan.id}/regenerate",
            null,
            StudyPlanDetailDto::class.java
        )

        assertEquals(HttpStatus.OK, regenResp.statusCode)
        val regenPlan = regenResp.body!!

        // Assert that itemToDone is preserved and still DONE!
        val preservedItem = regenPlan.items.find { it.id == itemToDone.id }
        assertNotNull(preservedItem, "DONE item must be preserved after regeneration")
        assertEquals(PlanItemStatus.DONE, preservedItem!!.status)
    }
}

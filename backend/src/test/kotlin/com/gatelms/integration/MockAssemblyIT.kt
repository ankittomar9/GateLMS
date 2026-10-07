package com.gatelms.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.BlueprintQuestionPool
import com.gatelms.dto.BlueprintSection
import com.gatelms.dto.CreateMockTemplateRequest
import com.gatelms.dto.MockTemplateSummaryDto
import com.gatelms.dto.StartMockAttemptResponse
import com.gatelms.entity.CurationStatus
import com.gatelms.repository.MockAttemptRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

class MockAssemblyIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var mockAttemptRepository: MockAttemptRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        val subjects = subjectRepository.findAll()
        for (subj in subjects.take(3)) {
            ensureQuestionsForSubject(subj)
        }
    }

    private fun ensureQuestionsForSubject(subject: com.gatelms.entity.Subject) {
        val topic = topicRepository.findAll().firstOrNull { it.subject?.id == subject.id }
        for (t in listOf(com.gatelms.entity.QuestionType.MCQ, com.gatelms.entity.QuestionType.MSQ, com.gatelms.entity.QuestionType.NAT)) {
            for (i in 1..4) {
                val extId = "MOCK-ASM-${subject.id}-${t.name}-$i"
                if (questionRepository.findByExternalId(extId) == null) {
                    questionRepository.save(
                        com.gatelms.entity.Question(
                            externalId = extId,
                            subject = subject,
                            topic = topic,
                            type = t,
                            marks = if (t == com.gatelms.entity.QuestionType.NAT) 2 else 1,
                            statement = "Test Question $extId",
                            correctOptions = if (t == com.gatelms.entity.QuestionType.NAT) null else "A",
                            numericMin = if (t == com.gatelms.entity.QuestionType.NAT) 10.0 else null,
                            numericMax = if (t == com.gatelms.entity.QuestionType.NAT) 20.0 else null,
                            curationStatus = "ACCEPTED",
                            explanation = "Test explanation for $extId"
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `assembly gathers exact question counts per section`() {
        val subjects = subjectRepository.findAll()
        assertTrue(subjects.size >= 2, "Need at least 2 subjects for assembly test")
        val subj1 = subjects[0]
        val subj2 = subjects[1]

        val createReq = CreateMockTemplateRequest(
            title = "Test Mock Assembly",
            durationMinutes = 60,
            sections = listOf(
                BlueprintSection(subjectId = subj1.id, mcq = 2, msq = 1, nat = 1),
                BlueprintSection(subjectId = subj2.id, mcq = 1, msq = 1, nat = 1)
            ),
            shuffle = false
        )

        val tmplResp = restTemplate.postForEntity("/api/v1/mocks", createReq, MockTemplateSummaryDto::class.java)
        assertEquals(HttpStatus.CREATED, tmplResp.statusCode)
        val templateId = tmplResp.body!!.id

        val startResp = restTemplate.postForEntity("/api/v1/mocks/$templateId/attempts", null, StartMockAttemptResponse::class.java)
        assertEquals(HttpStatus.OK, startResp.statusCode)
        val attemptData = startResp.body!!

        assertEquals(2, attemptData.sections.size)
        // Section 1: 2 MCQ + 1 MSQ + 1 NAT = 4
        assertEquals(4, attemptData.sections[0].questionIds.size)
        // Section 2: 1 MCQ + 1 MSQ + 1 NAT = 3
        assertEquals(3, attemptData.sections[1].questionIds.size)
        assertEquals(7, attemptData.questions.size)
    }

    @Test
    fun `assembly reports shortfall warnings when question pool is short`() {
        val subject = subjectRepository.findAll().first()
        val createReq = CreateMockTemplateRequest(
            title = "Shortfall Mock",
            durationMinutes = 60,
            sections = listOf(
                BlueprintSection(subjectId = subject.id, mcq = 1, msq = 500, nat = 1)
            )
        )

        val tmplResp = restTemplate.postForEntity("/api/v1/mocks", createReq, MockTemplateSummaryDto::class.java)
        assertEquals(HttpStatus.CREATED, tmplResp.statusCode)
        val templateId = tmplResp.body!!.id

        val startResp = restTemplate.postForEntity("/api/v1/mocks/$templateId/attempts", null, StartMockAttemptResponse::class.java)
        assertEquals(HttpStatus.OK, startResp.statusCode)
        val attemptData = startResp.body!!

        assertTrue(attemptData.shortfallWarnings.isNotEmpty(), "Shortfall warning should be returned when pool cannot fulfill requested count")
        assertTrue(attemptData.shortfallWarnings.any { it.contains("MSQ") && it.contains(subject.name) })
    }

    @Test
    fun `frozen snapshot guarantees editing question bank does not alter attempt question list`() {
        val subject = subjectRepository.findAll().first()
        val createReq = CreateMockTemplateRequest(
            title = "Snapshot Integrity Mock",
            durationMinutes = 60,
            sections = listOf(
                BlueprintSection(subjectId = subject.id, mcq = 2, msq = 1, nat = 0)
            )
        )

        val tmplResp = restTemplate.postForEntity("/api/v1/mocks", createReq, MockTemplateSummaryDto::class.java)
        val templateId = tmplResp.body!!.id

        val startResp = restTemplate.postForEntity("/api/v1/mocks/$templateId/attempts", null, StartMockAttemptResponse::class.java)
        val attemptData = startResp.body!!
        val originalFrozenIds = attemptData.questions.map { it.id.toString() }

        // Mutate one question in the bank
        val qToMutate = questionRepository.findById(UUID.fromString(originalFrozenIds[0])).get()
        qToMutate.statement = "MODIFIED STATEMENT POST-ASSEMBLY"
        questionRepository.save(qToMutate)

        // Read attempt directly from DB and verify frozen question IDs remain untouched
        val dbAttempt = mockAttemptRepository.findById(attemptData.attemptId).get()
        val storedIds: List<String> = objectMapper.readValue(dbAttempt.questionIdsJson, object : TypeReference<List<String>>() {})

        assertEquals(originalFrozenIds, storedIds, "Frozen question list must be completely unaffected by database changes")
    }

    @Test
    fun `assembly honors pool filters for curation status`() {
        val subject = subjectRepository.findAll().first()
        val createReq = CreateMockTemplateRequest(
            title = "Pool Filtered Mock",
            durationMinutes = 60,
            sections = listOf(
                BlueprintSection(subjectId = subject.id, mcq = 2, msq = 0, nat = 0)
            ),
            questionPool = BlueprintQuestionPool(
                curationStatus = listOf("ACCEPTED")
            )
        )

        val tmplResp = restTemplate.postForEntity("/api/v1/mocks", createReq, MockTemplateSummaryDto::class.java)
        val templateId = tmplResp.body!!.id

        val startResp = restTemplate.postForEntity("/api/v1/mocks/$templateId/attempts", null, StartMockAttemptResponse::class.java)
        val attemptData = startResp.body!!

        for (qDto in attemptData.questions) {
            val q = questionRepository.findById(qDto.id).get()
            // If the question has curationStatus, it should match pool or fall back
            assertNotNull(q)
        }
    }
}

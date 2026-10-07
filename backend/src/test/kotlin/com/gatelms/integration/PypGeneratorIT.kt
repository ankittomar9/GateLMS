package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.*
import com.gatelms.entity.*
import com.gatelms.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.UUID

class PypGeneratorIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var mockExamRepository: MockExamRepository

    @Autowired
    lateinit var mockAttemptRepository: MockAttemptRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var gaSubject: Subject
    private lateinit var algoSubject: Subject
    private lateinit var osSubject: Subject

    @BeforeEach
    fun setUp() {
        gaSubject = subjectRepository.findBySlug("general-aptitude")
            ?: subjectRepository.findAll().first()
        algoSubject = subjectRepository.findBySlug("algorithms")
            ?: subjectRepository.findAll().first { it.id != gaSubject.id }
        osSubject = subjectRepository.findBySlug("operating-systems")
            ?: subjectRepository.findAll().first { it.id != gaSubject.id && it.id != algoSubject.id }
    }

    private fun seedYearQuestions(year: Int, gaCount: Int, coreCount: Int) {
        val gaTopic = topicRepository.findAll().firstOrNull { it.subject?.id == gaSubject.id }
        val algoTopic = topicRepository.findAll().firstOrNull { it.subject?.id == algoSubject.id }
        val osTopic = topicRepository.findAll().firstOrNull { it.subject?.id == osSubject.id }

        // Seed GA
        for (i in 1..gaCount) {
            val marks = if (i <= 5) 1 else 2
            val type = when (i % 3) {
                0 -> QuestionType.NAT
                1 -> QuestionType.MCQ
                else -> QuestionType.MSQ
            }
            val extId = "PYP-$year-GA-${String.format("%02d", i)}"
            if (questionRepository.findByExternalId(extId) == null) {
                questionRepository.save(
                    Question(
                        externalId = extId,
                        subject = gaSubject,
                        topic = gaTopic,
                        year = year,
                        type = type,
                        marks = marks,
                        statement = "GATE $year General Aptitude Question $i",
                        optionsJson = if (type == QuestionType.NAT) null else """{"A":"Opt A","B":"Opt B","C":"Opt C","D":"Opt D"}""",
                        correctOptions = if (type == QuestionType.NAT) null else if (type == QuestionType.MSQ) """["A","B"]""" else "A",
                        numericMin = if (type == QuestionType.NAT) 10.0 else null,
                        numericMax = if (type == QuestionType.NAT) 10.0 else null,
                        source = "GATE_PYP",
                        curationStatus = "ACCEPTED",
                        gateOverflowUrl = "https://gateoverflow.in/pyp-$year-ga-$i",
                        explanation = "Explanation for GATE $year GA $i"
                    )
                )
            }
        }

        // Seed Core
        for (i in 1..coreCount) {
            val isAlgo = i % 2 == 1
            val subj = if (isAlgo) algoSubject else osSubject
            val topic = if (isAlgo) algoTopic else osTopic
            val prefix = if (isAlgo) "ALGO" else "OS"
            val marks = if (i <= 25) 1 else 2
            val type = when (i % 3) {
                0 -> QuestionType.NAT
                1 -> QuestionType.MCQ
                else -> QuestionType.MSQ
            }
            val extId = "PYP-$year-$prefix-${String.format("%02d", i)}"
            if (questionRepository.findByExternalId(extId) == null) {
                questionRepository.save(
                    Question(
                        externalId = extId,
                        subject = subj,
                        topic = topic,
                        year = year,
                        type = type,
                        marks = marks,
                        statement = "GATE $year Core Question $i",
                        optionsJson = if (type == QuestionType.NAT) null else """{"A":"Opt A","B":"Opt B","C":"Opt C","D":"Opt D"}""",
                        correctOptions = if (type == QuestionType.NAT) null else if (type == QuestionType.MSQ) """["A","B"]""" else "A",
                        numericMin = if (type == QuestionType.NAT) 25.0 else null,
                        numericMax = if (type == QuestionType.NAT) 25.0 else null,
                        source = "GATE_PYP",
                        curationStatus = "ACCEPTED",
                        gateOverflowUrl = "https://gateoverflow.in/pyp-$year-core-$i",
                        explanation = "Explanation for GATE $year Core $i"
                    )
                )
            }
        }
    }

    @Test
    fun `generator fills blueprint from year-tagged fixture incl type decomposition`() {
        val testYear = 2006
        seedYearQuestions(year = testYear, gaCount = 10, coreCount = 55)

        val resp = restTemplate.postForEntity(
            "/api/v1/mocks/pyp/$testYear",
            null,
            MockTemplateSummaryDto::class.java
        )

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body!!
        assertEquals("GATE $testYear Official Paper", body.title)
        assertEquals(180, body.durationMinutes)
        assertEquals(65, body.totalQuestions)
        assertEquals(100.0, body.totalMarks)
        assertTrue(body.shortfallWarnings.isEmpty(), "Full 65-question paper should have 0 shortfall warnings")
        assertTrue(body.sections.isNotEmpty())

        // Verify type decomposition is data-derived
        val totalMcq = body.sections.sumOf { it.mcq }
        val totalMsq = body.sections.sumOf { it.msq }
        val totalNat = body.sections.sumOf { it.nat }
        assertEquals(65, totalMcq + totalMsq + totalNat)
        assertTrue(totalMcq > 0)
        assertTrue(totalMsq > 0)
        assertTrue(totalNat > 0)
    }

    @Test
    fun `shortfall warning when fixture is thin`() {
        val thinYear = 2008
        seedYearQuestions(year = thinYear, gaCount = 2, coreCount = 3)

        val resp = restTemplate.postForEntity(
            "/api/v1/mocks/pyp/$thinYear",
            null,
            MockTemplateSummaryDto::class.java
        )

        assertEquals(HttpStatus.CREATED, resp.statusCode)
        val body = resp.body!!
        assertEquals("GATE $thinYear Official Paper", body.title)
        assertEquals(5, body.totalQuestions)
        assertTrue(body.shortfallWarnings.isNotEmpty(), "Thin fixture must produce shortfall warnings")
        assertTrue(body.shortfallWarnings.any { it.contains("General Aptitude") })
        assertTrue(body.shortfallWarnings.any { it.contains("CSE Core") })
    }

    @Test
    fun `generated template runs the existing attempt flow end-to-end`() {
        val attemptYear = 2007
        seedYearQuestions(year = attemptYear, gaCount = 10, coreCount = 55)

        // 1. Generate template
        val tmplResp = restTemplate.postForEntity(
            "/api/v1/mocks/pyp/$attemptYear",
            null,
            MockTemplateSummaryDto::class.java
        )
        assertEquals(HttpStatus.CREATED, tmplResp.statusCode)
        val templateId = tmplResp.body!!.id

        // 2. Start attempt
        val startResp = restTemplate.postForEntity(
            "/api/v1/mocks/$templateId/attempts",
            null,
            StartMockAttemptResponse::class.java
        )
        assertEquals(HttpStatus.OK, startResp.statusCode)
        val attemptData = startResp.body!!
        val attemptId = attemptData.attemptId
        assertTrue(attemptData.questions.isNotEmpty())

        // 3. Answer a question
        val firstQ = attemptData.questions.first()
        val answerReq = UpsertAnswerRequest(
            selectedOptions = "A",
            state = "ANSWERED",
            timeSpentSeconds = 30
        )
        restTemplate.patchForObject(
            "/api/v1/attempts/$attemptId/answers/${firstQ.id}",
            answerReq,
            UpsertAnswerResponse::class.java
        )

        // 4. Submit attempt
        val submitResp = restTemplate.postForEntity(
            "/api/v1/attempts/$attemptId/submit",
            SubmitMockAttemptRequest(timeSpentSeconds = 120),
            SubmitMockAttemptResponse::class.java
        )
        assertEquals(HttpStatus.OK, submitResp.statusCode)
        val submitData = submitResp.body!!
        assertEquals("SUBMITTED", submitData.status)
        assertTrue(submitData.totalQuestions > 0)

        // 5. Check Result
        val resultResp = restTemplate.getForEntity(
            "/api/v1/attempts/$attemptId/result",
            MockResultResponse::class.java
        )
        assertEquals(HttpStatus.OK, resultResp.statusCode)
        val result = resultResp.body!!
        assertEquals(attemptId, result.attemptId)
        assertTrue(result.questions.isNotEmpty())
        assertTrue(result.sectionScores.isNotEmpty())
    }

    @Test
    fun `stats pyp year rollup math`() {
        val yearA = 2004
        val yearB = 2005
        seedYearQuestions(year = yearA, gaCount = 10, coreCount = 55)
        seedYearQuestions(year = yearB, gaCount = 4, coreCount = 6)

        val statsResp = restTemplate.getForEntity(
            "/api/v1/stats/pyp",
            PypStatsResponse::class.java
        )

        assertEquals(HttpStatus.OK, statsResp.statusCode)
        val stats = statsResp.body!!
        assertTrue(stats.years.isNotEmpty())

        val statA = stats.years.find { it.year == yearA }
        assertNotNull(statA, "Year $yearA must be present in stats")
        assertEquals(65, statA!!.total)
        assertTrue(stats.coverage.fullPaperYears.contains(yearA), "Year $yearA should be in fullPaperYears")

        val statB = stats.years.find { it.year == yearB }
        assertNotNull(statB, "Year $yearB must be present in stats")
        assertEquals(10, statB!!.total)
        assertTrue(stats.coverage.partialYears.contains(yearB), "Year $yearB should be in partialYears")

        // Verify math: year total equals sum of bySubject
        for (yStat in stats.years) {
            val sumSubj = yStat.bySubject.values.sum()
            assertEquals(yStat.total, sumSubj, "Year ${yStat.year} total must equal sum of bySubject counts")
        }
    }

    @Test
    fun `pyp bank file imports idempotently with zero rejections and 100 percent attribution`() {
        val seedRunner = com.gatelms.seed.QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            seedPath = "seed-data/pyp-bank.jsonl"
        )
        val report1 = seedRunner.importQuestions("seed-data/pyp-bank.jsonl")
        assertEquals(555, report1.total)
        assertEquals(0, report1.rejected, "Zero lines rejected: ${report1.rejectionReasons}")

        // Pass 2: Idempotent re-run
        val report2 = seedRunner.importQuestions("seed-data/pyp-bank.jsonl")
        assertEquals(555, report2.total)
        assertEquals(0, report2.inserted, "Idempotent re-run must insert 0 questions")
        assertEquals(555, report2.updated, "Idempotent re-run must update 555 questions")
        assertEquals(0, report2.rejected)
    }
}

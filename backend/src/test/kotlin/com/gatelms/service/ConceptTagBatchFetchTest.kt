package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.anatomy.repository.findSlugsGroupedByQuestionIds
import com.gatelms.anatomy.service.CardSchedulingService
import com.gatelms.dto.PracticeSessionCreateRequest
import com.gatelms.entity.*
import com.gatelms.repository.*
import com.gatelms.security.CurrentUserProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.*

class ConceptTagBatchFetchTest {

    private val questionConceptTagRepository: QuestionConceptTagRepository = mock(QuestionConceptTagRepository::class.java)
    private val currentUserProvider: CurrentUserProvider = mock(CurrentUserProvider::class.java)

    private val testUserId = UUID.randomUUID()

    private fun anyUuid(): UUID = ArgumentMatchers.any(UUID::class.java) ?: UUID.randomUUID()
    private fun anyUuidCollection(): Collection<UUID> = ArgumentMatchers.anyList<UUID>() ?: emptyList()
    private fun anyPageable(): Pageable = ArgumentMatchers.any(Pageable::class.java) ?: Pageable.unpaged()
    private fun anyInstant(): Instant = ArgumentMatchers.any(Instant::class.java) ?: Instant.now()
    private fun anyBoolean(): Boolean = ArgumentMatchers.anyBoolean()
    private fun anyString(): String = ArgumentMatchers.anyString() ?: ""
    private fun <T> anyNullable(clazz: Class<T>): T? = ArgumentMatchers.nullable(clazz)

    @BeforeEach
    fun setup() {
        `when`(currentUserProvider.getCurrentUserId()).thenReturn(testUserId)
    }

    @Test
    fun `G4 - repository extension findSlugsGroupedByQuestionIds batches into single query and groups correctly`() {
        val q1 = UUID.randomUUID()
        val q2 = UUID.randomUUID()
        val q3 = UUID.randomUUID()

        // 1. Empty list returns emptyMap without querying
        val emptyResult = questionConceptTagRepository.findSlugsGroupedByQuestionIds(emptyList())
        assertEquals(emptyMap<UUID, List<String>>(), emptyResult)
        verify(questionConceptTagRepository, never()).findSlugPairsByQuestionIds(anyUuidCollection())

        // 2. Non-empty list queries once and groups
        val rows: List<Array<Any>> = listOf(
            arrayOf(q1, "tag-a"),
            arrayOf(q1, "tag-b"),
            arrayOf(q2, "tag-c")
        )
        `when`(questionConceptTagRepository.findSlugPairsByQuestionIds(listOf(q1, q2, q3))).thenReturn(rows)

        val grouped = questionConceptTagRepository.findSlugsGroupedByQuestionIds(listOf(q1, q2, q3))

        verify(questionConceptTagRepository, times(1)).findSlugPairsByQuestionIds(listOf(q1, q2, q3))
        assertEquals(listOf("tag-a", "tag-b"), grouped[q1])
        assertEquals(listOf("tag-c"), grouped[q2])
        assertEquals(null, grouped[q3])
    }

    @Test
    fun `G4 - MistakeService getMistakes fires exactly ONE batch query for page and zero per-row queries`() {
        val mistakeLogRepository: MistakeLogRepository = mock(MistakeLogRepository::class.java)
        val questionRepository: QuestionRepository = mock(QuestionRepository::class.java)
        val attemptAnswerRepository: AttemptAnswerRepository = mock(AttemptAnswerRepository::class.java)
        val topicRepository: TopicRepository = mock(TopicRepository::class.java)
        val topicMasteryRepository: TopicMasteryRepository = mock(TopicMasteryRepository::class.java)

        val mistakeService = MistakeService(
            mistakeLogRepository = mistakeLogRepository,
            questionRepository = questionRepository,
            attemptAnswerRepository = attemptAnswerRepository,
            topicRepository = topicRepository,
            topicMasteryRepository = topicMasteryRepository,
            currentUserProvider = currentUserProvider
        )
        mistakeService.questionConceptTagRepository = questionConceptTagRepository

        val q1 = Question(id = UUID.randomUUID(), statement = "Q1", type = QuestionType.MCQ, marks = 1)
        val q2 = Question(id = UUID.randomUUID(), statement = "Q2", type = QuestionType.MCQ, marks = 2)
        val q3 = Question(id = UUID.randomUUID(), statement = "Q3", type = QuestionType.MCQ, marks = 1)

        val mistakes = listOf(
            MistakeLog(id = UUID.randomUUID(), userId = testUserId, question = q1, errorCategory = MistakeErrorCategory.CONCEPTUAL, correctiveTakeaway = "Note 1"),
            MistakeLog(id = UUID.randomUUID(), userId = testUserId, question = q2, errorCategory = MistakeErrorCategory.CALCULATION, correctiveTakeaway = "Note 2"),
            MistakeLog(id = UUID.randomUUID(), userId = testUserId, question = q3, errorCategory = MistakeErrorCategory.MISREAD, correctiveTakeaway = "Note 3")
        )
        val page = PageImpl(mistakes)

        `when`(mistakeLogRepository.findFiltered(anyUuid(), anyNullable(Boolean::class.javaObjectType), anyBoolean(), anyInstant(), anyPageable())).thenReturn(page)
        val mockPairs: List<Array<Any>> = listOf(
            arrayOf(q1.id!!, "concept-alpha"),
            arrayOf(q2.id!!, "concept-beta")
        )
        `when`(questionConceptTagRepository.findSlugPairsByQuestionIds(anyUuidCollection())).thenReturn(mockPairs)

        val resultPage = mistakeService.getMistakes(null, false, Pageable.unpaged())

        assertEquals(3, resultPage.content.size)
        assertEquals(listOf("concept-alpha"), resultPage.content[0].conceptTags)
        assertEquals(listOf("concept-beta"), resultPage.content[1].conceptTags)
        assertEquals(emptyList<String>(), resultPage.content[2].conceptTags)

        // Strict Gate G4 assertion: 1 batch query, 0 per-row queries
        verify(questionConceptTagRepository, times(1)).findSlugPairsByQuestionIds(anyUuidCollection())
        verify(questionConceptTagRepository, never()).findSlugsByQuestionId(anyUuid())
    }

    @Test
    fun `G4 - MockTestService getResult fires exactly ONE batch query for all frozen questions and zero per-row queries`() {
        val mockExamRepository: MockExamRepository = mock(MockExamRepository::class.java)
        val mockAttemptRepository: MockAttemptRepository = mock(MockAttemptRepository::class.java)
        val attemptAnswerRepository: AttemptAnswerRepository = mock(AttemptAnswerRepository::class.java)
        val questionRepository: QuestionRepository = mock(QuestionRepository::class.java)
        val subjectRepository: SubjectRepository = mock(SubjectRepository::class.java)
        val mockAssemblyService: MockAssemblyService = mock(MockAssemblyService::class.java)
        val gradingService: GradingService = mock(GradingService::class.java)
        val cardSchedulingService: CardSchedulingService = mock(CardSchedulingService::class.java)
        val objectMapper = ObjectMapper()

        val mockTestService = MockTestService(
            mockExamRepository = mockExamRepository,
            mockAttemptRepository = mockAttemptRepository,
            attemptAnswerRepository = attemptAnswerRepository,
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            mockAssemblyService = mockAssemblyService,
            gradingService = gradingService,
            objectMapper = objectMapper,
            currentUserProvider = currentUserProvider,
            cardSchedulingService = cardSchedulingService
        )
        mockTestService.questionConceptTagRepository = questionConceptTagRepository

        val q1Id = UUID.randomUUID()
        val q2Id = UUID.randomUUID()
        val q3Id = UUID.randomUUID()

        val examId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()

        val exam = MockExam(
            id = examId,
            title = "Test Exam",
            durationMinutes = 30,
            totalMarks = 3.0
        )

        val attempt = MockAttempt(
            id = attemptId,
            mockExam = exam,
            userId = testUserId,
            status = MockAttemptStatus.SUBMITTED,
            questionIdsJson = objectMapper.writeValueAsString(listOf(q1Id.toString(), q2Id.toString(), q3Id.toString())),
            startedAt = Instant.now(),
            submittedAt = Instant.now()
        )

        `when`(mockAttemptRepository.findByIdAndUserId(attemptId, testUserId)).thenReturn(Optional.of(attempt))
        val q1 = Question(id = q1Id, statement = "Q1", type = QuestionType.MCQ, marks = 1)
        val q2 = Question(id = q2Id, statement = "Q2", type = QuestionType.MCQ, marks = 1)
        val q3 = Question(id = q3Id, statement = "Q3", type = QuestionType.MCQ, marks = 1)

        `when`(questionRepository.findById(q1Id)).thenReturn(Optional.of(q1))
        `when`(questionRepository.findById(q2Id)).thenReturn(Optional.of(q2))
        `when`(questionRepository.findById(q3Id)).thenReturn(Optional.of(q3))

        val mockPairs: List<Array<Any>> = listOf(
            arrayOf(q1Id, "tag-1"),
            arrayOf(q3Id, "tag-3")
        )
        `when`(questionConceptTagRepository.findSlugPairsByQuestionIds(anyUuidCollection())).thenReturn(mockPairs)

        val result = mockTestService.getResult(attemptId)

        assertEquals(3, result.questions.size)
        assertEquals(listOf("tag-1"), result.questions.find { it.questionId == q1Id }?.conceptTags)
        assertEquals(emptyList<String>(), result.questions.find { it.questionId == q2Id }?.conceptTags)
        assertEquals(listOf("tag-3"), result.questions.find { it.questionId == q3Id }?.conceptTags)

        // Strict Gate G4 assertion: 1 batch query, 0 per-row queries
        verify(questionConceptTagRepository, times(1)).findSlugPairsByQuestionIds(anyUuidCollection())
        verify(questionConceptTagRepository, never()).findSlugsByQuestionId(anyUuid())
    }

    @Test
    fun `G4 - PracticeService createSession and createDrillSession fire exactly ONE batch query each`() {
        val questionRepository: QuestionRepository = mock(QuestionRepository::class.java)
        val mockExamRepository: MockExamRepository = mock(MockExamRepository::class.java)
        val mockAttemptRepository: MockAttemptRepository = mock(MockAttemptRepository::class.java)
        val attemptAnswerRepository: AttemptAnswerRepository = mock(AttemptAnswerRepository::class.java)
        val topicRepository: TopicRepository = mock(TopicRepository::class.java)
        val questionProgressRepository: QuestionProgressRepository = mock(QuestionProgressRepository::class.java)
        val topicMasteryRepository: TopicMasteryRepository = mock(TopicMasteryRepository::class.java)
        val topicMasteryService: TopicMasteryService = mock(TopicMasteryService::class.java)
        val gradingService: GradingService = mock(GradingService::class.java)
        val objectMapper = ObjectMapper()
        val cardSchedulingService: CardSchedulingService = mock(CardSchedulingService::class.java)

        val practiceService = PracticeService(
            questionRepository = questionRepository,
            mockExamRepository = mockExamRepository,
            mockAttemptRepository = mockAttemptRepository,
            attemptAnswerRepository = attemptAnswerRepository,
            topicRepository = topicRepository,
            questionProgressRepository = questionProgressRepository,
            topicMasteryRepository = topicMasteryRepository,
            topicMasteryService = topicMasteryService,
            currentUserProvider = currentUserProvider,
            gradingService = gradingService,
            objectMapper = objectMapper,
            cardSchedulingService = cardSchedulingService
        )
        practiceService.questionConceptTagRepository = questionConceptTagRepository

        val q1 = Question(id = UUID.randomUUID(), statement = "Q1", type = QuestionType.MCQ, marks = 1)
        val q2 = Question(id = UUID.randomUUID(), statement = "Q2", type = QuestionType.MCQ, marks = 1)
        val questions = listOf(q1, q2)

        `when`(questionRepository.findWithFilters(
            anyUuid(),
            anyNullable(String::class.java),
            anyNullable(String::class.java),
            anyNullable(String::class.java),
            anyNullable(Int::class.javaObjectType),
            anyNullable(String::class.java),
            anyNullable(String::class.java),
            anyNullable(Boolean::class.javaObjectType),
            anyNullable(String::class.java),
            anyNullable(String::class.java),
            anyNullable(String::class.java),
            anyNullable(String::class.java),
            anyString(),
            anyPageable()
        )).thenReturn(PageImpl(questions))

        `when`(mockExamRepository.save(ArgumentMatchers.any(MockExam::class.java) ?: MockExam(durationMinutes = 10, totalMarks = 1.0))).thenAnswer { it.arguments[0] }
        `when`(mockAttemptRepository.save(ArgumentMatchers.any(MockAttempt::class.java) ?: MockAttempt())).thenAnswer { it.arguments[0] }

        val mockPairs: List<Array<Any>> = listOf(
            arrayOf(q1.id!!, "concept-x"),
            arrayOf(q2.id!!, "concept-y")
        )
        `when`(questionConceptTagRepository.findSlugPairsByQuestionIds(anyUuidCollection())).thenReturn(mockPairs)

        // 1. Test createSession
        val sessionResp = practiceService.createSession(PracticeSessionCreateRequest(topicId = "topic_sync", count = 2))
        assertEquals(2, sessionResp.questions.size)
        assertEquals(listOf("concept-x"), sessionResp.questions[0].conceptTags)
        assertEquals(listOf("concept-y"), sessionResp.questions[1].conceptTags)

        verify(questionConceptTagRepository, times(1)).findSlugPairsByQuestionIds(anyUuidCollection())
        verify(questionConceptTagRepository, never()).findSlugsByQuestionId(anyUuid())

        // 2. Test createDrillSession
        val drillResp = practiceService.createDrillSession(questions, "Drill Title", "WEAKNESS")
        assertEquals(2, drillResp.questions.size)
        assertEquals(listOf("concept-x"), drillResp.questions[0].conceptTags)
        assertEquals(listOf("concept-y"), drillResp.questions[1].conceptTags)

        // Across both flows, findSlugPairsByQuestionIds called 2 times (1 per flow), findSlugsByQuestionId called 0 times
        verify(questionConceptTagRepository, times(2)).findSlugPairsByQuestionIds(anyUuidCollection())
        verify(questionConceptTagRepository, never()).findSlugsByQuestionId(anyUuid())
    }
}

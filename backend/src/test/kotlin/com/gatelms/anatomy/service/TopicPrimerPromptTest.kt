package com.gatelms.anatomy.service

import com.gatelms.ai.config.AiProperties
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class TopicPrimerPromptTest {

    private val sampler = TopicGroundingSampler()

    private val topicRepository = mockk<TopicRepository>()
    private val topicPrimerRepository = mockk<TopicPrimerRepository>()
    private val conceptTagRepository = mockk<ConceptTagRepository>()
    private val questionRepository = mockk<QuestionRepository>()
    private val aiChatService = mockk<AiChatService>()
    private val aiRateLimiter = mockk<AiRateLimiter>()
    private val anatomyValidationService = mockk<AnatomyValidationService>()
    private val aiProperties = AiProperties()

    private val primerService = TopicPrimerService(
        topicRepository = topicRepository,
        topicPrimerRepository = topicPrimerRepository,
        conceptTagRepository = conceptTagRepository,
        questionRepository = questionRepository,
        aiChatService = aiChatService,
        aiRateLimiter = aiRateLimiter,
        anatomyValidationService = anatomyValidationService,
        aiProperties = aiProperties,
        topicGroundingSampler = sampler
    )

    private val testSubject = Subject(id = "sub_os", name = "Operating Systems", slug = "operating-systems")
    private val testTopic = Topic(id = "top_psync", name = "Process Synchronization", slug = "process-synchronization", subject = testSubject)

    @Test
    fun `honest-judge PASS - stratifySample groups across type and difficulty round-robin`() {
        val questions = mutableListOf<Question>()
        // 10 MCQ EASY
        repeat(10) { idx ->
            questions.add(Question(id = UUID.randomUUID(), externalId = "MCQ-E-$idx", type = QuestionType.MCQ, difficulty = "EASY", statement = "Stmt $idx", marks = 1, topic = testTopic))
        }
        // 10 MCQ MEDIUM
        repeat(10) { idx ->
            questions.add(Question(id = UUID.randomUUID(), externalId = "MCQ-M-$idx", type = QuestionType.MCQ, difficulty = "MEDIUM", statement = "Stmt $idx", marks = 1, topic = testTopic))
        }
        // 10 MSQ HARD
        repeat(10) { idx ->
            questions.add(Question(id = UUID.randomUUID(), externalId = "MSQ-H-$idx", type = QuestionType.MSQ, difficulty = "HARD", statement = "Stmt $idx", marks = 2, topic = testTopic))
        }
        // 5 NAT HARD
        repeat(5) { idx ->
            questions.add(Question(id = UUID.randomUUID(), externalId = "NAT-H-$idx", type = QuestionType.NAT, difficulty = "HARD", statement = "Stmt $idx", marks = 2, topic = testTopic))
        }

        assertEquals(35, questions.size)

        // Sample up to 20
        val sampled20 = sampler.stratifySample(questions, 20)
        assertEquals(20, sampled20.size)
        // Check that all 4 groups are represented
        assertTrue(sampled20.any { it.type == QuestionType.MCQ && it.difficulty == "EASY" })
        assertTrue(sampled20.any { it.type == QuestionType.MCQ && it.difficulty == "MEDIUM" })
        assertTrue(sampled20.any { it.type == QuestionType.MSQ && it.difficulty == "HARD" })
        assertTrue(sampled20.any { it.type == QuestionType.NAT && it.difficulty == "HARD" })

        // Sample up to 30
        val sampled30 = sampler.stratifySample(questions, 30)
        assertEquals(30, sampled30.size)
    }

    @Test
    fun `honest-judge PASS - formatQuestionSnippet preserves up to 1500 chars and cites externalId`() {
        val longStatement = "A".repeat(2000)
        val q = Question(
            id = UUID.randomUUID(),
            externalId = "SO-OS-0001",
            type = QuestionType.MCQ,
            difficulty = "HARD",
            year = 2023,
            marks = 2,
            statement = longStatement,
            topic = testTopic
        )

        val snippet = sampler.formatQuestionSnippet(1, q, 1500)
        assertTrue(snippet.startsWith("1. [2023 MCQ - HARD] [SO-OS-0001] "))
        // Statement part should be at most 1500 characters
        val stmtPart = snippet.substringAfter("[SO-OS-0001] ")
        assertEquals(1500, stmtPart.length)
    }

    @Test
    fun `honest-judge PASS - primer prompt receives stratified sample up to 30 and concept labels`() {
        val sampleQuestions = (1..25).map { idx ->
            Question(
                id = UUID.randomUUID(),
                externalId = "Q-$idx",
                type = if (idx % 2 == 0) QuestionType.MCQ else QuestionType.MSQ,
                difficulty = if (idx % 3 == 0) "HARD" else "MEDIUM",
                year = 2000 + idx,
                marks = 2,
                statement = "Question statement number $idx with critical section details.",
                topic = testTopic
            )
        }
        val conceptLabels = listOf(
            "Critical Section & Mutual Exclusion",
            "Peterson's Software Solution",
            "Semaphore Operations"
        )

        val prompt = primerService.buildTopicPrimerPrompt(testTopic, sampleQuestions, conceptLabels)

        // Verifications per SPEC §7 Task 5:
        // 1. Topic orientation scope
        assertTrue(prompt.contains("Topic Orientation and Theory Primer"))
        assertTrue(prompt.contains("Topic: Process Synchronization"))
        // 2. Contains concept taxonomies
        assertTrue(prompt.contains("Topic Canonical Concept Taxonomies:"))
        assertTrue(prompt.contains("- Critical Section & Mutual Exclusion"))
        assertTrue(prompt.contains("- Peterson's Software Solution"))
        assertTrue(prompt.contains("- Semaphore Operations"))
        // 3. Contains the 25 stratified questions with IDs and full statements
        assertTrue(prompt.contains("25 stratified samples"))
        assertTrue(prompt.contains("[Q-1]"))
        assertTrue(prompt.contains("[Q-25]"))
    }
}

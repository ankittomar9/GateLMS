package com.gatelms.anatomy.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.entity.ConceptTag
import com.gatelms.anatomy.entity.ConceptTagSource
import com.gatelms.anatomy.entity.QuestionConceptTag
import com.gatelms.anatomy.entity.QuestionConceptTagId
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.QuestionAnatomyRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.entity.Question
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.*

class EmissionTest {

    private val questionRepository = mock(QuestionRepository::class.java)
    private val questionProgressRepository = mock(QuestionProgressRepository::class.java)
    private val questionAnatomyRepository = mock(QuestionAnatomyRepository::class.java)
    private val conceptTagRepository = mock(ConceptTagRepository::class.java)
    private val questionConceptTagRepository = mock(QuestionConceptTagRepository::class.java)
    private val anatomyValidationService = mock(AnatomyValidationService::class.java)
    private val aiChatService = mock(AiChatService::class.java)
    private val aiRateLimiter = mock(AiRateLimiter::class.java)
    private val objectMapper = jacksonObjectMapper()

    private val service = AnatomyGenerationService(
        questionRepository,
        questionProgressRepository,
        questionAnatomyRepository,
        conceptTagRepository,
        questionConceptTagRepository,
        anatomyValidationService,
        aiChatService,
        aiRateLimiter,
        objectMapper
    )

    @Test
    fun `emission should be idempotent on repeated tags`() {
        val qId = UUID.randomUUID()
        val topic = Topic(id = "topic-dbms-sql", name = "SQL")
        val question = Question(id = qId, topic = topic)

        // Mock tag existence: "relational-algebra" does not exist first, "sql-queries" already exists
        `when`(conceptTagRepository.existsById("relational-algebra")).thenReturn(false)
        `when`(conceptTagRepository.existsById("sql-queries")).thenReturn(true)

        val qctId1 = QuestionConceptTagId(qId, "relational-algebra")
        val qctId2 = QuestionConceptTagId(qId, "sql-queries")
        `when`(questionConceptTagRepository.existsById(qctId1)).thenReturn(false)
        `when`(questionConceptTagRepository.existsById(qctId2)).thenReturn(true)

        val emitMethod = AnatomyGenerationService::class.java.getDeclaredMethod(
            "emitConceptTags",
            Question::class.java,
            List::class.java
        )
        emitMethod.isAccessible = true
        emitMethod.invoke(service, question, listOf("relational-algebra", "sql-queries"))

        // Verify "relational-algebra" was saved into concept_tags and linked in question_concept_tags
        verify(conceptTagRepository, times(1)).save(any(ConceptTag::class.java))
        verify(questionConceptTagRepository, times(1)).save(any(QuestionConceptTag::class.java))

        // Second invocation with same tags should be a complete no-op
        `when`(conceptTagRepository.existsById("relational-algebra")).thenReturn(true)
        `when`(questionConceptTagRepository.existsById(qctId1)).thenReturn(true)

        emitMethod.invoke(service, question, listOf("relational-algebra", "sql-queries"))

        // Counts should still be 1 (no duplicate saves)
        verify(conceptTagRepository, times(1)).save(any(ConceptTag::class.java))
        verify(questionConceptTagRepository, times(1)).save(any(QuestionConceptTag::class.java))
    }
}

package com.gatelms.service

import com.gatelms.dto.TopicMasteryUpdateDto
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.error.ConflictException
import com.gatelms.error.ValidationException
import com.gatelms.repository.TopicRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TopicServiceTest {

    private val topicRepository = mockk<TopicRepository>()
    private lateinit var topicService: TopicService

    private val testSubject = Subject(id = "subj_os", name = "Operating Systems", slug = "operating-systems")
    private val testTopic = Topic(
        id = "topic_paging",
        subject = testSubject,
        name = "Paging",
        slug = "paging",
        masteryLevel = 40,
        version = 1
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        topicService = TopicService(topicRepository)
    }

    @Test
    fun `honest-judge PASS - updateMastery with matching version succeeds and updates mastery`() {
        val now = Instant.now()
        val updateDto = TopicMasteryUpdateDto(
            version = 1,
            masteryLevel = 85,
            lastRevisedAt = now,
            nextRevisionDue = now.plusSeconds(86400 * 3)
        )

        every { topicRepository.findById("topic_paging") } returns Optional.of(testTopic)
        every { topicRepository.save(any()) } answers { firstArg() }

        val result = topicService.updateMastery("topic_paging", updateDto)

        assertNotNull(result)
        assertEquals(85, result.masteryLevel)
        assertEquals(1, result.version)
        verify(exactly = 1) { topicRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - deliberate-stale version causes ConflictException 409`() {
        val updateDto = TopicMasteryUpdateDto(
            version = 0, // Stale version (db has version = 1)
            masteryLevel = 90
        )

        every { topicRepository.findById("topic_paging") } returns Optional.of(testTopic)

        val ex = assertThrows<ConflictException> {
            topicService.updateMastery("topic_paging", updateDto)
        }

        assertEquals(true, ex.message?.contains("version mismatch"))
        verify(exactly = 0) { topicRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - deliberate-invalid masteryLevel greater than 100 is rejected`() {
        val updateDto = TopicMasteryUpdateDto(
            version = 1,
            masteryLevel = 105 // Deliberate invalid > 100
        )

        every { topicRepository.findById("topic_paging") } returns Optional.of(testTopic)

        val ex = assertThrows<ValidationException> {
            topicService.updateMastery("topic_paging", updateDto)
        }

        assertEquals(true, ex.fieldErrors.containsKey("masteryLevel"))
        verify(exactly = 0) { topicRepository.save(any()) }
    }

    @Test
    fun `honest-judge FAIL - getTopics without subjectId or subjectSlug is rejected`() {
        assertThrows<ValidationException> {
            topicService.getTopics(null, "")
        }
    }
}

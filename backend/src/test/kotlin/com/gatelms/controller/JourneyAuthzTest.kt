package com.gatelms.controller

import com.gatelms.anatomy.service.ConceptCoverageService
import com.gatelms.anatomy.service.ConceptLessonService
import com.gatelms.anatomy.service.TopicPrimerService
import com.gatelms.entity.User
import com.gatelms.entity.UserRole
import com.gatelms.security.CurrentUserProvider
import com.gatelms.service.JourneyService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.access.AccessDeniedException
import java.util.UUID

class JourneyAuthzTest {

    private val journeyService: JourneyService = mock(JourneyService::class.java)
    private val topicPrimerService: TopicPrimerService = mock(TopicPrimerService::class.java)
    private val currentUserProvider: CurrentUserProvider = mock(CurrentUserProvider::class.java)
    private val conceptCoverageService: ConceptCoverageService = mock(ConceptCoverageService::class.java)
    private val conceptLessonService: ConceptLessonService = mock(ConceptLessonService::class.java)

    private lateinit var controller: JourneyController

    private val adminUser = User(
        id = UUID.randomUUID(),
        username = "admin",
        passwordHash = "hash",
        role = UserRole.ADMIN
    )

    private val studentUser = User(
        id = UUID.randomUUID(),
        username = "student",
        passwordHash = "hash",
        role = UserRole.STUDENT
    )

    @BeforeEach
    fun setup() {
        controller = JourneyController(
            journeyService = journeyService,
            topicPrimerService = topicPrimerService,
            currentUserProvider = currentUserProvider,
            conceptCoverageService = conceptCoverageService,
            conceptLessonService = conceptLessonService
        )
    }

    @Test
    fun `G1 - student calling primer generate with force=true throws AccessDeniedException`() {
        `when`(currentUserProvider.getCurrentUser()).thenReturn(studentUser)

        val ex = assertThrows<AccessDeniedException> {
            controller.generatePrimer("topic_os", null, null, force = true, null)
        }
        assertEquals("force=true requires ADMIN", ex.message)
    }

    @Test
    fun `G1 - student calling lesson generate with force=true throws AccessDeniedException`() {
        `when`(currentUserProvider.getCurrentUser()).thenReturn(studentUser)

        val ex = assertThrows<AccessDeniedException> {
            controller.generateConceptLesson("critical-section", null, null, force = true, null)
        }
        assertEquals("force=true requires ADMIN", ex.message)
    }

    @Test
    fun `G2 - student calling primer generate with force=false does not throw AccessDeniedException`() {
        `when`(currentUserProvider.getCurrentUser()).thenReturn(studentUser)
        `when`(currentUserProvider.getCurrentUserId()).thenReturn(studentUser.id!!)

        assertDoesNotThrow {
            controller.generatePrimer("topic_os", null, null, force = false, null)
        }
    }

    @Test
    fun `G2 - student calling lesson generate with force=false does not throw AccessDeniedException`() {
        `when`(currentUserProvider.getCurrentUser()).thenReturn(studentUser)
        `when`(currentUserProvider.getCurrentUserId()).thenReturn(studentUser.id!!)

        assertDoesNotThrow {
            controller.generateConceptLesson("critical-section", null, null, force = false, null)
        }
    }

    @Test
    fun `G3 - admin calling primer generate with force=true succeeds`() {
        `when`(currentUserProvider.getCurrentUser()).thenReturn(adminUser)
        `when`(currentUserProvider.getCurrentUserId()).thenReturn(adminUser.id!!)

        assertDoesNotThrow {
            controller.generatePrimer("topic_os", null, null, force = true, null)
        }
    }

    @Test
    fun `G3 - admin calling lesson generate with force=true succeeds`() {
        `when`(currentUserProvider.getCurrentUser()).thenReturn(adminUser)
        `when`(currentUserProvider.getCurrentUserId()).thenReturn(adminUser.id!!)

        assertDoesNotThrow {
            controller.generateConceptLesson("critical-section", null, null, force = true, null)
        }
    }
}

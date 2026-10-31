package com.gatelms.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gatelms.anatomy.entity.QuestionAnatomy
import com.gatelms.anatomy.entity.TopicPrimer
import com.gatelms.anatomy.repository.QuestionAnatomyRepository
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.entity.*
import com.gatelms.repository.NoteRepository
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.security.CurrentUserProvider
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

class TopicWorkbookPdfServiceTest {

    private val topicRepository = mockk<TopicRepository>()
    private val questionRepository = mockk<QuestionRepository>()
    private val questionProgressRepository = mockk<QuestionProgressRepository>()
    private val noteRepository = mockk<NoteRepository>()
    private val topicPrimerRepository = mockk<TopicPrimerRepository>()
    private val questionAnatomyRepository = mockk<QuestionAnatomyRepository>()
    private val currentUserProvider = mockk<CurrentUserProvider>()
    private val objectMapper = jacksonObjectMapper()

    private val service = TopicWorkbookPdfService(
        topicRepository = topicRepository,
        questionRepository = questionRepository,
        questionProgressRepository = questionProgressRepository,
        noteRepository = noteRepository,
        topicPrimerRepository = topicPrimerRepository,
        questionAnatomyRepository = questionAnatomyRepository,
        currentUserProvider = currentUserProvider,
        objectMapper = objectMapper
    )

    private val userId = UUID.randomUUID()
    private val subject = Subject(id = "sub_cs", name = "Computer Science", slug = "cs")
    private val topic = Topic(id = "top_c_pointers", name = "C Pointers & Memory", slug = "c-pointers", subject = subject)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { currentUserProvider.getCurrentUserId() } returns userId
    }

    @Test
    fun `generateWorkbookPdf happyPath integrity G13`() {
        every { topicRepository.findById("top_c_pointers") } returns Optional.of(topic)
        every { topicRepository.findBySlug(any()) } returns null

        val note1 = Note(
            id = UUID.randomUUID(),
            userId = userId,
            subject = subject,
            topic = topic,
            title = "Pointer Quirks",
            contentMd = "Scale factors apply on pointer increment.",
            isPinned = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val note2 = Note(
            id = UUID.randomUUID(),
            userId = userId,
            subject = subject,
            topic = null, // Subject-wide note
            title = "General Memory Layout",
            contentMd = "Stack grows down, heap grows up.",
            isPinned = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Partition 1: topic notes
        every {
            noteRepository.findNotes(
                userId = userId,
                subjectId = null,
                topicId = topic.id,
                kind = null,
                searchPattern = null,
                pinnedFirst = true,
                pageable = PageRequest.of(0, 100)
            )
        } returns PageImpl(listOf(note1), PageRequest.of(0, 100), 1)

        // Partition 2: subject-wide notes
        every {
            noteRepository.findNotes(
                userId = userId,
                subjectId = subject.id,
                topicId = null,
                kind = null,
                searchPattern = null,
                pinnedFirst = true,
                pageable = PageRequest.of(0, 100)
            )
        } returns PageImpl(listOf(note2), PageRequest.of(0, 100), 1)

        val q1Id = UUID.randomUUID()
        val q1 = Question(
            id = q1Id,
            topic = topic,
            subject = subject,
            statement = "What is the stride of an int* pointer on 64-bit architecture?",
            type = QuestionType.MCQ,
            marks = 2,
            year = 2024,
            optionsJson = """[{"label":"A","text":"4 bytes"},{"label":"B","text":"8 bytes"}]""",
            correctOptions = "A",
            explanation = "int is 4 bytes on typical ILP32 and LP64 systems."
        )

        val q2Id = UUID.randomUUID()
        val q2 = Question(
            id = q2Id,
            topic = topic,
            subject = subject,
            statement = "Second question for multi-page test verification.",
            type = QuestionType.NAT,
            marks = 1,
            year = 2023,
            numericMin = 10.0,
            numericMax = 10.0,
            explanation = "Direct constant calculation."
        )

        every {
            questionRepository.findWithFilters(
                userId = userId,
                subjectId = null,
                topicId = topic.id,
                type = null,
                year = null,
                pass1Status = null,
                pass2Status = null,
                bookmarked = null,
                curationStatus = null,
                source = null,
                difficulty = null,
                q = null,
                sortColumn = "created_at",
                pageable = PageRequest.of(0, 100)
            )
        } returns PageImpl(listOf(q1, q2), PageRequest.of(0, 100), 2)

        val progress1 = QuestionProgress(
            id = QuestionProgressId(userId, q1Id),
            pass1Status = Pass1Status.SOLVED
        )
        every {
            questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, listOf(q1Id, q2Id))
        } returns listOf(progress1)

        val primer = TopicPrimer(
            topic = topic,
            motivation = "Understand raw memory indirection in systems programming.",
            coreIdea = "Pointer variable stores an address.",
            invariant = "Address scaling matches sizeof target.",
            workedExample = "int arr[5]; int *p = arr;",
            traps = "Confusing array name with address-of array."
        )
        every { topicPrimerRepository.findByTopicId(topic.id) } returns Optional.of(primer)

        val anatomy1 = QuestionAnatomy(
            question = q1,
            testedConcept = "Pointer stride arithmetic",
            invariantRule = "p + 1 == (uintptr_t)p + sizeof(*p)",
            similarPatternHint = "Watch for void* or char* casts"
        )
        every { questionAnatomyRepository.findByQuestionId(q1Id) } returns Optional.of(anatomy1)
        every { questionAnatomyRepository.findByQuestionId(q2Id) } returns Optional.empty()

        // Execute
        val (filename, pdfBytes) = service.generateWorkbookPdf("top_c_pointers")

        // Assert filename
        assertEquals("topic_c-pointers_workbook.pdf", filename)
        assertTrue(pdfBytes.isNotEmpty())

        // Magic bytes: %PDF-
        val magic = String(pdfBytes.sliceArray(0..3))
        assertEquals("%PDF", magic)

        // Read through OpenPDF PdfReader (G13)
        val reader = PdfReader(pdfBytes)
        assertTrue(reader.numberOfPages >= 2, "Expected multi-page workbook, got ${reader.numberOfPages}")

        // Assert text extracted across pages
        val extractor = PdfTextExtractor(reader)
        val fullText = (1..reader.numberOfPages).joinToString("\n") { extractor.getTextFromPage(it) }

        assertTrue(fullText.contains("C Pointers & Memory"), "Missing topic name in PDF text")
        assertTrue(fullText.contains("Q1."), "Missing Q1 marker in PDF text")
        assertTrue(fullText.contains("Pointer Quirks"), "Missing seeded topic note in PDF text")
        assertTrue(fullText.contains("General Memory Layout"), "Missing seeded subject-wide note in PDF text")
        assertTrue(fullText.contains("[Topic Note]"), "Missing [Topic Note] badge in PDF text")
        assertTrue(fullText.contains("[Subject-Wide Note]"), "Missing [Subject-Wide Note] badge in PDF text")
        assertTrue(fullText.contains("[SOLVED]"), "Missing [SOLVED] status badge in PDF text")
        assertTrue(fullText.contains("Pointer stride arithmetic"), "Missing anatomy dossier concept")
    }

    @Test
    fun `generateWorkbookPdf noPrimer skipsSection1Cleanly`() {
        every { topicRepository.findById("c-pointers") } returns Optional.empty()
        every { topicRepository.findBySlug("c-pointers") } returns topic

        every { noteRepository.findNotes(any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
        every { questionRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
        every { questionProgressRepository.findAllByUserIdAndQuestionIdIn(any(), any()) } returns emptyList()
        every { topicPrimerRepository.findByTopicId(topic.id) } returns Optional.empty()

        val (filename, pdfBytes) = service.generateWorkbookPdf("c-pointers")
        assertEquals("topic_c-pointers_workbook.pdf", filename)
        assertTrue(pdfBytes.isNotEmpty())

        val reader = PdfReader(pdfBytes)
        val text = PdfTextExtractor(reader).getTextFromPage(1)
        assertTrue(text.contains("Theory primer has not been generated"))
    }

    @Test
    fun `generateWorkbookPdf noNotes rendersEmptySection2`() {
        every { topicRepository.findById("top_c_pointers") } returns Optional.of(topic)

        every { noteRepository.findNotes(any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
        every { questionRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
        every { questionProgressRepository.findAllByUserIdAndQuestionIdIn(any(), any()) } returns emptyList()
        every { topicPrimerRepository.findByTopicId(topic.id) } returns Optional.empty()

        val (_, pdfBytes) = service.generateWorkbookPdf("top_c_pointers")
        val reader = PdfReader(pdfBytes)
        val text = PdfTextExtractor(reader).getTextFromPage(1)
        assertTrue(text.contains("No personal notes recorded"))
    }

    @Test
    fun `generateWorkbookPdf unknownTopic throws404`() {
        every { topicRepository.findById("non_existent") } returns Optional.empty()
        every { topicRepository.findBySlug("non_existent") } returns null

        val ex = assertThrows<ResponseStatusException> {
            service.generateWorkbookPdf("non_existent")
        }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `generateWorkbookPdf partitionSpy queriesBothTopicAndSubjectWide`() {
        every { topicRepository.findById("top_c_pointers") } returns Optional.of(topic)
        every { noteRepository.findNotes(any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
        every { questionRepository.findWithFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())
        every { questionProgressRepository.findAllByUserIdAndQuestionIdIn(any(), any()) } returns emptyList()
        every { topicPrimerRepository.findByTopicId(topic.id) } returns Optional.empty()

        service.generateWorkbookPdf("top_c_pointers")

        // Verify partition 1: topicId = topic.id, subjectId = null
        verify(exactly = 1) {
            noteRepository.findNotes(
                userId = userId,
                subjectId = null,
                topicId = topic.id,
                kind = null,
                searchPattern = null,
                pinnedFirst = true,
                pageable = PageRequest.of(0, 100)
            )
        }

        // Verify partition 2: subjectId = subject.id, topicId = null
        verify(exactly = 1) {
            noteRepository.findNotes(
                userId = userId,
                subjectId = subject.id,
                topicId = null,
                kind = null,
                searchPattern = null,
                pinnedFirst = true,
                pageable = PageRequest.of(0, 100)
            )
        }
    }

    @Test
    fun `sanitizeSlug rules`() {
        // CRLF & Header injection stripped
        assertEquals("topic-algo-asymptoticinjected-true", service.sanitizeSlug("topic_algo_asymptotic\r\nInjected: true"))

        // Unicode folded
        assertEquals("hello-world", service.sanitizeSlug("Héllo Wörld!"))

        // Capped at 60 chars
        val longSlug = "a".repeat(80)
        val sanitized = service.sanitizeSlug(longSlug)
        assertEquals(60, sanitized.length)

        // Multiple hyphens collapsed
        assertEquals("my-topic-name", service.sanitizeSlug("my---topic___name"))

        // Blank fallback
        assertEquals("topic", service.sanitizeSlug("   \n\r   "))
        assertEquals("topic", service.sanitizeSlug("???###@@@"))
    }
}

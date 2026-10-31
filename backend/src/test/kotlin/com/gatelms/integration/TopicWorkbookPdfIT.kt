package com.gatelms.integration

import com.gatelms.anatomy.entity.TopicPrimer
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.dto.CreateTextNoteRequest
import com.gatelms.dto.NoteDto
import com.gatelms.entity.*
import com.gatelms.repository.*
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class TopicWorkbookPdfIT : BaseIntegrationTest() {

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var questionProgressRepository: QuestionProgressRepository

    @Autowired
    lateinit var noteRepository: NoteRepository

    @Autowired
    lateinit var topicPrimerRepository: TopicPrimerRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var testSubject: Subject
    private lateinit var testTopic: Topic
    private lateinit var testQuestion: Question

    @BeforeEach
    fun setupEntities() {
        noteRepository.deleteAll()
        questionProgressRepository.deleteAll()

        testSubject = subjectRepository.findById("subj_wb_test").orElseGet {
            subjectRepository.save(
                Subject(
                    id = "subj_wb_test",
                    name = "Systems Architecture",
                    slug = "systems-arch",
                    cluster = "systems",
                    avgWeightage = 12.0
                )
            )
        }

        testTopic = topicRepository.findById("top_wb_memory").orElseGet {
            topicRepository.save(
                Topic(
                    id = "top_wb_memory",
                    subject = testSubject,
                    name = "Virtual Memory & Translation",
                    slug = "vm-translation"
                )
            )
        }

        testQuestion = questionRepository.findByExternalId("Q_WB_TEST_1") ?: run {
            questionRepository.save(
                Question(
                    externalId = "Q_WB_TEST_1",
                    subject = testSubject,
                    topic = testTopic,
                    year = 2024,
                    type = QuestionType.MCQ,
                    marks = 2,
                    statement = "What is the primary function of a Translation Lookaside Buffer (TLB)?",
                    optionsJson = """[{"label":"A","text":"Cache page table entries"},{"label":"B","text":"Store dirty pages"}]""",
                    correctOptions = "A",
                    explanation = "A TLB is a high-speed associative hardware cache for virtual-to-physical address translations."
                )
            )
        }
    }

    @Test
    fun `G11 - unauthenticated request returns 401`() {
        logout()
        val resp = restTemplate.getForEntity("/api/v1/journey/topics/${testTopic.id}/workbook/pdf", ByteArray::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, resp.statusCode)
    }

    @Test
    fun `G11 - unknown topic id or slug returns 404`() {
        loginAs("ankur", "admin123")
        val resp = restTemplate.getForEntity("/api/v1/journey/topics/non_existent_topic_slug/workbook/pdf", ByteArray::class.java)
        assertEquals(HttpStatus.NOT_FOUND, resp.statusCode)
    }

    @Test
    fun `G11 - happy path returns application-pdf with sanitized filename and no-store`() {
        loginAs("ankur", "admin123")
        val resp = restTemplate.getForEntity("/api/v1/journey/topics/${testTopic.id}/workbook/pdf", ByteArray::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertEquals(MediaType.APPLICATION_PDF, resp.headers.contentType)

        // Cache-Control: no-store
        val cc = resp.headers.cacheControl
        assertNotNull(cc)
        assertTrue(cc!!.contains("no-store"), "Expected Cache-Control: no-store, got: $cc")

        // Content-Disposition: attachment; filename=topic_vm-translation_workbook.pdf
        val cd = resp.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)
        assertNotNull(cd)
        assertTrue(cd!!.contains("attachment"), "Expected attachment disposition: $cd")
        assertTrue(cd.contains("topic_vm-translation_workbook.pdf"), "Filename mismatch: $cd")
        assertFalse(cd.contains("\r") || cd.contains("\n"), "Header injection detected in disposition: $cd")

        // Magic bytes
        val bytes = resp.body!!
        assertTrue(bytes.isNotEmpty())
        assertEquals("%PDF", String(bytes.sliceArray(0..3)))

        val reader = PdfReader(bytes)
        assertTrue(reader.numberOfPages >= 1)
    }

    @Test
    fun `G12 - cross-user note isolation guarantees caller notes only in PDF`() {
        // Create user A
        val userAName = "wb_user_a_${UUID.randomUUID().toString().take(6)}"
        val userA = userRepository.save(
            User(
                username = userAName,
                passwordHash = passwordEncoder.encode("Pass@123"),
                displayName = userAName,
                role = UserRole.STUDENT
            )
        )

        // Create user B
        val userBName = "wb_user_b_${UUID.randomUUID().toString().take(6)}"
        val userB = userRepository.save(
            User(
                username = userBName,
                passwordHash = passwordEncoder.encode("Pass@123"),
                displayName = userBName,
                role = UserRole.STUDENT
            )
        )

        // User A logs in and creates a topic note
        loginAs(userAName, "Pass@123")
        val noteAResp = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(
                title = "User A Private Insight",
                subjectId = testSubject.id,
                topicId = testTopic.id,
                contentMd = "Exclusive secret token: ALPHA-98765-TOKEN"
            ),
            NoteDto::class.java
        )
        assertEquals(HttpStatus.CREATED, noteAResp.statusCode)

        // User B logs in and creates a subject-wide note
        loginAs(userBName, "Pass@123")
        val noteBResp = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(
                title = "User B Private Insight",
                subjectId = testSubject.id,
                topicId = null, // subject-wide note
                contentMd = "Exclusive secret token: BETA-12345-TOKEN"
            ),
            NoteDto::class.java
        )
        assertEquals(HttpStatus.CREATED, noteBResp.statusCode)

        // User A downloads PDF: must contain ALPHA and NOT BETA
        loginAs(userAName, "Pass@123")
        val pdfRespA = restTemplate.getForEntity("/api/v1/journey/topics/${testTopic.id}/workbook/pdf", ByteArray::class.java)
        assertEquals(HttpStatus.OK, pdfRespA.statusCode)
        val readerA = PdfReader(pdfRespA.body!!)
        val textA = (1..readerA.numberOfPages).joinToString("\n") { PdfTextExtractor(readerA).getTextFromPage(it) }

        assertTrue(textA.contains("ALPHA-98765-TOKEN"), "User A workbook must contain User A note")
        assertFalse(textA.contains("BETA-12345-TOKEN"), "User A workbook must NEVER leak User B note")
        assertTrue(textA.contains("[Topic Note]"), "Expected [Topic Note] badge")

        // User B downloads PDF: must contain BETA and NOT ALPHA
        loginAs(userBName, "Pass@123")
        val pdfRespB = restTemplate.getForEntity("/api/v1/journey/topics/${testTopic.id}/workbook/pdf", ByteArray::class.java)
        assertEquals(HttpStatus.OK, pdfRespB.statusCode)
        val readerB = PdfReader(pdfRespB.body!!)
        val textB = (1..readerB.numberOfPages).joinToString("\n") { PdfTextExtractor(readerB).getTextFromPage(it) }

        assertTrue(textB.contains("BETA-12345-TOKEN"), "User B workbook must contain User B note")
        assertFalse(textB.contains("ALPHA-98765-TOKEN"), "User B workbook must NEVER leak User A note")
        assertTrue(textB.contains("[Subject-Wide Note]"), "Expected [Subject-Wide Note] badge")
    }

    @Test
    fun `G12 - solved derivation reflects caller question progress`() {
        val userCName = "wb_user_c_${UUID.randomUUID().toString().take(6)}"
        val userC = userRepository.save(
            User(
                username = userCName,
                passwordHash = passwordEncoder.encode("Pass@123"),
                displayName = userCName,
                role = UserRole.STUDENT
            )
        )

        // User C marks question as SOLVED
        questionProgressRepository.save(
            QuestionProgress(
                id = QuestionProgressId(userId = userC.id!!, questionId = testQuestion.id!!),
                pass1Status = Pass1Status.SOLVED
            )
        )

        loginAs(userCName, "Pass@123")
        val resp = restTemplate.getForEntity("/api/v1/journey/topics/${testTopic.slug}/workbook/pdf", ByteArray::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)

        val reader = PdfReader(resp.body!!)
        val text = (1..reader.numberOfPages).joinToString("\n") { PdfTextExtractor(reader).getTextFromPage(it) }
        assertTrue(text.contains("[SOLVED]"), "Expected question to be marked [SOLVED]")
        assertTrue(text.contains("1 Solved"), "Expected cover to state 1 Solved")
    }
}

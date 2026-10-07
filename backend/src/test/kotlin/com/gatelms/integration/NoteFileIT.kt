package com.gatelms.integration

import com.gatelms.dto.NoteDto
import com.gatelms.entity.Subject
import com.gatelms.repository.NoteRepository
import com.gatelms.repository.SubjectRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import java.util.UUID

class NoteFileIT : BaseIntegrationTest() {

    @Autowired
    lateinit var noteRepository: NoteRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    private lateinit var testSubject: Subject

    @BeforeEach
    fun setup() {
        noteRepository.deleteAll()

        testSubject = subjectRepository.findById("subj_os").orElseGet {
            subjectRepository.save(Subject(id = "subj_os", name = "Operating Systems", slug = "os", cluster = "systems", avgWeightage = 10.0))
        }
    }

    @Test
    fun `1 multipart upload stores file under UUID path and serves byte-identical content with correct headers`() {
        val samplePdfBytes = "%PDF-1.4\n%GATE-LMS-TEST-PDF-CONTENT\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF".toByteArray(Charsets.UTF_8)
        val fileResource = object : ByteArrayResource(samplePdfBytes) {
            override fun getFilename(): String = "silberschatz_os_ch8.pdf"
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add("file", fileResource)
        body.add("title", "Silberschatz Memory Management Reference")
        body.add("subjectId", testSubject.id)

        val requestEntity = HttpEntity(body, headers)
        val createResp = restTemplate.postForEntity("/api/v1/notes/files", requestEntity, NoteDto::class.java)

        assertEquals(HttpStatus.CREATED, createResp.statusCode)
        val note = createResp.body!!
        assertEquals("FILE", note.kind)
        assertEquals("silberschatz_os_ch8.pdf", note.originalFilename)
        assertEquals("application/pdf", note.contentType)
        assertEquals(samplePdfBytes.size.toLong(), note.fileSizeBytes)
        assertNotNull(note.filePath)
        assertTrue(note.filePath!!.contains(".pdf"))
        // Verify path does not contain client-supplied name (stored under noteId/UUID.ext)
        assertFalse(note.filePath!!.contains("silberschatz_os_ch8"))

        // Stream file and verify headers + bytes
        val fileResp = restTemplate.getForEntity("/api/v1/notes/${note.id}/file", ByteArray::class.java)
        assertEquals(HttpStatus.OK, fileResp.statusCode)
        assertArrayEquals(samplePdfBytes, fileResp.body)

        assertEquals(MediaType.APPLICATION_PDF, fileResp.headers.contentType)
        val contentDisp = fileResp.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)
        assertNotNull(contentDisp)
        assertTrue(contentDisp!!.contains("inline"))
        assertTrue(contentDisp.contains("silberschatz_os_ch8.pdf"))

        val cacheControl = fileResp.headers.cacheControl
        assertNotNull(cacheControl)
        assertTrue(cacheControl!!.contains("private"))
        assertTrue(cacheControl.contains("max-age=3600"))

        assertNotNull(fileResp.headers.eTag)
        assertTrue(fileResp.headers.lastModified > 0)
    }

    @Test
    fun `2 path traversal attempt returns 400 Bad Request never 500`() {
        val traversalId = "..%2F..%2Fetc%2Fpasswd"
        val resp = restTemplate.getForEntity("/api/v1/notes/$traversalId/file", Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        val fieldErrors = resp.body!!["fieldErrors"] as? Map<*, *>
        assertNotNull(fieldErrors)
        assertTrue(fieldErrors!!.containsKey("id") || fieldErrors.containsKey("file"))
    }

    @Test
    fun `3 wrong content type or extension mismatch is rejected with 400 Bad Request`() {
        val exeBytes = "MZ-MOCK-BINARY-EXECUTABLE".toByteArray()
        val fileResource = object : ByteArrayResource(exeBytes) {
            override fun getFilename(): String = "malware.exe"
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA

        val body = LinkedMultiValueMap<String, Any>()
        body.add("file", fileResource)
        body.add("title", "Malicious Upload")

        val resp = restTemplate.postForEntity("/api/v1/notes/files", HttpEntity(body, headers), Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        val fieldErrors = resp.body!!["fieldErrors"] as? Map<*, *>
        assertNotNull(fieldErrors)
        assertTrue(fieldErrors!!.containsKey("file"))
    }

    @Test
    fun `4 note with no file returns 404 when file endpoint is requested`() {
        val textNoteResp = restTemplate.postForEntity(
            "/api/v1/notes",
            com.gatelms.dto.CreateTextNoteRequest(title = "Text Note No File", contentMd = "Some markdown"),
            NoteDto::class.java
        )
        val note = textNoteResp.body!!

        val fileResp = restTemplate.getForEntity("/api/v1/notes/${note.id}/file", Map::class.java)
        assertEquals(HttpStatus.NOT_FOUND, fileResp.statusCode)
    }
}

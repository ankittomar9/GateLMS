package com.gatelms.integration

import com.gatelms.dto.CreateTextNoteRequest
import com.gatelms.dto.NoteDto
import com.gatelms.dto.NotePageResponse
import com.gatelms.dto.UpdateNoteRequest
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.NoteRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import java.util.UUID

class NoteApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var noteRepository: NoteRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    private lateinit var testSubject: Subject
    private lateinit var testTopic: Topic

    @BeforeEach
    fun setup() {
        noteRepository.deleteAll()

        testSubject = subjectRepository.findById("subj_os").orElseGet {
            subjectRepository.save(Subject(id = "subj_os", name = "Operating Systems", slug = "os", cluster = "systems", avgWeightage = 10.0))
        }

        testTopic = topicRepository.findById("top_os_paging").orElseGet {
            topicRepository.save(Topic(id = "top_os_paging", subject = testSubject, name = "Paging and TLB", slug = "paging-and-tlb"))
        }
    }

    @Test
    fun `1 CRUD flow - creates, retrieves, updates, and deletes text note`() {
        val createReq = CreateTextNoteRequest(
            title = "Virtual Memory Notes",
            subjectId = testSubject.id,
            topicId = testTopic.id,
            contentMd = "## Multi-level Paging\nEffective memory access time calculation: EAT = \$2 + \\alpha\$."
        )

        val createResp = restTemplate.postForEntity("/api/v1/notes", createReq, NoteDto::class.java)
        assertEquals(HttpStatus.CREATED, createResp.statusCode)
        val created = createResp.body!!
        assertNotNull(created.id)
        assertEquals("Virtual Memory Notes", created.title)
        assertEquals("TEXT", created.kind)
        assertEquals(0, created.version)
        assertFalse(created.isPinned)

        // Get by ID
        val getResp = restTemplate.getForEntity("/api/v1/notes/${created.id}", NoteDto::class.java)
        assertEquals(HttpStatus.OK, getResp.statusCode)
        assertEquals(created.id, getResp.body!!.id)

        // Update
        val updateReq = UpdateNoteRequest(
            title = "Virtual Memory & Inverted Page Tables",
            contentMd = "Updated content with KaTeX math: \$\$EAT = h(t + m) + (1-h)(t + 2m)\$\$",
            version = created.version
        )
        val updateHttpEntity = HttpEntity(updateReq)
        val updateResp = restTemplate.exchange("/api/v1/notes/${created.id}", HttpMethod.PUT, updateHttpEntity, NoteDto::class.java)
        assertEquals(HttpStatus.OK, updateResp.statusCode)
        val updated = updateResp.body!!
        assertEquals("Virtual Memory & Inverted Page Tables", updated.title)
        assertEquals(1, updated.version)

        // Delete
        val deleteResp = restTemplate.exchange("/api/v1/notes/${created.id}", HttpMethod.DELETE, null, Void::class.java)
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.statusCode)

        // Confirm 404
        val afterDeleteResp = restTemplate.getForEntity("/api/v1/notes/${created.id}", Map::class.java)
        assertEquals(HttpStatus.NOT_FOUND, afterDeleteResp.statusCode)
    }

    @Test
    fun `2 optimistic locking - stale version returns 409 Conflict`() {
        val createReq = CreateTextNoteRequest(
            title = "Concurrency Notes",
            subjectId = testSubject.id,
            topicId = testTopic.id,
            contentMd = "Peterson algorithm properties"
        )
        val created = restTemplate.postForEntity("/api/v1/notes", createReq, NoteDto::class.java).body!!

        // Stale update using version 99 instead of 0
        val staleReq = UpdateNoteRequest(
            title = "Concurrency Stale Title",
            version = 99
        )
        val staleResp = restTemplate.exchange(
            "/api/v1/notes/${created.id}",
            HttpMethod.PUT,
            HttpEntity(staleReq),
            Map::class.java
        )
        assertEquals(HttpStatus.CONFLICT, staleResp.statusCode)
    }

    @Test
    fun `3 search escaping - literal percent search does not match arbitrary text`() {
        val note1 = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "CPU Utilization 99% Peak", contentMd = "Normal operations"),
            NoteDto::class.java
        ).body!!

        val note2 = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "General CPU Overview", contentMd = "Standard metrics"),
            NoteDto::class.java
        ).body!!

        // Search for literal "%"
        val resp = restTemplate.getForEntity("/api/v1/notes?q=%", NotePageResponse::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val list = resp.body!!.content
        assertEquals(1, list.size)
        assertEquals(note1.id, list[0].id)
    }

    @Test
    fun `4 kind constraint - blank content without file is rejected with 400 Bad Request`() {
        val invalidReq = CreateTextNoteRequest(
            title = "Empty Content Note",
            contentMd = ""
        )
        val resp = restTemplate.postForEntity("/api/v1/notes", invalidReq, Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, resp.statusCode)
        val fieldErrors = resp.body!!["fieldErrors"] as? Map<*, *>
        assertNotNull(fieldErrors)
        assertTrue(fieldErrors!!.containsKey("contentMd"))
    }

    @Test
    fun `5 toggle pin - PATCH pin toggles isPinned state`() {
        val note = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "Pin Test Note", contentMd = "Pin test content"),
            NoteDto::class.java
        ).body!!
        assertFalse(note.isPinned)

        // Toggle to true
        val pinResp1 = restTemplate.exchange(
            "/api/v1/notes/${note.id}/pin",
            HttpMethod.PATCH,
            null,
            NoteDto::class.java
        )
        assertEquals(HttpStatus.OK, pinResp1.statusCode)
        assertTrue(pinResp1.body!!.isPinned)

        // Toggle back to false
        val pinResp2 = restTemplate.exchange(
            "/api/v1/notes/${note.id}/pin",
            HttpMethod.PATCH,
            null,
            NoteDto::class.java
        )
        assertEquals(HttpStatus.OK, pinResp2.statusCode)
        assertFalse(pinResp2.body!!.isPinned)
    }

    @Test
    fun `6 topic FK ON DELETE SET NULL preserves note`() {
        val tempUid = UUID.randomUUID().toString()
        val tempTopic = topicRepository.save(
            Topic(id = "top_temp_$tempUid", subject = testSubject, name = "Temp Topic", slug = "temp-slug-$tempUid")
        )

        val note = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(
                title = "Note With Temp Topic",
                subjectId = testSubject.id,
                topicId = tempTopic.id,
                contentMd = "Content linked to temporary topic"
            ),
            NoteDto::class.java
        ).body!!

        assertEquals(tempTopic.id, note.topicId)

        // Delete topic
        topicRepository.deleteById(tempTopic.id)

        // Note should still exist with topicId set to null
        val fetched = noteRepository.findById(note.id).orElseThrow()
        assertNull(fetched.topic)
        assertEquals("Note With Temp Topic", fetched.title)
    }
}

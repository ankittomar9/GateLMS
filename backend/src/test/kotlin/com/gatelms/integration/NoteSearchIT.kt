package com.gatelms.integration

import com.gatelms.dto.CreateTextNoteRequest
import com.gatelms.dto.NoteDto
import com.gatelms.dto.NotePageResponse
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.NoteRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap

class NoteSearchIT : BaseIntegrationTest() {

    @Autowired
    lateinit var noteRepository: NoteRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    private lateinit var subjAlgo: Subject
    private lateinit var subjOs: Subject
    private lateinit var topAlgoDp: Topic
    private lateinit var topOsVm: Topic

    @BeforeEach
    fun setup() {
        noteRepository.deleteAll()

        subjAlgo = subjectRepository.findById("subj_algo").orElseGet {
            subjectRepository.save(Subject(id = "subj_algo", name = "Algorithms", slug = "algo", cluster = "core", avgWeightage = 10.0))
        }
        subjOs = subjectRepository.findById("subj_os").orElseGet {
            subjectRepository.save(Subject(id = "subj_os", name = "Operating Systems", slug = "os", cluster = "systems", avgWeightage = 10.0))
        }

        topAlgoDp = topicRepository.findById("top_algo_dp").orElseGet {
            topicRepository.save(Topic(id = "top_algo_dp", subject = subjAlgo, name = "Dynamic Programming", slug = "dp"))
        }
        topOsVm = topicRepository.findById("top_os_vm").orElseGet {
            topicRepository.save(Topic(id = "top_os_vm", subject = subjOs, name = "Virtual Memory", slug = "vm"))
        }
    }

    @Test
    fun `1 filter composition filters accurately across subject, topic, kind, and search query`() {
        // Create 3 notes:
        // Note 1: Text, subj_algo, top_algo_dp, "Knapsack recurrence relation"
        restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "0-1 Knapsack DP", subjectId = subjAlgo.id, topicId = topAlgoDp.id, contentMd = "Recurrence for knapsack"),
            NoteDto::class.java
        )

        // Note 2: Text, subj_algo, top_algo_dp, "Bellman-Ford algorithm"
        restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "Shortest Paths", subjectId = subjAlgo.id, topicId = topAlgoDp.id, contentMd = "Bellman Ford DP formulation"),
            NoteDto::class.java
        )

        // Note 3: File, subj_algo, top_algo_dp, PDF
        val fileBytes = "%PDF-1.4 sample".toByteArray()
        val headers = HttpHeaders()
        headers.contentType = MediaType.MULTIPART_FORM_DATA
        val body = LinkedMultiValueMap<String, Any>()
        body.add("file", object : ByteArrayResource(fileBytes) { override fun getFilename() = "dp_cheatsheet.pdf" })
        body.add("title", "DP Master Cheatsheet")
        body.add("subjectId", subjAlgo.id)
        body.add("topicId", topAlgoDp.id)
        restTemplate.postForEntity("/api/v1/notes/files", HttpEntity(body, headers), NoteDto::class.java)

        // Note 4: Text, subj_os, top_os_vm
        restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "Paging Inversion", subjectId = subjOs.id, topicId = topOsVm.id, contentMd = "Inverted page tables"),
            NoteDto::class.java
        )

        // Query: subjectId=subj_algo & kind=TEXT
        val textAlgoResp = restTemplate.getForEntity(
            "/api/v1/notes?subjectId=${subjAlgo.id}&kind=TEXT",
            NotePageResponse::class.java
        )
        assertEquals(HttpStatus.OK, textAlgoResp.statusCode)
        assertEquals(2, textAlgoResp.body!!.content.size)
        assertTrue(textAlgoResp.body!!.content.all { it.kind == "TEXT" && it.subjectId == subjAlgo.id })

        // Query: kind=FILE
        val fileResp = restTemplate.getForEntity("/api/v1/notes?kind=FILE", NotePageResponse::class.java)
        assertEquals(HttpStatus.OK, fileResp.statusCode)
        assertEquals(1, fileResp.body!!.content.size)
        assertEquals("DP Master Cheatsheet", fileResp.body!!.content[0].title)

        // Query: q=Knapsack
        val searchResp = restTemplate.getForEntity("/api/v1/notes?q=Knapsack", NotePageResponse::class.java)
        assertEquals(HttpStatus.OK, searchResp.statusCode)
        assertEquals(1, searchResp.body!!.content.size)
        assertEquals("0-1 Knapsack DP", searchResp.body!!.content[0].title)
    }

    @Test
    fun `2 pinned first ordering places pinned notes at the top regardless of modification time`() {
        val note1 = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "First Note (Older)", contentMd = "Content 1"),
            NoteDto::class.java
        ).body!!

        val note2 = restTemplate.postForEntity(
            "/api/v1/notes",
            CreateTextNoteRequest(title = "Second Note (Newer)", contentMd = "Content 2"),
            NoteDto::class.java
        ).body!!

        // Pin the first (older) note
        restTemplate.exchange("/api/v1/notes/${note1.id}/pin", HttpMethod.PATCH, null, NoteDto::class.java)

        // Query with pinnedFirst=true (default)
        val resp = restTemplate.getForEntity("/api/v1/notes", NotePageResponse::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val content = resp.body!!.content
        assertEquals(2, content.size)
        assertEquals(note1.id, content[0].id)
        assertTrue(content[0].isPinned)
        assertEquals(note2.id, content[1].id)
        assertFalse(content[1].isPinned)
    }
}

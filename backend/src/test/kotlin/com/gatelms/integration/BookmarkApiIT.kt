package com.gatelms.integration

import com.gatelms.dto.BookmarkRequest
import com.gatelms.dto.QuestionDto
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.entity.Subject
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class BookmarkApiIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    private lateinit var subject: Subject
    private lateinit var topic: Topic

    @BeforeEach
    fun setup() {
        questionRepository.deleteAll()

        subject = subjectRepository.findById("subj_dbms").orElseGet {
            subjectRepository.save(Subject(id = "subj_dbms", name = "Databases", slug = "dbms", cluster = "core", avgWeightage = 7.0))
        }

        topic = topicRepository.findById("top_dbms_indexing").orElseGet {
            topicRepository.save(Topic(id = "top_dbms_indexing", subject = subject, name = "B+ Trees", slug = "indexing"))
        }
    }

    @Test
    fun `1 Toggle on and off sets and clears bookmarked and bookmarked_at`() {
        val q = questionRepository.save(Question(
            statement = "B+ Tree depth query",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "A",
            isBookmarked = false,
            bookmarkedAt = null
        ))

        // Bookmark = true
        val bookmarkResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}/bookmark",
            HttpMethod.PATCH,
            HttpEntity(BookmarkRequest(bookmarked = true)),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, bookmarkResp.statusCode)
        val bookmarkedDto = bookmarkResp.body!!
        assertTrue(bookmarkedDto.isBookmarked)
        assertNotNull(bookmarkedDto.bookmarkedAt)

        // Verify in DB
        val qInDb = questionRepository.findById(q.id!!).get()
        assertTrue(qInDb.isBookmarked)
        assertNotNull(qInDb.bookmarkedAt)

        // Bookmark = false
        val unbookmarkResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}/bookmark",
            HttpMethod.PATCH,
            HttpEntity(BookmarkRequest(bookmarked = false)),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, unbookmarkResp.statusCode)
        val unbookmarkedDto = unbookmarkResp.body!!
        assertFalse(unbookmarkedDto.isBookmarked)
        assertNull(unbookmarkedDto.bookmarkedAt)

        val qInDb2 = questionRepository.findById(q.id!!).get()
        assertFalse(qInDb2.isBookmarked)
        assertNull(qInDb2.bookmarkedAt)
    }

    @Test
    fun `2 Bookmark does not bump version conflict on statement edit`() {
        val q = questionRepository.save(Question(
            statement = "Original Statement",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            optionsJson = """[{"id":"A","text":"Opt A"},{"id":"B","text":"Opt B"}]""",
            correctOptions = "B",
            version = 0
        ))

        val initialVersion = q.version

        // Toggle bookmark
        val patchResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}/bookmark",
            HttpMethod.PATCH,
            HttpEntity(BookmarkRequest(bookmarked = true)),
            QuestionDto::class.java
        )
        assertEquals(HttpStatus.OK, patchResp.statusCode)

        // Statement edit with the original version (0) should SUCCEED because bookmarking didn't bump entity version
        val updateReq = QuestionUpsertDto(
            subjectId = subject.id,
            topicId = topic.id,
            type = QuestionType.MCQ,
            statement = "Updated Statement Without Version Conflict",
            optionsJson = """[{"id":"A","text":"Opt A"},{"id":"B","text":"Opt B"}]""",
            correctOptions = "B",
            version = initialVersion
        )

        val updateResp = restTemplate.exchange(
            "/api/v1/questions/${q.id}",
            HttpMethod.PUT,
            HttpEntity(updateReq),
            QuestionDto::class.java
        )

        assertEquals(HttpStatus.OK, updateResp.statusCode)
        val updatedDto = updateResp.body!!
        assertEquals("Updated Statement Without Version Conflict", updatedDto.statement)
        assertTrue(updatedDto.isBookmarked, "Bookmarked state should remain true")
    }

    @Test
    fun `3 List filter returns only bookmarked questions`() {
        val q1 = questionRepository.save(Question(
            statement = "Bookmarked Question 1",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "A",
            isBookmarked = true
        ))

        val q2 = questionRepository.save(Question(
            statement = "Unbookmarked Question",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "B",
            isBookmarked = false
        ))

        val q3 = questionRepository.save(Question(
            statement = "Bookmarked Question 2",
            subject = subject,
            topic = topic,
            type = QuestionType.MCQ,
            correctOptions = "C",
            isBookmarked = true
        ))

        val filterResp = restTemplate.getForEntity(
            "/api/v1/questions?bookmarked=true",
            Map::class.java
        )
        assertEquals(HttpStatus.OK, filterResp.statusCode)
        val content = filterResp.body!!["content"] as List<Map<String, Any>>

        assertEquals(2, content.size)
        val ids = content.map { it["id"].toString() }.toSet()
        assertTrue(ids.contains(q1.id.toString()))
        assertTrue(ids.contains(q3.id.toString()))
        assertFalse(ids.contains(q2.id.toString()))
    }
}

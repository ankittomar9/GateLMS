package com.gatelms.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import com.gatelms.seed.QuestionSeedRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.nio.file.Path

class SeedRunnerIT : BaseIntegrationTest() {

    @Autowired
    lateinit var questionRepository: QuestionRepository

    @Autowired
    lateinit var subjectRepository: SubjectRepository

    @Autowired
    lateinit var topicRepository: TopicRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private lateinit var seedRunner: QuestionSeedRunner

    @BeforeEach
    fun setUp() {
        seedRunner = QuestionSeedRunner(
            questionRepository = questionRepository,
            subjectRepository = subjectRepository,
            topicRepository = topicRepository,
            objectMapper = objectMapper,
            seedPath = "seed-data/questions.jsonl"
        )
    }

    @Test
    fun `import sample pack inserts questions cleanly with zero rejections`() {
        val report = seedRunner.importQuestions("seed-data/questions.jsonl")
        assertTrue(report.total >= 25, "Expected >= 25 total lines, found ${report.total}")
        assertEquals(0, report.rejected, "Expected 0 rejections, found ${report.rejected}")
        assertTrue(report.inserted > 0 || report.updated > 0)
    }

    @Test
    fun `honest-judge pair - re-running seed pipeline is completely idempotent`() {
        // Run once
        val report1 = seedRunner.importQuestions("seed-data/questions.jsonl")
        assertEquals(0, report1.rejected)

        // Re-run
        val report2 = seedRunner.importQuestions("seed-data/questions.jsonl")
        assertEquals(report1.total, report2.total)
        assertEquals(0, report2.inserted, "Idempotent re-run should insert 0 new questions")
        assertEquals(report1.total, report2.updated, "Idempotent re-run should update existing questions")
        assertEquals(0, report2.rejected)
    }

    @Test
    fun `honest-judge deliberate-wrong - corrupt fixture with valid and invalid items correctly rejects with reasons`(@TempDir tempDir: Path) {
        val corruptJsonl = """
            {"externalId":"CORRUPT-VAL-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Valid 1","options":{"A":"1","B":"2","C":"3","D":"4"},"correctOptions":["A"]}
            {"externalId":"CORRUPT-INV-1","year":2024,"type":"NAT","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Invalid NAT min > max","numericMin":10.0,"numericMax":5.0}
            {"externalId":"CORRUPT-VAL-2","year":2024,"type":"NAT","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Valid NAT","numericMin":5.0,"numericMax":10.0}
            {"externalId":"CORRUPT-INV-2","year":2024,"type":"MCQ","marks":3,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Invalid marks 3","options":{"A":"1","B":"2","C":"3","D":"4"},"correctOptions":["A"]}
            {"externalId":"CORRUPT-VAL-3","year":2024,"type":"MCQ","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Valid 3","options":{"A":"1","B":"2","C":"3","D":"4"},"correctOptions":["B"]}
        """.trimIndent()

        val tempFile = File(tempDir.toFile(), "test-corrupt.jsonl")
        tempFile.writeText(corruptJsonl)

        val report = seedRunner.importQuestions(tempFile.absolutePath)
        assertEquals(5, report.total)
        assertEquals(3, report.inserted + report.updated)
        assertEquals(2, report.rejected)
        assertTrue(report.rejectionReasons.any { it.contains("cannot exceed numericMax") })
        assertTrue(report.rejectionReasons.any { it.contains("marks must be 1 or 2") })
    }
}

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

class SeedBulkIT : BaseIntegrationTest() {

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
            seedPath = "seed-data/so-gate-bank.jsonl"
        )
    }

    @Test
    fun `honest-judge PASS - bulk pack fixture with over 100 items imports idempotently`(@TempDir tempDir: Path) {
        // Create 120 fixture questions
        val tempFile = File(tempDir.toFile(), "bulk-120.jsonl")
        tempFile.bufferedWriter().use { writer ->
            for (i in 1..120) {
                val qType = when (i % 3) {
                    0 -> "NAT"
                    1 -> "MCQ"
                    else -> "MSQ"
                }
                val line = if (qType == "NAT") {
                    """{"externalId":"BULK-FX-$i","year":2024,"type":"NAT","marks":2,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Bulk fixture question $i","numericMin":1.0,"numericMax":2.0,"source":"SO_DERIVED","difficulty":"MEDIUM","explanation":"Bulk test explanation"}"""
                } else {
                    """{"externalId":"BULK-FX-$i","year":2024,"type":"$qType","marks":1,"subjectSlug":"operating-systems","topicSlug":"virtual-memory-paging","statement":"Bulk fixture question $i","options":{"A":"1","B":"2","C":"3","D":"4"},"correctOptions":["A"],"source":"SO_DERIVED","difficulty":"MEDIUM","explanation":"Bulk test explanation"}"""
                }
                writer.write(line)
                writer.newLine()
            }
        }

        // Pass 1: Import
        val report1 = seedRunner.importQuestions(tempFile.absolutePath)
        assertEquals(120, report1.total)
        assertEquals(0, report1.rejected)
        assertTrue(report1.inserted > 0 || report1.updated > 0)

        // Pass 2: Re-import (must be 100% idempotent: 0 inserted, all updated, 0 rejected)
        val report2 = seedRunner.importQuestions(tempFile.absolutePath)
        assertEquals(120, report2.total)
        assertEquals(0, report2.inserted, "Idempotent re-run must insert 0 questions")
        assertEquals(120, report2.updated, "Idempotent re-run must update 120 questions")
        assertEquals(0, report2.rejected)
    }

    @Test
    fun `honest-judge PASS - imported bulk questions default to UNREVIEWED curation status with metadata passthrough`(@TempDir tempDir: Path) {
        val tempFile = File(tempDir.toFile(), "curation-default.jsonl")
        tempFile.writeText(
            """{"externalId":"BULK-CUR-DEF-01","year":2024,"type":"MCQ","marks":1,"subjectSlug":"algorithms","topicSlug":"asymptotic-analysis","statement":"Asymptotic notation query","options":{"A":"Omega","B":"Theta","C":"O","D":"o"},"correctOptions":["B"],"source":"SO_DERIVED","difficulty":"HARD","explanation":"Theta gives tight bound"}""" + "\n"
        )

        val report = seedRunner.importQuestions(tempFile.absolutePath)
        assertEquals(1, report.total)
        assertEquals(0, report.rejected)

        val q = questionRepository.findByExternalId("BULK-CUR-DEF-01")
        assertNotNull(q)
        assertEquals("UNREVIEWED", q!!.curationStatus)
        assertEquals("SO_DERIVED", q.source)
        assertEquals("HARD", q.difficulty)
        assertNull(q.curationNote)
    }

    @Test
    fun `honest-judge FAIL - corrupt line in bulk import is rejected with reason while valid lines succeed`(@TempDir tempDir: Path) {
        val tempFile = File(tempDir.toFile(), "bulk-corrupt.jsonl")
        tempFile.writeText(
            """{"externalId":"BULK-OK-1","year":2024,"type":"MCQ","marks":1,"subjectSlug":"algorithms","topicSlug":"asymptotic-analysis","statement":"Valid bulk item 1","options":{"A":"1","B":"2","C":"3","D":"4"},"correctOptions":["A"]}""" + "\n" +
            """{"externalId":"BULK-BAD-1","year":2024,"type":"NAT","marks":1,"subjectSlug":"algorithms","topicSlug":"asymptotic-analysis","statement":"Invalid numeric interval","numericMin":50.0,"numericMax":10.0}""" + "\n" +
            """{"externalId":"BULK-OK-2","year":2024,"type":"MCQ","marks":1,"subjectSlug":"algorithms","topicSlug":"asymptotic-analysis","statement":"Valid bulk item 2","options":{"A":"1","B":"2","C":"3","D":"4"},"correctOptions":["A"]}""" + "\n"
        )

        val report = seedRunner.importQuestions(tempFile.absolutePath)
        assertEquals(3, report.total)
        assertEquals(1, report.rejected)
        assertTrue(report.rejectionReasons.any { it.contains("cannot exceed numericMax") })

        assertNotNull(questionRepository.findByExternalId("BULK-OK-1"))
        assertNotNull(questionRepository.findByExternalId("BULK-OK-2"))
        assertNull(questionRepository.findByExternalId("BULK-BAD-1"))
    }
}

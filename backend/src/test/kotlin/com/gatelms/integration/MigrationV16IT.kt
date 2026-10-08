package com.gatelms.integration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class MigrationV16IT : BaseIntegrationTest() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `honest-judge PASS - migration V16 schema validation for topics sequence and topic_primers`() {
        // 1. Verify sequence column on topics
        val seqColCount = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'topics' AND column_name = 'sequence'
            """,
            Long::class.java
        ) ?: 0L
        assertEquals(1L, seqColCount, "topics.sequence column must exist")

        // 2. Verify topic_primers table exists
        val primerTableCount = jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM information_schema.tables 
            WHERE table_schema = 'public' AND table_name = 'topic_primers'
            """,
            Long::class.java
        ) ?: 0L
        assertEquals(1L, primerTableCount, "Table 'topic_primers' must exist")

        // 3. Verify indexes exist
        val indexList = jdbcTemplate.queryForList(
            """
            SELECT indexname FROM pg_indexes 
            WHERE schemaname = 'public' AND indexname IN ('idx_topics_subject_sequence', 'idx_topic_primers_status')
            """,
            String::class.java
        ).toSet()

        assertTrue(indexList.contains("idx_topics_subject_sequence"), "Index 'idx_topics_subject_sequence' must exist")
        assertTrue(indexList.contains("idx_topic_primers_status"), "Index 'idx_topic_primers_status' must exist")
    }

    @Test
    fun `honest-judge PASS - all topics have sequence greater than or equal to 1 with deterministic ordering`() {
        // Zero topics with sequence <= 0
        val unsequencedCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM topics WHERE sequence <= 0",
            Long::class.java
        ) ?: 0L
        assertEquals(0L, unsequencedCount, "All topics must have sequence >= 1")

        // Total topic count >= 30
        val totalTopics = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM topics",
            Long::class.java
        ) ?: 0L
        assertTrue(totalTopics >= 30L, "Expected >= 30 topics in seeded database")

        // Verify deterministic ordering per subject: minimum sequence for any subject is 1
        val minSequencePerSubject = jdbcTemplate.queryForList(
            """
            SELECT subject_id, min(sequence) as min_seq 
            FROM topics 
            GROUP BY subject_id
            """
        )
        for (row in minSequencePerSubject) {
            val minSeq = (row["min_seq"] as Number).toInt()
            assertEquals(1, minSeq, "Each subject's topic sequence must start at 1")
        }
    }
}

package com.gatelms.integration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class MigrationV14IT : BaseIntegrationTest() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private val operatorId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `1 schema validation — new tables exist and legacy columns dropped`() {
        // Verify new tables exist
        val tables = jdbcTemplate.queryForList(
            """
            SELECT table_name FROM information_schema.tables 
            WHERE table_schema = 'public' AND table_name IN ('users', 'topic_mastery', 'question_progress')
            """,
            String::class.java
        ).toSet()

        assertTrue(tables.contains("users"), "Table 'users' must exist")
        assertTrue(tables.contains("topic_mastery"), "Table 'topic_mastery' must exist")
        assertTrue(tables.contains("question_progress"), "Table 'question_progress' must exist")

        // Verify dropped columns on questions do NOT exist
        val questionColumns = jdbcTemplate.queryForList(
            """
            SELECT column_name FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'questions' 
              AND column_name IN ('pass1_status', 'pass2_status', 'is_bookmarked', 'bookmarked_at', 'last_attempted_at')
            """,
            String::class.java
        )
        assertTrue(questionColumns.isEmpty(), "Legacy columns must be dropped from 'questions': $questionColumns")

        // Verify dropped columns on topics do NOT exist
        val topicColumns = jdbcTemplate.queryForList(
            """
            SELECT column_name FROM information_schema.columns 
            WHERE table_schema = 'public' AND table_name = 'topics' 
              AND column_name IN ('mastery_level', 'last_revised_at', 'next_revision_due')
            """,
            String::class.java
        )
        assertTrue(topicColumns.isEmpty(), "Legacy columns must be dropped from 'topics': $topicColumns")
    }

    @Test
    fun `2 data preservation — operator user exists and scoped tables have non-null user_id`() {
        // Verify operator user exists
        val operatorUserCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE id = ? AND username = 'ankur'",
            Long::class.java,
            operatorId
        ) ?: 0L
        assertEquals(1L, operatorUserCount, "Operator user 'ankur' with id $operatorId must exist")

        // Verify scoped tables have no NULL user_id rows
        val nullAttempts = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM mock_attempts WHERE user_id IS NULL",
            Long::class.java
        ) ?: 0L
        assertEquals(0L, nullAttempts, "mock_attempts must have 0 rows with NULL user_id")

        val nullMistakes = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM mistake_logs WHERE user_id IS NULL",
            Long::class.java
        ) ?: 0L
        assertEquals(0L, nullMistakes, "mistake_logs must have 0 rows with NULL user_id")

        val nullNotes = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM notes WHERE user_id IS NULL",
            Long::class.java
        ) ?: 0L
        assertEquals(0L, nullNotes, "notes must have 0 rows with NULL user_id")

        val nullPlans = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM study_plans WHERE user_id IS NULL",
            Long::class.java
        ) ?: 0L
        assertEquals(0L, nullPlans, "study_plans must have 0 rows with NULL user_id")

        val nullRollups = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM daily_rollups WHERE user_id IS NULL",
            Long::class.java
        ) ?: 0L
        assertEquals(0L, nullRollups, "daily_rollups must have 0 rows with NULL user_id")
    }

    @Test
    fun `3 data preservation row counts receipt`() {
        val userCount = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Long::class.java) ?: 0L
        val questionsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM questions", Long::class.java) ?: 0L
        val subjectsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM subjects", Long::class.java) ?: 0L
        val topicsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM topics", Long::class.java) ?: 0L
        val notesCount = jdbcTemplate.queryForObject("SELECT count(*) FROM notes", Long::class.java) ?: 0L
        val progressCount = jdbcTemplate.queryForObject("SELECT count(*) FROM question_progress", Long::class.java) ?: 0L
        val masteryCount = jdbcTemplate.queryForObject("SELECT count(*) FROM topic_mastery", Long::class.java) ?: 0L
        val rollupsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM daily_rollups", Long::class.java) ?: 0L
        val goalsCount = jdbcTemplate.queryForObject("SELECT count(*) FROM user_goals", Long::class.java) ?: 0L
        val sessionCount = jdbcTemplate.queryForObject("SELECT count(*) FROM spring_session", Long::class.java) ?: 0L

        println("=== V14 ROW COUNTS RECEIPT ===")
        println("users: $userCount")
        println("questions: $questionsCount")
        println("subjects: $subjectsCount")
        println("topics: $topicsCount")
        println("notes: $notesCount")
        println("question_progress: $progressCount")
        println("topic_mastery: $masteryCount")
        println("daily_rollups: $rollupsCount")
        println("user_goals: $goalsCount")
        println("spring_session: $sessionCount")
        println("===============================")

        assertTrue(userCount >= 1L, "At least operator user must exist")
    }
}

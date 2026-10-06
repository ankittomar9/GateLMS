package com.gatelms.integration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class MigrationIntegrityIT : BaseIntegrationTest() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `all flyway migrations apply cleanly and successfully`() {
        val count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = true",
            Int::class.java
        )
        assertNotNull(count)
        assertTrue(count!! >= 5, "Expected at least 5 successful migrations in schema history, found $count")
    }

    @Test
    fun `honest-judge deliberate-wrong - invalid attempt_answers state violates TCS iON check constraint`() {
        val mockId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()
        val questionId = UUID.randomUUID()
        val answerId = UUID.randomUUID()

        jdbcTemplate.update(
            "INSERT INTO mock_exams (id, title, duration_minutes, total_marks, blueprint_json) VALUES (?, 'Integrity Mock', 180, 100.0, '{}'::jsonb)",
            mockId
        )
        jdbcTemplate.update(
            "INSERT INTO mock_attempts (id, mock_id) VALUES (?, ?)",
            attemptId, mockId
        )
        jdbcTemplate.update(
            """
            INSERT INTO questions (id, external_id, subject_id, topic_id, year, type, marks, statement, explanation)
            VALUES (?, 'CHK-Q-01', 'subj_algo', 'topic_algo_asymptotic', 2024, 'MCQ', 1, 'Check state question', 'Explanation')
            """,
            questionId
        )

        // Deliberate-wrong: 'BOGUS_STATE' violates chk_attempt_answer_state
        val ex = assertThrows(DataIntegrityViolationException::class.java) {
            jdbcTemplate.update(
                "INSERT INTO attempt_answers (id, attempt_id, question_id, state) VALUES (?, ?, ?, 'BOGUS_STATE')",
                answerId, attemptId, questionId
            )
        }
        assertTrue(
            ex.message?.contains("chk_attempt_answer_state") == true ||
            ex.message?.contains("attempt_answers") == true
        )
    }

    @Test
    fun `search_vector is automatically computed by V4 trigger on question insert`() {
        val qId = UUID.randomUUID()
        jdbcTemplate.update(
            """
            INSERT INTO questions (id, external_id, subject_id, topic_id, year, type, marks, statement, explanation)
            VALUES (?, 'TRIG-TEST-01', 'subj_algo', 'topic_algo_asymptotic', 2024, 'MCQ', 1, 'QuickSort average runtime complexity', 'QuickSort divide and conquer')
            """,
            qId
        )

        val vector = jdbcTemplate.queryForObject(
            "SELECT search_vector::text FROM questions WHERE id = ?",
            String::class.java,
            qId
        )
        assertNotNull(vector)
        assertTrue(vector!!.contains("quicksort"), "Expected search_vector to contain lexeme 'quicksort'")
    }
}

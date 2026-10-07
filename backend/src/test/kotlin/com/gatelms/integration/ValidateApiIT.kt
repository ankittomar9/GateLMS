package com.gatelms.integration

import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.dto.QuestionValidationResultDto
import com.gatelms.entity.QuestionType
import com.gatelms.repository.QuestionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class ValidateApiIT : BaseIntegrationTest() {

    @Autowired
    private lateinit var questionRepository: QuestionRepository

    @Test
    fun `validate endpoint returns valid true and empty fieldErrors for valid question`() {
        val validDto = QuestionUpsertDto(
            externalId = "VAL-TEST-Q01",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.MCQ,
            marks = 2,
            statement = "What is the worst-case time complexity of Merge Sort on array of size n? $$\\Theta(n \\log n)$$",
            optionsJson = """[{"label":"A","text":"O(n)"},{"label":"B","text":"O(n log n)"},{"label":"C","text":"O(n^2)"},{"label":"D","text":"O(1)"}]""",
            correctOptions = "B",
            explanation = "Merge Sort always divides in half and merges in linear time."
        )

        val resp = restTemplate.postForEntity("/api/v1/questions/validate", validDto, QuestionValidationResultDto::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertTrue(body.valid, "Expected valid to be true for properly formatted question")
        assertTrue(body.fieldErrors.isEmpty(), "Expected empty fieldErrors for valid question")
        assertTrue(body.warnings.any { it.contains("Display-math") }, "Expected display-math warning")
    }

    @Test
    fun `validate endpoint returns valid false with correctOptions fieldError for MCQ with multiple answers`() {
        val invalidMcq = QuestionUpsertDto(
            externalId = "VAL-INVALID-MCQ",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.MCQ,
            marks = 1,
            statement = "Invalid MCQ statement",
            optionsJson = """[{"label":"A","text":"1"},{"label":"B","text":"2"},{"label":"C","text":"3"},{"label":"D","text":"4"}]""",
            correctOptions = "A,B" // Invalid for MCQ: exactly 1 required
        )

        val resp = restTemplate.postForEntity("/api/v1/questions/validate", invalidMcq, QuestionValidationResultDto::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertFalse(body.valid, "Expected valid to be false for MCQ with multiple correct options")
        assertTrue(body.fieldErrors.containsKey("correctOptions"), "Expected correctOptions in fieldErrors")
    }

    @Test
    fun `validate endpoint returns valid false with numericMin fieldError for NAT with inverted bounds`() {
        val invalidNat = QuestionUpsertDto(
            externalId = "VAL-INVALID-NAT",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.NAT,
            marks = 2,
            statement = "Calculate speedup ratio",
            numericMin = 15.0,
            numericMax = 10.0 // Inverted bounds: min > max
        )

        val resp = restTemplate.postForEntity("/api/v1/questions/validate", invalidNat, QuestionValidationResultDto::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertFalse(body.valid, "Expected valid to be false for NAT with inverted bounds")
        assertTrue(body.fieldErrors.containsKey("numericMin"), "Expected numericMin in fieldErrors")
    }

    @Test
    fun `validate endpoint does not persist any question in database`() {
        val countBefore = questionRepository.count()

        val validDto = QuestionUpsertDto(
            externalId = "VAL-DRYRUN-NEVER-PERSIST",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.MCQ,
            marks = 1,
            statement = "Should not be saved into database table",
            optionsJson = """[{"label":"A","text":"1"},{"label":"B","text":"2"},{"label":"C","text":"3"},{"label":"D","text":"4"}]""",
            correctOptions = "A",
            explanation = "Dry-run verification test."
        )

        val resp = restTemplate.postForEntity("/api/v1/questions/validate", validDto, QuestionValidationResultDto::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        assertTrue(resp.body!!.valid)

        val countAfter = questionRepository.count()
        assertEquals(countBefore, countAfter, "Security gate: validate endpoint MUST NEVER persist data")
        assertNull(questionRepository.findByExternalId("VAL-DRYRUN-NEVER-PERSIST"))
    }

    @Test
    fun `validate endpoint reports non-blocking warnings for wide NAT tolerance and empty explanation`() {
        val wideNat = QuestionUpsertDto(
            externalId = "VAL-WIDE-NAT",
            subjectId = "subj_algo",
            topicId = "topic_algo_asymptotic",
            year = 2024,
            type = QuestionType.NAT,
            marks = 2,
            statement = "Estimate large value without display math",
            numericMin = 10.0,
            numericMax = 2000.0, // span = 1990 > 1000
            explanation = "" // Empty explanation
        )

        val resp = restTemplate.postForEntity("/api/v1/questions/validate", wideNat, QuestionValidationResultDto::class.java)
        assertEquals(HttpStatus.OK, resp.statusCode)
        val body = resp.body!!
        assertTrue(body.valid, "Valid question despite non-blocking warnings")
        assertTrue(body.warnings.any { it.contains("NAT tolerance span") }, "Expected NAT tolerance span warning")
        assertTrue(body.warnings.any { it.contains("Explanation is empty") }, "Expected Explanation is empty warning")
        assertTrue(body.warnings.any { it.contains("Display-math ($$...$$) absent") }, "Expected display-math absent warning")
    }
}

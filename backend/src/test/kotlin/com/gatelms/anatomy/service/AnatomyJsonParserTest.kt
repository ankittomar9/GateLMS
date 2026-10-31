package com.gatelms.anatomy.service

import com.gatelms.anatomy.dto.AnatomyJsonPayload
import com.gatelms.anatomy.dto.PrerequisiteItem
import com.gatelms.anatomy.dto.TrapAnalysisItem
import com.gatelms.anatomy.exception.AnatomyValidationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AnatomyJsonParserTest {

    private val validator = AnatomyValidationService()

    @Test
    fun `parseAndValidate should succeed for clean valid JSON`() {
        val json = """
            {
              "tested_concept": "B+ tree index search efficiency and order constraints",
              "invariant_rule": "Every internal node must have between ceil(p/2) and p pointers",
              "trap_analysis": [
                {
                  "option": "A",
                  "verdict": "INCORRECT",
                  "why_plausible": "Assumes minimum degree is floor instead of ceil",
                  "misconception": "Confusing lower bounds of B trees with binary trees"
                }
              ],
              "prerequisites": [
                {
                  "concept_slug": "b-plus-tree-order",
                  "concept_label": "B+ Tree Order",
                  "topic_slug": "dbms-indexing",
                  "must_read": true
                }
              ],
              "concept_tags": ["b-plus-trees", "database-indexing"],
              "similar_pattern_hint": "Look for questions specifying maximum order p and asking for minimum keys"
            }
        """.trimIndent()

        val result = validator.parseAndValidate(json)
        assertEquals("B+ tree index search efficiency and order constraints", result.testedConcept)
        assertEquals(2, result.conceptTags.size)
        assertEquals(1, result.trapAnalysis.size)
    }

    @Test
    fun `parseAndValidate should extract JSON wrapped in markdown code fence`() {
        val raw = """
            Here is the requested breakdown:
            ```json
            {
              "tested_concept": "Two-phase locking protocol conflict serializability",
              "invariant_rule": "Once a transaction releases a lock, it may not obtain new locks",
              "trap_analysis": [],
              "prerequisites": [],
              "concept_tags": ["two-phase-locking"],
              "similar_pattern_hint": "Check growing vs shrinking phase"
            }
            ```
            Hope this helps!
        """.trimIndent()

        val result = validator.parseAndValidate(raw)
        assertEquals("Two-phase locking protocol conflict serializability", result.testedConcept)
        assertEquals("two-phase-locking", result.conceptTags[0])
    }

    @Test
    fun `AF-1 violation - rejects student-population percentages and count claims`() {
        val payloadWithPercent = AnatomyJsonPayload(
            testedConcept = "Normal form decomposition",
            trapAnalysis = listOf(
                TrapAnalysisItem(
                    option = "B",
                    verdict = "INCORRECT",
                    whyPlausible = "60% of students choose B because of lossy join confusion",
                    misconception = "Failing to check attribute intersection"
                )
            ),
            conceptTags = listOf("bcnf-decomposition")
        )

        val ex = assertThrows<AnatomyValidationException> {
            validator.validate(payloadWithPercent)
        }
        assertTrue(ex.message!!.contains("AF-1 violation"))
    }

    @Test
    fun `AF-1 violation - rejects phrase majority of students`() {
        val payload = AnatomyJsonPayload(
            testedConcept = "Pipelining hazards",
            similarPatternHint = "The majority of students get tripped up on data forwarding",
            conceptTags = listOf("pipeline-hazards")
        )

        val ex = assertThrows<AnatomyValidationException> {
            validator.validate(payload)
        }
        assertTrue(ex.message!!.contains("AF-1 violation"))
    }

    @Test
    fun `AF-3 violation - rejects textbook page number citations`() {
        val payload = AnatomyJsonPayload(
            testedConcept = "Strict 2PL as discussed on Page 620",
            conceptTags = listOf("concurrency-control")
        )

        val ex = assertThrows<AnatomyValidationException> {
            validator.validate(payload)
        }
        assertTrue(ex.message!!.contains("AF-3 violation"))
    }

    @Test
    fun `AF-5 violation - rejects invalid slug format and count boundaries`() {
        // Upper case / invalid characters
        val badSlugPayload = AnatomyJsonPayload(
            testedConcept = "Dynamic Programming",
            conceptTags = listOf("Invalid_Slug!")
        )
        val ex1 = assertThrows<AnatomyValidationException> {
            validator.validate(badSlugPayload)
        }
        assertTrue(ex1.message!!.contains("AF-5 violation"))

        // More than 4 tags
        val tooManyTagsPayload = AnatomyJsonPayload(
            testedConcept = "Graph algorithms",
            conceptTags = listOf("tag-one", "tag-two", "tag-three", "tag-four", "tag-five")
        )
        val ex2 = assertThrows<AnatomyValidationException> {
            validator.validate(tooManyTagsPayload)
        }
        assertTrue(ex2.message!!.contains("AF-5 violation"))

        // Zero tags
        val zeroTagsPayload = AnatomyJsonPayload(
            testedConcept = "Graph algorithms",
            conceptTags = emptyList()
        )
        val ex3 = assertThrows<AnatomyValidationException> {
            validator.validate(zeroTagsPayload)
        }
        assertTrue(ex3.message!!.contains("AF-5 violation"))
    }

    @Test
    fun `tested_concept exceeding 300 chars should be rejected`() {
        val longConcept = "A".repeat(301)
        val payload = AnatomyJsonPayload(
            testedConcept = longConcept,
            conceptTags = listOf("valid-concept")
        )
        val ex = assertThrows<AnatomyValidationException> {
            validator.validate(payload)
        }
        assertTrue(ex.message!!.contains("tested_concept must be <= 300 characters"))
    }

    @Test
    fun `malformed truncated JSON should throw AnatomyValidationException`() {
        val malformed = """{"tested_concept": "Truncated json"""
        val ex = assertThrows<AnatomyValidationException> {
            validator.parseAndValidate(malformed)
        }
        assertTrue(ex.message!!.contains("Malformed or unparseable JSON"))
    }

    @Test
    fun `parseAndValidate should tolerate unquoted field names and single quotes`() {
        val relaxedJson = """
            {
              tested_concept: 'Asymptotic notation for algorithm analysis',
              invariant_rule: 'Growth rate bounds hold for large n',
              trap_analysis: [],
              prerequisites: [],
              concept_tags: ['asymptotic-notation'],
              similar_pattern_hint: 'Watch for constant factor limits'
            }
        """.trimIndent()

        val result = validator.parseAndValidate(relaxedJson)
        assertEquals("Asymptotic notation for algorithm analysis", result.testedConcept)
        assertEquals(listOf("asymptotic-notation"), result.conceptTags)
    }

    @Test
    fun `parseAndValidate should repair unescaped LaTeX backslashes in anatomy payloads`() {
        val latexJson = """
            {
              "tested_concept": "Time complexity ${'$'}T(n) \in \Theta(n \log n)${'$'} recurrence",
              "invariant_rule": "For all ${'$'}n \ge n_0${'$'}, ${'$'}f(n) \le c \cdot g(n)${'$'} holds",
              "trap_analysis": [],
              "prerequisites": [],
              "concept_tags": ["recurrence-relations"],
              "similar_pattern_hint": "Check ${'$'}\log_b a${'$'} vs ${'$'}f(n)${'$'}"
            }
        """.trimIndent()

        val result = validator.parseAndValidate(latexJson)
        assertTrue(result.testedConcept.contains("\\in"))
        assertTrue(result.testedConcept.contains("\\Theta"))
        assertTrue(result.invariantRule!!.contains("\\ge"))
        assertTrue(result.invariantRule!!.contains("\\le"))
        assertTrue(result.invariantRule!!.contains("\\cdot"))
        assertTrue(result.similarPatternHint!!.contains("\\log_b"))
    }
}

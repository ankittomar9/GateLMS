package com.gatelms.anatomy.service

import com.gatelms.anatomy.exception.AnatomyValidationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TopicPrimerValidationTest {

    private val validator = AnatomyValidationService()

    private val validWorkedExample = """
        Let us solve a canonical Relational Algebra problem step-by-step:
        Problem: Given schema R(A, B, C) and S(C, D, E), find all tuples where R.A > 10 and S.D = 'Active'.
        
        Step 1: Understand the base relations. Relation R has attributes A, B, and C with primary key A. Relation S has attributes C, D, and E with primary key C. The natural join R ⨝ S combines tuples that match on attribute C.
        
        Step 2: Apply selections early using heuristic optimization. We push selection condition σ_{A > 10}(R) directly to table R before computing the Cartesian product or join. This drastically reduces intermediate relation cardinality from 100,000 tuples down to 2,500 tuples.
        
        Step 3: Similarly apply selection σ_{D = 'Active'}(S) on relation S, filtering out inactive entities before join evaluation.
        
        Step 4: Perform the theta join on the equality condition R.C = S.C. The resulting schema is (A, B, C, D, E).
        
        Step 5: Project the required target attributes using π_{A, B, E}. Notice that duplicate elimination occurs implicitly under set-theoretic relational algebra semantics. The overall query equivalence holds strictly under relational completeness theorems.
    """.trimIndent()

    @Test
    fun `honest-judge PASS - valid topic primer passes validation`() {
        val payload = TopicPrimerPayload(
            motivation = "Relational Algebra provides the theoretical and algebraic foundation for querying relational databases.",
            coreIdea = "A procedural query language where operations take relations as input and produce relations as output.",
            invariant = "Relational closure property: every relational algebra operation on relations yields a relation.",
            workedExample = validWorkedExample,
            traps = "Confusing relational algebra (set semantics) with SQL (multiset/bag semantics where duplicates are preserved)."
        )

        assertDoesNotThrow {
            validator.validatePrimerPayload(payload)
        }
    }

    @Test
    fun `honest-judge FAIL - worked example under 150 words fails PC-1`() {
        val shallowExample = "Step 1: Do join on R and S. Step 2: Filter by A > 10. Step 3: Project attributes. This is the entire answer."
        val payload = TopicPrimerPayload(
            motivation = "Valid motivation",
            coreIdea = "Valid core idea",
            workedExample = shallowExample
        )

        val ex = assertThrows<AnatomyValidationException> {
            validator.validatePrimerPayload(payload)
        }
        assertTrue(ex.message!!.contains("PC-1 violation: worked_example must be >= 150 words"))
    }

    @Test
    fun `honest-judge FAIL - fabricated student population stats fails AF-1`() {
        val payload = TopicPrimerPayload(
            motivation = "Valid motivation where 85% of students fail to grasp normalization anomalies in exam conditions.",
            coreIdea = "Valid core idea",
            workedExample = validWorkedExample
        )

        val ex = assertThrows<AnatomyValidationException> {
            validator.validatePrimerPayload(payload)
        }
        assertTrue(ex.message!!.contains("AF-1 violation"))
    }

    @Test
    fun `honest-judge FAIL - malformed JSON triggers parse error exception`() {
        val brokenJson = "```json\n{ motivation: 'broken', core_idea: \n```"

        val ex = assertThrows<AnatomyValidationException> {
            validator.parseAndValidatePrimer(brokenJson)
        }
        assertTrue(ex.message!!.contains("Malformed or unparseable JSON"))
    }

    @Test
    fun `honest-judge FAIL - blank motivation or core idea fails`() {
        val payload = TopicPrimerPayload(
            motivation = "",
            coreIdea = "Core idea present",
            workedExample = validWorkedExample
        )

        val ex = assertThrows<AnatomyValidationException> {
            validator.validatePrimerPayload(payload)
        }
        assertTrue(ex.message!!.contains("motivation must not be blank"))
    }
}

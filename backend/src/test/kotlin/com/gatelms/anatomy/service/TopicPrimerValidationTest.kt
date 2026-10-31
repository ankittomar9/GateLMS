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
    fun `honest-judge PASS - mathematical percentages in networking or algorithm domain are permitted`() {
        val mathExample = "$validWorkedExample Step 5: Channel efficiency is calculated as \\eta = 1 / (1 + 2a) = 50% under standard bandwidth-delay conditions. Utilization reaches 80% with selective repeat protocol."
        val payload = TopicPrimerPayload(
            motivation = "Flow control governs data transmission rates to prevent receiver buffer overflow with packet loss below 5 percent.",
            coreIdea = "Sliding window protocol efficiency reaches up to 100% when window size W >= 1 + 2a.",
            workedExample = mathExample
        )

        org.junit.jupiter.api.Assertions.assertDoesNotThrow {
            validator.validatePrimerPayload(payload)
        }
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

    @Test
    fun `parseAndValidatePrimer parses and preserves unescaped LaTeX backslashes without corruption`() {
        // Raw JSON string as produced by an LLM with single-escaped LaTeX commands
        val rawAiResponse = """
            ```json
            {
              "motivation": "Asymptotic analysis characterizes growth as ${'$'}n \to \infty${'$'}. For functions ${'$'}f(n) \in \Theta(g(n))${'$'} where ${'$'}k \in \mathbb{N}${'$'}, it bounds asymptotic complexity.",
              "core_idea": "Formal definition: ${'$'}\{f(n) : \exists c_1, c_2 > 0, n_0 \text{ s.t. } 0 \le c_1 g(n) \le f(n) \le c_2 g(n) \ \forall n \ge n_0\}${'$'}. Notice ${'$'}\frac{a}{b}${'$'} and ${'$'}\beta \neq 0${'$'}.",
              "invariant": "For all ${'$'}n \ge n_0${'$'}, ${'$'}0 \le f(n) \le c \cdot g(n)${'$'}",
              "worked_example": "Canonical Recurrence Walkthrough: Step 1: Consider the recurrence relation ${'$'}T(n) = 2T(n/2) + n${'$'}. Here we identify constants ${'$'}a = 2, b = 2${'$'}, and driving function ${'$'}f(n) = n${'$'}. Step 2: Compute the critical exponent ${'$'}\log_b a = \log_2 2 = 1${'$'}. Comparing the growth rate of the polynomial watershed against the driving function, we observe that ${'$'}f(n) = \Theta(n^{\log_b a}) = \Theta(n^1) = \Theta(n)${'$'}. Because both terms possess identical asymptotic growth orders, Master Theorem Case 2 directly applies without any modifications. Step 3: Conclude that the overall running time complexity evaluates precisely to ${'$'}T(n) = \Theta(n \log n)${'$'}. Step 4: Verification using substitution method. Assume by inductive hypothesis that ${'$'}T(k) \le c k \log k${'$'} holds for all smaller problem sizes ${'$'}k < n${'$'}. Substituting into recurrence yields ${'$'}T(n) = 2c(n/2)\log(n/2) + n = cn\log n - cn\log 2 + n${'$'}. For binary logarithm, ${'$'}\log 2 = 1${'$'}, leading directly to ${'$'}T(n) = cn\log n + (1-c)n${'$'}. Step 5: For any constant choice ${'$'}c \ge 1${'$'}, the trailing linear component satisfies ${'$'}(1-c)n \le 0${'$'}. Thus ${'$'}T(n) \le cn\log n${'$'}, completing the formal mathematical induction for all ${'$'}n \ge 1${'$'} rigorously without loss of generality and confirming tight asymptotic containment.",
              "traps": "Confusing ${'$'}O${'$'} (upper bound) with ${'$'}\Theta${'$'} (tight bound). Writing ${'$'}f(n) = O(g(n))${'$'} is set membership ${'$'}f(n) \in O(g(n))${'$'}."
            }
            ```
        """.trimIndent()

        val parsed = validator.parseAndValidatePrimer(rawAiResponse)
        assertNotNull(parsed)
        // Verify motivation contains intact LaTeX commands (\to, \infty, \in, \Theta, \mathbb)
        assertTrue(parsed.motivation.contains("\\to"))
        assertTrue(parsed.motivation.contains("\\infty"))
        assertTrue(parsed.motivation.contains("\\in"))
        assertTrue(parsed.motivation.contains("\\Theta"))
        assertTrue(parsed.motivation.contains("\\mathbb"))

        // Verify coreIdea contains intact \frac, \le, \text, \forall, \ge, \beta, \neq (NOT transformed into \f formfeed or \t tab or \b backspace)
        assertTrue(parsed.coreIdea.contains("\\frac{a}{b}"))
        assertTrue(parsed.coreIdea.contains("\\le"))
        assertTrue(parsed.coreIdea.contains("\\ge"))
        assertTrue(parsed.coreIdea.contains("\\forall"))
        assertTrue(parsed.coreIdea.contains("\\beta"))
        assertTrue(parsed.coreIdea.contains("\\neq"))
        assertFalse(parsed.coreIdea.contains("\u000c")) // No form feed
        assertFalse(parsed.coreIdea.contains("\u0008")) // No backspace
    }

    @Test
    fun `repairJsonEscapes correctly handles unicode vs latex and legitimate newlines`() {
        val input = """{"text":"Newline:\nNext line. Valid unicode: \u00A0. Latex: \underbrace{x+y} and \to \infty and \neq 0 and \frac{1}{2} and \\alreadyEscaped."}"""
        val repaired = AnatomyValidationService.repairJsonEscapes(input)

        // Standard newline \n and valid \u00A0 preserved
        assertTrue(repaired.contains("\\nNext line"))
        assertTrue(repaired.contains("\\u00A0"))

        // LaTeX commands escaped
        assertTrue(repaired.contains("\\\\underbrace"))
        assertTrue(repaired.contains("\\\\to"))
        assertTrue(repaired.contains("\\\\neq"))
        assertTrue(repaired.contains("\\\\frac"))
        assertTrue(repaired.contains("\\\\alreadyEscaped"))
    }
}

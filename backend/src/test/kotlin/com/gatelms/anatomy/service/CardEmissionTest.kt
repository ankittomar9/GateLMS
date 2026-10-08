package com.gatelms.anatomy.service

import com.gatelms.anatomy.exception.AnatomyValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Honest-judge tests for the WP2 emission contract (PHASE7-M1-ANATOMY-SPEC §6.1, §6.4, §9.6):
 * 2-4 cards per unit, prompt != answer, no yes/no prompts, and the PC-1 worked-example
 * minimum length. Valid samples must PASS; defective samples must FAIL with a message
 * naming the violated rule.
 */
class CardEmissionTest {

    private val validator = AnatomyValidationService()

    private fun card(prompt: String, answer: String) = UnitGenerationCard(prompt = prompt, answer = answer)

    private fun goodCards() = listOf(
        card(
            "State the exact condition under which a decomposition into 3NF is guaranteed to be lossless-join.",
            "The decomposition is lossless iff at least one relation's attributes contain a candidate key of the original relation."
        ),
        card(
            "Walk through the closure computation used to test whether X -> Y holds in relation R.",
            "Start with X, repeatedly apply functional dependencies whose left side is contained in the current closure, add the right side, and fixpoint; check Y is contained in the final closure."
        ),
        card(
            "What happens to the number of comparisons in the worst case when merging k sorted runs with a min-heap instead of pairwise merges?",
            "A k-way merge with a min-heap performs N * log k element moves, versus N * k comparisons for naive pairwise scanning."
        )
    )

    private fun longWorkedExample(): String {
        val words = mutableListOf<String>()
        var n = 1
        while (words.size < 170) {
            words.add("step${n}")
            n++
        }
        return words.joinToString(" ")
    }

    private fun validPayload() = UnitGenerationPayload(
        title = "Lossless-join decomposition",
        motivation = "Why losslessness matters.",
        coreIdea = "A decomposition preserves information iff the original relation is reconstructible.",
        invariant = "R1 join R2 = R iff (R1 ∩ R2) -> R1 or (R1 ∩ R2) -> R2.",
        workedExample = longWorkedExample(),
        cards = goodCards()
    )

    @Test
    fun `1 Valid 3-card active-recall emission passes`() {
        validator.validateEmissionCards(goodCards()) // must not throw
        validator.validateConceptUnitPayload(validPayload()) // must not throw
    }

    @Test
    fun `2 Valid 2-card and 4-card emissions pass at the boundaries`() {
        validator.validateEmissionCards(goodCards().take(2))
        validator.validateEmissionCards(goodCards() + card(
            "Explain why the heap property alone does not make heapsort stable.",
            "Equal keys can swap relative order because heap operations compare only values, never original positions."
        ))
    }

    @Test
    fun `3 Five cards are rejected`() {
        val five = goodCards() + listOf(
            card(
                "Explain the role of the sentinel in heap arrays.",
                "The root sits at index 1 so parent and child indices are simple integer arithmetic."
            ),
            card(
                "Contrast the amortized cost of dynamic array append with the worst case.",
                "Amortized O(1) via doubling despite occasional O(n) reallocations."
            )
        )
        assertEquals(5, five.size)
        try {
            validator.validateEmissionCards(five)
            throw AssertionError("Expected rejection for 5 cards")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("between 2 and 4"))
        }
    }

    @Test
    fun `4 One card is rejected`() {
        try {
            validator.validateEmissionCards(goodCards().take(1))
            throw AssertionError("Expected rejection for 1 card")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("between 2 and 4"))
        }
    }

    @Test
    fun `5 Zero cards are rejected`() {
        try {
            validator.validateEmissionCards(emptyList())
            throw AssertionError("Expected rejection for 0 cards")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("between 2 and 4"))
        }
    }

    @Test
    fun `6 Prompt equal to answer is rejected`() {
        val cards = listOf(
            card("The invariant is X -> Y", "The invariant is X -> Y"),
            card("Unrelated valid prompt", "A distinct, valid answer.")
        )
        try {
            validator.validateEmissionCards(cards)
            throw AssertionError("Expected rejection for prompt == answer")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("prompt and answer must differ"))
        }
    }

    @Test
    fun `7 Yes-or-no prompts are rejected`() {
        val cards = listOf(
            card("Is a lossless decomposition always dependency-preserving?", "No, the two properties are independent."),
            card("Valid prompt two", "Valid answer two.")
        )
        try {
            validator.validateEmissionCards(cards)
            throw AssertionError("Expected rejection for yes/no prompt")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("yes/no prompts are forbidden"))
        }
    }

    @Test
    fun `8 Bare yes-no answers are rejected`() {
        val cards = listOf(
            card("Does BCNF imply 3NF?", "yes"),
            card("Valid prompt two", "Valid answer two.")
        )
        try {
            validator.validateEmissionCards(cards)
            throw AssertionError("Expected rejection for yes/no answer")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("yes/no answers are forbidden"))
        }
    }

    @Test
    fun `9 Worked example under 150 words fails PC-1`() {
        val short = validPayload().copy(workedExample = "Take R(A,B,C). Decompose into R1 and R2. Join them back. Done.")
        assertEquals(11, short.workedExample.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size)
        try {
            validator.validateConceptUnitPayload(short)
            throw AssertionError("Expected PC-1 rejection for a summary-length worked example")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("PC-1 violation"))
            assertTrue(e.message!!.contains(">= 150 words"))
        }
    }

    @Test
    fun `10 Blank unit fields fail validation`() {
        val blank = validPayload().copy(title = "", motivation = " ", coreIdea = "")
        try {
            validator.validateConceptUnitPayload(blank)
            throw AssertionError("Expected rejection for blank unit fields")
        } catch (e: AnatomyValidationException) {
            assertTrue(e.message!!.contains("title must not be blank"))
            assertTrue(e.message!!.contains("motivation must not be blank"))
            assertTrue(e.message!!.contains("core_idea must not be blank"))
        }
    }
}

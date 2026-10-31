package com.gatelms.anatomy.service

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gatelms.anatomy.dto.AnatomyJsonPayload
import com.gatelms.anatomy.exception.AnatomyValidationException
import org.springframework.stereotype.Service

@Service
class AnatomyValidationService(
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
) {
    private val slugRegex = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private val populationStatRegex = Regex(
        "(?i)(\\d+\\s*(%|percent)\\s+(of\\s+)?(students|candidates|aspirants|test-takers|examinees|cohort|learners)|" +
        "\\b(majority|minority|most|many)\\s+of\\s+(students|candidates|aspirants|test-takers|examinees)\\b|" +
        "\\b(students|candidates|aspirants|test-takers|examinees)\\s+[^.\\n]{0,60}\\b\\d+\\s*(%|percent))"
    )
    private val bookCitationRegex = Regex("(?i)\\b(page\\s+\\d+|ch(apter)?\\s*\\d+|section\\s+\\d+\\.\\d+)\\b")

    fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        val candidate = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed
        } else {
            val codeFenceMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE).find(trimmed)
            if (codeFenceMatch != null) {
                codeFenceMatch.groupValues[1].trim()
            } else {
                val firstBrace = trimmed.indexOf('{')
                val lastBrace = trimmed.lastIndexOf('}')
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    trimmed.substring(firstBrace, lastBrace + 1)
                } else {
                    trimmed
                }
            }
        }
        return repairJsonEscapes(candidate)
    }

    fun parseAndValidate(rawResponse: String): AnatomyJsonPayload {
        val jsonText = extractJson(rawResponse)
        val payload = try {
            objectMapper.readValue(jsonText, AnatomyJsonPayload::class.java)
        } catch (e: Exception) {
            throw AnatomyValidationException("Malformed or unparseable JSON: ${e.message}")
        }

        validate(payload)
        return payload
    }

    fun validate(payload: AnatomyJsonPayload) {
        val errors = mutableListOf<String>()

        // 1. tested_concept rules
        if (payload.testedConcept.isBlank()) {
            errors.add("tested_concept must not be blank")
        } else if (payload.testedConcept.length > 300) {
            errors.add("tested_concept must be <= 300 characters (actual: ${payload.testedConcept.length})")
        }

        // 2. AF-1: Anti-fabrication of student-population statistics
        val allText = buildString {
            append(payload.testedConcept).append(" ")
            payload.invariantRule?.let { append(it).append(" ") }
            payload.similarPatternHint?.let { append(it).append(" ") }
            for (trap in payload.trapAnalysis) {
                append(trap.whyPlausible).append(" ")
                append(trap.misconception).append(" ")
            }
        }

        if (populationStatRegex.containsMatchIn(allText)) {
            errors.add("AF-1 violation: dossier contains fabricated student-population statistics or percentage claims")
        }

        // 3. AF-3: No textbook or page citations in M1
        if (bookCitationRegex.containsMatchIn(allText)) {
            errors.add("AF-3 violation: dossier contains textbook citations or page numbers (forbidden in M1)")
        }

        // 4. AF-5: concept_tags 1 to 4 slugs, kebab-case regex
        if (payload.conceptTags.isEmpty()) {
            errors.add("AF-5 violation: at least 1 concept_tag is required")
        } else if (payload.conceptTags.size > 4) {
            errors.add("AF-5 violation: at most 4 concept_tags allowed (actual: ${payload.conceptTags.size})")
        } else {
            for (tag in payload.conceptTags) {
                if (!slugRegex.matches(tag)) {
                    errors.add("AF-5 violation: invalid concept_tag slug '$tag' (must match ^[a-z0-9]+(-[a-z0-9]+)*$)")
                }
            }
        }

        // 5. Prerequisites validation
        for (prereq in payload.prerequisites) {
            if (prereq.conceptSlug.isNotBlank() && !slugRegex.matches(prereq.conceptSlug)) {
                errors.add("Invalid prerequisite concept_slug '${prereq.conceptSlug}' (must be kebab-case)")
            }
            if (prereq.conceptLabel.isBlank()) {
                errors.add("Prerequisite concept_label must not be blank")
            }
        }

        if (errors.isNotEmpty()) {
            throw AnatomyValidationException(errors.joinToString("; "))
        }
    }

    // ---- WP2: Concept Unit + retrieval card emission validation (SPEC §6.1, §6.4) ----

    private val yesNoPromptRegex =
        Regex("(?i)^(is|are|was|were|do|does|did|can|could|will|would|should|shall|has|have|had)\\b[^?]*\\?\\s*$")
    private val yesNoAnswerSet = setOf("yes", "no", "true", "false", "y", "n")

    /**
     * PC-1 binding (SPEC §6.1): a concept unit is a teaching artifact, not a summary.
     * The differentiator is the worked example — the validator rejects anything under
     * 150 words, because a summary cannot contain a worked example. Also validates the
     * 2-4 active-recall card emission contract.
     */
    fun validateConceptUnitPayload(payload: UnitGenerationPayload) {
        val errors = mutableListOf<String>()

        if (payload.title.isBlank()) {
            errors.add("title must not be blank")
        }
        if (payload.motivation.isBlank()) {
            errors.add("motivation must not be blank")
        }
        if (payload.coreIdea.isBlank()) {
            errors.add("core_idea must not be blank")
        }

        val wordCount = payload.workedExample.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount < WORKED_EXAMPLE_MIN_WORDS) {
            errors.add(
                "PC-1 violation: worked_example must be >= $WORKED_EXAMPLE_MIN_WORDS words (actual: $wordCount). " +
                    "A concept unit cannot be a shallow summary."
            )
        }

        validateEmissionCardsInternal(payload.cards, errors)

        if (errors.isNotEmpty()) {
            throw AnatomyValidationException(errors.joinToString("; "))
        }
    }

    /**
     * Card emission gate (SPEC §6.4): 2-4 cards, prompt != answer, no yes/no prompts.
     * Prompts must force active recall — statement-recognition and binary questions are
     * rejected outright.
     */
    fun validateEmissionCards(cards: List<UnitGenerationCard>) {
        val errors = mutableListOf<String>()
        validateEmissionCardsInternal(cards, errors)
        if (errors.isNotEmpty()) {
            throw AnatomyValidationException(errors.joinToString("; "))
        }
    }

    private fun validateEmissionCardsInternal(cards: List<UnitGenerationCard>, errors: MutableList<String>) {
        if (cards.size !in 2..4) {
            errors.add("Unit must emit between 2 and 4 retrieval cards (actual: ${cards.size})")
        }
        for ((idx, card) in cards.withIndex()) {
            val prompt = card.prompt.trim()
            val answer = card.answer.trim()
            if (prompt.isBlank() || answer.isBlank()) {
                errors.add("Card ${idx + 1}: prompt and answer must not be blank")
                continue
            }
            if (prompt.equals(answer, ignoreCase = true)) {
                errors.add("Card ${idx + 1}: prompt and answer must differ (active recall, not recognition)")
            }
            if (yesNoPromptRegex.matches(prompt)) {
                errors.add("Card ${idx + 1}: yes/no prompts are forbidden (the prompt must force active recall)")
            }
            if (answer.lowercase() in yesNoAnswerSet) {
                errors.add("Card ${idx + 1}: yes/no answers are forbidden (the answer must state the mechanism)")
            }
        }
    }

    // ---- WP2: Topic Primer validation (SPEC §6.2) ----

    fun parseAndValidatePrimer(rawResponse: String): TopicPrimerPayload {
        val jsonText = extractJson(rawResponse)
        val payload = try {
            objectMapper.readValue(jsonText, TopicPrimerPayload::class.java)
        } catch (e: Exception) {
            throw AnatomyValidationException("Malformed or unparseable JSON: ${e.message}")
        }
        validatePrimerPayload(payload)
        return payload
    }

    fun validatePrimerPayload(payload: TopicPrimerPayload) {
        val errors = mutableListOf<String>()

        if (payload.motivation.isBlank()) {
            errors.add("motivation must not be blank")
        }
        if (payload.coreIdea.isBlank()) {
            errors.add("core_idea must not be blank")
        }

        val wordCount = payload.workedExample.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        if (wordCount < WORKED_EXAMPLE_MIN_WORDS) {
            errors.add(
                "PC-1 violation: worked_example must be >= $WORKED_EXAMPLE_MIN_WORDS words (actual: $wordCount). " +
                    "A topic primer cannot be a shallow summary."
            )
        }

        // AF-1: Anti-fabrication of student-population statistics
        val allText = buildString {
            append(payload.motivation).append(" ")
            append(payload.coreIdea).append(" ")
            payload.invariant?.let { append(it).append(" ") }
            append(payload.workedExample).append(" ")
            payload.traps?.let { append(it).append(" ") }
        }

        if (populationStatRegex.containsMatchIn(allText)) {
            errors.add("AF-1 violation: primer contains fabricated student-population statistics or percentage claims")
        }

        if (errors.isNotEmpty()) {
            throw AnatomyValidationException(errors.joinToString("; "))
        }
    }

    companion object {
        const val WORKED_EXAMPLE_MIN_WORDS = 150

        /**
         * Repairs unescaped LaTeX backslashes inside JSON string literals while preserving
         * valid JSON escapes (\", \\, \/, \uXXXX, and standard control escapes \n, \r, \t, \b, \f).
         *
         * When LLMs emit LaTeX formulas (e.g. $n \in \mathbb{N}$, $n \to \infty$, $f(n) \le c \cdot g(n)$)
         * inside JSON string fields, they frequently omit the second backslash.
         * RFC 8259 forbids unescaped backslashes such as \i, \l, \g, \c, \O, \T, etc.
         * Furthermore, standard JSON parsers turn \frac into <formfeed>rac and \to into <tab>o,
         * destroying math expressions.
         *
         * This scanner detects string literal boundaries and converts single-escaped LaTeX commands
         * into valid double-escaped JSON (\\in, \\to, \\frac, \\le, etc.), preserving exact LaTeX syntax
         * for downstream KaTeX rendering.
         */
        fun repairJsonEscapes(json: String): String {
            if (json.isEmpty()) return json
            val sb = StringBuilder(json.length + 64)
            var inString = false
            var i = 0
            val len = json.length

            while (i < len) {
                val c = json[i]
                if (!inString) {
                    if (c == '"') {
                        inString = true
                    }
                    sb.append(c)
                    i++
                } else {
                    if (c == '"') {
                        inString = false
                        sb.append(c)
                        i++
                    } else if (c == '\\') {
                        if (i + 1 >= len) {
                            // Trailing backslash
                            sb.append("\\\\")
                            i++
                            continue
                        }
                        val next = json[i + 1]
                        when {
                            // Valid standard JSON escapes that must remain intact
                            next == '"' || next == '\\' || next == '/' -> {
                                sb.append('\\')
                                sb.append(next)
                                i += 2
                            }
                            // Unicode escape \uXXXX vs LaTeX commands starting with u (\underbrace, \uparrow, etc.)
                            next == 'u' -> {
                                val isHex4 = (i + 5 < len) &&
                                    json[i + 2].isHexDigit() &&
                                    json[i + 3].isHexDigit() &&
                                    json[i + 4].isHexDigit() &&
                                    json[i + 5].isHexDigit()
                                if (isHex4) {
                                    sb.append(json, i, i + 6)
                                    i += 6
                                } else {
                                    sb.append("\\\\u")
                                    i += 2
                                }
                            }
                            // Form feed \f vs LaTeX \frac, \forall, \flat, \footnote, etc.
                            next == 'f' -> {
                                if (json.getOrNull(i + 2).isAsciiLetter()) {
                                    sb.append("\\\\f")
                                } else {
                                    sb.append("\\f")
                                }
                                i += 2
                            }
                            // Backspace \b vs LaTeX \beta, \binom, \mathbf, \boldsymbol, \bar, \bmod, etc.
                            next == 'b' -> {
                                if (json.getOrNull(i + 2).isAsciiLetter()) {
                                    sb.append("\\\\b")
                                } else {
                                    sb.append("\\b")
                                }
                                i += 2
                            }
                            // Tab \t vs LaTeX commands (\to, \text, \times, \theta, \tau, \tan, etc.)
                            next == 't' -> {
                                if (isLatexTCommand(json, i + 1)) {
                                    sb.append("\\\\t")
                                } else {
                                    sb.append("\\t")
                                }
                                i += 2
                            }
                            // Newline \n vs LaTeX commands (\neq, \ne, \notin, \nabla, etc.)
                            next == 'n' -> {
                                if (isLatexNCommand(json, i + 1)) {
                                    sb.append("\\\\n")
                                } else {
                                    sb.append("\\n")
                                }
                                i += 2
                            }
                            // Carriage return \r vs LaTeX commands (\rho, \right, \rangle, etc.)
                            next == 'r' -> {
                                if (isLatexRCommand(json, i + 1)) {
                                    sb.append("\\\\r")
                                } else {
                                    sb.append("\\r")
                                }
                                i += 2
                            }
                            // Any other character after backslash is an invalid escape in JSON (e.g. \i, \l, \g, \c, \O, \T, \{, \}, etc.)
                            else -> {
                                sb.append("\\\\")
                                sb.append(next)
                                i += 2
                            }
                        }
                    } else {
                        sb.append(c)
                        i++
                    }
                }
            }
            return sb.toString()
        }

        private fun Char.isHexDigit(): Boolean =
            this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

        private fun Char?.isAsciiLetter(): Boolean =
            this != null && ((this in 'a'..'z') || (this in 'A'..'Z'))

        private fun isLatexTCommand(json: String, startIdx: Int): Boolean {
            val sub = json.substring(startIdx)
            return when {
                sub.startsWith("text") -> true
                sub.startsWith("times") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("theta") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("tau") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("tanh") && !sub.getOrNull(4).isAsciiLetter() -> true
                sub.startsWith("tan") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("top") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("tilde") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("tfrac") -> true
                sub.startsWith("triangle") && !sub.getOrNull(8).isAsciiLetter() -> true
                sub.startsWith("therefore") && !sub.getOrNull(9).isAsciiLetter() -> true
                sub.startsWith("tag") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("to") && !sub.getOrNull(2).isAsciiLetter() -> true
                else -> false
            }
        }

        private fun isLatexNCommand(json: String, startIdx: Int): Boolean {
            val sub = json.substring(startIdx)
            return when {
                sub.startsWith("neq") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("notin") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("nabla") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("natural") && !sub.getOrNull(7).isAsciiLetter() -> true
                sub.startsWith("neg") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("not") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("nu") && !sub.getOrNull(2).isAsciiLetter() -> true
                sub.startsWith("ne") && !sub.getOrNull(2).isAsciiLetter() -> true
                else -> false
            }
        }

        private fun isLatexRCommand(json: String, startIdx: Int): Boolean {
            val sub = json.substring(startIdx)
            return when {
                sub.startsWith("rightarrow") && !sub.getOrNull(10).isAsciiLetter() -> true
                sub.startsWith("Rightarrow") && !sub.getOrNull(10).isAsciiLetter() -> true
                sub.startsWith("right") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("rho") && !sub.getOrNull(3).isAsciiLetter() -> true
                sub.startsWith("rangle") && !sub.getOrNull(6).isAsciiLetter() -> true
                sub.startsWith("rceil") && !sub.getOrNull(5).isAsciiLetter() -> true
                sub.startsWith("rfloor") && !sub.getOrNull(6).isAsciiLetter() -> true
                sub.startsWith("real") && !sub.getOrNull(4).isAsciiLetter() -> true
                else -> false
            }
        }
    }
}

data class TopicPrimerPayload(
    @com.fasterxml.jackson.annotation.JsonProperty("motivation") val motivation: String = "",
    @com.fasterxml.jackson.annotation.JsonProperty("core_idea") val coreIdea: String = "",
    @com.fasterxml.jackson.annotation.JsonProperty("invariant") val invariant: String? = null,
    @com.fasterxml.jackson.annotation.JsonProperty("worked_example") val workedExample: String = "",
    @com.fasterxml.jackson.annotation.JsonProperty("traps") val traps: String? = null
)


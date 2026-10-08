package com.gatelms.anatomy.service

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
) {
    private val slugRegex = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private val populationStatRegex = Regex("(?i)(\\d+\\s*%|\\d+\\s*percent|\\b(majority|minority|most|many)\\s+of\\s+(students|candidates|aspirants|test-takers|examinees)\\b)")
    private val bookCitationRegex = Regex("(?i)\\b(page\\s+\\d+|ch(apter)?\\s*\\d+|section\\s+\\d+\\.\\d+)\\b")

    fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        val codeFenceMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```").find(trimmed)
        if (codeFenceMatch != null) {
            return codeFenceMatch.groupValues[1].trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
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
}

package com.gatelms.anatomy.service

import com.gatelms.entity.Question
import org.springframework.stereotype.Component

@Component
class TopicGroundingSampler {

    /**
     * Performs round-robin stratified sampling across (type x difficulty) up to [limit] questions.
     */
    fun stratifySample(questions: List<Question>, limit: Int): List<Question> {
        if (questions.size <= limit) {
            return questions
        }

        // Group by (type, difficulty ?: "MEDIUM")
        val groups = questions.groupBy { Pair(it.type, it.difficulty ?: "MEDIUM") }
        val result = mutableListOf<Question>()
        var round = 0

        while (result.size < limit) {
            var addedInRound = false
            for ((_, group) in groups) {
                if (round < group.size && result.size < limit) {
                    result.add(group[round])
                    addedInRound = true
                }
            }
            if (!addedInRound) break
            round++
        }

        return result
    }

    /**
     * Formats a single grounding question statement up to [maxChars] characters (default 1500).
     */
    fun formatQuestionSnippet(index: Int, question: Question, maxChars: Int = 1500): String {
        val yr = question.year?.let { "$it " } ?: ""
        val diff = question.difficulty?.let { " - $it" } ?: ""
        val ext = question.externalId?.let { " [$it]" } ?: ""
        val stmt = question.statement.take(maxChars).trim()
        return "$index. [$yr${question.type}$diff]$ext $stmt"
    }
}

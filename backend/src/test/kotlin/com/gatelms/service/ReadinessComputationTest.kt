package com.gatelms.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReadinessComputationTest {

    @Test
    fun `hand-computable fixture validates exact readiness formula`() {
        val s1Topics = listOf(
            TopicData(id = "t1", masteryLevel = 4, revised = true),
            TopicData(id = "t2", masteryLevel = 2, revised = true)
        )
        val s1 = SubjectData(id = "s1", name = "Algorithms", weight = 10.0, topics = s1Topics)

        val s2Topics = listOf(
            TopicData(id = "t3", masteryLevel = 5, revised = true)
        )
        val s2 = SubjectData(id = "s2", name = "Operating Systems", weight = 10.0, topics = s2Topics)

        val verdicts = listOf(
            QuestionVerdict(questionId = "q1", subjectId = "s1", topicId = "t1", isCorrect = true),
            QuestionVerdict(questionId = "q2", subjectId = "s1", topicId = "t1", isCorrect = true),
            QuestionVerdict(questionId = "q3", subjectId = "s1", topicId = "t2", isCorrect = true),
            QuestionVerdict(questionId = "q4", subjectId = "s1", topicId = "t2", isCorrect = false),
            QuestionVerdict(questionId = "q5", subjectId = "s2", topicId = "t3", isCorrect = true),
            QuestionVerdict(questionId = "q6", subjectId = "s2", topicId = "t3", isCorrect = false)
        )

        val response = ReadinessCalculationEngine.compute(listOf(s1, s2), verdicts)

        // Hand calculation check:
        // s1: M = (4/5 + 2/5)/2 = 0.60, A = 3/4 = 0.75, C = 2/2 = 1.0
        // s1 R = 0.5*0.60 + 0.3*0.75 + 0.2*1.0 = 0.30 + 0.225 + 0.20 = 0.725 (72.5%)
        val s1Dto = response.perSubject.first { it.subjectId == "s1" }
        assertEquals(0.60, s1Dto.m, 0.001)
        assertEquals(0.75, s1Dto.a, 0.001)
        assertEquals(1.0, s1Dto.c, 0.001)
        assertEquals(72.5, s1Dto.readiness, 0.1)

        // s2: M = (5/5)/1 = 1.0, A = 1/2 = 0.50, C = 1/1 = 1.0
        // s2 R = 0.5*1.0 + 0.3*0.50 + 0.2*1.0 = 0.50 + 0.15 + 0.20 = 0.85 (85.0%)
        val s2Dto = response.perSubject.first { it.subjectId == "s2" }
        assertEquals(1.0, s2Dto.m, 0.001)
        assertEquals(0.50, s2Dto.a, 0.001)
        assertEquals(1.0, s2Dto.c, 0.001)
        assertEquals(85.0, s2Dto.readiness, 0.1)

        // Total weighted R = (10*0.725 + 10*0.85)/20 = 15.75/20 = 0.7875 -> round(78.75) = 79
        assertEquals(79, response.readiness)
    }

    @Test
    fun `inactive subject with zero activity contributes exactly zero`() {
        val activeTopics = listOf(TopicData(id = "t1", masteryLevel = 5, revised = true))
        val activeSubj = SubjectData(id = "s_active", name = "Active", weight = 10.0, topics = activeTopics)

        val inactiveTopics = listOf(TopicData(id = "t2", masteryLevel = 0, revised = false))
        val inactiveSubj = SubjectData(id = "s_inactive", name = "Inactive", weight = 10.0, topics = inactiveTopics)

        val verdicts = listOf(
            QuestionVerdict(questionId = "q1", subjectId = "s_active", topicId = "t1", isCorrect = true)
        )

        val response = ReadinessCalculationEngine.compute(listOf(activeSubj, inactiveSubj), verdicts)

        val inactiveDto = response.perSubject.first { it.subjectId == "s_inactive" }
        assertEquals(0.0, inactiveDto.m)
        assertEquals(0.0, inactiveDto.a)
        assertEquals(0.0, inactiveDto.c)
        assertEquals(0.0, inactiveDto.readiness)

        // Active subject: M=1.0, A=1.0, C=1.0 -> R=1.0. Overall = (10*1.0 + 10*0.0)/20 = 50%
        assertEquals(50, response.readiness)
    }

    @Test
    fun `clamping components prevents values beyond zero and one`() {
        val topics = listOf(TopicData(id = "t1", masteryLevel = 5, revised = true))
        val subj = SubjectData(id = "s1", name = "Math", weight = 5.0, topics = topics)
        val verdicts = listOf(
            QuestionVerdict(questionId = "q1", subjectId = "s1", topicId = "t1", isCorrect = true)
        )

        val response = ReadinessCalculationEngine.compute(listOf(subj), verdicts)
        val dto = response.perSubject.first()

        assertTrue(dto.m in 0.0..1.0)
        assertTrue(dto.a in 0.0..1.0)
        assertTrue(dto.c in 0.0..1.0)
        assertTrue(dto.readiness in 0.0..100.0)
        assertTrue(response.readiness in 0..100)
    }

    @Test
    fun `rounding logic scales and rounds correctly`() {
        // Construct 1 subject with a fractional result that tests half-up rounding
        val topics = listOf(TopicData(id = "t1", masteryLevel = 3, revised = true)) // M = 3/5 = 0.6
        val subj = SubjectData(id = "s1", name = "Discrete Math", weight = 10.0, topics = topics)
        // 3 questions, 1 correct: A = 1/3 = 0.3333...
        // Coverage = 1/1 = 1.0
        // R = 0.5*0.6 + 0.3*(1/3) + 0.2*1.0 = 0.3 + 0.1 + 0.2 = 0.6 -> 60%
        val verdicts = listOf(
            QuestionVerdict(questionId = "q1", subjectId = "s1", topicId = "t1", isCorrect = true),
            QuestionVerdict(questionId = "q2", subjectId = "s1", topicId = "t1", isCorrect = false),
            QuestionVerdict(questionId = "q3", subjectId = "s1", topicId = "t1", isCorrect = false)
        )

        val response = ReadinessCalculationEngine.compute(listOf(subj), verdicts)
        assertEquals(60, response.readiness)
    }

    @Test
    fun `empty bank or zero subjects returns zero readiness with zero components`() {
        val response = ReadinessCalculationEngine.compute(emptyList(), emptyList())
        assertEquals(0, response.readiness)
        assertTrue(response.perSubject.isEmpty())
    }

    @Test
    fun `perfect subject yields 100 percent readiness`() {
        val topics = listOf(TopicData(id = "t1", masteryLevel = 5, revised = true))
        val subj = SubjectData(id = "s1", name = "Theory of Computation", weight = 10.0, topics = topics)
        val verdicts = listOf(
            QuestionVerdict(questionId = "q1", subjectId = "s1", topicId = "t1", isCorrect = true)
        )

        val response = ReadinessCalculationEngine.compute(listOf(subj), verdicts)
        assertEquals(100, response.readiness)
        val dto = response.perSubject.first()
        assertEquals(1.0, dto.m)
        assertEquals(1.0, dto.a)
        assertEquals(1.0, dto.c)
        assertEquals(100.0, dto.readiness)
    }
}

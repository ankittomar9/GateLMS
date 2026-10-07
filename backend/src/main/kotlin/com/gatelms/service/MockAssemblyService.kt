package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.dto.BlueprintSection
import com.gatelms.dto.MockAttemptSectionDto
import com.gatelms.dto.MockBlueprint
import com.gatelms.entity.Question
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class AssembledMock(
    val sections: List<MockAttemptSectionDto>,
    val questions: List<Question>,
    val shortfallWarnings: List<String>
)

@Service
class MockAssemblyService(
    private val subjectRepository: SubjectRepository,
    private val questionRepository: QuestionRepository,
    private val objectMapper: ObjectMapper
) {

    fun parseBlueprint(blueprintJson: String): MockBlueprint {
        return try {
            objectMapper.readValue(blueprintJson, MockBlueprint::class.java)
        } catch (e: Exception) {
            MockBlueprint()
        }
    }

    fun assembleMock(blueprint: MockBlueprint): AssembledMock {
        val sectionsDtoList = mutableListOf<MockAttemptSectionDto>()
        val assembledQuestions = mutableListOf<Question>()
        val shortfallWarnings = mutableListOf<String>()
        val seenQuestionIds = mutableSetOf<UUID>()

        val filterCuration = !blueprint.questionPool?.curationStatus.isNullOrEmpty()
        val curationStatuses = blueprint.questionPool?.curationStatus ?: emptyList()

        val filterDifficulty = !blueprint.questionPool?.difficulty.isNullOrEmpty()
        val difficulties = blueprint.questionPool?.difficulty ?: emptyList()

        val targetYear = blueprint.year ?: blueprint.questionPool?.year
        val targetSource = blueprint.source ?: blueprint.questionPool?.source

        for (section in blueprint.sections) {
            val subject = subjectRepository.findById(section.subjectId).orElseThrow {
                ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid subjectId in blueprint section: ${section.subjectId}")
            }

            val sectionQuestions = mutableListOf<Question>()

            // 1. Gather MCQs
            if (section.mcq > 0) {
                val mcqs = gatherQuestions(
                    subjectId = section.subjectId,
                    type = "MCQ",
                    requestedCount = section.mcq,
                    marks = section.marks,
                    year = targetYear,
                    source = targetSource,
                    filterCuration = filterCuration,
                    curationStatuses = curationStatuses,
                    filterDifficulty = filterDifficulty,
                    difficulties = difficulties,
                    excludeIds = seenQuestionIds
                )
                if (mcqs.size < section.mcq) {
                    shortfallWarnings.add("Section '${subject.name}' requested ${section.mcq} MCQ but only ${mcqs.size} available in pool")
                }
                sectionQuestions.addAll(mcqs)
                seenQuestionIds.addAll(mcqs.mapNotNull { it.id })
            }

            // 2. Gather MSQs
            if (section.msq > 0) {
                val msqs = gatherQuestions(
                    subjectId = section.subjectId,
                    type = "MSQ",
                    requestedCount = section.msq,
                    marks = section.marks,
                    year = targetYear,
                    source = targetSource,
                    filterCuration = filterCuration,
                    curationStatuses = curationStatuses,
                    filterDifficulty = filterDifficulty,
                    difficulties = difficulties,
                    excludeIds = seenQuestionIds
                )
                if (msqs.size < section.msq) {
                    shortfallWarnings.add("Section '${subject.name}' requested ${section.msq} MSQ but only ${msqs.size} available in pool")
                }
                sectionQuestions.addAll(msqs)
                seenQuestionIds.addAll(msqs.mapNotNull { it.id })
            }

            // 3. Gather NATs
            if (section.nat > 0) {
                val nats = gatherQuestions(
                    subjectId = section.subjectId,
                    type = "NAT",
                    requestedCount = section.nat,
                    marks = section.marks,
                    year = targetYear,
                    source = targetSource,
                    filterCuration = filterCuration,
                    curationStatuses = curationStatuses,
                    filterDifficulty = filterDifficulty,
                    difficulties = difficulties,
                    excludeIds = seenQuestionIds
                )
                if (nats.size < section.nat) {
                    shortfallWarnings.add("Section '${subject.name}' requested ${section.nat} NAT but only ${nats.size} available in pool")
                }
                sectionQuestions.addAll(nats)
                seenQuestionIds.addAll(nats.mapNotNull { it.id })
            }

            if (blueprint.shuffle) {
                sectionQuestions.shuffle()
            } else {
                sectionQuestions.sortBy { it.marks }
            }

            sectionsDtoList.add(
                MockAttemptSectionDto(
                    subjectId = subject.id,
                    name = subject.name,
                    questionIds = sectionQuestions.mapNotNull { it.id }
                )
            )
            assembledQuestions.addAll(sectionQuestions)
        }

        return AssembledMock(
            sections = sectionsDtoList,
            questions = assembledQuestions,
            shortfallWarnings = shortfallWarnings
        )
    }

    private fun gatherQuestions(
        subjectId: String,
        type: String,
        requestedCount: Int,
        marks: Int?,
        year: Int?,
        source: String?,
        filterCuration: Boolean,
        curationStatuses: List<String>,
        filterDifficulty: Boolean,
        difficulties: List<String>,
        excludeIds: Set<UUID>
    ): List<Question> {
        val candidates = mutableListOf<Question>()

        // First attempt with exact marks if specified
        if (marks != null) {
            val list = questionRepository.findCandidatesForAssembly(
                subjectId = subjectId,
                type = type,
                marks = marks,
                year = year,
                source = source,
                filterCuration = filterCuration,
                curationStatuses = if (filterCuration) curationStatuses else listOf("DUMMY"),
                filterDifficulty = filterDifficulty,
                difficulties = if (filterDifficulty) difficulties else listOf("DUMMY"),
                limit = requestedCount * 2
            ).filter { it.id != null && !excludeIds.contains(it.id!!) }

            candidates.addAll(list.take(requestedCount))
        }

        // If not enough, query with marks = null
        if (candidates.size < requestedCount) {
            val needed = requestedCount - candidates.size
            val alreadySelectedIds = candidates.mapNotNull { it.id }.toSet() + excludeIds

            val fallback = questionRepository.findCandidatesForAssembly(
                subjectId = subjectId,
                type = type,
                marks = null,
                year = year,
                source = source,
                filterCuration = filterCuration,
                curationStatuses = if (filterCuration) curationStatuses else listOf("DUMMY"),
                filterDifficulty = filterDifficulty,
                difficulties = if (filterDifficulty) difficulties else listOf("DUMMY"),
                limit = requestedCount * 3
            ).filter { it.id != null && !alreadySelectedIds.contains(it.id!!) }

            candidates.addAll(fallback.take(needed))
        }

        return candidates
    }
}

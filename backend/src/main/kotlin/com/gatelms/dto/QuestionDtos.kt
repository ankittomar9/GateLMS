package com.gatelms.dto

import com.gatelms.entity.Pass1Status
import com.gatelms.entity.Pass2Status
import com.gatelms.entity.QuestionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class QuestionSummaryDto(
    val id: UUID,
    val externalId: String?,
    val subjectId: String?,
    val subjectName: String?,
    val topicId: String?,
    val topicName: String?,
    val year: Int?,
    val type: QuestionType,
    val marks: Int,
    val statement: String,
    val pass1Status: Pass1Status,
    val pass2Status: Pass2Status,
    val isBookmarked: Boolean,
    val source: String = "SAMPLE",
    val curationStatus: String = "UNREVIEWED",
    val curationNote: String? = null,
    val difficulty: String? = null,
    val createdAt: Instant
)

data class QuestionDto(
    val id: UUID,
    val externalId: String?,
    val subjectId: String?,
    val subjectName: String?,
    val topicId: String?,
    val topicName: String?,
    val year: Int?,
    val type: QuestionType,
    val marks: Int,
    val statement: String,
    val optionsJson: String?,
    val correctOptions: String?,
    val numericMin: Double?,
    val numericMax: Double?,
    val explanation: String?,
    val gateOverflowUrl: String?,
    val pass1Status: Pass1Status,
    val pass2Status: Pass2Status,
    val isBookmarked: Boolean,
    val source: String = "SAMPLE",
    val curationStatus: String = "UNREVIEWED",
    val curationNote: String? = null,
    val difficulty: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val version: Int
)

data class QuestionUpsertDto(
    val externalId: String? = null,
    @field:NotBlank(message = "subjectId is required")
    val subjectId: String,
    @field:NotBlank(message = "topicId is required")
    val topicId: String,
    val year: Int? = null,
    @field:NotNull(message = "type is required")
    val type: QuestionType,
    val marks: Int = 1,
    @field:NotBlank(message = "statement is required")
    val statement: String,
    val optionsJson: String? = null,
    val correctOptions: String? = null,
    val numericMin: Double? = null,
    val numericMax: Double? = null,
    val explanation: String? = null,
    val gateOverflowUrl: String? = null,
    val source: String? = null,
    val curationStatus: String? = null,
    val curationNote: String? = null,
    val difficulty: String? = null,
    val version: Int? = null
)

data class QuestionStatusUpdateDto(
    val pass1Status: Pass1Status? = null,
    val pass2Status: Pass2Status? = null,
    val isBookmarked: Boolean? = null
)

data class CurationUpdateRequest(
    val status: String? = null,
    val note: String? = null
)

data class QuestionValidationResultDto(
    val valid: Boolean,
    val fieldErrors: Map<String, String> = emptyMap(),
    val warnings: List<String> = emptyList()
)

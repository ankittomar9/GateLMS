package com.gatelms.dto

import java.time.Instant
import java.util.UUID

data class NoteDto(
    val id: UUID,
    val title: String,
    val filePath: String?,
    val originalFilename: String?,
    val contentType: String?,
    val fileSizeBytes: Long?,
    val contentMd: String?,
    val subjectId: String?,
    val subjectName: String?,
    val topicId: String?,
    val topicName: String?,
    val isPinned: Boolean,
    val kind: String,
    val version: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateTextNoteRequest(
    val title: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val contentMd: String? = null
)

data class UpdateNoteRequest(
    val title: String? = null,
    val subjectId: String? = null,
    val topicId: String? = null,
    val contentMd: String? = null,
    val isPinned: Boolean? = null,
    val version: Int
)

data class NotePageResponse(
    val content: List<NoteDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isFirst: Boolean,
    val isLast: Boolean
)

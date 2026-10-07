package com.gatelms.service

import com.gatelms.dto.*
import com.gatelms.entity.Note
import com.gatelms.error.ConflictException
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.NoteRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val noteStorageService: NoteStorageService
) {

    @Transactional(readOnly = true)
    fun getNotes(
        subjectId: String?,
        topicId: String?,
        kind: String?,
        q: String?,
        pinnedFirst: Boolean = true,
        page: Int = 0,
        size: Int = 20
    ): NotePageResponse {
        val normalizedKind = when (kind?.uppercase()) {
            null, "" -> null
            "TEXT" -> "TEXT"
            "FILE" -> "FILE"
            else -> throw ValidationException("Invalid kind parameter", mapOf("kind" to "Must be TEXT or FILE"))
        }

        val searchPattern = if (!q.isNullOrBlank()) {
            val escaped = q.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            "%$escaped%"
        } else {
            null
        }

        val pageable = PageRequest.of(Math.max(0, page), Math.max(1, size))
        val pageResult = noteRepository.findNotes(
            subjectId = subjectId?.ifBlank { null },
            topicId = topicId?.ifBlank { null },
            kind = normalizedKind,
            searchPattern = searchPattern,
            pinnedFirst = pinnedFirst,
            pageable = pageable
        )

        return NotePageResponse(
            content = pageResult.content.map { it.toDto() },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            isFirst = pageResult.isFirst,
            isLast = pageResult.isLast
        )
    }

    @Transactional(readOnly = true)
    fun getNote(id: UUID): NoteDto {
        val note = noteRepository.findById(id).orElseThrow {
            EntityNotFoundException("Note not found with id $id")
        }
        return note.toDto()
    }

    @Transactional
    fun createTextNote(request: CreateTextNoteRequest): NoteDto {
        if (request.title.isBlank()) {
            throw ValidationException("Invalid title", mapOf("title" to "Title cannot be blank"))
        }

        if (request.contentMd.isNullOrBlank()) {
            throw ValidationException(
                "Invalid note content",
                mapOf("contentMd" to "Content cannot be empty for text notes")
            )
        }

        val subject = request.subjectId?.takeIf { it.isNotBlank() }?.let {
            subjectRepository.findById(it).orElseThrow {
                EntityNotFoundException("Subject not found: $it")
            }
        }

        val topic = request.topicId?.takeIf { it.isNotBlank() }?.let {
            topicRepository.findById(it).orElseThrow {
                EntityNotFoundException("Topic not found: $it")
            }
        }

        val note = Note(
            title = request.title.trim(),
            contentMd = request.contentMd.trim(),
            filePath = null,
            subject = subject,
            topic = topic,
            isPinned = false
        )

        val saved = noteRepository.save(note)
        return saved.toDto()
    }

    @Transactional
    fun createFileNote(
        title: String,
        subjectId: String?,
        topicId: String?,
        file: MultipartFile
    ): NoteDto {
        if (title.isBlank()) {
            throw ValidationException("Invalid title", mapOf("title" to "Title cannot be blank"))
        }

        val subject = subjectId?.takeIf { it.isNotBlank() }?.let {
            subjectRepository.findById(it).orElseThrow {
                EntityNotFoundException("Subject not found: $it")
            }
        }

        val topic = topicId?.takeIf { it.isNotBlank() }?.let {
            topicRepository.findById(it).orElseThrow {
                EntityNotFoundException("Topic not found: $it")
            }
        }

        val noteId = UUID.randomUUID()
        val fileInfo = noteStorageService.storeFile(noteId, file)

        val note = Note(
            id = noteId,
            title = title.trim(),
            filePath = fileInfo.relativePath,
            originalFilename = fileInfo.originalFilename,
            contentType = fileInfo.contentType,
            fileSizeBytes = fileInfo.sizeBytes,
            contentMd = null,
            subject = subject,
            topic = topic,
            isPinned = false
        )

        val saved = noteRepository.save(note)
        return saved.toDto()
    }

    @Transactional
    fun updateNote(id: UUID, request: UpdateNoteRequest): NoteDto {
        val note = noteRepository.findById(id).orElseThrow {
            EntityNotFoundException("Note not found with id $id")
        }

        if (note.version != request.version) {
            throw ConflictException("Note was modified concurrently. Expected version ${note.version}, got ${request.version}")
        }

        if (request.title != null) {
            if (request.title.isBlank()) {
                throw ValidationException("Invalid title", mapOf("title" to "Title cannot be blank"))
            }
            note.title = request.title.trim()
        }

        if (request.contentMd != null) {
            note.contentMd = request.contentMd
        }

        if (request.isPinned != null) {
            note.isPinned = request.isPinned
        }

        if (request.subjectId != null) {
            note.subject = if (request.subjectId.isNotBlank()) {
                subjectRepository.findById(request.subjectId).orElseThrow {
                    EntityNotFoundException("Subject not found: ${request.subjectId}")
                }
            } else null
        }

        if (request.topicId != null) {
            note.topic = if (request.topicId.isNotBlank()) {
                topicRepository.findById(request.topicId).orElseThrow {
                    EntityNotFoundException("Topic not found: ${request.topicId}")
                }
            } else null
        }

        // Validate constraint: content_md IS NOT NULL OR file_path IS NOT NULL
        if (note.contentMd.isNullOrBlank() && note.filePath.isNullOrBlank()) {
            throw ValidationException("Invalid note content", mapOf("contentMd" to "Note must contain text or a file"))
        }

        note.updatedAt = Instant.now()
        val saved = noteRepository.saveAndFlush(note)
        return saved.toDto()
    }

    @Transactional
    fun togglePin(id: UUID): NoteDto {
        val note = noteRepository.findById(id).orElseThrow {
            EntityNotFoundException("Note not found with id $id")
        }
        note.isPinned = !note.isPinned
        note.updatedAt = Instant.now()
        return noteRepository.saveAndFlush(note).toDto()
    }

    @Transactional
    fun deleteNote(id: UUID) {
        val note = noteRepository.findById(id).orElseThrow {
            EntityNotFoundException("Note not found with id $id")
        }
        noteStorageService.deleteNoteFiles(id)
        noteRepository.delete(note)
    }

    @Transactional(readOnly = true)
    fun getNoteFile(id: UUID): Pair<Note, Resource> {
        val note = noteRepository.findById(id).orElseThrow {
            EntityNotFoundException("Note not found with id $id")
        }
        val filePath = note.filePath ?: throw EntityNotFoundException("Note does not have an attached file")
        val resource = noteStorageService.loadFileAsResource(filePath)
        return Pair(note, resource)
    }

    private fun Note.toDto() = NoteDto(
        id = id,
        title = title,
        filePath = filePath,
        originalFilename = originalFilename,
        contentType = contentType,
        fileSizeBytes = fileSizeBytes,
        contentMd = contentMd,
        subjectId = subject?.id,
        subjectName = subject?.name,
        topicId = topic?.id,
        topicName = topic?.name,
        isPinned = isPinned,
        kind = kind,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

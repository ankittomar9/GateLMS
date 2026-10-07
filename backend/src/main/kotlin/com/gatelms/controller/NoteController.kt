package com.gatelms.controller

import com.gatelms.dto.CreateTextNoteRequest
import com.gatelms.dto.NoteDto
import com.gatelms.dto.NotePageResponse
import com.gatelms.dto.UpdateNoteRequest
import com.gatelms.error.ValidationException
import com.gatelms.service.NoteService
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/v1/notes")
class NoteController(
    private val noteService: NoteService
) {

    @GetMapping
    fun getNotes(
        @RequestParam(required = false) subjectId: String?,
        @RequestParam(required = false) topicId: String?,
        @RequestParam(required = false) kind: String?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "true") pinnedFirst: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): NotePageResponse {
        return noteService.getNotes(
            subjectId = subjectId,
            topicId = topicId,
            kind = kind,
            q = q,
            pinnedFirst = pinnedFirst,
            page = page,
            size = size
        )
    }

    @GetMapping("/{id}")
    fun getNote(@PathVariable id: String): NoteDto {
        val uuid = parseUuidOrThrow(id)
        return noteService.getNote(uuid)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTextNote(@RequestBody request: CreateTextNoteRequest): NoteDto {
        return noteService.createTextNote(request)
    }

    @PutMapping("/{id}")
    fun updateNote(
        @PathVariable id: String,
        @RequestBody request: UpdateNoteRequest
    ): NoteDto {
        val uuid = parseUuidOrThrow(id)
        return noteService.updateNote(uuid, request)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteNote(@PathVariable id: String) {
        val uuid = parseUuidOrThrow(id)
        noteService.deleteNote(uuid)
    }

    @PatchMapping("/{id}/pin")
    fun togglePin(@PathVariable id: String): NoteDto {
        val uuid = parseUuidOrThrow(id)
        return noteService.togglePin(uuid)
    }

    @PostMapping("/files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun createFileNote(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("title") title: String,
        @RequestParam(name = "subjectId", required = false) subjectId: String?,
        @RequestParam(name = "topicId", required = false) topicId: String?
    ): NoteDto {
        return noteService.createFileNote(
            file = file,
            title = title,
            subjectId = subjectId,
            topicId = topicId
        )
    }

    @GetMapping("/{id}/file")
    fun getNoteFile(@PathVariable id: String): ResponseEntity<Resource> {
        val uuid = parseUuidOrThrow(id)
        val (note, resource) = noteService.getNoteFile(uuid)

        val disposition = ContentDisposition.inline()
            .filename(note.originalFilename ?: "file", StandardCharsets.UTF_8)
            .build()

        val contentType = note.contentType?.let { MediaType.parseMediaType(it) }
            ?: MediaType.APPLICATION_OCTET_STREAM

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePrivate())
            .eTag("\"${note.id}-${note.updatedAt.toEpochMilli()}\"")
            .lastModified(note.updatedAt.toEpochMilli())
            .body(resource)
    }

    private fun parseUuidOrThrow(idStr: String): UUID {
        if (idStr.contains("..") || idStr.contains("/") || idStr.contains("\\")) {
            throw ValidationException("Path traversal rejected", mapOf("id" to "Path traversal attempt detected"))
        }
        return try {
            UUID.fromString(idStr)
        } catch (ex: IllegalArgumentException) {
            throw ValidationException("Invalid UUID format: $idStr", mapOf("id" to "Must be a valid UUID"))
        }
    }
}

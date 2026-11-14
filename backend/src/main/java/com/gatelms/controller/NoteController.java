package com.gatelms.controller;

import com.gatelms.dto.CreateTextNoteRequest;
import com.gatelms.dto.NoteDto;
import com.gatelms.dto.NotePageResponse;
import com.gatelms.dto.UpdateNoteRequest;
import com.gatelms.error.ValidationException;
import com.gatelms.service.NoteService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public NotePageResponse getNotes(
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean pinnedFirst,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return noteService.getNotes(subjectId, topicId, kind, q, pinnedFirst, page, size);
    }

    @GetMapping("/{id}")
    public NoteDto getNote(@PathVariable String id) {
        UUID uuid = parseUuidOrThrow(id);
        return noteService.getNote(uuid);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteDto createTextNote(@RequestBody CreateTextNoteRequest request) {
        return noteService.createTextNote(request);
    }

    @PutMapping("/{id}")
    public NoteDto updateNote(
            @PathVariable String id,
            @RequestBody UpdateNoteRequest request
    ) {
        UUID uuid = parseUuidOrThrow(id);
        return noteService.updateNote(uuid, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable String id) {
        UUID uuid = parseUuidOrThrow(id);
        noteService.deleteNote(uuid);
    }

    @PatchMapping("/{id}/pin")
    public NoteDto togglePin(@PathVariable String id) {
        UUID uuid = parseUuidOrThrow(id);
        return noteService.togglePin(uuid);
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public NoteDto createFileNote(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(name = "subjectId", required = false) String subjectId,
            @RequestParam(name = "topicId", required = false) String topicId
    ) {
        return noteService.createFileNote(file, title, subjectId, topicId);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getNoteFile(@PathVariable String id) {
        UUID uuid = parseUuidOrThrow(id);
        NoteService.NoteFileResult result = noteService.getNoteFile(uuid);
        var note = result.note();
        var resource = result.resource();

        ContentDisposition disposition = ContentDisposition.inline()
                .filename(note.getOriginalFilename() != null ? note.getOriginalFilename() : "file", StandardCharsets.UTF_8)
                .build();

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (note.getContentType() != null) {
            try {
                contentType = MediaType.parseMediaType(note.getContentType());
            } catch (Exception ignored) {}
        }

        long lastModifiedMillis = note.getUpdatedAt() != null ? note.getUpdatedAt().toEpochMilli() : System.currentTimeMillis();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.maxAge(3600, TimeUnit.SECONDS).cachePrivate())
                .eTag("\"" + note.getId() + "-" + lastModifiedMillis + "\"")
                .lastModified(lastModifiedMillis)
                .body(resource);
    }

    private UUID parseUuidOrThrow(String idStr) {
        if (idStr.contains("..") || idStr.contains("/") || idStr.contains("\\")) {
            throw new ValidationException("Path traversal rejected", Map.of("id", "Path traversal attempt detected"));
        }
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid UUID format: " + idStr, Map.of("id", "Must be a valid UUID"));
        }
    }
}

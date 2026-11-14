package com.gatelms.service;

import com.gatelms.dto.*;
import com.gatelms.entity.Note;
import com.gatelms.entity.Subject;
import com.gatelms.entity.Topic;
import com.gatelms.error.Exceptions.ConflictException;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.repository.NoteRepository;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final NoteStorageService noteStorageService;
    private final CurrentUserProvider currentUserProvider;

    public NoteService(
        NoteRepository noteRepository,
        SubjectRepository subjectRepository,
        TopicRepository topicRepository,
        NoteStorageService noteStorageService,
        CurrentUserProvider currentUserProvider
    ) {
        this.noteRepository = noteRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.noteStorageService = noteStorageService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public NotePageResponse getNotes(
        String subjectId,
        String topicId,
        String kind,
        String q,
        boolean pinnedFirst,
        int page,
        int size
    ) {
        String normalizedKind = null;
        if (kind != null && !kind.isBlank()) {
            String k = kind.trim().toUpperCase();
            if ("TEXT".equals(k) || "FILE".equals(k)) {
                normalizedKind = k;
            } else {
                throw new ValidationException("Invalid kind parameter", Map.of("kind", "Must be TEXT or FILE"));
            }
        }

        String searchPattern = null;
        if (q != null && !q.isBlank()) {
            String escaped = q.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            searchPattern = "%" + escaped + "%";
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<Note> pageResult = noteRepository.findNotes(
            currentUserId,
            (subjectId != null && !subjectId.isBlank()) ? subjectId : null,
            (topicId != null && !topicId.isBlank()) ? topicId : null,
            normalizedKind,
            searchPattern,
            pinnedFirst,
            pageable
        );

        List<NoteDto> dtos = pageResult.getContent().stream().map(this::toDto).toList();
        return new NotePageResponse(
            dtos,
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.isFirst(),
            pageResult.isLast()
        );
    }

    @Transactional(readOnly = true)
    public NoteDto getNote(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Note not found with id " + id)
        );
        return toDto(note);
    }

    @Transactional
    public NoteDto createTextNote(CreateTextNoteRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ValidationException("Invalid title", Map.of("title", "Title cannot be blank"));
        }
        if (request.contentMd() == null || request.contentMd().isBlank()) {
            throw new ValidationException("Invalid note content", Map.of("contentMd", "Content cannot be empty for text notes"));
        }

        Subject subject = null;
        if (request.subjectId() != null && !request.subjectId().isBlank()) {
            subject = subjectRepository.findById(request.subjectId()).orElseThrow(() ->
                new EntityNotFoundException("Subject not found: " + request.subjectId())
            );
        }

        Topic topic = null;
        if (request.topicId() != null && !request.topicId().isBlank()) {
            topic = topicRepository.findById(request.topicId()).orElseThrow(() ->
                new EntityNotFoundException("Topic not found: " + request.topicId())
            );
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = new Note();
        note.setUserId(currentUserId);
        note.setSubject(subject);
        note.setTopic(topic);
        note.setTitle(request.title().trim());
        note.setContentMd(request.contentMd());
        note.setIsPinned(false);
        note.setCreatedAt(Instant.now());
        note.setUpdatedAt(Instant.now());

        Note saved = noteRepository.save(note);
        return toDto(saved);
    }

    @Transactional
    public NoteDto uploadFileNote(
        String title,
        String subjectId,
        String topicId,
        Boolean isPinned,
        MultipartFile file
    ) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Invalid title", Map.of("title", "Title cannot be blank"));
        }
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Invalid file", Map.of("file", "File cannot be empty"));
        }

        Subject subject = null;
        if (subjectId != null && !subjectId.isBlank()) {
            subject = subjectRepository.findById(subjectId).orElseThrow(() ->
                new EntityNotFoundException("Subject not found: " + subjectId)
            );
        }

        Topic topic = null;
        if (topicId != null && !topicId.isBlank()) {
            topic = topicRepository.findById(topicId).orElseThrow(() ->
                new EntityNotFoundException("Topic not found: " + topicId)
            );
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        UUID noteId = UUID.randomUUID();

        NoteStorageService.StoredFileInfo info = noteStorageService.storeFile(noteId, file);

        Note note = new Note();
        note.setId(noteId);
        note.setUserId(currentUserId);
        note.setSubject(subject);
        note.setTopic(topic);
        note.setTitle(title.trim());
        note.setFilePath(info.relativePath());
        note.setOriginalFilename(info.originalFilename());
        note.setContentType(info.contentType());
        note.setFileSizeBytes(info.sizeBytes());
        note.setIsPinned(isPinned != null ? isPinned : false);
        note.setCreatedAt(Instant.now());
        note.setUpdatedAt(Instant.now());

        Note saved = noteRepository.save(note);
        return toDto(saved);
    }

    @Transactional
    public NoteDto updateNote(UUID id, UpdateNoteRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Note not found with id " + id)
        );

        if (note.getVersion() != request.version()) {
            throw new ConflictException("Note version mismatch: expected " + note.getVersion() + ", got " + request.version());
        }

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new ValidationException("Invalid title", Map.of("title", "Title cannot be blank"));
            }
            note.setTitle(request.title().trim());
        }

        if ("TEXT".equals(note.getKind()) && request.contentMd() != null) {
            if (request.contentMd().isBlank()) {
                throw new ValidationException("Invalid note content", Map.of("contentMd", "Content cannot be empty for text notes"));
            }
            note.setContentMd(request.contentMd());
        }

        if (request.subjectId() != null) {
            if (request.subjectId().isBlank()) {
                note.setSubject(null);
            } else {
                Subject s = subjectRepository.findById(request.subjectId()).orElseThrow(() ->
                    new EntityNotFoundException("Subject not found: " + request.subjectId())
                );
                note.setSubject(s);
            }
        }

        if (request.topicId() != null) {
            if (request.topicId().isBlank()) {
                note.setTopic(null);
            } else {
                Topic t = topicRepository.findById(request.topicId()).orElseThrow(() ->
                    new EntityNotFoundException("Topic not found: " + request.topicId())
                );
                note.setTopic(t);
            }
        }

        if (request.isPinned() != null) {
            note.setIsPinned(request.isPinned());
        }

        note.setUpdatedAt(Instant.now());

        try {
            Note saved = noteRepository.saveAndFlush(note);
            return toDto(saved);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflictException("Note was modified concurrently");
        }
    }

    @Transactional
    public NoteDto togglePin(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Note not found with id " + id)
        );

        note.setIsPinned(!Boolean.TRUE.equals(note.getIsPinned()));
        note.setUpdatedAt(Instant.now());
        Note saved = noteRepository.save(note);
        return toDto(saved);
    }

    @Transactional
    public void deleteNote(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Note not found with id " + id)
        );

        if ("FILE".equals(note.getKind())) {
            noteStorageService.deleteNoteFiles(id);
        }
        noteRepository.delete(note);
    }

    public record NoteFileResult(Note note, Resource resource) {}

    @Transactional
    public NoteDto createFileNote(MultipartFile file, String title, String subjectId, String topicId) {
        return uploadFileNote(title, subjectId, topicId, false, file);
    }

    @Transactional(readOnly = true)
    public NoteFileResult getNoteFile(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Note not found with id " + id)
        );
        String filePath = note.getFilePath();
        if (filePath == null) {
            throw new EntityNotFoundException("Note does not have an attached file");
        }
        Resource resource = noteStorageService.loadFileAsResource(filePath);
        return new NoteFileResult(note, resource);
    }

    @Transactional(readOnly = true)
    public Resource getNoteFileResource(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Note not found with id " + id)
        );

        if (!"FILE".equals(note.getKind()) || note.getFilePath() == null || note.getFilePath().isBlank()) {
            throw new ValidationException("Invalid note type", Map.of("kind", "Note has no attached file"));
        }

        return noteStorageService.loadFileAsResource(note.getFilePath());
    }

    public NoteDto toDto(Note n) {
        return new NoteDto(
            n.getId(),
            n.getTitle(),
            n.getFilePath(),
            n.getOriginalFilename(),
            n.getContentType(),
            n.getFileSizeBytes(),
            n.getContentMd(),
            n.getSubject() != null ? n.getSubject().getId() : null,
            n.getSubject() != null ? n.getSubject().getName() : null,
            n.getTopic() != null ? n.getTopic().getId() : null,
            n.getTopic() != null ? n.getTopic().getName() : null,
            Boolean.TRUE.equals(n.getIsPinned()),
            n.getKind(),
            n.getVersion() != null ? n.getVersion() : 0,
            n.getCreatedAt(),
            n.getUpdatedAt()
        );
    }
}

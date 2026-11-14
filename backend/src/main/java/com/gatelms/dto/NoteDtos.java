package com.gatelms.dto;

import java.time.Instant;
import java.util.UUID;

public class NoteDtos {
    public record NoteSummaryDto(
        UUID id,
        String title,
        String subjectId,
        String subjectName,
        String topicId,
        String topicName,
        UUID questionId,
        boolean isPinned,
        String tags,
        Instant updatedAt
    ) {}

    public record NoteDetailDto(
        UUID id,
        String title,
        String content,
        String subjectId,
        String subjectName,
        String topicId,
        String topicName,
        UUID questionId,
        boolean isPinned,
        String tags,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record CreateNoteRequest(
        String title,
        String content,
        String subjectId,
        String topicId,
        UUID questionId,
        Boolean isPinned,
        String tags
    ) {}

    public record UpdateNoteRequest(
        String title,
        String content,
        String subjectId,
        String topicId,
        UUID questionId,
        Boolean isPinned,
        String tags
    ) {}
}

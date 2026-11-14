package com.gatelms.repository;

import com.gatelms.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<Note, UUID> {

    @Query(
        value = """
            SELECT n.* FROM notes n
            WHERE n.user_id = :userId
              AND (:subjectId IS NULL OR n.subject_id = :subjectId)
              AND (:topicId IS NULL OR n.topic_id = :topicId)
              AND (
                :kind IS NULL 
                OR (:kind = 'FILE' AND n.file_path IS NOT NULL) 
                OR (:kind = 'TEXT' AND n.file_path IS NULL)
              )
              AND (
                :searchPattern IS NULL 
                OR n.title ILIKE :searchPattern ESCAPE '\\'
                OR n.content_md ILIKE :searchPattern ESCAPE '\\'
              )
            ORDER BY
              CASE WHEN :pinnedFirst = true THEN n.is_pinned END DESC NULLS LAST,
              n.updated_at DESC,
              n.created_at DESC
        """,
        countQuery = """
            SELECT count(*) FROM notes n
            WHERE n.user_id = :userId
              AND (:subjectId IS NULL OR n.subject_id = :subjectId)
              AND (:topicId IS NULL OR n.topic_id = :topicId)
              AND (
                :kind IS NULL 
                OR (:kind = 'FILE' AND n.file_path IS NOT NULL) 
                OR (:kind = 'TEXT' AND n.file_path IS NULL)
              )
              AND (
                :searchPattern IS NULL 
                OR n.title ILIKE :searchPattern ESCAPE '\\'
                OR n.content_md ILIKE :searchPattern ESCAPE '\\'
              )
        """,
        nativeQuery = true
    )
    Page<Note> findNotes(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        @Param("kind") String kind,
        @Param("searchPattern") String searchPattern,
        @Param("pinnedFirst") boolean pinnedFirst,
        Pageable pageable
    );

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    List<Note> findByUserIdAndTopicId(UUID userId, String topicId);
    List<Note> findByUserIdAndSubjectId(UUID userId, String subjectId);
}

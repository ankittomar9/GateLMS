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

    @Query("""
        SELECT n FROM Note n
        WHERE n.userId = :userId
          AND (:subjectId IS NULL OR n.subject.id = :subjectId)
          AND (:topicId IS NULL OR n.topic.id = :topicId)
          AND (:questionId IS NULL OR n.question.id = :questionId)
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    Page<Note> findFiltered(
        @Param("userId") UUID userId,
        @Param("subjectId") String subjectId,
        @Param("topicId") String topicId,
        @Param("questionId") UUID questionId,
        Pageable pageable
    );

    @Query("""
        SELECT n FROM Note n
        WHERE n.userId = :userId
          AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(n.tags) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY n.isPinned DESC, n.updatedAt DESC
    """)
    Page<Note> searchNotes(@Param("userId") UUID userId, @Param("query") String query, Pageable pageable);

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);

    List<Note> findByUserIdAndTopicId(UUID userId, String topicId);
    List<Note> findByUserIdAndSubjectId(UUID userId, String subjectId);
}

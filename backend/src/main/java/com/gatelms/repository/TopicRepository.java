package com.gatelms.repository;

import com.gatelms.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, String> {
    List<Topic> findBySubjectIdOrderBySequenceAsc(String subjectId);
    Optional<Topic> findBySubjectIdAndSlug(String subjectId, String slug);
    Optional<Topic> findBySlug(String slug);

    List<Topic> findBySubjectId(String subjectId);

    @Query("SELECT t FROM Topic t JOIN t.subject s WHERE s.slug = :subjectSlug")
    List<Topic> findBySubjectSlug(@Param("subjectSlug") String subjectSlug);

    @Query("SELECT t FROM Topic t WHERE (t.subject.id = :subjectIdOrSlug OR t.subject.slug = :subjectIdOrSlug) AND t.slug = :topicSlug")
    Optional<Topic> findBySubjectAndTopicSlug(@Param("subjectIdOrSlug") String subjectIdOrSlug, @Param("topicSlug") String topicSlug);

    @Query("SELECT t FROM Topic t WHERE t.subject.id = :subjectIdOrSlug OR t.subject.slug = :subjectIdOrSlug ORDER BY t.sequence ASC, t.name ASC")
    List<Topic> findBySubjectIdOrSlugOrderBySequenceAscNameAsc(@Param("subjectIdOrSlug") String subjectIdOrSlug);
}

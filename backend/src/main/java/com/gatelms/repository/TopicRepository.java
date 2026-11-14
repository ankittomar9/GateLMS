package com.gatelms.repository;

import com.gatelms.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, String> {
    List<Topic> findBySubjectIdOrderBySequenceAsc(String subjectId);
    Optional<Topic> findBySubjectIdAndSlug(String subjectId, String slug);
    Optional<Topic> findBySlug(String slug);
}

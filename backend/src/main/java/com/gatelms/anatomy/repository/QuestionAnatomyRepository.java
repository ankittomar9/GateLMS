package com.gatelms.anatomy.repository;

import com.gatelms.anatomy.entity.AnatomyStatus;
import com.gatelms.anatomy.entity.QuestionAnatomy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionAnatomyRepository extends JpaRepository<QuestionAnatomy, UUID> {
    Optional<QuestionAnatomy> findByQuestionId(UUID questionId);

    @Query("SELECT qa FROM QuestionAnatomy qa WHERE qa.question.id IN :questionIds")
    List<QuestionAnatomy> findByQuestionIdIn(@Param("questionIds") Collection<UUID> questionIds);

    Page<QuestionAnatomy> findByStatus(AnatomyStatus status, Pageable pageable);
}

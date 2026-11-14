package com.gatelms.repository;

import com.gatelms.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {
    Optional<Subject> findBySlug(String slug);
    List<Subject> findAllByOrderByPriorityTierAsc();

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Subject s WHERE s.id = :idOrSlug OR s.slug = :idOrSlug")
    Optional<Subject> findByIdOrSlug(@org.springframework.data.repository.query.Param("idOrSlug") String idOrSlug);
}

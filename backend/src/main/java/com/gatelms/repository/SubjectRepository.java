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
}

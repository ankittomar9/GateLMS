package com.gatelms.repository;

import com.gatelms.entity.ExamMode;
import com.gatelms.entity.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MockExamRepository extends JpaRepository<MockExam, UUID> {
    List<MockExam> findByModeOrderByCreatedAtDesc(ExamMode mode);
    MockExam findFirstByTitle(String title);
}

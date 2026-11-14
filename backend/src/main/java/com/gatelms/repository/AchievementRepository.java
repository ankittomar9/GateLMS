package com.gatelms.repository;

import com.gatelms.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    Optional<Achievement> findByUserIdAndCode(UUID userId, String code);
    boolean existsByUserIdAndCode(UUID userId, String code);
    List<Achievement> findAllByUserIdOrderByEarnedAtAsc(UUID userId);

    Optional<Achievement> findByCode(String code);
    boolean existsByCode(String code);
    List<Achievement> findAllByOrderByEarnedAtAsc();
}

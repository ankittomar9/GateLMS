package com.gatelms.repository;

import com.gatelms.entity.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, UUID> {
    Optional<UserGoal> findFirstByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<UserGoal> findFirstByOrderByUpdatedAtDesc();
}

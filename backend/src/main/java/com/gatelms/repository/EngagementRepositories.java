package com.gatelms.repository;

import com.gatelms.entity.Achievement;
import com.gatelms.entity.Notification;
import com.gatelms.entity.NotificationType;
import com.gatelms.entity.UserGoal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, UUID> {
    Optional<UserGoal> findFirstByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<UserGoal> findFirstByOrderByUpdatedAtDesc();
}

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    Optional<Achievement> findByUserIdAndCode(UUID userId, String code);
    boolean existsByUserIdAndCode(UUID userId, String code);
    List<Achievement> findAllByUserIdOrderByEarnedAtAsc(UUID userId);

    Optional<Achievement> findByCode(String code);
    boolean existsByCode(String code);
    List<Achievement> findAllByOrderByEarnedAtAsc();
}

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    long countByUserIdAndReadAtIsNull(UUID userId);
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllAsRead(@Param("userId") UUID userId, @Param("now") Instant now);

    boolean existsByUserIdAndTypeAndCreatedAtBetween(
        UUID userId,
        NotificationType type,
        Instant start,
        Instant end
    );
}

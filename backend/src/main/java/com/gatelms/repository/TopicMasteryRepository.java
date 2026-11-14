package com.gatelms.repository;

import com.gatelms.entity.TopicMastery;
import com.gatelms.entity.TopicMasteryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicMasteryRepository extends JpaRepository<TopicMastery, TopicMasteryId> {
    List<TopicMastery> findByIdUserId(UUID userId);
    List<TopicMastery> findAllByIdUserId(UUID userId);

    @Query("SELECT tm FROM TopicMastery tm WHERE tm.id.userId = :userId")
    List<TopicMastery> findByUserId(@Param("userId") UUID userId);
    Optional<TopicMastery> findByIdUserIdAndIdTopicId(UUID userId, String topicId);

    @Query("SELECT tm FROM TopicMastery tm WHERE tm.id.userId = :userId AND tm.nextRevisionDue <= :now")
    List<TopicMastery> findDueForRevision(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT COUNT(tm) FROM TopicMastery tm WHERE tm.id.userId = :userId AND tm.nextRevisionDue IS NOT NULL AND tm.nextRevisionDue <= :cutoff")
    long countDueForRevision(@Param("userId") UUID userId, @Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(tm) FROM TopicMastery tm WHERE tm.id.userId = :userId AND tm.masteryLevel >= :level")
    long countMasteredTopics(@Param("userId") UUID userId, @Param("level") int level);

    @Query("SELECT AVG(tm.masteryLevel) FROM TopicMastery tm WHERE tm.id.userId = :userId")
    Double getAverageMastery(@Param("userId") UUID userId);

    long countByIdUserIdAndNextRevisionDueLessThanEqual(UUID userId, Instant now);

    List<TopicMastery> findByIdTopicId(String topicId);
}

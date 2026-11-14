package com.gatelms.service;

import com.gatelms.dto.NotificationDto;
import com.gatelms.entity.Notification;
import com.gatelms.entity.NotificationType;
import com.gatelms.entity.PlanItem;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserGoalRepository userGoalRepository;
    private final DailyRollupRepository dailyRollupRepository;
    private final CurrentUserProvider currentUserProvider;
    private final MistakeLogRepository mistakeLogRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final PlanItemRepository planItemRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserGoalRepository userGoalRepository,
            DailyRollupRepository dailyRollupRepository,
            CurrentUserProvider currentUserProvider,
            @Autowired(required = false) MistakeLogRepository mistakeLogRepository,
            @Autowired(required = false) TopicMasteryRepository topicMasteryRepository,
            @Autowired(required = false) PlanItemRepository planItemRepository,
            @Autowired(required = false) UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userGoalRepository = userGoalRepository;
        this.dailyRollupRepository = dailyRollupRepository;
        this.currentUserProvider = currentUserProvider;
        this.mistakeLogRepository = mistakeLogRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.planItemRepository = planItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(boolean unreadOnly, Pageable pageable) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(currentUserId, pageable)
                : notificationRepository.findAllByUserIdOrderByCreatedAtDesc(currentUserId, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return notificationRepository.countByUserIdAndReadAtIsNull(currentUserId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getTop10() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public NotificationDto markAsRead(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    @Transactional
    public int markAllAsRead() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return notificationRepository.markAllAsRead(currentUserId, Instant.now());
    }

    @Transactional
    public Notification createNotification(
            NotificationType type,
            String title,
            String body,
            String link,
            boolean forceDuplicate,
            UUID userId
    ) {
        UUID targetUserId = userId != null ? userId : currentUserProvider.getCurrentUserId();
        Instant now = Instant.now();
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        if (!forceDuplicate && type != NotificationType.ACHIEVEMENT_EARNED) {
            boolean exists = notificationRepository.existsByUserIdAndTypeAndCreatedAtBetween(targetUserId, type, startOfDay, endOfDay);
            if (exists) {
                logger.info("Notification of type {} already produced today for user {}. Skipping de-dup.", type, targetUserId);
                return null;
            }
        }

        Notification notification = new Notification(
                targetUserId,
                type,
                title,
                body,
                link,
                now
        );
        Notification saved = notificationRepository.save(notification);
        logger.info("Produced notification [{}] id={} for user {}", type, saved.getId(), targetUserId);
        return saved;
    }

    public Notification createNotification(NotificationType type, String title, String body, String link) {
        return createNotification(type, title, body, link, false, null);
    }

    @Scheduled(cron = "${app.notifications.morning-cron:0 0 8 * * *}")
    @Transactional
    public void runMorningNotificationJob() {
        logger.info("Executing morning notification job...");
        List<com.gatelms.entity.User> users = userRepository != null ? userRepository.findAll() : List.of();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        for (var u : users) {
            try {
                long dueMistakes = mistakeLogRepository != null ? mistakeLogRepository.countDueReviews(u.getId(), now) : 0L;
                long dueTopics = topicMasteryRepository != null ? topicMasteryRepository.countDueForRevision(u.getId(), now) : 0L;
                if (dueMistakes + dueTopics > 0) {
                    createNotification(
                            NotificationType.REVISION_DUE,
                            "Revisions Due Today",
                            dueMistakes + " mistake reviews and " + dueTopics + " topics due for revision.",
                            "/revision",
                            false,
                            u.getId()
                    );
                }
            } catch (Exception e) {
                logger.warn("Failed to check revision due in morning job for user {}: {}", u.getId(), e.getMessage());
            }

            try {
                List<PlanItem> openItems = planItemRepository != null ? planItemRepository.findOpenItemsForDate(today).stream()
                        .filter(it -> it.getPlan().getUserId().equals(u.getId()))
                        .toList() : List.of();
                if (!openItems.isEmpty()) {
                    createNotification(
                            NotificationType.PLAN_DUE,
                            "Daily Study Plan Due",
                            openItems.size() + " study plan items scheduled for today.",
                            "/planner",
                            false,
                            u.getId()
                    );
                }
            } catch (Exception e) {
                logger.warn("Failed to check plan due in morning job for user {}: {}", u.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "${app.notifications.evening-cron:0 0 20 * * *}")
    @Transactional
    public void runEveningNotificationJob() {
        logger.info("Executing evening notification job...");
        List<com.gatelms.entity.User> users = userRepository != null ? userRepository.findAll() : List.of();
        LocalDate today = LocalDate.now();

        for (var u : users) {
            try {
                var rollups = dailyRollupRepository.findAllByUserIdOrderByActivityDateDesc(u.getId());
                Map<LocalDate, Integer> activityMap = rollups.stream()
                        .collect(Collectors.toMap(r -> r.getId().getActivityDate(), com.gatelms.entity.DailyRollup::getQuestionsAnswered, (a, b) -> a));
                int todayAnswered = activityMap.getOrDefault(today, 0);

                var goal = userGoalRepository.findFirstByUserIdOrderByUpdatedAtDesc(u.getId()).orElse(null);
                boolean streakProtection = goal != null && goal.isStreakProtection();

                StreakCalculator.StreakResult streakRes = StreakCalculator.computeStreak(
                        activityMap,
                        today,
                        streakProtection
                );

                if (streakRes.streak() > 0 && todayAnswered == 0) {
                    createNotification(
                            NotificationType.STREAK_AT_RISK,
                            "Streak At Risk!",
                            "You have an active streak of " + streakRes.streak() + " days. Complete a study session today to keep it alive!",
                            "/practice",
                            false,
                            u.getId()
                    );
                }
            } catch (Exception e) {
                logger.warn("Failed to check streak at risk in evening job for user {}: {}", u.getId(), e.getMessage());
            }
        }
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getLink(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}

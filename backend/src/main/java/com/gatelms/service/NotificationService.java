package com.gatelms.service;

import com.gatelms.dto.EngagementDtos.NotificationDto;
import com.gatelms.entity.Notification;
import com.gatelms.entity.NotificationType;
import com.gatelms.entity.PlanItem;
import com.gatelms.entity.UserGoal;
import com.gatelms.repository.DailyRollupRepository;
import com.gatelms.repository.NotificationRepository;
import com.gatelms.repository.PlanItemRepository;
import com.gatelms.repository.UserGoalRepository;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserGoalRepository userGoalRepository;
    private final DailyRollupRepository dailyRollupRepository;
    private final PlanItemRepository planItemRepository;
    private final CurrentUserProvider currentUserProvider;
    private final RevisionService revisionService;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserGoalRepository userGoalRepository,
        DailyRollupRepository dailyRollupRepository,
        PlanItemRepository planItemRepository,
        CurrentUserProvider currentUserProvider,
        @Autowired(required = false) RevisionService revisionService
    ) {
        this.notificationRepository = notificationRepository;
        this.userGoalRepository = userGoalRepository;
        this.dailyRollupRepository = dailyRollupRepository;
        this.planItemRepository = planItemRepository;
        this.currentUserProvider = currentUserProvider;
        this.revisionService = revisionService;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(boolean unreadOnly, Pageable pageable) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Page<Notification> page = notificationRepository.findNotifications(currentUserId, unreadOnly, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return notificationRepository.countUnread(currentUserId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getTop10() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Page<Notification> page = notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(currentUserId, PageRequest.of(0, 10));
        return page.getContent().stream().map(this::toDto).toList();
    }

    @Transactional
    public NotificationDto markAsRead(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Notification notif = notificationRepository.findByIdAndUserId(id, currentUserId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: " + id)
        );

        if (!Boolean.TRUE.equals(notif.getIsRead())) {
            notif.setIsRead(true);
            notif.setReadAt(Instant.now());
            notif = notificationRepository.save(notif);
        }
        return toDto(notif);
    }

    @Transactional
    public int markAllAsRead() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return notificationRepository.markAllAsRead(currentUserId, Instant.now());
    }

    @Transactional
    public NotificationDto createNotification(
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String actionUrl
    ) {
        Notification notif = new Notification();
        notif.setUserId(userId);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setActionUrl(actionUrl);
        notif.setIsRead(false);
        notif.setCreatedAt(Instant.now());

        Notification saved = notificationRepository.save(notif);
        return toDto(saved);
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void runMorningNotificationJob() {
        logger.info("Executing morning scheduled notification dispatch");
        LocalDate today = LocalDate.now();

        List<UserGoal> goals = userGoalRepository.findAll();
        for (UserGoal goal : goals) {
            UUID userId = goal.getUserId();
            if (userId == null) continue;

            List<PlanItem> todayItems = planItemRepository.findDailyItems(userId, today);
            int plannedQuestions = todayItems.stream().mapToInt(PlanItem::getTargetQuestions).sum();
            int plannedMinutes = todayItems.stream().mapToInt(PlanItem::getTargetMinutes).sum();

            if (plannedQuestions > 0 || plannedMinutes > 0) {
                createNotification(
                    userId,
                    NotificationType.SYSTEM,
                    "Today's Study Plan Ready",
                    "You have " + plannedQuestions + " questions (" + plannedMinutes + " mins) scheduled for today.",
                    "/plan"
                );
            }
        }
    }

    @Scheduled(cron = "0 0 20 * * *")
    @Transactional
    public void runEveningNotificationJob() {
        logger.info("Executing evening scheduled notification dispatch");
        LocalDate today = LocalDate.now();

        List<UserGoal> goals = userGoalRepository.findAll();
        for (UserGoal goal : goals) {
            UUID userId = goal.getUserId();
            if (userId == null) continue;

            dailyRollupRepository.findByUserIdAndActivityDate(userId, today).ifPresent(rollup -> {
                int target = goal.getDailyTargetQuestions();
                int done = rollup.getQuestionsAnswered();
                if (done < target) {
                    int remaining = target - done;
                    createNotification(
                        userId,
                        NotificationType.STREAK,
                        "Streak At Risk!",
                        "You need " + remaining + " more questions today to keep your daily goal.",
                        "/practice"
                    );
                }
            });
        }
    }

    public NotificationDto toDto(Notification n) {
        return new NotificationDto(
            n.getId(),
            n.getType() != null ? n.getType().name() : "SYSTEM",
            n.getTitle(),
            n.getMessage(),
            n.getActionUrl(),
            n.getIsRead(),
            n.getCreatedAt()
        );
    }
}

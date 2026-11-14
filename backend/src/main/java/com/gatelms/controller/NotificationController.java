package com.gatelms.controller;

import com.gatelms.dto.MarkAllReadResponse;
import com.gatelms.dto.NotificationDto;
import com.gatelms.dto.UnreadCountResponse;
import com.gatelms.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationDto> getNotifications(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotifications(unread, PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(notificationService.getUnreadCount());
    }

    @GetMapping("/top")
    public List<NotificationDto> getTop10() {
        return notificationService.getTop10();
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }

    @PostMapping("/read-all")
    public MarkAllReadResponse markAllAsRead() {
        int updated = notificationService.markAllAsRead();
        return new MarkAllReadResponse(updated);
    }
}

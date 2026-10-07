package com.gatelms.controller

import com.gatelms.dto.MarkAllReadResponse
import com.gatelms.dto.NotificationDto
import com.gatelms.dto.UnreadCountResponse
import com.gatelms.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getNotifications(
        @RequestParam(defaultValue = "false") unread: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<NotificationDto> {
        return notificationService.getNotifications(unread, PageRequest.of(page, size))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(): UnreadCountResponse {
        return UnreadCountResponse(notificationService.getUnreadCount())
    }

    @GetMapping("/top")
    fun getTop10(): List<NotificationDto> {
        return notificationService.getTop10()
    }

    @PatchMapping("/{id}/read")
    fun markAsRead(@PathVariable id: UUID): NotificationDto {
        return notificationService.markAsRead(id)
    }

    @PostMapping("/read-all")
    fun markAllAsRead(): MarkAllReadResponse {
        val updated = notificationService.markAllAsRead()
        return MarkAllReadResponse(updated)
    }
}

package com.seodashboard.api.notification.controller;

import com.seodashboard.api.notification.dto.NotificationResponse;
import com.seodashboard.api.notification.dto.UnreadCountResponse;
import com.seodashboard.api.notification.service.NotificationService;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.dto.ApiResponse;
import com.seodashboard.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "User notification API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications", description = "Get paginated list of notifications")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<NotificationResponse> response = notificationService.getNotifications(
                user.getId(), isRead, type, severity, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Get unread count", description = "Get count of unread notifications by severity")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal User user
    ) {
        UnreadCountResponse response = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long notificationId
    ) {
        NotificationResponse response = notificationService.markAsRead(user.getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read"));
    }

    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal User user
    ) {
        int updated = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success(updated, "All notifications marked as read"));
    }
}

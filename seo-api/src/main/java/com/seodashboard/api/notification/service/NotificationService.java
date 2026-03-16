package com.seodashboard.api.notification.service;

import com.seodashboard.api.notification.dto.NotificationResponse;
import com.seodashboard.api.notification.dto.UnreadCountResponse;
import com.seodashboard.api.notification.repository.NotificationRepository;
import com.seodashboard.common.domain.Notification;
import com.seodashboard.common.domain.User;
import com.seodashboard.common.dto.PageResponse;
import com.seodashboard.common.exception.BusinessException;
import com.seodashboard.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(Long userId, Boolean isRead,
                                                                 String type, String severity,
                                                                 Pageable pageable) {
        Page<Notification> page;

        if (isRead != null) {
            page = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        } else if (type != null && !type.isBlank()) {
            page = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else if (severity != null && !severity.isBlank()) {
            page = notificationRepository.findByUserIdAndSeverityOrderByCreatedAtDesc(userId, severity, pageable);
        } else {
            page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long totalUnread = notificationRepository.countByUserIdAndIsRead(userId, false);

        List<Object[]> severityCounts = notificationRepository.countUnreadBySeverity(userId);
        Map<String, Long> bySeverity = new HashMap<>();
        for (Object[] row : severityCounts) {
            bySeverity.put((String) row[0], (Long) row[1]);
        }

        return new UnreadCountResponse(totalUnread, bySeverity);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
        log.info("Notification marked as read: id={}, userId={}", notificationId, userId);

        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("All notifications marked as read: userId={}, count={}", userId, updated);
        return updated;
    }

    @Transactional
    public Notification createNotification(User user, String type, String title, String message,
                                            String referenceType, Long referenceId, String severity) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .severity(severity)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created: id={}, type={}, userId={}", notification.getId(), type, user.getId());

        return notification;
    }
}

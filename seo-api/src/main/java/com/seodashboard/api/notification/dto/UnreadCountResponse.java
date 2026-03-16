package com.seodashboard.api.notification.dto;

import java.util.Map;

public record UnreadCountResponse(
        long totalUnread,
        Map<String, Long> bySeverity
) {
}

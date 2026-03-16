package com.seodashboard.api.auth.dto;

import com.seodashboard.common.domain.User;
import com.seodashboard.common.domain.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        boolean isActive,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}

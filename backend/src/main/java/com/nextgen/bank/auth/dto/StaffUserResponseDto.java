package com.nextgen.bank.auth.dto;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.common.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record StaffUserResponseDto(
        UUID userId,
        String username,
        String email,
        UserRole role,
        boolean isActive,
        Instant createdAt
) {
    public static StaffUserResponseDto fromEntity(User user) {
        return new StaffUserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}

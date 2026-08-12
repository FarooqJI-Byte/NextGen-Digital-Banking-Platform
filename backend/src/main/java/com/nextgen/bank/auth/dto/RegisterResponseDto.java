package com.nextgen.bank.auth.dto;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.common.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponseDto(
        UUID userId,
        String username,
        String email,
        UserRole role,
        Instant createdAt
) {
    public static RegisterResponseDto fromEntity(User user) {
        return new RegisterResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

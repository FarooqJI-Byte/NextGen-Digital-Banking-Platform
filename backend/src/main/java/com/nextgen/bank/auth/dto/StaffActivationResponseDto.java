package com.nextgen.bank.auth.dto;

import java.util.UUID;

public record StaffActivationResponseDto(
        UUID userId,
        String username,
        String email,
        String message
) {
}

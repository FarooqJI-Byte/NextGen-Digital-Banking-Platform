package com.nextgen.bank.auth.dto;

import java.time.Instant;

public record OtpVerificationResponseDto(
        String identifier,
        String purpose,
        boolean verified,
        String message,
        Instant timestamp
) {
    public static OtpVerificationResponseDto success(String identifier, String purpose, String message) {
        return new OtpVerificationResponseDto(identifier, purpose, true, message, Instant.now());
    }

    public static OtpVerificationResponseDto failure(String identifier, String purpose, String message) {
        return new OtpVerificationResponseDto(identifier, purpose, false, message, Instant.now());
    }
}

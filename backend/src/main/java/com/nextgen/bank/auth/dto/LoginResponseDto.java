package com.nextgen.bank.auth.dto;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public LoginResponseDto(String accessToken, String refreshToken, long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}

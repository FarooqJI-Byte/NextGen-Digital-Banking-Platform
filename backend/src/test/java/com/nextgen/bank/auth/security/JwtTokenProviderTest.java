package com.nextgen.bank.auth.security;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLTEyMzQ1Njc4OTA="; // Valid 32-byte Base64 secret

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(testSecret, 900000L, 604800000L);
    }

    @Test
    @DisplayName("Should successfully generate JWT access token with correct claims")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = new User("testuser", "test@example.com", "hash", UserRole.CUSTOMER);
        user.setUserId(userId);

        String token = jwtTokenProvider.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo(UserRole.CUSTOMER);
        assertThat(jwtTokenProvider.extractExpiration(token)).isNotNull();
    }

    @Test
    @DisplayName("Should generate refresh token with ref_ prefix")
    void testGenerateRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        assertThat(refreshToken).isNotBlank();
        assertThat(refreshToken).startsWith("ref_");
    }

    @Test
    @DisplayName("Should fail validation for invalid, malformed, or empty tokens")
    void testValidateInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.jwt.token")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
    }

    @Test
    @DisplayName("Should report correct expiration in seconds (900s)")
    void testGetAccessTokenExpirationSeconds() {
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(900L);
    }
}

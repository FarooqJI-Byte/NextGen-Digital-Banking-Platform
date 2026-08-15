package com.nextgen.bank.auth.security;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public JwtTokenProvider() {
    }

    public JwtTokenProvider(String jwtSecret, long accessTokenExpirationMs, long refreshTokenExpirationMs) {
        this.jwtSecret = Objects.requireNonNull(jwtSecret, "JWT secret cannot be null");
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    private SecretKey getSigningKey() {
        if (this.jwtSecret == null || this.jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured or empty");
        }

        byte[] keyBytes = null;

        try {
            byte[] decoded = Decoders.BASE64.decode(this.jwtSecret.trim());
            if (decoded != null && decoded.length >= 32) {
                keyBytes = decoded;
            }
        } catch (Exception ignored) {
            // Not a valid Base64 string, will evaluate raw UTF-8 bytes below
        }

        if (keyBytes == null) {
            byte[] rawBytes = this.jwtSecret.getBytes(StandardCharsets.UTF_8);
            if (rawBytes.length >= 32) {
                keyBytes = rawBytes;
            }
        }

        if (keyBytes == null || keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret key must be at least 256 bits (32 bytes) long in Base64 or UTF-8 format for HS256"
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getUserId() != null ? user.getUserId().toString() : null)
                .claim("role", user.getRole() != null ? user.getRole().name() : null)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken() {
        return "ref_" + UUID.randomUUID();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String userIdStr = extractAllClaims(token).get("userId", String.class);
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    public UserRole extractRole(String token) {
        String roleStr = extractAllClaims(token).get("role", String.class);
        return roleStr != null ? UserRole.valueOf(roleStr) : null;
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            logger.warn("Invalid JWT signature or format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty or invalid: {}", e.getMessage());
        }
        return false;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}

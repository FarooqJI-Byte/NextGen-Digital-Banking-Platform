package com.nextgen.bank.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_otps")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "otp_id", nullable = false, updatable = false)
    private UUID otpId;

    @Column(name = "identifier", nullable = false, length = 100)
    private String identifier;

    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OtpToken() {
    }

    public OtpToken(String identifier, String purpose, String otpHash, Instant expiresAt) {
        this.identifier = Objects.requireNonNull(identifier, "Identifier cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "Purpose cannot be null");
        this.otpHash = Objects.requireNonNull(otpHash, "OTP hash cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "ExpiresAt cannot be null");
        this.attempts = 0;
        this.isUsed = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(this.expiresAt);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markUsed(Instant now) {
        this.isUsed = true;
        this.usedAt = now;
    }

    public UUID getOtpId() {
        return otpId;
    }

    public void setOtpId(UUID otpId) {
        this.otpId = otpId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

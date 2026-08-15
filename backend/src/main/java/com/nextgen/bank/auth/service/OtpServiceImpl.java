package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.domain.OtpToken;
import com.nextgen.bank.auth.dto.OtpVerificationResponseDto;
import com.nextgen.bank.auth.event.CustomerOtpNotificationEvent;
import com.nextgen.bank.auth.repository.OtpTokenRepository;
import com.nextgen.bank.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 3;
    private static final int EXPIRATION_MINUTES = 10;

    private final OtpTokenRepository otpTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean devMode;

    public OtpServiceImpl(
            OtpTokenRepository otpTokenRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.dev-mode:false}") boolean devMode
    ) {
        this.otpTokenRepository = otpTokenRepository;
        this.eventPublisher = eventPublisher;
        this.devMode = devMode;
    }

    @Override
    public void generateAndSendOtp(String identifier, String purpose) {
        Objects.requireNonNull(identifier, "Identifier cannot be null");
        Objects.requireNonNull(purpose, "Purpose cannot be null");

        String normalizedIdentifier = identifier.trim().toLowerCase();
        String normalizedPurpose = purpose.trim().toUpperCase();

        // 1. Invalidate any existing active OTPs for this identifier/purpose
        otpTokenRepository.invalidatePreviousOtps(normalizedIdentifier, normalizedPurpose, Instant.now());

        // 2. Generate 6-digit cryptographically secure numeric OTP
        int rawNumber = SECURE_RANDOM.nextInt(1_000_000);
        String rawOtp = String.format("%06d", rawNumber);

        // 3. Hash OTP with SHA-256 for persistent database storage
        String otpHash = hashOtp(rawOtp);
        Instant expiresAt = Instant.now().plus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        OtpToken otpToken = new OtpToken(normalizedIdentifier, normalizedPurpose, otpHash, expiresAt);
        otpTokenRepository.save(otpToken);

        // 4. Dispatch in-memory notification event after database commit
        publishNotificationAfterCommit(normalizedIdentifier, rawOtp, normalizedPurpose, expiresAt);

        // 5. Development-only logging fallback (strictly disabled in production)
        if (devMode) {
            log.info("[DEV FALLBACK - DO NOT USE IN PRODUCTION] Generated OTP for [{}] ({}): [{}]",
                    normalizedIdentifier, normalizedPurpose, rawOtp);
        }
    }

    @Override
    public OtpVerificationResponseDto verifyOtp(String identifier, String purpose, String otp) {
        Objects.requireNonNull(identifier, "Identifier cannot be null");
        Objects.requireNonNull(purpose, "Purpose cannot be null");
        Objects.requireNonNull(otp, "OTP cannot be null");

        String normalizedIdentifier = identifier.trim().toLowerCase();
        String normalizedPurpose = purpose.trim().toUpperCase();
        String normalizedOtp = otp.trim();

        OtpToken otpToken = otpTokenRepository.findActiveOtp(normalizedIdentifier, normalizedPurpose)
                .orElseThrow(() -> new BusinessException(
                        "No active OTP found. Please request a new OTP.",
                        HttpStatus.BAD_REQUEST,
                        "NO_ACTIVE_OTP"
                ));

        Instant now = Instant.now();

        // Check expiration
        if (otpToken.isExpired(now)) {
            otpToken.markUsed(now);
            otpTokenRepository.save(otpToken);
            throw new BusinessException(
                    "OTP has expired. Please request a new OTP.",
                    HttpStatus.BAD_REQUEST,
                    "OTP_EXPIRED"
            );
        }

        // Check max attempts
        if (otpToken.getAttempts() >= MAX_ATTEMPTS) {
            otpToken.markUsed(now);
            otpTokenRepository.save(otpToken);
            throw new BusinessException(
                    "Maximum verification attempts exceeded. Please request a new OTP.",
                    HttpStatus.BAD_REQUEST,
                    "OTP_MAX_ATTEMPTS_EXCEEDED"
            );
        }

        // Validate hash
        String presentedHash = hashOtp(normalizedOtp);
        if (!presentedHash.equals(otpToken.getOtpHash())) {
            otpToken.incrementAttempts();
            otpTokenRepository.save(otpToken);

            int remainingAttempts = MAX_ATTEMPTS - otpToken.getAttempts();
            throw new BusinessException(
                    "Invalid OTP. " + remainingAttempts + " attempts remaining.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_OTP"
            );
        }

        // Mark OTP used atomically
        otpToken.markUsed(now);
        otpTokenRepository.save(otpToken);

        log.info("OTP successfully verified for [{}] under purpose [{}].", normalizedIdentifier, normalizedPurpose);
        return OtpVerificationResponseDto.success(
                normalizedIdentifier,
                normalizedPurpose,
                "OTP verified successfully."
        );
    }

    @Override
    public void resendOtp(String identifier, String purpose) {
        generateAndSendOtp(identifier, purpose);
    }

    private void publishNotificationAfterCommit(String identifier, String rawOtp, String purpose, Instant expiresAt) {
        CustomerOtpNotificationEvent event = new CustomerOtpNotificationEvent(identifier, rawOtp, purpose, expiresAt);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                }
            });
        } else {
            eventPublisher.publishEvent(event);
        }
    }

    private String hashOtp(String rawOtp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawOtp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

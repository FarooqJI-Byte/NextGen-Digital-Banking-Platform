package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.domain.StaffActivationToken;
import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.dto.CreateStaffRequestDto;
import com.nextgen.bank.auth.dto.StaffUserResponseDto;
import com.nextgen.bank.auth.event.StaffActivationNotificationEvent;
import com.nextgen.bank.auth.event.UserRegisteredEvent;
import com.nextgen.bank.auth.repository.StaffActivationTokenRepository;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class AdminStaffServiceImpl implements AdminStaffService {

    private static final Logger log = LoggerFactory.getLogger(AdminStaffServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final StaffActivationTokenRepository tokenRepository;
    private final OutboxEventWriter outboxEventWriter;
    private final ApplicationEventPublisher eventPublisher;
    private final String frontendUrl;
    private final boolean devMode;

    public AdminStaffServiceImpl(
            UserRepository userRepository,
            StaffActivationTokenRepository tokenRepository,
            OutboxEventWriter outboxEventWriter,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${app.dev-mode:false}") boolean devMode
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.outboxEventWriter = outboxEventWriter;
        this.eventPublisher = eventPublisher;
        this.frontendUrl = frontendUrl;
        this.devMode = devMode;
    }

    @Override
    public StaffUserResponseDto createStaff(CreateStaffRequestDto requestDto, UUID adminUserId) {
        Objects.requireNonNull(requestDto, "Create staff request cannot be null");
        Objects.requireNonNull(adminUserId, "Admin user ID cannot be null");

        String normalizedUsername = requestDto.username().trim();
        String normalizedEmail = requestDto.email().trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BusinessException(
                    "Username is already taken",
                    HttpStatus.CONFLICT,
                    "USERNAME_EXISTS"
            );
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(
                    "Email is already registered",
                    HttpStatus.CONFLICT,
                    "EMAIL_EXISTS"
            );
        }

        try {
            // Unactivated employee identity: unmatchable placeholder hash and isActive=false
            String unactivatedPlaceholderHash = "UNACTIVATED$" + UUID.randomUUID();
            User staffUser = new User(
                    normalizedUsername,
                    normalizedEmail,
                    unactivatedPlaceholderHash,
                    UserRole.BANK_STAFF
            );
            staffUser.setActive(false);

            User saved = userRepository.save(staffUser);

            // Generate cryptographically secure random 32-byte activation token (256-bit entropy)
            String rawActivationToken = generateAndPersistActivationToken(saved.getUserId());

            // Write registration event to persistent outbox (NO raw token in outbox)
            outboxEventWriter.write(new UserRegisteredEvent(
                    saved.getUserId(),
                    saved.getEmail(),
                    saved.getRole()
            ));

            // Dispatch in-memory ephemeral notification event after transaction commit
            publishActivationEventAfterCommit(saved, rawActivationToken);

            log.info("Admin [{}] successfully provisioned unactivated BANK_STAFF account for [{}].",
                    adminUserId, normalizedUsername);

            return StaffUserResponseDto.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate creation prevented for staff account: {}", e.getMessage());
            throw new BusinessException(
                    "Username or email is already in use by another account",
                    HttpStatus.CONFLICT,
                    "USER_ALREADY_EXISTS"
            );
        }
    }

    @Override
    public StaffUserResponseDto resendStaffActivation(UUID staffUserId, UUID adminUserId) {
        Objects.requireNonNull(staffUserId, "Staff user ID cannot be null");
        Objects.requireNonNull(adminUserId, "Admin user ID cannot be null");

        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with ID: " + staffUserId));

        if (staffUser.getRole() != UserRole.BANK_STAFF) {
            throw new BusinessException(
                    "User is not a bank staff member",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STAFF_ROLE"
            );
        }

        if (staffUser.isActive()) {
            throw new BusinessException(
                    "Staff user is already activated",
                    HttpStatus.BAD_REQUEST,
                    "USER_ALREADY_ACTIVATED"
            );
        }

        // 1. Invalidate previous unconsumed activation tokens
        tokenRepository.invalidateTokensForUser(staffUserId, Instant.now());

        // 2. Generate and persist fresh activation token hash
        String rawActivationToken = generateAndPersistActivationToken(staffUserId);

        // 3. Dispatch in-memory ephemeral notification event after transaction commit
        publishActivationEventAfterCommit(staffUser, rawActivationToken);

        log.info("Admin [{}] successfully generated fresh activation link for unactivated staff account [{}].",
                adminUserId, staffUser.getUsername());

        return StaffUserResponseDto.fromEntity(staffUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffUserResponseDto> listStaff() {
        return userRepository.findByRole(UserRole.BANK_STAFF)
                .stream()
                .map(StaffUserResponseDto::fromEntity)
                .toList();
    }

    private String generateAndPersistActivationToken(UUID userId) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawActivationToken = HexFormat.of().formatHex(randomBytes);

        // Persist ONLY SHA-256 token hash with 24-hour expiration (Zero plaintext storage)
        String tokenHash = hashToken(rawActivationToken);
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        StaffActivationToken token = new StaffActivationToken(userId, tokenHash, expiresAt);
        tokenRepository.save(token);

        return rawActivationToken;
    }

    private void publishActivationEventAfterCommit(User user, String rawActivationToken) {
        String activationUrl = frontendUrl + "/staff/activate?token=" + rawActivationToken;
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        StaffActivationNotificationEvent event = new StaffActivationNotificationEvent(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                activationUrl,
                expiresAt
        );

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

        if (devMode) {
            log.info("[DEV FALLBACK - DO NOT USE IN PRODUCTION] Generated Staff Activation Link for [{}] ({}): [{}]",
                    user.getUsername(), user.getEmail(), activationUrl);
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
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

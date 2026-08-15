package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.domain.StaffActivationToken;
import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.dto.ActivateStaffRequestDto;
import com.nextgen.bank.auth.dto.StaffActivationResponseDto;
import com.nextgen.bank.auth.event.StaffActivatedEvent;
import com.nextgen.bank.auth.repository.StaffActivationTokenRepository;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;

@Service
@Transactional
public class StaffActivationServiceImpl implements StaffActivationService {

    private static final Logger log = LoggerFactory.getLogger(StaffActivationServiceImpl.class);

    private final StaffActivationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxEventWriter outboxEventWriter;

    public StaffActivationServiceImpl(
            StaffActivationTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OutboxEventWriter outboxEventWriter
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Override
    public StaffActivationResponseDto activateStaff(ActivateStaffRequestDto requestDto) {
        Objects.requireNonNull(requestDto, "Activation request cannot be null");

        // 1. Validate password confirmation
        if (!requestDto.password().equals(requestDto.confirmPassword())) {
            throw new BusinessException(
                    "Password and confirmation password do not match",
                    HttpStatus.BAD_REQUEST,
                    "PASSWORDS_DO_NOT_MATCH"
            );
        }

        // 2. Hash presented token with SHA-256 for secure lookup
        String tokenHash = hashToken(requestDto.token().trim());

        // 3. Database-Level Atomic Token Consumption:
        // Executes UPDATE auth_activation_tokens SET is_used = true, used_at = now WHERE token_hash = ? AND is_used = false AND expires_at > now
        Instant now = Instant.now();
        int affectedRows = tokenRepository.consumeToken(tokenHash, now, now);
        if (affectedRows != 1) {
            throw new BusinessException(
                    "Invalid, expired, or already used activation token",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_OR_EXPIRED_TOKEN"
            );
        }

        // 4. Retrieve token record to obtain associated userId
        StaffActivationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(
                        "Invalid activation token",
                        HttpStatus.BAD_REQUEST,
                        "INVALID_TOKEN"
                ));

        // 5. Retrieve target user
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "Associated staff account not found",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));

        // 6. Verify target account role is strictly BANK_STAFF
        if (user.getRole() != UserRole.BANK_STAFF) {
            throw new BusinessException(
                    "Invalid account role for staff activation",
                    HttpStatus.FORBIDDEN,
                    "INVALID_ROLE"
            );
        }

        // 7. Update User credentials and activate
        user.setPasswordHash(passwordEncoder.encode(requestDto.password()));
        user.setActive(true);
        user.resetFailedAttempts();
        userRepository.save(user);

        // 8. Write transactional audit event to outbox
        outboxEventWriter.write(new StaffActivatedEvent(user.getUserId(), user.getEmail()));

        log.info("Staff account [{}] successfully activated by token holder.", user.getUsername());

        return new StaffActivationResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                "Staff account successfully activated. You may now log in to the Staff Portal."
        );
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

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffActivationServiceTest {

    @Mock
    private StaffActivationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    @InjectMocks
    private StaffActivationServiceImpl activationService;

    private final UUID staffUserId = UUID.randomUUID();
    private final String rawToken = "a1b2c3d4e5f60123456789abcdef0123456789abcdef0123456789abcdef0123";
    private String tokenHash;
    private User unactivatedStaffUser;
    private StaffActivationToken sampleToken;

    @BeforeEach
    void setUp() {
        tokenHash = hashToken(rawToken);

        unactivatedStaffUser = new User(
                "sarah_staff",
                "sarah.connor@nextgenbank.com",
                "UNACTIVATED$PLACEHOLDER",
                UserRole.BANK_STAFF
        );
        unactivatedStaffUser.setUserId(staffUserId);
        unactivatedStaffUser.setActive(false);

        sampleToken = new StaffActivationToken(
                staffUserId,
                tokenHash,
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
    }

    @Test
    @DisplayName("Staff Activation: Valid token atomically consumed, encodes password and activates staff account")
    void testActivateStaff_Success() {
        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                rawToken,
                "NewSecurePassword2026!",
                "NewSecurePassword2026!"
        );

        when(tokenRepository.consumeToken(eq(tokenHash), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(sampleToken));
        when(userRepository.findById(staffUserId)).thenReturn(Optional.of(unactivatedStaffUser));
        when(passwordEncoder.encode("NewSecurePassword2026!")).thenReturn("bcrypt_new_password_hash");

        StaffActivationResponseDto response = activationService.activateStaff(request);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(staffUserId);
        assertThat(response.username()).isEqualTo("sarah_staff");

        // Verify user is activated with new password hash
        assertThat(unactivatedStaffUser.isActive()).isTrue();
        assertThat(unactivatedStaffUser.getPasswordHash()).isEqualTo("bcrypt_new_password_hash");
        verify(userRepository).save(unactivatedStaffUser);

        // Verify outbox event is written
        verify(outboxEventWriter).write(any(StaffActivatedEvent.class));
    }

    @Test
    @DisplayName("Staff Activation: Concurrent race or already consumed/expired token returns 0 affected rows and throws 400")
    void testActivateStaff_ConcurrentRace_ZeroAffectedRows_ThrowsBadRequest() {
        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                rawToken,
                "NewSecurePassword2026!",
                "NewSecurePassword2026!"
        );

        // Atomic DB consumption returns 0 (already consumed by concurrent thread or expired)
        when(tokenRepository.consumeToken(eq(tokenHash), any(Instant.class), any(Instant.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> activationService.activateStaff(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid, expired, or already used")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any());
    }

    @Test
    @DisplayName("Staff Activation: Password confirmation mismatch throws 400 BAD_REQUEST")
    void testActivateStaff_PasswordMismatch_ThrowsBadRequest() {
        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                rawToken,
                "NewSecurePassword2026!",
                "DifferentPassword2026!"
        );

        assertThatThrownBy(() -> activationService.activateStaff(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("do not match")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(tokenRepository, never()).consumeToken(any(), any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Staff Activation: Token for non-BANK_STAFF role is rejected with 403 FORBIDDEN")
    void testActivateStaff_NonStaffUserRole_ThrowsForbidden() {
        unactivatedStaffUser.setRole(UserRole.CUSTOMER);

        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                rawToken,
                "NewSecurePassword2026!",
                "NewSecurePassword2026!"
        );

        when(tokenRepository.consumeToken(eq(tokenHash), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(sampleToken));
        when(userRepository.findById(staffUserId)).thenReturn(Optional.of(unactivatedStaffUser));

        assertThatThrownBy(() -> activationService.activateStaff(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid account role")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Staff Activation: Associated user not found throws 404 NOT_FOUND")
    void testActivateStaff_TargetUserNotFound_ThrowsNotFound() {
        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                rawToken,
                "NewSecurePassword2026!",
                "NewSecurePassword2026!"
        );

        when(tokenRepository.consumeToken(eq(tokenHash), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(sampleToken));
        when(userRepository.findById(staffUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationService.activateStaff(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Associated staff account not found")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Staff Activation: Token record not found after consumption throws 400 BAD_REQUEST")
    void testActivateStaff_TokenRecordNotFound_ThrowsBadRequest() {
        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                rawToken,
                "NewSecurePassword2026!",
                "NewSecurePassword2026!"
        );

        when(tokenRepository.consumeToken(eq(tokenHash), any(Instant.class), any(Instant.class)))
                .thenReturn(1);
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationService.activateStaff(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid activation token")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userRepository, never()).save(any());
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

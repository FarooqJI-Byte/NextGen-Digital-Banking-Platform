package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.domain.OtpToken;
import com.nextgen.bank.auth.dto.OtpVerificationResponseDto;
import com.nextgen.bank.auth.event.CustomerOtpNotificationEvent;
import com.nextgen.bank.auth.repository.OtpTokenRepository;
import com.nextgen.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OtpServiceImpl otpService;

    private final String identifier = "john.doe@example.com";
    private final String purpose = "CUSTOMER_REGISTRATION";

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl(otpTokenRepository, eventPublisher, true);
    }

    @Test
    @DisplayName("Should generate 6-digit OTP, store SHA-256 hash, and publish in-memory event")
    void testGenerateAndSendOtp_Success() {
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        ArgumentCaptor<CustomerOtpNotificationEvent> eventCaptor = ArgumentCaptor.forClass(CustomerOtpNotificationEvent.class);

        otpService.generateAndSendOtp(identifier, purpose);

        verify(otpTokenRepository).invalidatePreviousOtps(eq(identifier), eq(purpose), any(Instant.class));
        verify(otpTokenRepository).save(tokenCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OtpToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getIdentifier()).isEqualTo(identifier);
        assertThat(savedToken.getPurpose()).isEqualTo(purpose);
        assertThat(savedToken.getOtpHash()).isNotBlank().hasSize(64);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isUsed()).isFalse();

        CustomerOtpNotificationEvent event = eventCaptor.getValue();
        assertThat(event.identifier()).isEqualTo(identifier);
        assertThat(event.purpose()).isEqualTo(purpose);
        assertThat(event.otp()).matches("^[0-9]{6}$");
    }

    @Test
    @DisplayName("Should successfully verify valid OTP and mark it used atomically")
    void testVerifyOtp_Success() {
        String rawOtp = "123456";
        String otpHash = hashOtp(rawOtp);
        OtpToken otpToken = new OtpToken(identifier, purpose, otpHash, Instant.now().plus(10, ChronoUnit.MINUTES));

        when(otpTokenRepository.findActiveOtp(identifier, purpose)).thenReturn(Optional.of(otpToken));

        OtpVerificationResponseDto response = otpService.verifyOtp(identifier, purpose, rawOtp);

        assertThat(response.verified()).isTrue();
        assertThat(otpToken.isUsed()).isTrue();
        assertThat(otpToken.getUsedAt()).isNotNull();
        verify(otpTokenRepository).save(otpToken);
    }

    @Test
    @DisplayName("Should reject expired OTP with OTP_EXPIRED")
    void testVerifyOtp_Expired() {
        String rawOtp = "123456";
        String otpHash = hashOtp(rawOtp);
        OtpToken expiredToken = new OtpToken(identifier, purpose, otpHash, Instant.now().minus(1, ChronoUnit.MINUTES));

        when(otpTokenRepository.findActiveOtp(identifier, purpose)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> otpService.verifyOtp(identifier, purpose, rawOtp))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OTP has expired")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("OTP_EXPIRED"));
    }

    @Test
    @DisplayName("Should increment attempts on mismatch and reject invalid OTP")
    void testVerifyOtp_InvalidOtp_IncrementsAttempts() {
        String correctOtp = "123456";
        String otpHash = hashOtp(correctOtp);
        OtpToken otpToken = new OtpToken(identifier, purpose, otpHash, Instant.now().plus(10, ChronoUnit.MINUTES));

        when(otpTokenRepository.findActiveOtp(identifier, purpose)).thenReturn(Optional.of(otpToken));

        assertThatThrownBy(() -> otpService.verifyOtp(identifier, purpose, "999999"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid OTP")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("INVALID_OTP"));

        assertThat(otpToken.getAttempts()).isEqualTo(1);
        verify(otpTokenRepository).save(otpToken);
    }

    @Test
    @DisplayName("Should reject OTP when max attempts limit is exceeded")
    void testVerifyOtp_MaxAttemptsExceeded() {
        String correctOtp = "123456";
        String otpHash = hashOtp(correctOtp);
        OtpToken otpToken = new OtpToken(identifier, purpose, otpHash, Instant.now().plus(10, ChronoUnit.MINUTES));
        otpToken.setAttempts(3);

        when(otpTokenRepository.findActiveOtp(identifier, purpose)).thenReturn(Optional.of(otpToken));

        assertThatThrownBy(() -> otpService.verifyOtp(identifier, purpose, correctOtp))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Maximum verification attempts exceeded")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("OTP_MAX_ATTEMPTS_EXCEEDED"));
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
            throw new RuntimeException(e);
        }
    }
}

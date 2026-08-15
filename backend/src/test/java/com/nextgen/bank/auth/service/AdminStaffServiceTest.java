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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
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
class AdminStaffServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffActivationTokenRepository tokenRepository;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminStaffServiceImpl adminStaffService;

    private final UUID adminUserId = UUID.randomUUID();
    private CreateStaffRequestDto validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateStaffRequestDto(
                "sarah_staff",
                "sarah.connor@nextgenbank.com"
        );
        adminStaffService = new AdminStaffServiceImpl(
                userRepository,
                tokenRepository,
                outboxEventWriter,
                eventPublisher,
                "http://localhost:5173",
                true
        );
    }

    @Test
    @DisplayName("Admin creates BANK_STAFF successfully with unactivated state, token hash in DB, and in-memory event")
    void testCreateStaff_Success() {
        when(userRepository.existsByUsername("sarah_staff")).thenReturn(false);
        when(userRepository.existsByEmail("sarah.connor@nextgenbank.com")).thenReturn(false);

        User savedUser = new User(
                "sarah_staff",
                "sarah.connor@nextgenbank.com",
                "UNACTIVATED$HASH",
                UserRole.BANK_STAFF
        );
        UUID staffId = UUID.randomUUID();
        savedUser.setUserId(staffId);
        savedUser.setActive(false);
        savedUser.setCreatedAt(Instant.now());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        StaffUserResponseDto response = adminStaffService.createStaff(validRequest, adminUserId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(staffId);
        assertThat(response.username()).isEqualTo("sarah_staff");
        assertThat(response.email()).isEqualTo("sarah.connor@nextgenbank.com");
        assertThat(response.role()).isEqualTo(UserRole.BANK_STAFF); // Role strictly BANK_STAFF
        assertThat(response.isActive()).isFalse(); // Initial state: not activated

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();
        assertThat(created.getRole()).isEqualTo(UserRole.BANK_STAFF);
        assertThat(created.isActive()).isFalse();

        // Verify SHA-256 token hash is persisted
        ArgumentCaptor<StaffActivationToken> tokenCaptor = ArgumentCaptor.forClass(StaffActivationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        StaffActivationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenHash()).isNotBlank();
        assertThat(savedToken.getTokenHash()).hasSize(64); // SHA-256 hex length
        assertThat(savedToken.getUserId()).isEqualTo(staffId);
        assertThat(savedToken.isUsed()).isFalse();

        verify(outboxEventWriter).write(any(UserRegisteredEvent.class));
        verify(eventPublisher).publishEvent(any(StaffActivationNotificationEvent.class));
    }

    @Test
    @DisplayName("Should successfully resend activation link by invalidating old tokens and generating fresh token")
    void testResendStaffActivation_Success() {
        UUID staffId = UUID.randomUUID();
        User staffUser = new User(
                "sarah_staff",
                "sarah.connor@nextgenbank.com",
                "UNACTIVATED$HASH",
                UserRole.BANK_STAFF
        );
        staffUser.setUserId(staffId);
        staffUser.setActive(false);

        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));

        StaffUserResponseDto response = adminStaffService.resendStaffActivation(staffId, adminUserId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(staffId);

        verify(tokenRepository).invalidateTokensForUser(eq(staffId), any(Instant.class));
        verify(tokenRepository).save(any(StaffActivationToken.class));
        verify(eventPublisher).publishEvent(any(StaffActivationNotificationEvent.class));
    }

    @Test
    @DisplayName("Should reject resend activation if staff user is already activated")
    void testResendStaffActivation_AlreadyActive_ThrowsBadRequest() {
        UUID staffId = UUID.randomUUID();
        User staffUser = new User(
                "sarah_staff",
                "sarah.connor@nextgenbank.com",
                "REAL_PASSWORD_HASH",
                UserRole.BANK_STAFF
        );
        staffUser.setUserId(staffId);
        staffUser.setActive(true);

        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));

        assertThatThrownBy(() -> adminStaffService.resendStaffActivation(staffId, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already activated")
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo("USER_ALREADY_ACTIVATED"));

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject staff creation if username is already taken with 409 Conflict")
    void testCreateStaff_DuplicateUsername_ThrowsConflict() {
        when(userRepository.existsByUsername("sarah_staff")).thenReturn(true);

        assertThatThrownBy(() -> adminStaffService.createStaff(validRequest, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username is already taken")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any());
    }

    @Test
    @DisplayName("Should reject staff creation if email is already taken with 409 Conflict")
    void testCreateStaff_DuplicateEmail_ThrowsConflict() {
        when(userRepository.existsByUsername("sarah_staff")).thenReturn(false);
        when(userRepository.existsByEmail("sarah.connor@nextgenbank.com")).thenReturn(true);

        assertThatThrownBy(() -> adminStaffService.createStaff(validRequest, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email is already registered")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle concurrent database duplicate key violation safely")
    void testCreateStaff_ConcurrentRace_HandlesGracefully() {
        when(userRepository.existsByUsername("sarah_staff")).thenReturn(false);
        when(userRepository.existsByEmail("sarah.connor@nextgenbank.com")).thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key violates unique constraint"));

        assertThatThrownBy(() -> adminStaffService.createStaff(validRequest, adminUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already in use")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Should list all BANK_STAFF employees")
    void testListStaff_ReturnsRoster() {
        User staff1 = new User("staff1", "staff1@bank.com", "hash1", UserRole.BANK_STAFF);
        staff1.setUserId(UUID.randomUUID());
        staff1.setCreatedAt(Instant.now());

        User staff2 = new User("staff2", "staff2@bank.com", "hash2", UserRole.BANK_STAFF);
        staff2.setUserId(UUID.randomUUID());
        staff2.setCreatedAt(Instant.now());

        when(userRepository.findByRole(UserRole.BANK_STAFF)).thenReturn(List.of(staff1, staff2));

        List<StaffUserResponseDto> list = adminStaffService.listStaff();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).username()).isEqualTo("staff1");
        assertThat(list.get(1).username()).isEqualTo("staff2");
    }
}

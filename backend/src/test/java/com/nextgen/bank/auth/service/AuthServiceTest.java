package com.nextgen.bank.auth.service;

import com.nextgen.bank.auth.domain.User;
import com.nextgen.bank.auth.domain.UserSession;
import com.nextgen.bank.auth.dto.LoginRequestDto;
import com.nextgen.bank.auth.dto.LoginResponseDto;
import com.nextgen.bank.auth.dto.RegisterRequestDto;
import com.nextgen.bank.auth.dto.RegisterResponseDto;
import com.nextgen.bank.auth.event.UserLoggedInEvent;
import com.nextgen.bank.auth.event.UserRegisteredEvent;
import com.nextgen.bank.auth.repository.UserRepository;
import com.nextgen.bank.auth.repository.UserSessionRepository;
import com.nextgen.bank.auth.security.JwtTokenProvider;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private final UUID sampleUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sampleUser = new User("johndoe", "john.doe@example.com", "hashed_password", UserRole.CUSTOMER);
        sampleUser.setUserId(sampleUserId);
        sampleUser.setActive(true);
        sampleUser.setFailedLoginAttempts(0);
        sampleUser.setCreatedAt(Instant.now());
        sampleUser.setUpdatedAt(Instant.now());
    }

    @Test
    @DisplayName("Should successfully register a new user, encode password, and enforce CUSTOMER role")
    void testValidRegistration() {
        RegisterRequestDto request = new RegisterRequestDto("johndoe", "john.doe@example.com", "Password123!", UserRole.CUSTOMER);

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        RegisterResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(sampleUserId);
        assertThat(response.username()).isEqualTo("johndoe");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(outboxEventWriter).write(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Attempt to register with ADMIN role must NOT create ADMIN - role is forced to CUSTOMER")
    void testRegisterWithAdminRole_EnforcesCustomerRole() {
        RegisterRequestDto request = new RegisterRequestDto("badactor", "admin.hack@example.com", "Password123!", UserRole.ADMIN);

        when(userRepository.existsByUsername("badactor")).thenReturn(false);
        when(userRepository.existsByEmail("admin.hack@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });

        RegisterResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Attempt to register with BANK_STAFF role must NOT create BANK_STAFF - role is forced to CUSTOMER")
    void testRegisterWithBankStaffRole_EnforcesCustomerRole() {
        RegisterRequestDto request = new RegisterRequestDto("staffaspirant", "staff.hack@example.com", "Password123!", UserRole.BANK_STAFF);

        when(userRepository.existsByUsername("staffaspirant")).thenReturn(false);
        when(userRepository.existsByEmail("staff.hack@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });

        RegisterResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Attempt to register with AUDITOR role must NOT create AUDITOR - role is forced to CUSTOMER")
    void testRegisterWithAuditorRole_EnforcesCustomerRole() {
        RegisterRequestDto request = new RegisterRequestDto("auditoraspirant", "auditor.hack@example.com", "Password123!", UserRole.AUDITOR);

        when(userRepository.existsByUsername("auditoraspirant")).thenReturn(false);
        when(userRepository.existsByEmail("auditor.hack@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(UUID.randomUUID());
            return u;
        });

        RegisterResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Should reject registration if username already exists with 409 Conflict")
    void testDuplicateUsername() {
        RegisterRequestDto request = new RegisterRequestDto("johndoe", "john.doe@example.com", "Password123!", UserRole.CUSTOMER);
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username is already taken")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any());
    }

    @Test
    @DisplayName("Should reject registration if email already exists with 409 Conflict")
    void testDuplicateEmail() {
        RegisterRequestDto request = new RegisterRequestDto("newuser", "john.doe@example.com", "Password123!", UserRole.CUSTOMER);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email is already registered")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Customer Login: should succeed for user with CUSTOMER role, reset failed attempts, persist session")
    void testCustomerLogin_WithCustomerRole_Success() {
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("customer.access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("ref_cust_123");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        LoginResponseDto response = authService.customerLogin(request, "127.0.0.1", "Browser");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("customer.access.token");
        assertThat(response.refreshToken()).isEqualTo("ref_cust_123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);

        // Verify session persistence
        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        UserSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(sampleUserId);

        // Verify outbox event
        verify(outboxEventWriter).write(any(UserLoggedInEvent.class));
    }

    @Test
    @DisplayName("Customer Login: should reject BANK_STAFF role with generic 401 Unauthorized WITHOUT incrementing failed attempts")
    void testCustomerLogin_WithBankStaffRole_RejectedWithoutLockoutIncrement() {
        sampleUser.setRole(UserRole.BANK_STAFF);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "Browser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        // Wrong portal with correct password must NOT increment failed attempts
        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(0);
        verify(userRepository, never()).save(sampleUser);
    }

    @Test
    @DisplayName("Customer Login: should reject ADMIN role with generic 401 Unauthorized WITHOUT incrementing failed attempts")
    void testCustomerLogin_WithAdminRole_RejectedWithoutLockoutIncrement() {
        sampleUser.setRole(UserRole.ADMIN);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "Browser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(0);
        verify(userRepository, never()).save(sampleUser);
    }

    @Test
    @DisplayName("Staff Login: should succeed for BANK_STAFF role")
    void testStaffLogin_WithBankStaffRole_Success() {
        sampleUser.setRole(UserRole.BANK_STAFF);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("staff.access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("ref_staff_123");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        LoginResponseDto response = authService.staffLogin(request, "127.0.0.1", "Browser");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("staff.access.token");
    }

    @Test
    @DisplayName("Staff Login: should succeed for ADMIN role")
    void testStaffLogin_WithAdminRole_Success() {
        sampleUser.setRole(UserRole.ADMIN);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("admin.access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("ref_admin_123");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        LoginResponseDto response = authService.staffLogin(request, "127.0.0.1", "Browser");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("admin.access.token");
    }

    @Test
    @DisplayName("Staff Login: should succeed for AUDITOR role")
    void testStaffLogin_WithAuditorRole_Success() {
        sampleUser.setRole(UserRole.AUDITOR);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("auditor.access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("ref_auditor_123");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        LoginResponseDto response = authService.staffLogin(request, "127.0.0.1", "Browser");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("auditor.access.token");
    }

    @Test
    @DisplayName("Staff Login: should reject CUSTOMER role with generic 401 Unauthorized WITHOUT incrementing failed attempts")
    void testStaffLogin_WithCustomerRole_RejectedWithoutLockoutIncrement() {
        sampleUser.setRole(UserRole.CUSTOMER);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.staffLogin(request, "127.0.0.1", "Browser"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(0);
        verify(userRepository, never()).save(sampleUser);
    }

    @Test
    @DisplayName("Should increment failed attempts on incorrect password and throw 401 Unauthorized")
    void testWrongPassword_IncrementsFailedAttempts() {
        LoginRequestDto request = new LoginRequestDto("johndoe", "WrongPassword!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword!", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Should lock account for 15 minutes upon 5th consecutive failed attempt (BR-AUTH-002)")
    void testWrongPassword_LocksOutAfter5Attempts() {
        sampleUser.setFailedLoginAttempts(4);
        LoginRequestDto request = new LoginRequestDto("johndoe", "WrongPassword!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword!", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account locked due to 5 consecutive failed login attempts")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.LOCKED));

        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(sampleUser.getLockoutUntil()).isNotNull();
        assertThat(sampleUser.getLockoutUntil()).isAfter(Instant.now());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Should reject login if account is currently locked out")
    void testLogin_AccountAlreadyLocked() {
        sampleUser.setLockoutUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account is temporarily locked")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.LOCKED));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should allow login and reset failed attempts if previous lockout has expired")
    void testLogin_LockoutExpired_AllowsLogin() {
        sampleUser.setFailedLoginAttempts(5);
        sampleUser.setLockoutUntil(Instant.now().minus(5, ChronoUnit.MINUTES)); // Expired lockout
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password123!", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("new.access.token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("ref_new");
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        LoginResponseDto response = authService.customerLogin(request, "127.0.0.1", "agent");

        assertThat(response).isNotNull();
        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(sampleUser.getLockoutUntil()).isNull();
    }

    @Test
    @DisplayName("Should reset failed attempts to 1 when wrong password entered after previous lockout expired")
    void testLogin_LockoutExpired_WrongPasswordResetsCounterToOne() {
        sampleUser.setFailedLoginAttempts(5);
        sampleUser.setLockoutUntil(Instant.now().minus(5, ChronoUnit.MINUTES)); // Expired lockout
        LoginRequestDto request = new LoginRequestDto("johndoe", "WrongPassword!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword!", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid username or password")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));

        assertThat(sampleUser.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(sampleUser.getLockoutUntil()).isNull();
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Should reject login if user account is inactive with 403 Forbidden")
    void testLogin_InactiveAccount() {
        sampleUser.setActive(false);
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.customerLogin(request, "127.0.0.1", "agent"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account is disabled")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}

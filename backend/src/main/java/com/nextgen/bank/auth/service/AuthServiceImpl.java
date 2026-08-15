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
import java.util.Set;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final Set<UserRole> CUSTOMER_ROLES = Set.of(UserRole.CUSTOMER);
    private static final Set<UserRole> STAFF_ROLES = Set.of(UserRole.BANK_STAFF, UserRole.ADMIN, UserRole.AUDITOR);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OutboxEventWriter outboxEventWriter;
    private final OtpService otpService;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            OutboxEventWriter outboxEventWriter,
            OtpService otpService
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.outboxEventWriter = outboxEventWriter;
        this.otpService = otpService;
    }

    @Override
    public RegisterResponseDto register(RegisterRequestDto requestDto) {
        Objects.requireNonNull(requestDto, "Register request cannot be null");

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

        // BR-AUTH: Public registration MUST ALWAYS assign UserRole.CUSTOMER.
        // Untrusted client-supplied roles (e.g. ADMIN, BANK_STAFF, AUDITOR) are never permitted.
        User user = new User(
                normalizedUsername,
                normalizedEmail,
                passwordEncoder.encode(requestDto.password()),
                UserRole.CUSTOMER
        );

        User savedUser = userRepository.save(user);

        outboxEventWriter.write(new UserRegisteredEvent(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getRole()
        ));

        // Automatically trigger OTP generation and dispatch ephemeral notification event
        otpService.generateAndSendOtp(savedUser.getEmail(), "CUSTOMER_REGISTRATION");

        return RegisterResponseDto.fromEntity(savedUser);
    }

    @Override
    public LoginResponseDto customerLogin(LoginRequestDto requestDto, String ipAddress, String userAgent) {
        return authenticateAndCreateSession(requestDto, ipAddress, userAgent, CUSTOMER_ROLES);
    }

    @Override
    public LoginResponseDto staffLogin(LoginRequestDto requestDto, String ipAddress, String userAgent) {
        return authenticateAndCreateSession(requestDto, ipAddress, userAgent, STAFF_ROLES);
    }

    private LoginResponseDto authenticateAndCreateSession(
            LoginRequestDto requestDto,
            String ipAddress,
            String userAgent,
            Set<UserRole> allowedRoles
    ) {
        User user = userRepository.findByUsername(requestDto.username().trim())
                .orElseThrow(() -> new BusinessException(
                        "Invalid username or password",
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS"
                ));

        // Check if account is active
        if (!user.isActive()) {
            throw new BusinessException(
                    "Account is disabled. Please contact bank support.",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_DISABLED"
            );
        }

        // BR-AUTH-002: Account Lockout Invariant Check
        if (user.isLockedOut()) {
            throw new BusinessException(
                    "Account is temporarily locked due to multiple failed login attempts. Please try again later.",
                    HttpStatus.LOCKED,
                    "ACCOUNT_LOCKED"
            );
        }

        // Validate password
        if (!passwordEncoder.matches(requestDto.password(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);

            if (user.isLockedOut()) {
                throw new BusinessException(
                        "Account locked due to 5 consecutive failed login attempts. Locked for 15 minutes.",
                        HttpStatus.LOCKED,
                        "ACCOUNT_LOCKED"
                );
            }

            throw new BusinessException(
                    "Invalid username or password",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }

        // Role verification for portal boundary (Customer portal vs. Staff/Admin portal)
        if (allowedRoles != null && !allowedRoles.contains(user.getRole())) {
            // Role mismatch for this portal with valid password:
            // Do NOT increment failed attempts (prevents lockout DoS attacks across portals),
            // but return identical generic 401 Unauthorized to prevent role/account enumeration.
            throw new BusinessException(
                    "Invalid username or password",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }

        // Successful authentication: reset failed attempts
        user.resetFailedAttempts();
        userRepository.save(user);

        // Generate JWT Access and Refresh Tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Persist Session (Store SHA-256 hashed refresh token per security specification)
        String refreshTokenHash = hashRefreshToken(refreshToken);
        Instant sessionExpiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        UserSession session = new UserSession(
                user.getUserId(),
                refreshTokenHash,
                ipAddress != null ? ipAddress : "UNKNOWN",
                userAgent != null ? userAgent : "UNKNOWN",
                sessionExpiresAt
        );
        userSessionRepository.save(session);

        // Write transactional outbox event
        outboxEventWriter.write(new UserLoggedInEvent(
                user.getUserId(),
                ipAddress,
                userAgent
        ));

        return new LoginResponseDto(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
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

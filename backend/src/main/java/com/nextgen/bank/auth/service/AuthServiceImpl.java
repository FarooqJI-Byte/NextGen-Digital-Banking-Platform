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
import com.nextgen.bank.common.event.OutboxEventWriter;
import com.nextgen.bank.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OutboxEventWriter outboxEventWriter;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            OutboxEventWriter outboxEventWriter
    ) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Override
    public RegisterResponseDto register(RegisterRequestDto requestDto) {
        String normalizedEmail = requestDto.email().trim().toLowerCase();

        if (userRepository.existsByUsername(requestDto.username().trim())) {
            throw new BusinessException(
                    "Username is already taken",
                    HttpStatus.CONFLICT,
                    "USERNAME_ALREADY_EXISTS"
            );
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(
                    "Email is already registered",
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS"
            );
        }

        String encodedPassword = passwordEncoder.encode(requestDto.password());

        User user = new User(
                requestDto.username().trim(),
                normalizedEmail,
                encodedPassword,
                requestDto.role()
        );

        User savedUser = userRepository.save(user);

        outboxEventWriter.write(new UserRegisteredEvent(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getRole()
        ));

        return RegisterResponseDto.fromEntity(savedUser);
    }

    @Override
    public LoginResponseDto login(LoginRequestDto requestDto, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(requestDto.username().trim())
                .orElseThrow(() -> new BusinessException(
                        "Invalid username or password",
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS"
                ));

        Instant now = Instant.now();

        if (user.getLockoutUntil() != null) {
            if (user.getLockoutUntil().isAfter(now)) {
                throw new BusinessException(
                        "Account is temporarily locked due to multiple failed login attempts. Please try again later.",
                        HttpStatus.LOCKED,
                        "ACCOUNT_LOCKED"
                );
            } else {
                user.setLockoutUntil(null);
                user.setFailedLoginAttempts(0);
            }
        }

        if (!user.isActive()) {
            throw new BusinessException(
                    "Account is disabled",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_DISABLED"
            );
        }

        if (!passwordEncoder.matches(requestDto.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setLockoutUntil(now.plus(15, ChronoUnit.MINUTES));
                userRepository.save(user);
                throw new BusinessException(
                        "Account locked due to 5 consecutive failed login attempts",
                        HttpStatus.LOCKED,
                        "ACCOUNT_LOCKED"
                );
            }

            userRepository.save(user);
            throw new BusinessException(
                    "Invalid username or password",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }

        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        String refreshTokenHash = hashToken(refreshToken);

        Instant sessionExpiry = now.plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        UserSession session = new UserSession(
                user.getUserId(),
                refreshTokenHash,
                ipAddress != null ? ipAddress : "unknown",
                userAgent != null ? userAgent : "unknown",
                sessionExpiry
        );
        userSessionRepository.save(session);

        outboxEventWriter.write(new UserLoggedInEvent(
                user.getUserId(),
                ipAddress,
                userAgent
        ));

        return new LoginResponseDto(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm unavailable", e);
        }
    }
}

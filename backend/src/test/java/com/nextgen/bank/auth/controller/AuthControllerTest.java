package com.nextgen.bank.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.bank.auth.dto.LoginRequestDto;
import com.nextgen.bank.auth.dto.LoginResponseDto;
import com.nextgen.bank.auth.dto.RegisterRequestDto;
import com.nextgen.bank.auth.dto.RegisterResponseDto;
import com.nextgen.bank.auth.service.AuthService;
import com.nextgen.bank.common.enums.UserRole;
import com.nextgen.bank.common.exception.BusinessException;
import com.nextgen.bank.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 Created on valid payload")
    void testRegisterSuccess() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "Password123!",
                UserRole.CUSTOMER
        );

        UUID userId = UUID.randomUUID();
        RegisterResponseDto response = new RegisterResponseDto(
                userId,
                "johndoe",
                "john.doe@example.com",
                UserRole.CUSTOMER,
                Instant.now()
        );

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 Bad Request on weak password")
    void testRegisterWeakPassword() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "weak",
                UserRole.CUSTOMER
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 Conflict on duplicate username")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "johndoe",
                "john.doe@example.com",
                "Password123!",
                UserRole.CUSTOMER
        );

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new BusinessException("Username is already taken", HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK and token payload when valid")
    void testLoginSuccess() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");
        LoginResponseDto response = new LoginResponseDto(
                "mock.access.token",
                "ref_123456",
                "Bearer",
                900L
        );

        when(authService.login(any(LoginRequestDto.class), eq("192.168.1.100"), eq("Mozilla/5.0 (TestAgent)")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "192.168.1.100")
                        .header("User-Agent", "Mozilla/5.0 (TestAgent)")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("ref_123456"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should fallback to remoteAddr when X-Forwarded-For is absent")
    void testLoginWithRemoteAddrFallback() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");
        LoginResponseDto response = new LoginResponseDto(
                "mock.access.token",
                "ref_123456",
                "Bearer",
                900L
        );

        when(authService.login(any(LoginRequestDto.class), eq("127.0.0.1"), eq("CustomAgent/1.0")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("User-Agent", "CustomAgent/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock.access.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 Unauthorized on invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "WrongPass!");

        when(authService.login(any(LoginRequestDto.class), anyString(), nullable(String.class)))
                .thenThrow(new BusinessException("Invalid username or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 423 Locked when account is locked out")
    void testLoginAccountLocked() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(authService.login(any(LoginRequestDto.class), anyString(), nullable(String.class)))
                .thenThrow(new BusinessException("Account is temporarily locked", HttpStatus.LOCKED, "ACCOUNT_LOCKED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }
}

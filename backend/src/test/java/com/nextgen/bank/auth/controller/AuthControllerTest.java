package com.nextgen.bank.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.bank.auth.dto.ActivateStaffRequestDto;
import com.nextgen.bank.auth.dto.GenerateOtpRequestDto;
import com.nextgen.bank.auth.dto.LoginRequestDto;
import com.nextgen.bank.auth.dto.LoginResponseDto;
import com.nextgen.bank.auth.dto.OtpVerificationResponseDto;
import com.nextgen.bank.auth.dto.RegisterRequestDto;
import com.nextgen.bank.auth.dto.RegisterResponseDto;
import com.nextgen.bank.auth.dto.ResendOtpRequestDto;
import com.nextgen.bank.auth.dto.StaffActivationResponseDto;
import com.nextgen.bank.auth.dto.VerifyOtpRequestDto;
import com.nextgen.bank.auth.service.AuthService;
import com.nextgen.bank.auth.service.OtpService;
import com.nextgen.bank.auth.service.StaffActivationService;
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
import static org.mockito.Mockito.doNothing;
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

    @Mock
    private StaffActivationService staffActivationService;

    @Mock
    private OtpService otpService;

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
    @DisplayName("POST /api/v1/auth/otp/generate should return 200 OK")
    void testGenerateOtp_Success() throws Exception {
        GenerateOtpRequestDto request = new GenerateOtpRequestDto("john.doe@example.com", "CUSTOMER_REGISTRATION");
        doNothing().when(otpService).generateAndSendOtp(eq("john.doe@example.com"), eq("CUSTOMER_REGISTRATION"));

        mockMvc.perform(post("/api/v1/auth/otp/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP generated and dispatched to notification service."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/otp/verify should return 200 OK on valid OTP")
    void testVerifyOtp_Success() throws Exception {
        VerifyOtpRequestDto request = new VerifyOtpRequestDto("john.doe@example.com", "CUSTOMER_REGISTRATION", "123456");
        OtpVerificationResponseDto response = OtpVerificationResponseDto.success("john.doe@example.com", "CUSTOMER_REGISTRATION", "OTP verified successfully.");

        when(otpService.verifyOtp(eq("john.doe@example.com"), eq("CUSTOMER_REGISTRATION"), eq("123456")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.identifier").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/otp/resend should return 200 OK")
    void testResendOtp_Success() throws Exception {
        ResendOtpRequestDto request = new ResendOtpRequestDto("john.doe@example.com", "CUSTOMER_REGISTRATION");
        doNothing().when(otpService).resendOtp(eq("john.doe@example.com"), eq("CUSTOMER_REGISTRATION"));

        mockMvc.perform(post("/api/v1/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fresh OTP generated and dispatched to notification service."));
    }

    @Test
    @DisplayName("POST /api/v1/auth/customer/login should return 200 OK on valid credentials")
    void testCustomerLoginSuccess() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");
        LoginResponseDto response = new LoginResponseDto("access.jwt.token", "refresh.jwt.token", "Bearer", 900L);

        when(authService.customerLogin(any(LoginRequestDto.class), anyString(), nullable(String.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /api/v1/auth/staff/login should return 200 OK for valid staff")
    void testStaffLoginSuccess() throws Exception {
        LoginRequestDto request = new LoginRequestDto("staffuser", "Password123!");
        LoginResponseDto response = new LoginResponseDto("staff.access.token", "staff.refresh.token", "Bearer", 900L);

        when(authService.staffLogin(any(LoginRequestDto.class), anyString(), nullable(String.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("staff.access.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/customer/login should return 401 Unauthorized on invalid credentials")
    void testCustomerLoginInvalidCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "WrongPass!");

        when(authService.customerLogin(any(LoginRequestDto.class), anyString(), nullable(String.class)))
                .thenThrow(new BusinessException("Invalid username or password", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/customer/login should return 423 Locked on locked account")
    void testCustomerLoginAccountLocked() throws Exception {
        LoginRequestDto request = new LoginRequestDto("johndoe", "Password123!");

        when(authService.customerLogin(any(LoginRequestDto.class), anyString(), nullable(String.class)))
                .thenThrow(new BusinessException("Account is temporarily locked", HttpStatus.LOCKED, "ACCOUNT_LOCKED"));

        mockMvc.perform(post("/api/v1/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/staff/activate should return 200 OK on valid activation")
    void testActivateStaffSuccess() throws Exception {
        ActivateStaffRequestDto request = new ActivateStaffRequestDto(
                "token_12345",
                "NewPassword123!",
                "NewPassword123!"
        );

        UUID staffId = UUID.randomUUID();
        StaffActivationResponseDto response = new StaffActivationResponseDto(
                staffId,
                "staffuser",
                "staff@bank.com",
                "Staff account successfully activated"
        );

        when(staffActivationService.activateStaff(any(ActivateStaffRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/staff/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(staffId.toString()))
                .andExpect(jsonPath("$.username").value("staffuser"));
    }
}

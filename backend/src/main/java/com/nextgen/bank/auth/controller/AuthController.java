package com.nextgen.bank.auth.controller;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final StaffActivationService staffActivationService;
    private final OtpService otpService;

    public AuthController(
            AuthService authService,
            StaffActivationService staffActivationService,
            OtpService otpService
    ) {
        this.authService = authService;
        this.staffActivationService = staffActivationService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        RegisterResponseDto response = authService.register(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<Map<String, String>> generateOtp(@Valid @RequestBody GenerateOtpRequestDto requestDto) {
        otpService.generateAndSendOtp(requestDto.identifier(), requestDto.purpose());
        return ResponseEntity.ok(Map.of("message", "OTP generated and dispatched to notification service."));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<OtpVerificationResponseDto> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto requestDto) {
        OtpVerificationResponseDto response = otpService.verifyOtp(
                requestDto.identifier(),
                requestDto.purpose(),
                requestDto.otp()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ResendOtpRequestDto requestDto) {
        otpService.resendOtp(requestDto.identifier(), requestDto.purpose());
        return ResponseEntity.ok(Map.of("message", "Fresh OTP generated and dispatched to notification service."));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<LoginResponseDto> customerLogin(
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletRequest request
    ) {
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponseDto response = authService.customerLogin(requestDto, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff/login")
    public ResponseEntity<LoginResponseDto> staffLogin(
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletRequest request
    ) {
        String ipAddress = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponseDto response = authService.staffLogin(requestDto, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff/activate")
    public ResponseEntity<StaffActivationResponseDto> activateStaff(
            @Valid @RequestBody ActivateStaffRequestDto requestDto
    ) {
        StaffActivationResponseDto response = staffActivationService.activateStaff(requestDto);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

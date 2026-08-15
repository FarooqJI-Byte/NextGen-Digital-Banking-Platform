package com.nextgen.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequestDto(
        @NotBlank(message = "Identifier is mandatory")
        String identifier,

        @NotBlank(message = "Purpose is mandatory")
        String purpose,

        @NotBlank(message = "OTP is mandatory")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be exactly 6 numeric digits")
        String otp
) {
}

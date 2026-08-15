package com.nextgen.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateOtpRequestDto(
        @NotBlank(message = "Identifier (email or mobile) is mandatory")
        String identifier,

        @NotBlank(message = "Purpose is mandatory")
        String purpose
) {
}

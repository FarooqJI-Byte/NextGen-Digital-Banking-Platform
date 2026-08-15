package com.nextgen.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequestDto(
        @NotBlank(message = "Identifier is mandatory")
        String identifier,

        @NotBlank(message = "Purpose is mandatory")
        String purpose
) {
}

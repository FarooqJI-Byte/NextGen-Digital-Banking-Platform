package com.nextgen.bank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActivateStaffRequestDto(
        @NotBlank(message = "Activation token is mandatory")
        String token,

        @NotBlank(message = "Password is mandatory")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must be at least 8 characters with 1 uppercase, 1 lowercase, 1 digit, and 1 special character (@$!%*?&)"
        )
        String password,

        @NotBlank(message = "Password confirmation is mandatory")
        String confirmPassword
) {
}

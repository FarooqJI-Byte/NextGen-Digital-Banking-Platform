package com.nextgen.bank.customer.dto;

import com.nextgen.bank.common.enums.KYCStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record KYCVerificationRequestDto(
        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "Verification status is required")
        KYCStatus status,

        String remarks
) {
}

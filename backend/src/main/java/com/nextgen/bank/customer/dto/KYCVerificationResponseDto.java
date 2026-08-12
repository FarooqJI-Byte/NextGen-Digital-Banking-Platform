package com.nextgen.bank.customer.dto;

import com.nextgen.bank.common.enums.KYCStatus;

import java.time.Instant;
import java.util.UUID;

public record KYCVerificationResponseDto(
        UUID customerId,
        KYCStatus kycStatus,
        Instant verifiedAt
) {
}

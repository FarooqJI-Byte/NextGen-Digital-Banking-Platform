package com.nextgen.bank.customer.dto;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.customer.domain.enums.DocumentType;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary DTO representing an active KYC submission in the Bank Staff review queue.
 */
public record PendingKycItemDto(
        UUID customerId,
        String customerNumber,
        String customerName,
        String email,
        String phone,
        KYCStatus kycStatus,
        UUID documentId,
        DocumentType documentType,
        String fileReference,
        Instant submittedAt
) {
}

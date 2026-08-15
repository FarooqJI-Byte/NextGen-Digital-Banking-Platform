package com.nextgen.bank.customer.dto;

import com.nextgen.bank.customer.domain.KYCDocument;
import com.nextgen.bank.customer.domain.enums.DocumentType;
import com.nextgen.bank.customer.domain.enums.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for staff inspection of submitted customer KYC documents.
 */
public record KYCDocumentResponseDto(
        UUID documentId,
        DocumentType documentType,
        VerificationStatus verificationStatus,
        String fileReference,
        Instant verifiedAt,
        UUID verifiedBy
) {
    public static KYCDocumentResponseDto fromEntity(KYCDocument doc) {
        return new KYCDocumentResponseDto(
                doc.getDocumentId(),
                doc.getDocumentType(),
                doc.getVerificationStatus(),
                doc.getFileReference(),
                doc.getVerifiedAt(),
                doc.getVerifiedBy()
        );
    }
}

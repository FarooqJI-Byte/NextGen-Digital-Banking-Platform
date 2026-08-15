package com.nextgen.bank.customer.dto;

import com.nextgen.bank.customer.domain.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record KYCSubmissionRequestDto(
        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Document number is required")
        String documentNumber,

        @NotBlank(message = "File reference is required")
        String fileReference
) {
}

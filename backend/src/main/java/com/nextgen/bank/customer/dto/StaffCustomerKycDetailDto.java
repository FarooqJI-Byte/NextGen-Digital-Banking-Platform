package com.nextgen.bank.customer.dto;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.customer.domain.Customer;
import com.nextgen.bank.customer.domain.enums.RiskCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed DTO for compliance staff reviewing a specific customer's identity submission.
 */
public record StaffCustomerKycDetailDto(
        UUID customerId,
        String customerNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        KYCStatus kycStatus,
        RiskCategory riskCategory,
        Instant createdAt,
        List<CustomerAddressDto> addresses,
        List<KYCDocumentResponseDto> documents
) {
    public static StaffCustomerKycDetailDto fromEntities(
            Customer customer,
            List<CustomerAddressDto> addresses,
            List<KYCDocumentResponseDto> documents
    ) {
        return new StaffCustomerKycDetailDto(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getKycStatus(),
                customer.getRiskCategory(),
                customer.getCreatedAt(),
                addresses,
                documents
        );
    }
}

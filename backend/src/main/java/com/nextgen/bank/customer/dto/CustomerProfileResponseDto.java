package com.nextgen.bank.customer.dto;

import com.nextgen.bank.common.enums.KYCStatus;
import com.nextgen.bank.customer.domain.Customer;
import com.nextgen.bank.customer.domain.enums.RiskCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerProfileResponseDto(
        UUID customerId,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phone,
        String email,
        KYCStatus kycStatus,
        RiskCategory riskCategory,
        List<CustomerAddressDto> addresses,
        List<NomineeDto> nominees,
        Instant createdAt
) {
    public static CustomerProfileResponseDto fromEntity(
            Customer customer,
            List<CustomerAddressDto> addresses,
            List<NomineeDto> nominees
    ) {
        return new CustomerProfileResponseDto(
                customer.getCustomerId(),
                customer.getUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getKycStatus(),
                customer.getRiskCategory(),
                addresses,
                nominees,
                customer.getCreatedAt()
        );
    }
}
